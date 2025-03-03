package de.halfbit.comachine

import de.halfbit.comachine.dsl.ComachineBlock
import de.halfbit.comachine.dsl.ComachineDelegateBlock
import de.halfbit.comachine.dsl.StateTransitionAllowlist
import kotlin.reflect.KClass

fun <State : Any, Event : Any> MutableComachine(
    initialState: State,
    stateTransitionAllowlist:  StateTransitionAllowlist<State>? = null,
    block: ComachineBlock<State, Event>.() -> Unit = {},
): MutableComachine<State, Event> =
    ComachineBlock<State, Event>(initialState).also(block).buildComachine(stateTransitionAllowlist)

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