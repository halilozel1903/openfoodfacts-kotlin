package io.github.halilozel1903.openfoodfacts

import io.github.halilozel1903.openfoodfacts.transport.OpenFoodFactsTransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/** Settings for [OpenFoodFactsClient], set in its builder block. */
public class OpenFoodFactsConfig internal constructor() {
    /**
     * The server. `https://world.openfoodfacts.org` by default; use `https://world.openfoodfacts.net` (the staging
     * server, basic auth `off:off`) while developing, or a country subdomain such as `https://fr.openfoodfacts.org`.
     */
    public var baseUrl: String = OpenFoodFactsClient.DEFAULT_BASE_URL

    /** Where image urls are built when the response has no ready url. */
    public var imagesBaseUrl: String = OpenFoodFactsClient.DEFAULT_IMAGES_BASE_URL

    /** The `lc` parameter: the language of product names, ingredients and tag names in responses. `null` lets the server pick. */
    public var language: String? = null

    /** The `cc` parameter: the country used for country specific data and search scope. `null` means world. */
    public var country: String? = null

    /** Client side request budgets. See [RateLimits]. */
    public var rateLimits: RateLimits = RateLimits.Default

    /** How many responses to keep in memory (`0` turns the cache off). Cached responses do not use the rate limit. */
    public var cacheSize: Int = 100

    /** How long a cached response is used. */
    public var cacheTtl: Duration = 10.minutes

    /**
     * The Ktor engine. `null` uses CIO. Pass `OkHttp.create()` (add `io.ktor:ktor-client-okhttp`) to share an
     * Android app's OkHttp stack, or a `MockEngine` in tests. The client does not close an engine you pass.
     */
    public var engine: HttpClientEngine? = null

    /** A ready Ktor client to use as is, instead of [engine]. [requestTimeoutMillis] and [retries] then do not apply. */
    public var httpClient: HttpClient? = null

    /** Replaces HTTP entirely, for tests or a custom stack. */
    public var transport: OpenFoodFactsTransport? = null

    /** Timeout of one request, in milliseconds. Search can be slow; keep this generous. */
    public var requestTimeoutMillis: Long = 30_000

    /** Retries on network errors and 5xx responses, with exponential backoff. Never on `429`. */
    public var retries: Int = 2

    /** The clock of the rate limiters and the cache. Tests pass `testScheduler.timeSource`. */
    public var timeSource: TimeSource = TimeSource.Monotonic
}
