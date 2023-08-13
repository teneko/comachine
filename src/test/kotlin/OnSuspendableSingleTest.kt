package de.halfbit.comachine.tests

import de.halfbit.comachine.Comachine
import de.halfbit.comachine.launchIn
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.runBlockingTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnSuspendableSingleTest {

    data class State(val done: Boolean = false)
    data class Event(val index: Int)

    @Test
    fun newEventsAreIgnoredWhileCurrentIsStillInProgress() {

        val events = mutableListOf<Event>()
        var firstEventJob: Job? = null
        val allEventsSent = CompletableDeferred<Unit>()

        val machine = Comachine<State, Event>(startWith = State()) {
            whenIn<State> {
                onSingle<Event> { event ->
                    events.add(event)
                    coroutineScope {
                        if (event.index == 0) {
                            firstEventJob = launch { delay(1000) }
                        }
                        withTimeout(1000) {
                            allEventsSent.await()
                        }
                        state.update { copy(done = true) }
                    }
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
            repeat(10) {
                machine.send(Event(index = it))
            }
            allEventsSent.complete(Unit)
            machine.await<State> { done }

            val isFirstEventJobActive = firstEventJob?.isActive == true
            coroutineContext.cancelChildren()

            assertEquals(
                expected = listOf(Event(index = 0)),
                actual = events
            )

            assertTrue(
                actual = isFirstEventJobActive,
                message = "Expect first event's job to not be cancelled"
            )

        }
    }
}