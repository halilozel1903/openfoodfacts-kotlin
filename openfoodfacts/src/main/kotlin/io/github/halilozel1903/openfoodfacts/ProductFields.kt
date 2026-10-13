package io.github.halilozel1903.openfoodfacts

/**
 * Field lists for the `fields` parameter. A full product response is often over 100 KB; asking only for what you
 * show makes requests faster and lighter on the servers. Pass `null` to get every field.
 */
public object ProductFields {
    /** What a list row needs: name, brand, quantity, the three scores and the front thumbnails. */
    public val Summary: List<String> = listOf(
        "code", "product_name", "product_name_en", "generic_name", "brands", "quantity", "lang",
        "nutriscore_grade", "nova_group", "environmental_score_grade", "ecoscore_grade",
        "image_front_url", "image_front_small_url", "image_front_thumb_url",
    )

    /** Everything the typed model reads: [Summary] plus nutrition, ingredients, allergens, tags and all image data. */
    public val Detail: List<String> = Summary + listOf(
        "serving_size", "categories_tags", "labels_tags", "countries_tags", "nutriscore_score",
        "allergens_tags", "traces_tags", "additives_tags", "ingredients_text", "ingredients_text_en", "ingredients",
        "ingredients_analysis_tags", "nutriments", "nutrient_levels", "nutrition_data_per",
        "image_url", "image_small_url", "image_thumb_url", "image_ingredients_url", "image_ingredients_small_url",
        "image_nutrition_url", "image_nutrition_small_url", "image_packaging_url", "selected_images", "images",
        "last_modified_t",
    )
}
