package io.github.halilozel1903.openfoodfacts.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.openfoodfacts.images.ImageSize
import io.github.halilozel1903.openfoodfacts.model.Allergen
import io.github.halilozel1903.openfoodfacts.model.Ingredient
import io.github.halilozel1903.openfoodfacts.model.Nutrient
import io.github.halilozel1903.openfoodfacts.model.NutrientLevel
import io.github.halilozel1903.openfoodfacts.model.NutritionFacts
import io.github.halilozel1903.openfoodfacts.model.Presence
import io.github.halilozel1903.openfoodfacts.model.Product
import io.github.halilozel1903.openfoodfacts.model.Tag
import java.util.Locale

/** A product looked up by [barcode]: photo, scores, then nutrition or ingredients. */
@Composable
fun ProductScreen(
    repository: FoodRepository,
    barcode: String,
    tab: ProductTab,
    onTabChange: (ProductTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var reload by remember { mutableIntStateOf(0) }
    val state by produceState<Load<Product>>(Load.Loading, barcode, reload) {
        value = Load.Loading
        value = load { repository.product(barcode) }
    }
    ReportLoad(state)

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { BackIcon(MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp)) }
            Text(barcode, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when (val s = state) {
            Load.Loading -> LoadingState()
            is Load.Failed -> ErrorState(s.message, onRetry = { reload++ })
            is Load.Ready -> ProductContent(s.value, tab, onTabChange)
        }
    }
}

@Composable
private fun ProductContent(product: Product, tab: ProductTab, onTabChange: (ProductTab) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Header(product) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                NutriScoreBadge(product.nutriScore, Modifier.fillMaxHeight())
                NovaBadge(product.nova, Modifier.fillMaxHeight())
                GreenScoreBadge(product.greenScore, Modifier.fillMaxHeight())
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProductTab.entries.forEach { t ->
                    FilterChip(selected = t == tab, onClick = { onTabChange(t) }, label = { Text(t.label) })
                }
            }
        }
        when (tab) {
            ProductTab.Nutrition -> {
                item { NutrientLevels(product) }
                item { NutritionTable(product) }
            }
            ProductTab.Ingredients -> {
                item { Allergens(product) }
                item { Analysis(product) }
                item { IngredientList(product) }
            }
        }
    }
}

