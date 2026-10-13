package io.github.halilozel1903.openfoodfacts.model

/**
 * The Nutri-Score grade, `nutriscore_grade` in the API: A (best) to E. [Unknown] when Open Food Facts lacks the data
 * to compute it, [NotApplicable] for products it does not apply to (water, baby food, ...).
 */
public enum class NutriScore(public val apiValue: String, public val color: Long) {
    A("a", 0xFF038141),
    B("b", 0xFF85BB2F),
    C("c", 0xFFFECB02),
    D("d", 0xFFEE8100),
    E("e", 0xFFE63E11),
    Unknown("unknown", 0xFF9E9E9E),
    NotApplicable("not-applicable", 0xFF9E9E9E),
    ;

    /** `true` for A to E. */
    public val isGraded: Boolean get() = ordinal <= E.ordinal

    /** "A" to "E", or `null` when not graded. */
    public val letter: String? get() = if (isGraded) name else null

    public companion object {
        /** The five grades, A to E. */
        public val grades: List<NutriScore> = listOf(A, B, C, D, E)

        public fun fromApi(value: String?): NutriScore = when (val v = value?.trim()?.lowercase()) {
            null, "" -> Unknown
            else -> entries.firstOrNull { it.apiValue == v } ?: Unknown
        }
    }
}

/** The NOVA group, `nova_group` in the API: how processed a food is. */
public enum class NovaGroup(public val group: Int, public val label: String, public val color: Long) {
    Unprocessed(1, "Unprocessed or minimally processed foods", 0xFF00AA00),
    CulinaryIngredients(2, "Processed culinary ingredients", 0xFFFFCC00),
    Processed(3, "Processed foods", 0xFFFF6600),
    UltraProcessed(4, "Ultra-processed food and drink products", 0xFFFF0000),
    ;

    public companion object {
        public fun fromGroup(group: Int?): NovaGroup? = entries.firstOrNull { it.group == group }
    }
}

/**
 * The Green-Score (formerly Eco-Score), the environmental impact grade: A+ (best) to F. The API sends it as
 * `environmental_score_grade`, and as `ecoscore_grade` on older data; [Product.greenScore] reads either.
 */
public enum class GreenScore(public val apiValue: String, public val label: String, public val color: Long) {
    APlus("a-plus", "A+", 0xFF1E8F4E),
    A("a", "A", 0xFF2E9D52),
    B("b", "B", 0xFF60AC0E),
    C("c", "C", 0xFFEEAE0E),
    D("d", "D", 0xFFFF6F1E),
    E("e", "E", 0xFFDF1F1F),
    F("f", "F", 0xFFA51B1B),
    Unknown("unknown", "?", 0xFF9E9E9E),
    NotApplicable("not-applicable", "N/A", 0xFF9E9E9E),
    ;

    public val isGraded: Boolean get() = ordinal <= F.ordinal

    public companion object {
        public fun fromApi(value: String?): GreenScore = when (val v = value?.trim()?.lowercase()) {
            null, "" -> Unknown
            else -> entries.firstOrNull { it.apiValue == v } ?: Unknown
        }
    }
}

/** The traffic light level of a nutrient, from `nutrient_levels`. */
public enum class NutrientLevel(public val apiValue: String) {
    Low("low"),
    Moderate("moderate"),
    High("high"),
    ;

    public companion object {
        public fun fromApi(value: String?): NutrientLevel? = entries.firstOrNull { it.apiValue == value?.lowercase() }
    }
}
