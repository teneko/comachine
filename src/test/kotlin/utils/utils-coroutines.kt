package de.halfbit.comachine.tests.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

fun runBlockingTest(block: suspend CoroutineScope.() -> Unit) {
    runBlocking {
        withTimeout(3.seconds) {
            block()
        }
    }
}
