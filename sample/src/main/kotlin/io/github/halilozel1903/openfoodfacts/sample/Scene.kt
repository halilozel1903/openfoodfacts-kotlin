package io.github.halilozel1903.openfoodfacts.sample

/** The two halves of the product screen. */
enum class ProductTab(val label: String) {
    Nutrition("Nutrition"),
    Ingredients("Ingredients"),
}

/** The bottom bar destinations. */
enum class Section(val label: String) {
    Scan("Scan"),
    Search("Search"),
}

/**
 * Screenshot scenes, picked with the `scene` intent extra. Each loads live data from Open Food Facts like a normal
 * launch. [barcode] opens a product directly, the way a scan would.
 */
enum class Scene(val key: String, val section: Section, val barcode: String?, val tab: ProductTab) {
    /** The scan screen with a typed barcode and the example products, loaded live. */
    Scan("scan", Section.Scan, null, ProductTab.Nutrition),

    /** Nutella: Nutri-Score, NOVA, Green-Score, nutrient levels and the nutrition table. */
    Product("product", Section.Scan, "3017620422003", ProductTab.Nutrition),

    /** Nutella: allergens, ingredient analysis and the ingredient list. */
    Ingredients("ingredients", Section.Scan, "3017620422003", ProductTab.Ingredients),

    /** Coca-Cola's nutrition. */
    Cola("cola", Section.Scan, "5449000000996", ProductTab.Nutrition),

    /** Breakfast cereals, most popular first, with their Nutri-Scores. */
    Search("search", Section.Search, null, ProductTab.Nutrition),
    ;

    companion object {
        fun from(key: String?): Scene? = entries.firstOrNull { it.key == key }
    }
}
