package io.github.halilozel1903.openfoodfacts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RateLimiterTest {
    @Test
    fun waitsForTheWindow() = runTest {
        val limiter = RateLimiter(permits = 3, window = 1.minutes, timeSource = testScheduler.timeSource)
        repeat(3) { limiter.acquire() }
        assertEquals(0L, currentTime)
        assertEquals(0, limiter.available())
        limiter.acquire()
        assertEquals(60_000L, currentTime)
    }

    @Test
    fun tryAcquireReportsTheWait() = runTest {
        val limiter = RateLimiter(permits = 2, window = 1.minutes, timeSource = testScheduler.timeSource)
        assertNull(limiter.tryAcquire())
        testScheduler.advanceTimeBy(20.seconds)
        assertNull(limiter.tryAcquire())
        assertEquals(40.seconds, limiter.tryAcquire())
        testScheduler.advanceTimeBy(40.seconds)
        assertEquals(1, limiter.available())
        assertNull(limiter.tryAcquire())
        assertNotNull(limiter.tryAcquire())
    }
}
