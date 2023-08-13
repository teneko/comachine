package de.halfbit.comachine.tests

import de.halfbit.comachine.MutableComachine
import de.halfbit.comachine.launchIn
import de.halfbit.comachine.tests.utils.runBlockingTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ExceptionCatchingTests {
    private class DuringOnEnterException : Throwable()

    @Test
    fun `machine should propagate exception from onEnter`() {
        val comachine = MutableComachine<Unit, Unit>(Unit) {
            whenIn<Unit> {
                onEnter {
                    throw DuringOnEnterException()
                }
            }
        }

        assertThrows<DuringOnEnterException> {
            runBlockingTest {
                comachine.launchIn(this)
            }
        }
    }
}