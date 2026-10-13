package io.github.halilozel1903.openfoodfacts.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.delay

/** How often a failed image is requested again before it counts as failed. */
private const val ImageRetries = 3

/**
 * A product photo from `images.openfoodfacts.org`, picked with `product.imageUrl(...)`, on a white card so packaging
 * shots look right in dark mode too. Products without a photo get a drawn placeholder. Screenshot scenes wait until
 * every photo has loaded.
 */
@Composable
fun ProductImage(url: String?, description: String, modifier: Modifier = Modifier, corner: Dp = 12.dp, padding: Dp = 6.dp) {
    val monitor = LocalSceneMonitor.current
    var attempt by remember(url) { mutableIntStateOf(0) }
    var status by remember(url) { mutableStateOf(ImageStatus.Loading) }
    if (monitor != null && !url.isNullOrEmpty()) {
        val id = remember { Any() }
        DisposableEffect(monitor, id) { onDispose { monitor.image(id, null) } }
        LaunchedEffect(monitor, id, url) {
            snapshotFlow { if (status == ImageStatus.Failed && attempt < ImageRetries) ImageStatus.Loading else status }
                .collect { monitor.image(id, it) }
        }
    }
    if (status == ImageStatus.Failed && !url.isNullOrEmpty() && attempt < ImageRetries) {
        LaunchedEffect(url, attempt) {
            delay(1_000L shl attempt)
            status = ImageStatus.Loading
            attempt++
        }
    }
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrEmpty() || status != ImageStatus.Loaded) {
            PackagePlaceholder(Modifier.fillMaxSize().padding(padding * 2))
        }
        if (!url.isNullOrEmpty()) {
            key(url, attempt) {
                AsyncImage(
                    model = url,
                    contentDescription = description,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onState = { state ->
                        status = when (state) {
                            is AsyncImagePainter.State.Success -> ImageStatus.Loaded
                            is AsyncImagePainter.State.Error -> ImageStatus.Failed
                            else -> ImageStatus.Loading
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PackagePlaceholder(modifier: Modifier) {
    val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Canvas(modifier) {
        val s = size.minDimension * 0.6f
        val left = (size.width - s) / 2
        val top = (size.height - s) / 2
        drawRoundRect(color, Offset(left, top), Size(s, s), CornerRadius(s * 0.12f), style = Stroke(width = s * 0.06f))
        drawLine(color, Offset(left, top + s * 0.3f), Offset(left + s, top + s * 0.3f), strokeWidth = s * 0.06f)
    }
}
