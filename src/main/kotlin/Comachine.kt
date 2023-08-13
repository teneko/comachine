package de.halfbit.comachine

import de.halfbit.comachine.dsl.ComachineBlock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun <State : Any, Event : Any> Comachine(
    startWith: State,
    block: ComachineBlock<State, Event>.() -> Unit
): Comachine<State, Event> =
    ComachineBlock<State, Event>(startWith).also(block).build()

/**
 * Launches the machine in the given scope and suspends until the initial state is
 * emitted. The machine is fully prepared and usable after this method exists.
 *
 * @return the coroutine job, in which the machine processes the events.
 */
suspend fun Comachine<*, *>.launchIn(scope: CoroutineScope): Job {
    val whenStarted = CompletableDeferred<Unit>()
    val job = scope.launch { loop { whenStarted.complete(Unit) } }
    whenStarted.await()
    return job
}

interface Comachine<State : Any, Event : Any> {
    val state: Flow<State>
    suspend fun send(event: Event)
    suspend fun loop(whenStarted: (() -> Unit)? = null)
}
