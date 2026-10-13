package io.github.halilozel1903.openfoodfacts.transport

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText

/** The default transport: GET requests with Ktor. Closes the client only when it created it. */
internal class KtorTransport(
    httpClient: HttpClient?,
    engine: HttpClientEngine?,
    requestTimeoutMillis: Long,
    retries: Int,
) : OpenFoodFactsTransport, AutoCloseable {

    private val ownsClient = httpClient == null
    private val client: HttpClient = httpClient ?: run {
        val setup: HttpClientConfig<*>.() -> Unit = {
            expectSuccess = false
            install(HttpTimeout) {
                this.requestTimeoutMillis = requestTimeoutMillis
            }
            if (retries > 0) {
                install(HttpRequestRetry) {
                    // 5xx only: a 429 must not be retried right away, the client reports it instead.
                    retryOnServerErrors(maxRetries = retries)
                    retryOnException(maxRetries = retries, retryOnTimeout = true)
                    exponentialDelay()
                }
            }
        }
        if (engine != null) HttpClient(engine, setup) else HttpClient(CIO, setup)
    }

    override suspend fun get(url: String, headers: Map<String, String>): TransportResponse {
        val response = client.get(url) {
            headers.forEach { (name, value) -> header(name, value) }
        }
        return TransportResponse(
            status = response.status.value,
            body = response.bodyAsText(),
            headers = response.headers.entries().associate { (name, values) -> name.lowercase() to values.joinToString(",") },
        )
    }

    override fun close() {
        if (ownsClient) client.close()
    }
}
