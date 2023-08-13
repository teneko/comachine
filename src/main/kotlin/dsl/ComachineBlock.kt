package de.halfbit.comachine.dsl

import de.halfbit.comachine.MutableComachine
import de.halfbit.comachine.runtime.ComachineRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.reflect.KClass

@DslMarker
annotation class ComachineDsl

internal typealias WhenInsMap<SuperState, State> =
    MutableMap<KClass<out State>, WhenIn<SuperState, State>>

@ComachineDsl
class ComachineBlock<State : Any, Event : Any>
internal constructor(
    @PublishedApi internal val initialState: State,
    @PublishedApi internal val whenInMap: WhenInsMap<State, out State> = mutableMapOf(),
) {
    inline fun <reified SubState : State> whenIn(
        block: WhenInBlock<State, SubState, Event>.() -> Unit
    ) {
        whenInMap[SubState::class] =
            WhenIn<State, SubState>(SubState::class)
                .also { block(WhenInBlock(it)) }
    }

    fun build(): MutableComachine<State, Event> =
        MutableComachineImpl(
            initialState = initialState,
            whenInMap = whenInMap,
        )

    private class MutableComachineImpl<State : Any, Event : Any>(
        private val initialState: State,
        private val whenInMap: WhenInsMap<State, out State>,
        stateExtraBufferCapacity: Int = 16,
    ) : MutableComachine<State, Event> {
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
