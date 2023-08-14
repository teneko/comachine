package de.halfbit.comachine.runtime

import kotlin.reflect.KClass

class StateTransitionException : RuntimeException {
    internal constructor(s: String) : super(s)

    internal constructor(fromState: KClass<*>) : super("The transition to ${fromState.simpleName} is not allowed")

    internal constructor(
        fromState: KClass<*>,
        toState: KClass<*>
    ) : super("The transition from ${fromState.simpleName} to ${toState.simpleName} is not allowed")
}