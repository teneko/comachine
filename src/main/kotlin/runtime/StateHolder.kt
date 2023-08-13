package de.halfbit.comachine.runtime

import kotlinx.coroutines.flow.MutableSharedFlow

@PublishedApi
internal class StateHolder<State : Any, SubState : State>(
    private var state: SubState,
    private val stateFlow: MutableSharedFlow<State>,
) {
    fun get(): SubState = state

    fun set(newState: SubState) {
        state = newState
        check(stateFlow.tryEmit(newState)) {
            reportError("StateFlow suspended although it never should.")
        }
    }
}