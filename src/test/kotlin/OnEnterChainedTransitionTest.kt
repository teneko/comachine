package de.halfbit.comachine.tests

import de.halfbit.comachine.Comachine
import de.halfbit.comachine.launchIn
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.runBlockingTest
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals

class OnEnterChainedTransitionTest {

    sealed interface State {
        object Zero : State
        object One : State
        object Two : State
        object Three : State
        object Four : State
        object Five : State
        object Six : State
        object Seven : State
        object Eight : State
        object Nine : State
        object Ten : State
    }

    @Test
    fun test() {

        val machine = Comachine<State, Unit>(
            initialState = State.Zero
        ) {
            whenIn<State.Zero> { onEnter { transitionTo { State.One } } }
            whenIn<State.One> { onEnter { transitionTo { State.Two } } }
            whenIn<State.Two> { onEnter { transitionTo { State.Three } } }
            whenIn<State.Three> { onEnter { transitionTo { State.Four } } }
            whenIn<State.Four> { onEnter { transitionTo { State.Five } } }
            whenIn<State.Five> { onEnter { transitionTo { State.Six } } }
            whenIn<State.Six> { onEnter { transitionTo { State.Seven } } }
            whenIn<State.Seven> { onEnter { transitionTo { State.Eight } } }
            whenIn<State.Eight> { onEnter { transitionTo { State.Nine } } }
            whenIn<State.Nine> { onEnter { transitionTo { State.Ten } } }
            whenIn<State.Ten> { }
        }

        runBlockingTest {
            val states = mutableListOf<State>()
            launch {
                machine.state.collect {
                    states += it
                }
            }

            machine.launchIn(this)
            machine.await<State.Ten>()

            coroutineContext.cancelChildren()

            assertEquals(
                states,
                listOf(
                    State.Zero,
                    State.One,
                    State.Two,
                    State.Three,
                    State.Four,
                    State.Five,
                    State.Six,
                    State.Seven,
                    State.Eight,
                    State.Nine,
                    State.Ten,
                )
            )
        }
    }
}