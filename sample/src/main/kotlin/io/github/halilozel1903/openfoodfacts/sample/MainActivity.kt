package io.github.halilozel1903.openfoodfacts.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

/**
 * A food scanner built on openfoodfacts-kotlin: scan a barcode with the camera (CameraX and ML Kit) or type it,
 * then see the product's Nutri-Score, NOVA group, Green-Score, nutrition table, ingredients and allergens, all
 * loaded live from Open Food Facts. `scripts/screenshots.sh` starts it with `--es scene <scene>` to open a fixed
 * screen, loaded live like a normal launch (see [Scene]). Emulators in CI have no camera to point at a product, so
 * scenes open products by their real barcodes instead of scanning them.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val scene = Scene.from(intent.getStringExtra(EXTRA_SCENE))
        setContent {
            FoodTheme(dark = isSystemInDarkTheme()) {
                // The Surface makes text default to onBackground, so it stays readable in dark mode.
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    FoodApp(scene = scene, repository = FoodRepository.shared)
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
    }
}
