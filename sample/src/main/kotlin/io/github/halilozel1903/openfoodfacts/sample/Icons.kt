package io.github.halilozel1903.openfoodfacts.sample

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** Small drawn icons, so the sample needs no icon library. */
@Composable
fun BarcodeIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val widths = listOf(0.06f, 0.03f, 0.08f, 0.03f, 0.05f, 0.03f, 0.08f, 0.04f)
        var x = s * 0.12f
        widths.forEachIndexed { i, w ->
            drawLine(color, Offset(x + s * w / 2, s * 0.2f), Offset(x + s * w / 2, s * 0.8f), strokeWidth = s * w)
            x += s * w + s * (if (i % 2 == 0) 0.04f else 0.03f)
        }
    }
}

@Composable
fun SearchIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.1f
        drawCircle(color, radius = s * 0.3f, center = Offset(s * 0.42f, s * 0.42f), style = Stroke(width = stroke))
        drawLine(color, Offset(s * 0.64f, s * 0.64f), Offset(s * 0.88f, s * 0.88f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun BackIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.1f
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.82f, s * 0.5f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.46f, s * 0.24f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.46f, s * 0.76f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/** The four corners of a viewfinder. */
@Composable
fun ViewfinderIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val l = minOf(w, h) * 0.18f
        val stroke = minOf(w, h) * 0.035f
        listOf(Offset(0f, 0f) to Offset(1f, 1f), Offset(w, 0f) to Offset(-1f, 1f), Offset(0f, h) to Offset(1f, -1f), Offset(w, h) to Offset(-1f, -1f))
            .forEach { (corner, dir) ->
                val c = Offset(corner.x + dir.x * stroke / 2, corner.y + dir.y * stroke / 2)
                drawLine(color, c, Offset(c.x + dir.x * l, c.y), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color, c, Offset(c.x, c.y + dir.y * l), strokeWidth = stroke, cap = StrokeCap.Round)
            }
    }
}
