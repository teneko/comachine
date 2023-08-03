package de.halfbit.comachine

import de.halfbit.comachine.dsl.ComachineBlock
import de.halfbit.comachine.dsl.ComachineDelegateBlock

/**
 * Mutable comachine can be extended by feature specific logic encapsulated
 * in a delegate. This can be used to decompose complex state machine into
 * multiple mini state machines operating on the same state.
 *
 * For example, a complex screen where a user can perform multiple actions
 * can be split into multiple delegates responsible for a single action.
 *
 * New delegates can only be added to a not yet looped mutable comachine.
 */
interface MutableComachine<State : Any, Event : Any> : Comachine<State, Event> {
    fun registerDelegate(block: ComachineDelegateBlock<State, Event>.() -> Unit)
}

fun <State : Any, Event : Any> mutableComachine(
    startWith: State,
    block: ComachineBlock<State, Event>.() -> Unit = {}
): MutableComachine<State, Event> =
    ComachineBlock<State, Event>(startWith).also(block).build()
