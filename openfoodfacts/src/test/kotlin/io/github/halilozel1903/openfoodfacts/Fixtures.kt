package io.github.halilozel1903.openfoodfacts

import io.github.halilozel1903.openfoodfacts.transport.OpenFoodFactsTransport
import io.github.halilozel1903.openfoodfacts.transport.TransportResponse

/**
 * Test responses shaped like the Open Food Facts API v2 (field names, nesting, and the API's mixed number and
 * string values), trimmed to what the tests read. They are fixtures for unit tests, not a copy of live data.
 */
object Fixtures {
    fun read(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/$name")) { "missing fixture $name" }.readText()
}

/** Records requests and answers with [respond]. */
class RecordingTransport(
    private val respond: (String) -> TransportResponse,
) : OpenFoodFactsTransport {
    val urls = mutableListOf<String>()
    val headers = mutableListOf<Map<String, String>>()

    override suspend fun get(url: String, headers: Map<String, String>): TransportResponse {
        urls += url
        this.headers += headers
        return respond(url)
    }
}

val testAgent = UserAgent("OffTests", "1.0", "tests@example.com")
