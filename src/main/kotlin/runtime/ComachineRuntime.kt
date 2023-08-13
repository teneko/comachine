package de.halfbit.comachine.runtime

import de.halfbit.comachine.dsl.ComachineBlock
import de.halfbit.comachine.dsl.WhenIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlin.reflect.KClass

internal typealias EmitMessage = suspend (Message) -> Unit

internal sealed interface Message {
    data class OnStateInitialized(val state: Any) : Message
    data class OnEventReceived(val event: Any) : Message
    data class OnEventCompleted(val event: Any) : Message

    /**
     * Used by state transitions and state updates.
     */
    data class OnCallback(val callback: suspend () -> Unit) : Message
}

/**
 * The comachine used by [ComachineBlock] represents a finite state machine.
 * The state machine reacts on [Event] to mutate [State].
 */
internal class ComachineRuntime<State : Any, Event : Any>(
    private val initialState: State,
    private val machineScope: CoroutineScope,
    private val stateFlow: MutableSharedFlow<State>,
    private val whenInMap: MutableMap<KClass<out State>, WhenIn<State, out State>>,
) {
    private val messageFlow = MutableSharedFlow<Message>()
    private var whenInRuntime: WhenInRuntime<State, out State, Event>? = null
    private var unprocessedTransitionToState: State? = null

    suspend fun send(event: Event) {
        messageFlow.emit(Message.OnEventReceived(event))
    }

    suspend fun loop(whenStarted: (() -> Unit)?) {
        messageFlow
            .onSubscription {
                emit(Message.OnStateInitialized(initialState))
            }
            .collect {
                when (it) {
                    is Message.OnStateInitialized -> {
                        onEnterState(it.state as State)
                        whenStarted?.invoke()
                    }

                    is Message.OnEventReceived -> onEventReceived(it.event as Event)
                    is Message.OnEventCompleted -> onEventCompleted(it.event as Event)
                    is Message.OnCallback -> it.callback()
                }

                var state = unprocessedTransitionToState
                while (state != null) {
                    unprocessedTransitionToState = null
                    onEnterState(state)
                    state = unprocessedTransitionToState
                }
            }
    }

    private fun <SubState : State> createWhereInOrNull(
        state: SubState
    ): WhenInRuntime<State, SubState, Event>? = (whenInMap[state::class] as? WhenIn<State, SubState>)?.let {
        WhenInRuntime(
            stateHolder = StateHolder(state, stateFlow),
            whenIn = it,
            machineScope = machineScope,
            transitionToFct = ::transitionTo,
            emitMessage = messageFlow::emit,
        )
    }

    private fun onEventReceived(event: Event) {
        checkNotNull(whenInRuntime) {
            "WhenIn block is missing for $event in $initialState"
        }.onEventReceived(event)
    }

    private fun onEventCompleted(event: Event) {
        whenInRuntime?.onEventCompleted(event)
    }

    private fun transitionTo(state: State) {
        whenInRuntime?.onExit()
        check(unprocessedTransitionToState == null) {
            reportError("Pending entry state is already set.")
        }
        unprocessedTransitionToState = state
    }

    private fun onEnterState(state: State) {
        emitState(state)
        whenInRuntime = createWhereInOrNull(state)
        whenInRuntime?.onEnter()
    }

    private fun emitState(state: State) {
        check(stateFlow.tryEmit(state)) {
            reportError("StateFlow suspends although it never should.")
        }
    }
}

internal fun reportError(message: String): String =
    "$message Please report this bug to https://github.com/beworker/comachine/"
