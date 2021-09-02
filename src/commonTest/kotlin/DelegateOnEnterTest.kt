package de.halfbit.comachine.tests

import de.halfbit.comachine.mutableComachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.flow.collect
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

        val machine = mutableComachine<State, Unit>(startWith = State())

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

        executeBlockingTest {
            val states = mutableListOf<State>()
            launch {
                machine.state.collect {
                    states += it
                    println("$it")
                }
            }

            machine.startInScope(this)
            machine.await<State> { position == 1 && saved && playing }

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

        val machine = mutableComachine<State, Unit>(startWith = State())

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

        executeBlockingTest {
            val states = mutableListOf<State>()
            launch {
                machine.state.collect {
                    states += it
                    println("$it")
                }
            }

            machine.startInScope(this)
            machine.await<State> { position == 1 && saved && playing }

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

        val machine = mutableComachine<State, Unit>(startWith = State())

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