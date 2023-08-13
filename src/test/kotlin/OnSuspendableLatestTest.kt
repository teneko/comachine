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

class OnSuspendableLatestTest {

    data class State(val done: Boolean = false)
    data class Event(val index: Int)

    @Test
    fun newEventsReplaceTheCurrentOneIfItWasInProgress() {

        val events = mutableListOf<Event>()
        var firstEventJob: Job? = null
        val allEventsSent = CompletableDeferred<Unit>()

        val machine = Comachine<State, Event>(initialState = State()) {
            whenIn<State> {
                onLatest<Event> { event ->
                    events.add(event)
                    coroutineScope {
                        if (event.index == 0) {
                            firstEventJob = launch { delay(2000) }
                        }
                        withTimeout(2000) {
                            allEventsSent.await()
                        }
                        events.add(event)
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

            coroutineContext.cancelChildren()

            assertTrue(
                actual = events.size >= 3,
                message = "The list can have less events, because some events can be" +
                    " cancelled even before they are scheduled for launch. Nonetheless," +
                    " the very first and the very last event (twice, because the last event" +
                    " is processed completely) are expected."
            )
            assertEquals(events[0], Event(index = 0))
            assertEquals(events[events.lastIndex - 1], Event(index = 9))
            assertEquals(events[events.lastIndex], Event(index = 9))

            assertTrue(
                actual = firstEventJob?.isCancelled == true,
                message = "Expect first event's job to be cancelled"
            )
        }
    }
}