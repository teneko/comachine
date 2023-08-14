package de.halfbit.comachine.dsl

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KClass

typealias MutableStateTransitionToAllowlist<State> = MutableSet<KClass<out State>>
typealias StateTransitionToAllowlist<State> = Set<KClass<out State>>

typealias MutableStateTransitionAllowlist<State> = MutableMap<KClass<out State>, StateTransitionToAllowlist<State>?>
typealias StateTransitionAllowlist<State> = Map<KClass<out State>, StateTransitionToAllowlist<State>?>

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun <State : Any> MutableStateTransitionAllowlist<State>.put(
    fromState: KClass<State>,
    @BuilderInference toStateAllowlistBuilder: MutableStateTransitionToAllowlist<State>.() -> Unit
) {
    contract { callsInPlace(toStateAllowlistBuilder, InvocationKind.EXACTLY_ONCE) }
    val transitionToStateAllowlist = buildSet(toStateAllowlistBuilder)
    put(fromState, transitionToStateAllowlist)
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun <State : Any> buildStateTransitionAllowlist(
    @BuilderInference builderAction: MutableStateTransitionAllowlist<State>.() -> Unit
): StateTransitionAllowlist<State> {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return buildMap(builderAction)
}