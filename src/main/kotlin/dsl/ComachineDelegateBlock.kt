package de.halfbit.comachine.dsl

@ComachineDsl
class ComachineDelegateBlock<State : Any, Event : Any>
internal constructor(
    @PublishedApi internal val whenInMap: WhenInsMap<State, out State>
) {
    inline fun <reified SubState : State> whenIn(
        block: WhenInBlock<State, SubState, Event>.() -> Unit
    ) {
        whenInMap
            .getOrPut(SubState::class) { WhenIn(SubState::class) }
            .also { block(WhenInBlock(it as WhenIn<State, SubState>)) }
    }
}
