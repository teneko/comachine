package de.halfbit.comachine.tests

import de.halfbit.comachine.MutableComachine
import de.halfbit.comachine.launchIn
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.runBlockingTest
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class DelegateOnAnyCanBeRegisteredTest {

    data class State(
        val position: Int = 0,
        val playing: Boolean = false,
        val saved: Boolean = false,
    )

    sealed interface Event {
        object Play : Event
        object Seek : Event
    }

    @Test
    fun test() {

        val machine = MutableComachine<State, Event>(startWith = State())

        machine.registerDelegate {
            whenIn<State> {
                onEnter {
                    launchInState {
                        state.update {
                            copy(saved = true)
                        }
                    }
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                on<Event.Play> {
                    state.update { copy(playing = true) }
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                on<Event.Seek> {
                    state.update { copy(position = state.position + 1) }
                }
            }
        }

        runBlockingTest {
            val states = mutableListOf<State>()
            launch {
                machine.state.collect {
                    states += it
                    println("$it")
                }
            }

            machine.launchIn(this)
            machine.await<State> { saved }

            machine.send(Event.Seek)
            machine.await<State> { position == 1 }

            machine.send(Event.Play)
            machine.await<State> { playing }

            delay(1.seconds)
            coroutineContext.cancelChildren()

            assertEquals(
                expected = listOf(
                    State(position = 0, playing = false, saved = false),
                    State(position = 0, playing = false, saved = true),
                    State(position = 1, playing = false, saved = true),
                    State(position = 1, playing = true, saved = true),
                ),
                actual = states
            )
        }
    }
}