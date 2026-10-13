package io.github.halilozel1903.openfoodfacts.sample

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.openfoodfacts.SearchQuery
import io.github.halilozel1903.openfoodfacts.SearchSort
import io.github.halilozel1903.openfoodfacts.model.NutriScore
import io.github.halilozel1903.openfoodfacts.model.Product
import io.github.halilozel1903.openfoodfacts.model.SearchPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/** What the search screen shows: all pages loaded so far. */
private data class Results(val count: Int, val products: List<Product>, val hasMore: Boolean, val page: Int)

private fun SearchPage.toResults(previous: Results? = null) = Results(
    count = count,
    products = (previous?.products.orEmpty() + products).distinctBy { it.code },
    hasMore = hasNextPage,
    page = page,
)

/**
 * Search by category with Nutri-Score filters and a sort order (`/api/v2/search`), or by words
 * (`/cgi/search.pl`). Searches are limited to 10 a minute; the client queues extra ones, and the screen shows how
 * many are left.
 */
@Composable
fun SearchScreen(repository: FoodRepository, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    var category by rememberSaveable { mutableStateOf(Category.BreakfastCereals) }
    var gradesKey by rememberSaveable { mutableStateOf("") }
    val grades = remember(gradesKey) { gradesKey.mapNotNull { c -> NutriScore.grades.firstOrNull { it.name == c.toString() } }.toSet() }
    var bestFirst by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }

    var state by remember { mutableStateOf<Load<Results>>(Load.Loading) }
    var loadingMore by remember { mutableStateOf(false) }
    var searchesLeft by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    fun query(page: Int) = SearchQuery(
        categories = listOf(category.tag),
        nutritionGrades = grades,
        sortBy = if (bestFirst) SearchSort.NutriScore else SearchSort.Popularity,
        page = page,
        pageSize = FoodRepository.PAGE_SIZE,
    )

    suspend fun fetch(page: Int): SearchPage =
        if (text.isBlank()) repository.search(query(page)) else repository.searchText(text, page)

    LaunchedEffect(category, gradesKey, bestFirst, text, reload) {
        // Typing waits for a pause, so each word does not use one of the 10 searches a minute.
        if (text.isNotBlank()) delay(700)
        state = Load.Loading
        state = load { fetch(1).toResults() }
        searchesLeft = repository.searchesLeft()
    }
    ReportLoad(state)

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Search", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
        item {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Product or brand") },
                leadingIcon = { SearchIcon(MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (text.isBlank()) {
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Category.entries.forEach { c ->
                        FilterChip(selected = c == category, onClick = { category = c }, label = { Text(c.label) })
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nutri-Score", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    NutriScore.grades.forEach { g ->
                        FilterChip(
                            selected = g in grades,
                            onClick = {
                                val next = if (g in grades) grades - g else grades + g
                                gradesKey = next.sortedBy { it.ordinal }.joinToString("") { it.name }
                            },
                            label = { Text(g.name) },
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !bestFirst, onClick = { bestFirst = false }, label = { Text("Most scanned") })
                    FilterChip(selected = bestFirst, onClick = { bestFirst = true }, label = { Text("Best Nutri-Score") })
                }
            }
        }
        when (val s = state) {
            Load.Loading -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            is Load.Failed -> item {
                Box(Modifier.fillMaxWidth()) { ErrorState(s.message, onRetry = { reload++ }) }
            }
            is Load.Ready -> {
                val results = s.value
                item {
                    val left = searchesLeft?.let { " · $it searches left this minute" }.orEmpty()
                    Text(
                        "${NumberFormat.getIntegerInstance(Locale.US).format(results.count)} products$left",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(results.products, key = { it.code }) { product ->
                    ProductRow(product, onClick = { onOpen(product.code) })
                }
                if (results.hasMore) {
                    item {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (loadingMore) {
                                CircularProgressIndicator(Modifier.padding(8.dp))
                            } else {
                                OutlinedButton(onClick = {
                                    loadingMore = true
                                    scope.launch {
                                        val more = load { fetch(results.page + 1).toResults(results) }
                                        if (more is Load.Ready) state = more
                                        searchesLeft = repository.searchesLeft()
                                        loadingMore = false
                                    }
                                }) { Text("Load more") }
                            }
                        }
                    }
                }
            }
        }
    }
}