@Composable
private fun Header(product: Product) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ProductImage(
            url = product.imageUrl(size = ImageSize.Display, languages = listOf("en")),
            description = product.displayName,
            modifier = Modifier.size(width = 112.dp, height = 132.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(product.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            product.brandList.takeIf { it.isNotEmpty() }?.let {
                Text(it.joinToString(", "), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            product.quantity?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private val levelColors = mapOf(
    NutrientLevel.Low to Color(0xFF2E9D52),
    NutrientLevel.Moderate to Color(0xFFEEAE0E),
    NutrientLevel.High to Color(0xFFE63E11),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NutrientLevels(product: Product) {
    val levels = listOf(Nutrient.Fat, Nutrient.SaturatedFat, Nutrient.Sugars, Nutrient.Salt)
        .mapNotNull { n -> product.nutrientLevel(n)?.let { n to it } }
    if (levels.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        levels.forEach { (nutrient, level) ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Dot(levelColors.getValue(level))
                Text("${level.name} ${nutrient.label.lowercase()}", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun NutritionTable(product: Product) {
    val per100 = product.nutriments.per100g
    val serving = product.nutriments.perServing.takeUnless { it.isEmpty }
    Card {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            Text("Nutrition facts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("100 g", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(76.dp))
            if (serving != null) {
                Text(
                    product.servingSize?.takeIf { it.length <= 10 } ?: "Serving",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(76.dp),
                    maxLines = 1,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val rows = listOf(
            Nutrient.EnergyKcal, Nutrient.Fat, Nutrient.SaturatedFat, Nutrient.Carbohydrates,
            Nutrient.Sugars, Nutrient.Fiber, Nutrient.Proteins, Nutrient.Salt,
        )
        rows.forEach { nutrient ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    nutrient.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (nutrient.isSubNutrient) FontWeight.Normal else FontWeight.Medium,
                    color = if (nutrient.isSubNutrient) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).padding(start = if (nutrient.isSubNutrient) 14.dp else 0.dp),
                )
                Text(per100.format(nutrient), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(76.dp))
                if (serving != null) Text(serving.format(nutrient), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(76.dp))
            }
        }
        Text(
            "Values per 100 g from the label, as entered on Open Food Facts.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private fun NutritionFacts.format(nutrient: Nutrient): String {
    val value = get(nutrient) ?: return "—"
    return when (nutrient) {
        Nutrient.EnergyKcal -> String.format(Locale.US, "%.0f kcal", value)
        Nutrient.EnergyKj -> String.format(Locale.US, "%.0f kJ", value)
        else -> when {
            value == 0.0 -> "0 g"
            value < 1 -> String.format(Locale.US, "%.2f g", value)
            value < 10 -> String.format(Locale.US, "%.1f g", value)
            else -> String.format(Locale.US, "%.0f g", value)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Allergens(product: Product) {
    Card {
        Text("Allergens", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.size(8.dp))
        if (product.allergens.isEmpty()) {
            Text("None declared", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                product.allergens.forEach { TagPill(it.allergenName(), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer) }
            }
        }
        if (product.traces.isNotEmpty()) {
            Text(
                "May contain " + product.traces.joinToString(", ") { it.allergenName().lowercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

private fun Tag.allergenName(): String = Allergen.fromTag(this)?.label ?: displayName

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Analysis(product: Product) {
    val analysis = product.ingredientsAnalysis
    val items = listOfNotNull(
        when (analysis.palmOil) {
            Presence.Yes -> "Contains palm oil" to false
            Presence.No -> "Palm oil free" to true
            Presence.Maybe -> "May contain palm oil" to false
            Presence.Unknown -> null
        },
        when (analysis.vegan) {
            Presence.Yes -> "Vegan" to true
            Presence.No -> "Not vegan" to false
            Presence.Maybe -> "Maybe vegan" to false
            Presence.Unknown -> null
        },
        when (analysis.vegetarian) {
            Presence.Yes -> "Vegetarian" to true
            Presence.No -> "Not vegetarian" to false
            Presence.Maybe -> "Maybe vegetarian" to false
            Presence.Unknown -> null
        },
    )
    if (items.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, good) ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Dot(if (good) Color(0xFF2E9D52) else Color(0xFFEE8100))
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun IngredientList(product: Product) {
    Card {
        Text("Ingredients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.size(8.dp))
        if (product.ingredients.isEmpty()) {
            Text(
                product.ingredientsTextEn ?: product.ingredientsLabel ?: "No ingredient list on Open Food Facts yet.",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Card
        }
        product.ingredients.forEach { ingredient ->
            IngredientRow(ingredient)
        }
        Text(
            "Percentages in bold are on the label; the others are Open Food Facts' estimates.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun IngredientRow(ingredient: Ingredient) {
    val percent = ingredient.bestPercent
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ingredient.englishName(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (percent != null) {
                Text(
                    (if (ingredient.percent == null) "~" else "") + String.format(Locale.US, if (percent < 1) "%.1f%%" else "%.0f%%", percent),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (ingredient.percent != null) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (percent != null) {
            FractionBar((percent / 100).toFloat(), MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

/** The taxonomy's English name for known ingredients (`en:` ids), else the text from the label. */
private fun Ingredient.englishName(): String {
    val tag = id?.let(::Tag)
    val base = if (tag?.language == "en") tag.displayName else name
    val sub = ingredients.map { it.englishName() }
    return if (sub.isEmpty()) base else "$base (${sub.joinToString(", ")})"
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Column { content() }
    }
}

@Composable
private fun TagPill(text: String, background: Color, foreground: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = foreground,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}
