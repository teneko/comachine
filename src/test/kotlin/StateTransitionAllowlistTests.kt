package de.halfbit.comachine.tests

import de.halfbit.comachine.MutableComachine
import de.halfbit.comachine.dsl.addAll
import de.halfbit.comachine.dsl.buildStateTransitionAllowlist
import de.halfbit.comachine.dsl.put
import de.halfbit.comachine.launchIn
import de.halfbit.comachine.runtime.StateTransitionException
import de.halfbit.comachine.tests.utils.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class StateTransitionAllowlistTests {
    private interface State {
        fun getSimpleName() = this::class.simpleName

        object One : State
        object Two : State
    }

    init {
        buildStateTransitionAllowlist {
            put(State.One::class) {
                addAll(
                    State.One::class,
                    State.Two::class
                )
            }
        }

        buildStateTransitionAllowlist {
            put(State.One::class)
        }
    }

    @Test
    fun `builder should build only-from-state transition allowlist`() {
        val stateTransitionAllowlist = buildStateTransitionAllowlist {
            put(Unit::class, null)
        }

        assertEquals(
            expected = buildMap {
                put(Unit::class, null)
            },
            actual = stateTransitionAllowlist
        )
    }

    @Test
    fun `builder should build from-to-state transition allowlist`() {
        val stateTransitionAllowlist = buildStateTransitionAllowlist {
            put(Unit::class) {
                add(Unit::class)
            }
        }

        assertEquals(
            expected = buildMap {
                put(Unit::class, setOf(Unit::class))
            },
            actual = stateTransitionAllowlist
        )
    }

    @Test
    fun `comachine should throw at runtime`() {
        val stateTransitionAllowlist = buildStateTransitionAllowlist<State> {
            put(State.One::class)
        }

        val comachine = MutableComachine<State, Unit>(State.One, stateTransitionAllowlist) {
            whenIn<State.One> {
                onEnter {
                    transitionTo { State.Two }
                }
            }
        }

        assertThrows<StateTransitionException> {
            runBlockingTest {
                comachine.launchIn(this)
            }
        }.also {
            assertThat(it).hasMessageContaining(
                "transition from ${State.One.getSimpleName()} to ${State.Two.getSimpleName()} is not allowed"
            )
        }
    }

    @Test
    fun `comachine should throw at build time with empty allowlist`() {
        val stateTransitionAllowlist = buildStateTransitionAllowlist<Unit> { }

        assertThrows<StateTransitionException> {
            MutableComachine<Unit, Unit>(Unit, stateTransitionAllowlist) {
                whenIn<Unit> { }
            }
        }.also {
            assertThat(it)
                .hasMessageContaining("<${Unit::class.simpleName}>")
                .hasMessageContaining("allowlist is empty")
        }
    }

    @Test
    fun `comachine should throw at build time with non-empty allowlist`() {
        val stateTransitionAllowlist = buildStateTransitionAllowlist<State> {
            put(State.One::class, null)
        }

        assertThrows<StateTransitionException> {
            MutableComachine<State, Unit>(State.One, stateTransitionAllowlist) {
                whenIn<State.Two> { }
            }
        }.also {
            assertThat(it)
                .hasMessageContaining("<${State.Two.getSimpleName()}>")
                .hasMessageContaining("allowlist does not contain")
        }
    }
}