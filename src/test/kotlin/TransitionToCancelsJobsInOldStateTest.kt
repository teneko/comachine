package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.runBlockingTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertTrue

class TransitionToCancelsJobsInOldStateTest {

    sealed interface State {
        object Loading : State
        object Ready : State
    }

    object Event

    @Test
    fun test() {

        var job: Job? = null
        val loadingStarted = CompletableDeferred<Unit>()

        val machine = comachine<State, Event>(
            startWith = State.Loading
        ) {
            whenIn<State.Loading> {
                onEnter {
                    job = launchInState {
                        loadingStarted.complete(Unit)
                        delay(10000)
                    }
                }
                onSequential<Event> {
                    transitionTo { State.Ready }
                }
            }
        }

        runBlockingTest {

            machine.startInScope(this)
            loadingStarted.await()

            machine.send(Event)
            machine.await<State.Ready>()

            coroutineContext.cancelChildren()

            assertTrue(job?.isActive == false)
        }
    }
}