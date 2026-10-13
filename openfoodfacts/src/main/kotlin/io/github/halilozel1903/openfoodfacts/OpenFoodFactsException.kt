package io.github.halilozel1903.openfoodfacts

import kotlin.time.Duration

/** Everything [OpenFoodFactsClient] throws. */
public sealed class OpenFoodFactsException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** No product has this barcode in the database. */
    public class NotFound(public val barcode: String) :
        OpenFoodFactsException("No product with barcode $barcode on Open Food Facts.")

    /** The barcode is empty or has characters other than digits. */
    public class InvalidBarcode(public val barcode: String) :
        OpenFoodFactsException("\"$barcode\" is not a barcode: use the digits only.")

    /**
     * A request would exceed the API's rate limit ([RateLimits.whenExceeded] is [RateLimitBehavior.Fail]), or the
     * server answered `429 Too Many Requests`. [retryAfter] is how long to wait, when known.
     */
    public class RateLimited(public val kind: RequestKind, public val retryAfter: Duration?) :
        OpenFoodFactsException(
            "Open Food Facts rate limit for ${kind.label} reached" + (retryAfter?.let { "; retry in ${it.inWholeSeconds + 1} s." } ?: "."),
        )

    /** The server answered with an unexpected HTTP status. */
    public class Http(public val url: String, public val status: Int) :
        OpenFoodFactsException("Open Food Facts answered HTTP $status for $url")

    /** The request did not complete: offline, DNS, TLS or a timeout. */
    public class Network(public val url: String, cause: Throwable) :
        OpenFoodFactsException("Could not reach Open Food Facts at $url: ${cause.message ?: cause::class.simpleName}", cause)

    /** The response was not the JSON this client expects. */
    public class Decoding(public val url: String, cause: Throwable) :
        OpenFoodFactsException("Unexpected response from $url: ${cause.message}", cause)
}
