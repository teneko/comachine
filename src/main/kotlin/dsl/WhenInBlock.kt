package de.halfbit.comachine.dsl

import kotlin.reflect.KClass

@ComachineDsl
class WhenInBlock<State : Any, SubState : State, Event : Any>
@PublishedApi
internal constructor(
    @PublishedApi internal val whenIn: WhenIn<State, SubState>
) {
    /** A new event waits for the current event to complete. Multiple new events are buffered. */
    inline fun <reified SubEvent : Event> onSequential(
        noinline block: suspend LaunchBlock<State, SubState>.(SubEvent) -> Unit
    ) = setSuspendableEvent(OnEvent.Suspendable(SubEvent::class, LaunchMode.Sequential, block))

    /** A new event is executed concurrently to already processed events. */
    inline fun <reified SubEvent : Event> onConcurrent(
        noinline block: suspend LaunchBlock<State, SubState>.(SubEvent) -> Unit
    ) = setSuspendableEvent(OnEvent.Suspendable(SubEvent::class, LaunchMode.Concurrent, block))

    /** A new event replaces the current event, if such. Current event is cancelled. */
    inline fun <reified SubEvent : Event> onLatest(
        noinline block: suspend LaunchBlock<State, SubState>.(SubEvent) -> Unit
    ) = setSuspendableEvent(OnEvent.Suspendable(SubEvent::class, LaunchMode.Latest, block))

    /** A new event is ignored if there is an event in processing. */
    inline fun <reified SubEvent : Event> onSingle(
        noinline block: suspend LaunchBlock<State, SubState>.(SubEvent) -> Unit
    ) = setSuspendableEvent(OnEvent.Suspendable(SubEvent::class, LaunchMode.Single, block))

    /** Register new non suspendable event handler. */
    inline fun <reified SubEvent : Event> on(
        main: Boolean = false,
        noinline block: OnEventBlock<State, SubState>.(SubEvent) -> Unit,
    ) = setOrChainNonSuspendableEvent(SubEvent::class, main, block)

    fun onEnter(
        main: Boolean = false,
        block: OnEventBlock<State, SubState>.() -> Unit,
    ) {
        val onEnter = whenIn.onEnter
        when {
            main && onEnter?.mainBlock == true -> throw MultipleMainHandlersException("onEnter")
            main || onEnter == null -> whenIn.onEnter = OnEnter(block, main, onEnter)
            else -> onEnter.last.next = OnEnter(block)
        }
    }

    fun onExit(
        main: Boolean = false,
        block: OnExitBlock<SubState>.() -> Unit,
    ) {
        val onExit = whenIn.onExit
        when {
            main && onExit?.mainBlock == true -> throw MultipleMainHandlersException("onExit")
            main || onExit == null -> whenIn.onExit = OnExit(block, main, onExit)
            else -> onExit.last.next = OnExit(block)
        }
    }

    @PublishedApi
    internal fun <SubEvent : Event> setOrChainNonSuspendableEvent(
        eventType: KClass<SubEvent>,
        main: Boolean,
        block: OnEventBlock<State, SubState>.(SubEvent) -> Unit
    ) {
        when (val onEvent = whenIn.onEvent[eventType]) {
            null, is OnEvent.NonSuspendable -> {
                val nonSuspendableOnEvent = onEvent as OnEvent.NonSuspendable<State, SubState, SubEvent>?

                when {
                    main && nonSuspendableOnEvent?.mainBlock == true ->
                        throw MultipleMainHandlersException("on<${eventType.simpleName}>")

                    main || nonSuspendableOnEvent == null ->
                        whenIn.onEvent[eventType] =
                            OnEvent.NonSuspendable(eventType, block, main, nonSuspendableOnEvent)

                    else ->
                        nonSuspendableOnEvent.last.next =
                            OnEvent.NonSuspendable(eventType, block)
                }
            }

            is OnEvent.Suspendable ->
                throw IllegalArgumentException(
                    "Suspendable event handler for $eventType is already declared" +
                            " in ${whenIn.stateType} with launch mode ${onEvent.launchMode}." +
                            " Mixing non-suspendable on<Event> with suspendable on-handlers" +
                            " is not supported. Use either multiple on<Event> handlers or a single" +
                            " suspendable handler."
                )
        }
    }

    @PublishedApi
    internal fun setSuspendableEvent(event: OnEvent<State, SubState, *>) {
        whenIn.onEvent.put(event.eventType, event)?.let {
            throw IllegalArgumentException(
                "Event handler for ${event.eventType} is already declared in ${whenIn.stateType}"
            )
        }
    }
}