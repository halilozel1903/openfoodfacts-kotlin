package io.github.halilozel1903.openfoodfacts.sample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The app: Scan and Search tabs, and a product screen on top when a barcode is scanned, typed or tapped.
 * Screenshot [scene]s start on a fixed tab or product and report to a [SceneMonitor] once loaded.
 */
@Composable
fun FoodApp(scene: Scene?, repository: FoodRepository) {
    val monitor = remember(scene) { scene?.let { SceneMonitor(it) } }
    monitor?.Report()

    var section by rememberSaveable { mutableStateOf(scene?.section ?: Section.Scan) }
    var barcode by rememberSaveable { mutableStateOf(scene?.barcode) }
    var tab by rememberSaveable { mutableStateOf(scene?.tab ?: ProductTab.Nutrition) }

    CompositionLocalProvider(LocalSceneMonitor provides monitor) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            val open = barcode
            if (open != null) {
                BackHandler { barcode = null }
                key(open) {
                    ProductScreen(
                        repository = repository,
                        barcode = open,
                        tab = tab,
                        onTabChange = { tab = it },
                        onBack = { barcode = null },
                        modifier = Modifier
                            .weight(1f)
                            .windowInsetsPadding(WindowInsets.navigationBars),
                    )
                }
            } else {
                val openProduct: (String) -> Unit = {
                    tab = ProductTab.Nutrition
                    barcode = it
                }
                when (section) {
                    Section.Scan -> ScanScreen(
                        repository = repository,
                        // The scan scene shows a typed barcode: emulators have no product to point the camera at.
                        initialBarcode = if (scene == Scene.Scan) "3017620422003" else "",
                        cameraAvailable = scene == null,
                        onOpen = openProduct,
                        modifier = Modifier.weight(1f),
                    )
                    Section.Search -> SearchScreen(repository = repository, onOpen = openProduct, modifier = Modifier.weight(1f))
                }
                NavigationBar {
                    Section.entries.forEach { s ->
                        NavigationBarItem(
                            selected = s == section,
                            onClick = { section = s },
                            icon = {
                                val color = LocalContentColor.current
                                when (s) {
                                    Section.Scan -> BarcodeIcon(color, Modifier.size(24.dp))
                                    Section.Search -> SearchIcon(color, Modifier.size(24.dp))
                                }
                            },
                            label = { Text(s.label) },
                        )
                    }
                }
            }
        }
    }
}
