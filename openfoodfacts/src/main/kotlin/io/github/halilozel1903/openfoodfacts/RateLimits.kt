package io.github.halilozel1903.openfoodfacts

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** The two kinds of request Open Food Facts limits separately. */
public enum class RequestKind(public val label: String) {
    /** `GET /api/v2/product/{barcode}`: 100 requests per minute. */
    ProductRead("product reads"),

    /** `GET /api/v2/search` and `/cgi/search.pl`: 10 requests per minute. */
    Search("searches"),
}

/** What the client does when a request would go over a limit. */
public enum class RateLimitBehavior {
    /** Suspend until the request fits in the limit, then send it. */
    Wait,

    /** Throw [OpenFoodFactsException.RateLimited] without sending the request. */
    Fail,
}

/**
 * The request budgets the client keeps to. The defaults are the limits Open Food Facts publishes: 100 product reads
 * and 10 searches per minute per user. Responses served from the client's cache do not count.
 */
public data class RateLimits(
    val productReadsPerMinute: Int? = 100,
    val searchesPerMinute: Int? = 10,
    val whenExceeded: RateLimitBehavior = RateLimitBehavior.Wait,
) {
    init {
        require(productReadsPerMinute == null || productReadsPerMinute > 0) { "productReadsPerMinute must be positive" }
        require(searchesPerMinute == null || searchesPerMinute > 0) { "searchesPerMinute must be positive" }
    }

    public companion object {
        /** The published limits, waiting when a budget is used up. */
        public val Default: RateLimits = RateLimits()

        /** No client side limiting, for a self-hosted instance. The server's `429` still raises [OpenFoodFactsException.RateLimited]. */
        public val None: RateLimits = RateLimits(productReadsPerMinute = null, searchesPerMinute = null)
    }
}

/**
 * A sliding window limiter: at most [permits] acquisitions in any [window]. [acquire] suspends until a permit is
 * free; [tryAcquire] returns how long to wait instead. Safe to share between coroutines.
 */
public class RateLimiter(
    public val permits: Int,
    public val window: Duration = 1.minutes,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    init {
        require(permits > 0) { "permits must be positive" }
        require(window.isPositive()) { "window must be positive" }
    }

    private val mutex = Mutex()
    private val granted = ArrayDeque<TimeMark>()

    /** Suspends until a permit is free, then takes it. */
    public suspend fun acquire() {
        while (true) {
            val wait = tryAcquire() ?: return
            delay(wait)
        }
    }

    /** Takes a permit and returns `null`, or returns how long until one is free without taking it. */
    public suspend fun tryAcquire(): Duration? = mutex.withLock {
        prune()
        if (granted.size < permits) {
            granted.addLast(timeSource.markNow())
            null
        } else {
            (window - granted.first().elapsedNow()).coerceAtLeast(Duration.ZERO)
        }
    }

    /** Permits free right now. */
    public suspend fun available(): Int = mutex.withLock {
        prune()
        permits - granted.size
    }

    private fun prune() {
        while (granted.isNotEmpty() && granted.first().elapsedNow() >= window) granted.removeFirst()
    }
}
