package io.github.halilozel1903.openfoodfacts.model

/**
 * A taxonomy tag such as `en:milk` or `fr:pates-a-tartiner`: a language prefix and an id. [displayName] is a
 * readable English fallback built from the id; ask the API with `lc` and the `*_tags_<lc>` fields for translations.
 */
public data class Tag(val raw: String) {
    /** The language prefix, like `en`, or `null` when there is none. */
    val language: String? get() = raw.substringBefore(':', missingDelimiterValue = "").takeIf { it.isNotEmpty() && raw.contains(':') }

    /** The id after the prefix, like `milk` or `sulphur-dioxide-and-sulphites`. */
    val id: String get() = raw.substringAfter(':')

    /** "Milk", "Sulphur dioxide and sulphites". */
    val displayName: String get() = id.replace('-', ' ').replaceFirstChar { it.uppercase() }

    override fun toString(): String = raw
}

/** The 14 allergens EU law requires on labels, with their Open Food Facts tags. */
public enum class Allergen(public val tag: String, public val label: String) {
    Gluten("en:gluten", "Gluten"),
    Crustaceans("en:crustaceans", "Crustaceans"),
    Eggs("en:eggs", "Eggs"),
    Fish("en:fish", "Fish"),
    Peanuts("en:peanuts", "Peanuts"),
    Soybeans("en:soybeans", "Soy"),
    Milk("en:milk", "Milk"),
    Nuts("en:nuts", "Tree nuts"),
    Celery("en:celery", "Celery"),
    Mustard("en:mustard", "Mustard"),
    Sesame("en:sesame-seeds", "Sesame"),
    Sulphites("en:sulphur-dioxide-and-sulphites", "Sulphites"),
    Lupin("en:lupin", "Lupin"),
    Molluscs("en:molluscs", "Molluscs"),
    ;

    public companion object {
        public fun fromTag(tag: Tag): Allergen? = entries.firstOrNull { it.tag == tag.raw }
    }
}

/** Yes, no or maybe, as used by `ingredients_analysis_tags` and the ingredient flags. */
public enum class Presence { Yes, No, Maybe, Unknown }

/**
 * Open Food Facts' analysis of the ingredient list (`ingredients_analysis_tags`): whether a product contains palm
 * oil and whether it is vegan and vegetarian. [Presence.Unknown] when the ingredients could not be analysed.
 */
public data class IngredientsAnalysis(
    val palmOil: Presence,
    val vegan: Presence,
    val vegetarian: Presence,
) {
    public companion object {
        public fun fromTags(tags: List<String>): IngredientsAnalysis {
            val set = tags.toSet()
            fun pick(yes: String, no: String, maybe: String) = when {
                yes in set -> Presence.Yes
                no in set -> Presence.No
                maybe in set -> Presence.Maybe
                else -> Presence.Unknown
            }
            return IngredientsAnalysis(
                palmOil = pick("en:palm-oil", "en:palm-oil-free", "en:may-contain-palm-oil"),
                vegan = pick("en:vegan", "en:non-vegan", "en:maybe-vegan"),
                vegetarian = pick("en:vegetarian", "en:non-vegetarian", "en:maybe-vegetarian"),
            )
        }
    }
}
