package io.github.halilozel1903.openfoodfacts.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.openfoodfacts.OpenFoodFactsException
import kotlinx.coroutines.CancellationException

/** The state of something loading. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
    data class Failed(val message: String) : Load<Nothing>
}

/** Runs [block] and wraps its result or a readable error. */
suspend fun <T> load(block: suspend () -> T): Load<T> = try {
    Load.Ready(block())
} catch (e: CancellationException) {
    throw e
} catch (e: OpenFoodFactsException.NotFound) {
    Load.Failed("No product with barcode ${e.barcode} on Open Food Facts yet. You can add it on openfoodfacts.org.")
} catch (e: OpenFoodFactsException.InvalidBarcode) {
    Load.Failed("A barcode is made of digits only.")
} catch (e: OpenFoodFactsException.RateLimited) {
    Load.Failed("Too many requests in a minute. Wait a moment and try again.")
} catch (e: OpenFoodFactsException.Network) {
    Load.Failed("Could not reach Open Food Facts. Check your connection.")
} catch (e: OpenFoodFactsException) {
    Load.Failed(e.message ?: "Open Food Facts returned an error.")
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.titleLarge)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) { Text("Try again") }
    }
}
