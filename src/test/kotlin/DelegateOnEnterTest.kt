package de.halfbit.comachine.tests

import de.halfbit.comachine.MutableComachine
import de.halfbit.comachine.launchIn
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.runBlockingTest
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class DelegateOnEnterTest {

    data class State(
        val position: Int = 0,
        val playing: Boolean = false,
        val saved: Boolean = false,
    )

    @Test
    fun multipleSecondaryHandlersCanBeRegistered() {

        val machine = MutableComachine<State, Unit>(startWith = State())

        machine.registerDelegate {
            whenIn<State> {
                onEnter {
                    state.update { copy(position = 1) }
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                onEnter {
                    state.update { copy(playing = true) }
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                onEnter {
                    state.update { copy(saved = true) }
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
            machine.await<State> { position == 1 && saved && playing }

            coroutineContext.cancelChildren()

            assertEquals(
                expected = listOf(
                    State(position = 0, playing = false, saved = false),
                    State(position = 1, playing = false, saved = false),
                    State(position = 1, playing = true, saved = false),
                    State(position = 1, playing = true, saved = true),
                ),
                actual = states
            )
        }
    }

    @Test
    fun singleMainHandlerCanBeRegistered() {

        val machine = MutableComachine<State, Unit>(startWith = State())

        machine.registerDelegate {
            whenIn<State> {
                onEnter {
                    state.update { copy(position = 1) }
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                onEnter(main = true) {
                    state.update { copy(playing = true) }
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                onEnter {
                    state.update { copy(saved = true) }
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
            machine.await<State> { position == 1 && saved && playing }

            coroutineContext.cancelChildren()

            assertEquals(
                expected = listOf(
                    State(position = 0, playing = false, saved = false),
                    State(position = 0, playing = true, saved = false),
                    State(position = 1, playing = true, saved = false),
                    State(position = 1, playing = true, saved = true),
                ),
                actual = states
            )
        }
    }

    @Test
    fun multipleMainHandlersCannotBeRegistered() {

        val machine = MutableComachine<State, Unit>(startWith = State())

        machine.registerDelegate {
            whenIn<State> {
                onEnter {
                    state.update { copy(position = 1) }
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                onEnter(main = true) {
                    state.update { copy(playing = true) }
                }
            }
        }

        try {
            machine.registerDelegate {
                whenIn<State> {
                    onEnter(main = true) {
                        state.update { copy(saved = true) }
                    }
                }
            }
        } catch (err: IllegalArgumentException) {
            assertTrue(err.message?.contains("onEnter") == true)
            return
        }

        fail("IllegalArgumentException not thrown")
    }
}