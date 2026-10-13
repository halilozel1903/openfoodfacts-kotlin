package io.github.halilozel1903.openfoodfacts.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One ingredient of the parsed ingredient list, in label order. Percentages are what the label states ([percent])
 * or Open Food Facts' estimate ([percentEstimate], always present on analysed products). Compound ingredients have
 * their own [ingredients].
 */
@Serializable
public data class Ingredient(
    /** Taxonomy id, like `en:sugar`, or `en:<text>` for an ingredient the taxonomy does not know. */
    val id: String? = null,
    /** The ingredient as written on the label. */
    val text: String? = null,
    @Serializable(with = LenientDoubleSerializer::class) val percent: Double? = null,
    @SerialName("percent_estimate") @Serializable(with = LenientDoubleSerializer::class) val percentEstimate: Double? = null,
    @SerialName("percent_min") @Serializable(with = LenientDoubleSerializer::class) val percentMin: Double? = null,
    @SerialName("percent_max") @Serializable(with = LenientDoubleSerializer::class) val percentMax: Double? = null,
    @SerialName("vegan") val veganValue: String? = null,
    @SerialName("vegetarian") val vegetarianValue: String? = null,
    @SerialName("from_palm_oil") val fromPalmOilValue: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
) {
    /** [text], or a name built from [id]. */
    val name: String get() = text?.trim()?.takeIf { it.isNotEmpty() } ?: id?.let { Tag(it).displayName }.orEmpty()

    /** The stated percentage, or else the estimate. */
    val bestPercent: Double? get() = percent ?: percentEstimate

    val vegan: Presence get() = presence(veganValue)
    val vegetarian: Presence get() = presence(vegetarianValue)
    val fromPalmOil: Presence get() = presence(fromPalmOilValue)

    /** This ingredient followed by all nested ones, depth first. */
    public fun flatten(): List<Ingredient> = listOf(this) + ingredients.flatMap { it.flatten() }

    private fun presence(value: String?) = when (value) {
        "yes" -> Presence.Yes
        "no" -> Presence.No
        "maybe" -> Presence.Maybe
        else -> Presence.Unknown
    }
}
