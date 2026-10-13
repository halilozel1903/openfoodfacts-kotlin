package io.github.halilozel1903.openfoodfacts.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.openfoodfacts.model.GreenScore
import io.github.halilozel1903.openfoodfacts.model.NovaGroup
import io.github.halilozel1903.openfoodfacts.model.NutriScore

private fun Long.color() = Color(this)

/** Dark text on the light grades (B, C), white on the rest. */
private fun onGrade(color: Long) = if (color == 0xFFFECB02 || color == 0xFF85BB2F || color == 0xFFFFCC00) Color(0xFF1A1A1A) else Color.White

/**
 * The Nutri-Score as a strip of the five grades with the product's grade raised, in the scale's colours. Ungraded
 * products show the strip greyed out with a note.
 */
@Composable
fun NutriScoreBadge(score: NutriScore, modifier: Modifier = Modifier) {
    val description = score.letter?.let { "Nutri-Score $it" } ?: "Nutri-Score not computed"
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("NUTRI-SCORE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            NutriScore.grades.forEachIndexed { i, grade ->
                val selected = grade == score
                val shape = when (i) {
                    0 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    4 -> RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                    else -> RoundedCornerShape(0.dp)
                }
                val fill = if (score.isGraded) grade.color.color() else Color(0xFFBDBDBD)
                Box(
                    Modifier
                        .then(
                            if (selected) {
                                Modifier
                                    .size(width = 40.dp, height = 46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(fill)
                                    .border(3.dp, Color.White, RoundedCornerShape(10.dp))
                            } else {
                                Modifier
                                    .size(width = 28.dp, height = 34.dp)
                                    .clip(shape)
                                    .background(fill.copy(alpha = if (score.isGraded) 0.55f else 1f))
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        grade.name,
                        color = if (selected) onGrade(grade.color) else Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Black,
                        fontSize = if (selected) 26.sp else 16.sp,
                    )
                }
            }
        }
        if (!score.isGraded) {
            Text(
                if (score == NutriScore.NotApplicable) "Not applicable" else "Not enough data",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** The NOVA group as a tall numbered tile. */
@Composable
fun NovaBadge(nova: NovaGroup?, modifier: Modifier = Modifier) {
    ScoreTile(
        title = "NOVA",
        value = nova?.group?.toString() ?: "?",
        color = nova?.color ?: 0xFF9E9E9E,
        caption = when (nova) {
            NovaGroup.Unprocessed -> "Unprocessed"
            NovaGroup.CulinaryIngredients -> "Ingredient"
            NovaGroup.Processed -> "Processed"
            // Broken at the hyphen on purpose: the tile is too narrow for the whole word on one line.
            NovaGroup.UltraProcessed -> "Ultra-\nprocessed"
            null -> "Unknown"
        },
        modifier = modifier.semantics { contentDescription = nova?.let { "NOVA group ${it.group}: ${it.label}" } ?: "NOVA group unknown" },
    )
}

/** The Green-Score (environmental impact) as a tile. */
@Composable
fun GreenScoreBadge(score: GreenScore, modifier: Modifier = Modifier) {
    ScoreTile(
        title = "GREEN",
        value = score.label,
        color = score.color,
        caption = if (score.isGraded) "Green-Score" else "Not rated",
        modifier = modifier.semantics { contentDescription = "Green-Score ${score.label}" },
    )
}

@Composable
private fun ScoreTile(title: String, value: String, color: Long, caption: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .widthIn(min = 68.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(color.color()),
            contentAlignment = Alignment.Center,
        ) {
            Text(value, color = onGrade(color), fontWeight = FontWeight.Black, fontSize = if (value.length > 1) 15.sp else 20.sp)
        }
        Text(
            caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

/** A compact Nutri-Score letter for list rows. */
@Composable
fun NutriScoreChip(score: NutriScore, modifier: Modifier = Modifier) {
    val fill = if (score.isGraded) score.color.color() else MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 8.dp, end = 3.dp, top = 3.dp, bottom = 3.dp)
            .semantics { contentDescription = score.letter?.let { "Nutri-Score $it" } ?: "No Nutri-Score" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Nutri", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .size(width = 22.dp, height = 22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(fill),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                score.letter ?: "?",
                color = if (score.isGraded) onGrade(score.color) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
            )
        }
    }
}

/** A coloured dot for a traffic light level or similar. */
@Composable
fun Dot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}

/** A thin horizontal bar filled to [fraction]. */
@Composable
fun FractionBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
    }
}
