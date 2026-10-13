package io.github.halilozel1903.openfoodfacts

import io.github.halilozel1903.openfoodfacts.internal.ResponseCache
import io.github.halilozel1903.openfoodfacts.internal.queryString
import io.github.halilozel1903.openfoodfacts.model.Product
import io.github.halilozel1903.openfoodfacts.model.RawProductResponse
import io.github.halilozel1903.openfoodfacts.model.RawSearchPage
import io.github.halilozel1903.openfoodfacts.model.SearchPage
import io.github.halilozel1903.openfoodfacts.transport.KtorTransport
import io.github.halilozel1903.openfoodfacts.transport.OpenFoodFactsTransport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * A coroutine client for the Open Food Facts API v2.
 *
 * ```kotlin
 * val off = OpenFoodFactsClient(UserAgent("FoodScanner", "1.0", "dev@example.com")) {
 *     language = "en"
 * }
 * val nutella = off.product("3017620422003")
 * nutella.nutriScore                  // NutriScore.E
 * nutella.nutriments.per100g.sugars   // grams of sugar per 100 g
 * ```
 *
 * [userAgent] is required: Open Food Facts asks every app to identify itself. The client keeps to the published rate
 * limits (100 product reads and 10 searches per minute) by waiting, or by failing fast with
 * [OpenFoodFactsException.RateLimited] if you choose [RateLimitBehavior.Fail]. Responses are cached in memory for
 * [OpenFoodFactsConfig.cacheTtl], and cached reads do not count against the limits.
 *
 * Create one client and share it; it is safe to use from many coroutines. Every call is main-safe.
 */
