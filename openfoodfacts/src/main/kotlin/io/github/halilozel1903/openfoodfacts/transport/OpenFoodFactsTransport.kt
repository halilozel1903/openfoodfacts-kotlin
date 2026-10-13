package io.github.halilozel1903.openfoodfacts.transport

/** One HTTP response, as far as the client needs it. Header names are lower case. */
public class TransportResponse(
    public val status: Int,
    public val body: String,
    public val headers: Map<String, String> = emptyMap(),
)

/**
 * Sends GET requests for [io.github.halilozel1903.openfoodfacts.OpenFoodFactsClient]. The default uses Ktor; replace
 * it to route requests through your own HTTP stack. Throw any exception for a failed request; the client reports it
 * as [io.github.halilozel1903.openfoodfacts.OpenFoodFactsException.Network].
 */
public fun interface OpenFoodFactsTransport {
    public suspend fun get(url: String, headers: Map<String, String>): TransportResponse
}
