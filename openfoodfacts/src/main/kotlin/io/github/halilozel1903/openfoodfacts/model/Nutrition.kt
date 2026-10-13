package io.github.halilozel1903.openfoodfacts.model

import kotlinx.serialization.json.JsonElement

/** The nutrients most labels show, with their `nutriments` keys and display units. */
public enum class Nutrient(public val key: String, public val label: String, public val unit: String) {
    EnergyKj("energy-kj", "Energy", "kJ"),
    EnergyKcal("energy-kcal", "Energy", "kcal"),
    Fat("fat", "Fat", "g"),
    SaturatedFat("saturated-fat", "Saturated fat", "g"),
    Carbohydrates("carbohydrates", "Carbohydrates", "g"),
    Sugars("sugars", "Sugars", "g"),
    Fiber("fiber", "Fiber", "g"),
    Proteins("proteins", "Proteins", "g"),
    Salt("salt", "Salt", "g"),
    Sodium("sodium", "Sodium", "g"),
    ;

    /** `true` for nutrients shown indented under another one (saturated fat, sugars). */
    public val isSubNutrient: Boolean get() = this == SaturatedFat || this == Sugars
}

/** One amount of nutrient values, per 100 g (or 100 ml) or per serving. Values are `null` when not on the label. */
public data class NutritionFacts(
    val energyKj: Double?,
    val energyKcal: Double?,
    val fat: Double?,
    val saturatedFat: Double?,
    val carbohydrates: Double?,
    val sugars: Double?,
    val fiber: Double?,
    val proteins: Double?,
    val salt: Double?,
    val sodium: Double?,
) {
    public operator fun get(nutrient: Nutrient): Double? = when (nutrient) {
        Nutrient.EnergyKj -> energyKj
        Nutrient.EnergyKcal -> energyKcal
        Nutrient.Fat -> fat
        Nutrient.SaturatedFat -> saturatedFat
        Nutrient.Carbohydrates -> carbohydrates
        Nutrient.Sugars -> sugars
        Nutrient.Fiber -> fiber
        Nutrient.Proteins -> proteins
        Nutrient.Salt -> salt
        Nutrient.Sodium -> sodium
    }

    /** `true` when no value is known. */
    public val isEmpty: Boolean get() = Nutrient.entries.all { get(it) == null }
}

/**
 * A product's `nutriments` object. Keys are `<nutrient>_100g`, `<nutrient>_serving`, `<nutrient>_unit` and
 * `<nutrient>_value`; values are numbers, or strings on some older products, and both are read.
 *
 * ```kotlin
 * product.nutriments.per100g.sugars          // 56.3
 * product.nutriments.per100g(Nutrient.Salt)  // 0.107
 * product.nutriments.per100g("caffeine")     // any other nutrient by key
 * ```
 */
public class Nutriments(public val raw: Map<String, JsonElement>) {

    /** A nutrient per 100 g / 100 ml by its key, such as `"vitamin-c"`. */
    public fun per100g(key: String): Double? = raw["${key}_100g"].lenientDouble()

    /** A nutrient per serving by its key. */
    public fun perServing(key: String): Double? = raw["${key}_serving"].lenientDouble()

    public fun per100g(nutrient: Nutrient): Double? = per100g.get(nutrient)
    public fun perServing(nutrient: Nutrient): Double? = perServing.get(nutrient)

    /** The unit the label used for a nutrient, such as `g`, `mg` or `kcal`. Values above are always in g (or kJ/kcal). */
    public fun unit(key: String): String? = raw["${key}_unit"].lenientString()

    /** Every nutrient key present, without suffixes: `energy-kcal`, `fat`, `nova-group`, ... */
    public val keys: Set<String>
        get() = raw.keys.mapNotNullTo(sortedSetOf()) { k ->
            listOf("_100g", "_serving", "_unit", "_value").firstOrNull { k.endsWith(it) }?.let { k.removeSuffix(it) }
        }

    /** The values per 100 g / 100 ml. */
    public val per100g: NutritionFacts get() = facts { per100g(it) }

    /** The values per serving, when the label gives them. */
    public val perServing: NutritionFacts get() = facts { perServing(it) }

    private inline fun facts(value: (String) -> Double?): NutritionFacts {
        val kj = value("energy-kj") ?: value("energy")
        val kcal = value("energy-kcal") ?: kj?.let { it / KJ_PER_KCAL }
        return NutritionFacts(
            energyKj = kj ?: kcal?.let { it * KJ_PER_KCAL },
            energyKcal = kcal,
            fat = value("fat"),
            saturatedFat = value("saturated-fat"),
            carbohydrates = value("carbohydrates"),
            sugars = value("sugars"),
            fiber = value("fiber"),
            proteins = value("proteins"),
            salt = value("salt") ?: value("sodium")?.let { it * SALT_PER_SODIUM },
            sodium = value("sodium") ?: value("salt")?.let { it / SALT_PER_SODIUM },
        )
    }

    override fun toString(): String = "Nutriments(per100g=$per100g)"

    private companion object {
        const val KJ_PER_KCAL = 4.184
        const val SALT_PER_SODIUM = 2.5
    }
}