public class OpenFoodFactsClient(
    public val userAgent: UserAgent,
    configure: OpenFoodFactsConfig.() -> Unit = {},
) : AutoCloseable {

    private val config = OpenFoodFactsConfig().apply(configure)
    private val baseUrl = config.baseUrl.trimEnd('/')
    private val transport: OpenFoodFactsTransport =
        config.transport ?: KtorTransport(config.httpClient, config.engine, config.requestTimeoutMillis, config.retries)
    private val cache = if (config.cacheSize > 0) ResponseCache(config.cacheSize, config.cacheTtl, config.timeSource) else null
    private val limiters: Map<RequestKind, RateLimiter> = buildMap<RequestKind, RateLimiter> {
        config.rateLimits.productReadsPerMinute?.let { put(RequestKind.ProductRead, RateLimiter(it, 1.minutes, config.timeSource)) }
        config.rateLimits.searchesPerMinute?.let { put(RequestKind.Search, RateLimiter(it, 1.minutes, config.timeSource)) }
    }
    private val headers = mapOf("User-Agent" to userAgent.value, "Accept" to "application/json")

    /** The images server, for building urls with [io.github.halilozel1903.openfoodfacts.images.ProductImages]. */
    public val imagesBaseUrl: String get() = config.imagesBaseUrl

    /**
     * The product with [barcode] (`GET /api/v2/product/{barcode}`), with [fields] only (`null` for all of them).
     * Spaces and dashes in the barcode are ignored.
     *
     * @throws OpenFoodFactsException.NotFound when no product has this barcode.
     * @throws OpenFoodFactsException.InvalidBarcode when [barcode] is not made of digits.
     */
    public suspend fun product(barcode: String, fields: List<String>? = ProductFields.Detail): Product {
        val code = Barcode.normalize(barcode) ?: throw OpenFoodFactsException.InvalidBarcode(barcode)
        val url = "$baseUrl/api/v2/product/$code" + query(listOf("fields" to fields?.joinToString(",")))
        val body = try {
            get(url, RequestKind.ProductRead)
        } catch (e: OpenFoodFactsException.Http) {
            if (e.status == 404) throw OpenFoodFactsException.NotFound(code) else throw e
        }
        val response = decode(url, body, RawProductResponse.serializer())
        val product = response.product
        if (response.status == 0 || product == null) throw OpenFoodFactsException.NotFound(code)
        return if (product.code.isEmpty()) product.copy(code = response.code ?: code) else product
    }

    /** Like [product], but `null` when there is no such product. */
    public suspend fun productOrNull(barcode: String, fields: List<String>? = ProductFields.Detail): Product? = try {
        product(barcode, fields)
    } catch (e: OpenFoodFactsException.NotFound) {
        null
    }

    /** One page of a structured search (`GET /api/v2/search`). Counts against the search limit. */
    public suspend fun search(query: SearchQuery): SearchPage {
        val url = "$baseUrl/api/v2/search" + query(query.parameters())
        return decode(url, get(url, RequestKind.Search), RawSearchPage.serializer()).toPage(query.page, query.pageSize)
    }

    /**
     * Pages of [query], from its page on, fetched as you collect, until the last page or [maxPages]. Each page is one
     * search request, so the client's search limit (10 per minute) paces long collections.
     */
    public fun searchPages(query: SearchQuery, maxPages: Int = Int.MAX_VALUE): Flow<SearchPage> = flow {
        var next: SearchQuery? = query
        var fetched = 0
        while (next != null && fetched < maxPages) {
            val page = search(next)
            emit(page)
            fetched++
            next = if (page.hasNextPage && page.products.isNotEmpty()) next.next() else null
        }
    }

    /**
     * A full text search on product names, brands and more (`GET /cgi/search.pl?search_terms=`), which the v2 search
     * does not offer. Counts against the search limit.
     */
    public suspend fun searchText(
        terms: String,
        page: Int = 1,
        pageSize: Int = 24,
        fields: List<String>? = ProductFields.Summary,
    ): SearchPage {
        require(terms.isNotBlank()) { "terms may not be blank" }
        require(page >= 1 && pageSize in 1..100) { "page starts at 1, pageSize is 1 to 100" }
        val url = "$baseUrl/cgi/search.pl" + query(
            listOf(
                "search_terms" to terms.trim(),
                "search_simple" to "1",
                "action" to "process",
                "json" to "1",
                "page" to page.toString(),
                "page_size" to pageSize.toString(),
                "fields" to fields?.joinToString(","),
            ),
        )
        return decode(url, get(url, RequestKind.Search), RawSearchPage.serializer()).toPage(page, pageSize)
    }

    /** The product's page on the Open Food Facts website. */
    public fun productPageUrl(barcode: String): String = "$baseUrl/product/${Barcode.normalize(barcode) ?: barcode}"

    /** Requests of [kind] that can be sent right now without waiting, or `null` when [kind] is not limited. */
    public suspend fun remainingRequests(kind: RequestKind): Int? = limiters[kind]?.available()

    /** Forgets cached responses. */
    public suspend fun clearCache() {
        cache?.clear()
    }

    /** Closes the HTTP client this client created. An engine or client you passed in stays open. */
    override fun close() {
        (transport as? AutoCloseable)?.close()
    }

    private fun query(params: List<Pair<String, String?>>): String {
        val all = params + listOf("lc" to config.language, "cc" to config.country)
        val q = queryString(all)
        return if (q.isEmpty()) "" else "?$q"
    }

    private suspend fun get(url: String, kind: RequestKind): String {
        cache?.get(url)?.let { return it }
        limiters[kind]?.let { limiter ->
            when (config.rateLimits.whenExceeded) {
                RateLimitBehavior.Wait -> limiter.acquire()
                RateLimitBehavior.Fail -> limiter.tryAcquire()?.let { throw OpenFoodFactsException.RateLimited(kind, it) }
            }
        }
        val response = try {
            transport.get(url, headers)
        } catch (e: CancellationException) {
            throw e
        } catch (e: OpenFoodFactsException) {
            throw e
        } catch (e: Exception) {
            throw OpenFoodFactsException.Network(url, e)
        }
        when {
            response.status == 429 -> throw OpenFoodFactsException.RateLimited(kind, retryAfter(response.headers["retry-after"]))
            response.status !in 200..299 -> throw OpenFoodFactsException.Http(url, response.status)
        }
        cache?.put(url, response.body)
        return response.body
    }

    private fun retryAfter(header: String?): Duration? = header?.trim()?.toLongOrNull()?.takeIf { it >= 0 }?.seconds

    private fun <T> decode(url: String, body: String, serializer: KSerializer<T>): T = try {
        json.decodeFromString(serializer, body)
    } catch (e: Exception) {
        throw OpenFoodFactsException.Decoding(url, e)
    }

    private fun RawSearchPage.toPage(requestedPage: Int, requestedSize: Int) = SearchPage(
        count = count ?: products.size,
        page = page ?: requestedPage,
        pageSize = pageSize ?: requestedSize,
        products = products,
    )

    public companion object {
        public const val DEFAULT_BASE_URL: String = "https://world.openfoodfacts.org"
        public const val DEFAULT_IMAGES_BASE_URL: String = "https://images.openfoodfacts.org"

        /** The JSON settings the client decodes with: unknown fields are ignored. */
        public val json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        }
    }
}
