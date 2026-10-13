package io.github.halilozel1903.openfoodfacts

import io.github.halilozel1903.openfoodfacts.model.NutriScore
import io.github.halilozel1903.openfoodfacts.model.GreenScore
import io.github.halilozel1903.openfoodfacts.transport.TransportResponse
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ClientTest {
    private fun ok(name: String) = TransportResponse(200, Fixtures.read(name))

    @Test
    fun productRequestWithFieldsAndUserAgent() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v2/product/3017620422003", request.url.encodedPath)
            assertEquals("FoodScanner/2.1 (dev@example.com) openfoodfacts-kotlin/1.0.0", request.headers[HttpHeaders.UserAgent])
            assertEquals("code,product_name", request.url.parameters["fields"])
            assertEquals("en", request.url.parameters["lc"])
            respond(Fixtures.read("product-3017620422003.json"), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        OpenFoodFactsClient(UserAgent("FoodScanner", "2.1", "dev@example.com")) {
            this.engine = engine
            language = "en"
        }.use { client ->
            val product = client.product("3017 6204 22003", fields = listOf("code", "product_name"))
            assertEquals("3017620422003", product.code)
            assertEquals(NutriScore.E, product.nutriScore)
        }
    }

    @Test
    fun notFoundFromStatusZeroAnd404() = runTest {
        val status0 = OpenFoodFactsClient(testAgent) { transport = RecordingTransport { ok("product-not-found.json") } }
        assertFailsWith<OpenFoodFactsException.NotFound> { status0.product("0000000000000") }
        val http404 = OpenFoodFactsClient(testAgent) {
            transport = RecordingTransport { TransportResponse(404, Fixtures.read("product-not-found.json")) }
        }
        val e = assertFailsWith<OpenFoodFactsException.NotFound> { http404.product("0000000000000") }
        assertEquals("0000000000000", e.barcode)
        assertNull(http404.productOrNull("0000000000000"))
        assertFailsWith<OpenFoodFactsException.InvalidBarcode> { http404.product("nutella") }
    }

    @Test
    fun errorsAreTyped() = runTest {
        val server = OpenFoodFactsClient(testAgent) { transport = RecordingTransport { TransportResponse(503, "busy") } }
        assertEquals(503, assertFailsWith<OpenFoodFactsException.Http> { server.product("1") }.status)

        val broken = OpenFoodFactsClient(testAgent) { transport = RecordingTransport { TransportResponse(200, "<html>") } }
        assertFailsWith<OpenFoodFactsException.Decoding> { broken.product("1") }

        val offline = OpenFoodFactsClient(testAgent) { transport = RecordingTransport { throw java.io.IOException("offline") } }
        assertFailsWith<OpenFoodFactsException.Network> { offline.product("1") }

        val limited = OpenFoodFactsClient(testAgent) {
            transport = RecordingTransport { TransportResponse(429, "", mapOf("retry-after" to "30")) }
        }
        val e = assertFailsWith<OpenFoodFactsException.RateLimited> { limited.search(SearchQuery()) }
        assertEquals(RequestKind.Search, e.kind)
        assertEquals(30.seconds, e.retryAfter)
    }

    @Test
    fun searchParameters() = runTest {
        val transport = RecordingTransport { ok("search-breakfast-cereals.json") }
        val client = OpenFoodFactsClient(testAgent) { this.transport = transport; country = "fr" }
        val page = client.search(
            SearchQuery(
                categories = listOf("breakfast-cereals", "fr:cereales-au-chocolat"),
                nutritionGrades = setOf(NutriScore.B, NutriScore.A),
                sortBy = SearchSort.NutriScore,
                pageSize = 2,
                fields = listOf("code", "product_name"),
            ),
        )
        assertEquals(
            "https://world.openfoodfacts.org/api/v2/search?categories_tags=en%3Abreakfast-cereals%2Cfr%3Acereales-au-chocolat" +
                "&nutrition_grades_tags=a%7Cb&sort_by=nutriscore_score&page=1&page_size=2&fields=code%2Cproduct_name&cc=fr",
            transport.urls.single(),
        )
        assertEquals(53, page.count)
        assertEquals(27, page.pageCount)
        assertTrue(page.hasNextPage)
        assertEquals(NutriScore.A, page.products[0].nutriScore)
        assertEquals(4, page.products[0].novaGroup)
        assertEquals("Oat flakes", page.products[1].displayName)
        assertEquals(NutriScore.Unknown, page.products[1].nutriScore)
        assertEquals(GreenScore.APlus, page.products[1].greenScore)
        assertEquals(emptyList(), page.products[1].brandList)
    }

    @Test
    fun searchTextUsesTheCgiEndpoint() = runTest {
        val transport = RecordingTransport { ok("search-text-page.json") }
        val client = OpenFoodFactsClient(testAgent) { this.transport = transport }
        val page = client.searchText("coca cola")
        assertTrue(transport.urls.single().startsWith("https://world.openfoodfacts.org/cgi/search.pl?search_terms=coca%20cola&search_simple=1&action=process&json=1"))
        assertEquals(3, page.count)
        assertEquals(24, page.pageSize)
        assertEquals("Coca-Cola", page.products.single().displayName)
    }

    @Test
    fun cacheSkipsTheNetworkAndTheLimit() = runTest {
        val transport = RecordingTransport { ok("product-3017620422003.json") }
        val client = OpenFoodFactsClient(testAgent) {
            this.transport = transport
            timeSource = testScheduler.timeSource
        }
        repeat(5) { client.product("3017620422003") }
        assertEquals(1, transport.urls.size)
        assertEquals(99, client.remainingRequests(RequestKind.ProductRead))
        client.clearCache()
        client.product("3017620422003")
        assertEquals(2, transport.urls.size)
    }

    @Test
    fun searchLimitWaits() = runTest {
        val transport = RecordingTransport { ok("search-breakfast-cereals.json") }
        val client = OpenFoodFactsClient(testAgent) {
            this.transport = transport
            cacheSize = 0
            timeSource = testScheduler.timeSource
        }
        repeat(10) { client.search(SearchQuery(page = it + 1)) }
        assertEquals(0L, currentTime)
        client.search(SearchQuery(page = 11))
        assertEquals(60_000L, currentTime)
        assertEquals(11, transport.urls.size)
    }

    @Test
    fun searchLimitCanFailFast() = runTest {
        val client = OpenFoodFactsClient(testAgent) {
            transport = RecordingTransport { ok("search-breakfast-cereals.json") }
            cacheSize = 0
            rateLimits = RateLimits(searchesPerMinute = 1, whenExceeded = RateLimitBehavior.Fail)
            timeSource = testScheduler.timeSource
        }
        client.search(SearchQuery())
        val e = assertFailsWith<OpenFoodFactsException.RateLimited> { client.search(SearchQuery(page = 2)) }
        assertEquals(60.seconds, e.retryAfter)
    }

    @Test
    fun searchPagesFollowsUntilTheLastPage() = runTest {
        val transport = RecordingTransport { url ->
            val page = Regex("page=(\\d+)").find(url)!!.groupValues[1]
            TransportResponse(200, """{"count": 5, "page": $page, "page_size": 2, "products": [{"code": "$page"}]}""")
        }
        val client = OpenFoodFactsClient(testAgent) { this.transport = transport; rateLimits = RateLimits.None }
        val pages = client.searchPages(SearchQuery(pageSize = 2)).toList()
        assertEquals(listOf(1, 2, 3), pages.map { it.page })
        assertEquals(2, client.searchPages(SearchQuery(pageSize = 2), maxPages = 2).toList().size)
    }

    @Test
    fun userAgentRules() {
        assertEquals("My-Food-App/1.0 (a@b.c) openfoodfacts-kotlin/1.0.0", UserAgent("My Food App", "1.0", "a@b.c").value)
        assertFailsWith<IllegalArgumentException> { UserAgent("App", "1.0", " ") }
        assertFailsWith<IllegalArgumentException> { UserAgent.custom("ktor-client") }
        assertEquals("Shelf/3 (https://shelf.example)", UserAgent.custom("Shelf/3 (https://shelf.example)").value)
    }
}
