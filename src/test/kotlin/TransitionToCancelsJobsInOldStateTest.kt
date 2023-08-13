package de.halfbit.comachine.tests

import de.halfbit.comachine.Comachine
import de.halfbit.comachine.launchIn
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

        val machine = Comachine<State, Event>(
            initialState = State.Loading
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

            machine.launchIn(this)
            loadingStarted.await()

            machine.send(Event)
            machine.await<State.Ready>()

            val isJobInactive =job?.isActive == false
            coroutineContext.cancelChildren()

            assertTrue(isJobInactive)
        }
    }
}