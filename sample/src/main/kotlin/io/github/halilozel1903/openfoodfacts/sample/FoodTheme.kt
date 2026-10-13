package io.github.halilozel1903.openfoodfacts.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6B3A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4F1B6),
    onPrimaryContainer = Color(0xFF00210A),
    secondary = Color(0xFF8A5100),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB8),
    onSecondaryContainer = Color(0xFF2C1700),
    background = Color(0xFFF7FBF2),
    onBackground = Color(0xFF181D17),
    surface = Color(0xFFF7FBF2),
    onSurface = Color(0xFF181D17),
    surfaceVariant = Color(0xFFE6EDE1),
    onSurfaceVariant = Color(0xFF424940),
    surfaceContainer = Color(0xFFECF0E6),
    surfaceContainerHigh = Color(0xFFE6EAE1),
    outline = Color(0xFF72796F),
    outlineVariant = Color(0xFFC2C9BD),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF98D49B),
    onPrimary = Color(0xFF003914),
    primaryContainer = Color(0xFF155224),
    onPrimaryContainer = Color(0xFFB4F1B6),
    secondary = Color(0xFFFFB960),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF693C00),
    onSecondaryContainer = Color(0xFFFFDDB8),
    background = Color(0xFF101510),
    onBackground = Color(0xFFDFE4DA),
    surface = Color(0xFF101510),
    onSurface = Color(0xFFDFE4DA),
    surfaceVariant = Color(0xFF2A322A),
    onSurfaceVariant = Color(0xFFC2C9BD),
    surfaceContainer = Color(0xFF1C211C),
    surfaceContainerHigh = Color(0xFF262B26),
    outline = Color(0xFF8C9388),
    outlineVariant = Color(0xFF424940),
)

@Composable
fun FoodTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
