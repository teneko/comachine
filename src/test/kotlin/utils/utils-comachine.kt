package de.halfbit.comachine.tests.utils

import de.halfbit.comachine.Comachine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

suspend inline fun <reified State : Any> Comachine<*, *>.await(
    timeout: Duration = 1000.milliseconds
) = await<State>(timeout) { this::class == State::class }

suspend inline fun <reified State : Any> Comachine<*, *>.await(
    timeout: Duration = 1000.milliseconds,
    crossinline block: State.() -> Boolean
) {
    coroutineScope {
        try {
            withTimeout(timeout) {
                state.collect {
                    if (it is State && block(it)) {
                        cancel("state detected")
                    }
                }
            }
        } catch (error: TimeoutCancellationException) {
            // ignored
        } catch (error: CancellationException) {
            // ignored
        }
    }
}