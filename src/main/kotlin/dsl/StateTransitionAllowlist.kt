package de.halfbit.comachine.dsl

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KClass

typealias StateType<State> = KClass<out State>

typealias MutableStateTransitionToAllowlist<State> = MutableSet<StateType<State>>
typealias StateTransitionToAllowlist<State> = Set<StateType<State>>

typealias MutableStateTransitionAllowlist<State> = MutableMap<StateType<State>, StateTransitionToAllowlist<State>?>
typealias StateTransitionAllowlist<State> = Map<StateType<State>, StateTransitionToAllowlist<State>?>

fun <State : Any> MutableStateTransitionToAllowlist<State>.addAll(vararg toStateAllowlist: StateType<State>) {
    addAll(toStateAllowlist)
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun <State : Any> MutableStateTransitionAllowlist<State>.put(
    fromState: StateType<State>,
    @BuilderInference toStateAllowlistBuilder: MutableStateTransitionToAllowlist<State>.() -> Unit
) {
    contract { callsInPlace(toStateAllowlistBuilder, InvocationKind.EXACTLY_ONCE) }
    val toStateAllowlist = buildSet(toStateAllowlistBuilder)
    put(fromState, toStateAllowlist)
}

fun <State : Any> MutableStateTransitionAllowlist<State>.put(
    fromState: StateType<State>
) {
    put(fromState, setOf())
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun <State : Any> buildStateTransitionAllowlist(
    @BuilderInference builderAction: MutableStateTransitionAllowlist<State>.() -> Unit
): StateTransitionAllowlist<State> {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return buildMap(builderAction)
}