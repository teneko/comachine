package de.halfbit.comachine.tests

import de.halfbit.comachine.Comachine
import de.halfbit.comachine.launchIn
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.runBlockingTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class OnSuspendableConcurrentTest {

    data class State(val counter: Int = 0)
    data class Event(val index: Int)

    @Test
    fun newEventsAreProcessedConcurrentlyToCurrentEvents() {

        val events = mutableListOf<Event>()
        val secondBucketSent = CompletableDeferred<Unit>()

        val machine = Comachine<State, Event>(initialState = State()) {
            whenIn<State> {
                onConcurrent<Event> { event ->
                    if (event.index < 5) {
                        withTimeout(1000) {
                            secondBucketSent.await()
                        }
                    }
                    events.add(event)
                    state.update { copy(counter = counter + event.index) }
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

            launch {
                repeat(5) {
                    machine.send(Event(index = it))
                }
            }

            launch {
                repeat(5) {
                    machine.send(Event(index = 5 + it))
                }
                secondBucketSent.complete(Unit)
            }

            machine.await<State> { counter == 45 }
            coroutineContext.cancelChildren()
            assertEquals(expected = events.size, actual = 10)
        }
    }
}