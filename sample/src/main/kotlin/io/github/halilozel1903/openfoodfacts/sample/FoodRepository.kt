package io.github.halilozel1903.openfoodfacts.sample

import io.github.halilozel1903.openfoodfacts.OpenFoodFactsClient
import io.github.halilozel1903.openfoodfacts.ProductFields
import io.github.halilozel1903.openfoodfacts.RequestKind
import io.github.halilozel1903.openfoodfacts.SearchQuery
import io.github.halilozel1903.openfoodfacts.UserAgent
import io.github.halilozel1903.openfoodfacts.model.Product
import io.github.halilozel1903.openfoodfacts.model.SearchPage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Real products to try without a camera: their barcodes are looked up live, like a scan. */
val ExampleBarcodes = listOf(
    "3017620422003", // Nutella
    "5449000000996", // Coca-Cola
    "3274080005003", // Cristaline water
    "7622210449283", // Prince chocolate biscuits
)

/** Search shortcuts: Open Food Facts category tags with an English label. */
enum class Category(val tag: String, val label: String) {
    BreakfastCereals("en:breakfast-cereals", "Breakfast cereals"),
    Sodas("en:sodas", "Sodas"),
    Yogurts("en:yogurts", "Yogurts"),
    Biscuits("en:biscuits", "Biscuits"),
    Cheeses("en:cheeses", "Cheeses"),
}

/**
 * The sample's only data source: the live Open Food Facts API through one shared [OpenFoodFactsClient], which keeps
 * to the API's rate limits and caches responses for a few minutes.
 */
class FoodRepository(private val client: OpenFoodFactsClient) {

    suspend fun product(barcode: String): Product = client.product(barcode, ProductFields.Detail)

    /** The example products, in order. */
    suspend fun examples(): List<Product> = coroutineScope {
        ExampleBarcodes.map { code -> async { client.product(code, ProductFields.Summary) } }.awaitAll()
    }

    suspend fun search(query: SearchQuery): SearchPage = client.search(query)

    suspend fun searchText(terms: String, page: Int): SearchPage = client.searchText(terms, page = page, pageSize = PAGE_SIZE)

    suspend fun searchesLeft(): Int? = client.remainingRequests(RequestKind.Search)

    companion object {
        const val PAGE_SIZE = 20

        val shared: FoodRepository by lazy {
            FoodRepository(
                OpenFoodFactsClient(
                    // Open Food Facts asks every app to name itself and a way to reach its developers.
                    UserAgent(
                        appName = "OpenFoodFactsKotlinSample",
                        version = "1.0.0",
                        contact = "https://github.com/halilozel1903/openfoodfacts-kotlin",
                    ),
                ) {
                    engine = OkHttp.create()
                    language = "en"
                },
            )
        }
    }
}
