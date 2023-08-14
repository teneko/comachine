package de.halfbit.comachine.dsl

import de.halfbit.comachine.MutableComachine
import de.halfbit.comachine.runtime.ComachineRuntime
import de.halfbit.comachine.runtime.StateTransitionException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.reflect.KClass

@DslMarker
annotation class ComachineDsl

internal typealias WhenInMap<SuperState, State> = MutableMap<KClass<out State>, WhenIn<SuperState, State>>

@ComachineDsl
class ComachineBlock<State : Any, Event : Any>
internal constructor(
    @PublishedApi internal val initialState: State,
    @PublishedApi internal val whenInMap: WhenInMap<State, out State> = mutableMapOf(),
) {
    inline fun <reified SubState : State> whenIn(
        block: WhenInBlock<State, SubState, Event>.() -> Unit
    ) {
        whenInMap[SubState::class] =
            WhenIn<State, SubState>(SubState::class)
                .apply { block(WhenInBlock(this)) }
    }

    internal fun buildComachine(stateTransitionAllowlist: StateTransitionAllowlist<State>?): MutableComachine<State, Event> =
        MutableComachineImpl(
            initialState,
            whenInMap,
            stateTransitionAllowlist
        )

    private class MutableComachineImpl<State : Any, Event : Any>(
        private val initialState: State,
        private val whenInMap: WhenInMap<State, out State>,
        private val stateTransitionAllowlist: StateTransitionAllowlist<State>?,
        stateExtraBufferCapacity: Int = 16,
    ) : MutableComachine<State, Event> {
        init {
            fun validateWhenInMap() {
                if (stateTransitionAllowlist == null) {
                    return
                }

                if (whenInMap.isEmpty()) {
                    return
                }

                if (stateTransitionAllowlist.isEmpty()) {
                    throw StateTransitionException(
                        "The usage of ${
                            whenInMap.map { "whenIn<${it.key.simpleName}>" }.joinToString()
                        } is not allowed, because the state transition allowlist is empty"
                    )
                }

                val invalidWhenIns = whenInMap.filter {
                    !stateTransitionAllowlist.containsKey(it.key)
                }

                if (invalidWhenIns.isEmpty()) {
                    return
                }

                invalidWhenIns.map {
                    "whenIn<${it.key.simpleName}>"
                }.also {
                    throw StateTransitionException(
                        "The usage of ${it.joinToString()} is not allowed," +
                                " because the state transition allowlist does not contain them"
                    )
                }
            }

            validateWhenInMap()
        }

        private var comachineRuntime: ComachineRuntime<State, Event>? = null

        private val stateFlow = MutableSharedFlow<State>(
            replay = 1,
            extraBufferCapacity = stateExtraBufferCapacity,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

        override val state: Flow<State> get() = stateFlow

        override fun registerDelegate(block: ComachineDelegateBlock<State, Event>.() -> Unit) {
            check(comachineRuntime == null) {
                "Register can only be called when event loop() is not stated"
            }
            ComachineDelegateBlock<State, Event>(whenInMap).also(block)
        }

        override suspend fun send(event: Event) {
            checkNotNull(comachineRuntime) {
                "Start event loop by called `launch { store.loop() }` first"
            }.send(event)
        }

        override suspend fun loop(whenStarted: (() -> Unit)?) {
            check(comachineRuntime == null) { "Event loop is already started" }
            coroutineScope {
                val machineScope = CoroutineScope(Job(coroutineContext[Job]))
                val machineRuntime = ComachineRuntime<State, Event>(
                    initialState = initialState,
                    machineScope = machineScope,
                    stateFlow = stateFlow,
                    whenInMap = whenInMap,
                    stateTransitionAllowlist
                )
                comachineRuntime = machineRuntime
                try {
                    machineRuntime.loop(whenStarted)
                } finally {
                    comachineRuntime = null
                }
            }
        }
    }
}
