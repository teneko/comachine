package de.halfbit.comachine.tests

import de.halfbit.comachine.Comachine
import de.halfbit.comachine.launchIn
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.runBlockingTest
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlin.test.Test

class ExtrasCanBeSetAndGetInSameStateTest {

    data class State(val value: String = "initial")

    @Test
    fun test() {

        val machine = Comachine<State, Unit>(
            initialState = State()
        ) {
            whenIn<State> {
                onEnter {
                    setExtra("custom value")
                }
                on<Unit> {
                    val value = getExtra<String>()
                    state.update { copy(value = value) }
                }
            }
        }

        runBlockingTest {
            launch {
                machine.state.collect {
                    println("$it")
                }
            }

            machine.launchIn(this)
            machine.send(Unit)
            machine.await<State> { value == "custom value" }

            coroutineContext.cancelChildren()
        }
    }
}