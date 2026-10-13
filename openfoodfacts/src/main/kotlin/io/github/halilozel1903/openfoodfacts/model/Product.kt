package io.github.halilozel1903.openfoodfacts.model

import io.github.halilozel1903.openfoodfacts.images.ImageKind
import io.github.halilozel1903.openfoodfacts.images.ImageSize
import io.github.halilozel1903.openfoodfacts.images.ProductImages
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A product, with the API's field names. Only the fields you ask for are filled (see
 * [io.github.halilozel1903.openfoodfacts.ProductFields]); everything else keeps its default. The typed views
 * ([nutriScore], [nova], [greenScore], [nutriments], [allergens], [ingredientsAnalysis], [imageUrl]) read the raw
 * fields and tolerate missing or oddly typed values.
 */
@Serializable
public data class Product(
    val code: String = "",
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_name_en") val productNameEn: String? = null,
    @SerialName("generic_name") val genericName: String? = null,
    val brands: String? = null,
    val quantity: String? = null,
    @SerialName("serving_size") val servingSize: String? = null,
    /** The main language of the product's label, like `fr`. */
    val lang: String? = null,
    @SerialName("categories_tags") val categoriesTags: List<String> = emptyList(),
    @SerialName("labels_tags") val labelsTags: List<String> = emptyList(),
    @SerialName("countries_tags") val countriesTags: List<String> = emptyList(),
    @SerialName("nutriscore_grade") val nutriscoreGrade: String? = null,
    @SerialName("nutriscore_score") @Serializable(with = LenientIntSerializer::class) val nutriscoreScore: Int? = null,
    @SerialName("nova_group") @Serializable(with = LenientIntSerializer::class) val novaGroup: Int? = null,
    @SerialName("environmental_score_grade") val environmentalScoreGrade: String? = null,
    @SerialName("ecoscore_grade") val ecoscoreGrade: String? = null,
    @SerialName("allergens_tags") val allergensTags: List<String> = emptyList(),
    @SerialName("traces_tags") val tracesTags: List<String> = emptyList(),
    @SerialName("additives_tags") val additivesTags: List<String> = emptyList(),
    @SerialName("ingredients_text") val ingredientsText: String? = null,
    @SerialName("ingredients_text_en") val ingredientsTextEn: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
    @SerialName("ingredients_analysis_tags") val ingredientsAnalysisTags: List<String> = emptyList(),
    @SerialName("nutriments") val nutrimentsRaw: Map<String, JsonElement> = emptyMap(),
    @SerialName("nutrient_levels") val nutrientLevels: Map<String, String> = emptyMap(),
    /** `100g` or `serving`: which amount the label's table is for. */
    @SerialName("nutrition_data_per") val nutritionDataPer: String? = null,
    @SerialName("image_url") val imageUrlField: String? = null,
    @SerialName("image_small_url") val imageSmallUrl: String? = null,
    @SerialName("image_thumb_url") val imageThumbUrl: String? = null,
    @SerialName("image_front_url") val imageFrontUrl: String? = null,
    @SerialName("image_front_small_url") val imageFrontSmallUrl: String? = null,
    @SerialName("image_front_thumb_url") val imageFrontThumbUrl: String? = null,
    @SerialName("image_ingredients_url") val imageIngredientsUrl: String? = null,
    @SerialName("image_ingredients_small_url") val imageIngredientsSmallUrl: String? = null,
    @SerialName("image_nutrition_url") val imageNutritionUrl: String? = null,
    @SerialName("image_nutrition_small_url") val imageNutritionSmallUrl: String? = null,
    @SerialName("image_packaging_url") val imagePackagingUrl: String? = null,
    /** `selected_images`: kind (`front`, ...) to size (`display`, `small`, `thumb`) to language to url. */
    @SerialName("selected_images") val selectedImages: Map<String, Map<String, Map<String, String>>> = emptyMap(),
    /** `images`: the selected images by `<kind>_<lang>` (with `rev` and `sizes`) and the raw uploads by number. */
    val images: Map<String, JsonElement> = emptyMap(),
    @SerialName("last_modified_t") @Serializable(with = LenientIntSerializer::class) val lastModified: Int? = null,
) {
    /** The name to show: the product name, the English name, the generic name or, at worst, the barcode. */
    val displayName: String
        get() = listOf(productName, productNameEn, genericName).firstOrNull { !it.isNullOrBlank() }?.trim() ?: code

    /** The brands, split from the comma separated `brands` field. */
    val brandList: List<String> get() = brands.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }

    /** The first brand, or `null`. */
    val brand: String? get() = brandList.firstOrNull()

    val nutriScore: NutriScore get() = NutriScore.fromApi(nutriscoreGrade)

    /** The NOVA group, or `null` when unknown. */
    val nova: NovaGroup? get() = NovaGroup.fromGroup(novaGroup)

    /** The Green-Score from `environmental_score_grade`, else the legacy `ecoscore_grade`. */
    val greenScore: GreenScore
        get() = GreenScore.fromApi(environmentalScoreGrade).takeIf { it != GreenScore.Unknown } ?: GreenScore.fromApi(ecoscoreGrade)

    val nutriments: Nutriments get() = Nutriments(nutrimentsRaw)

    /** The traffic light level of `fat`, `saturated-fat`, `sugars` or `salt`. */
    public fun nutrientLevel(nutrient: Nutrient): NutrientLevel? = NutrientLevel.fromApi(nutrientLevels[nutrient.key])

    val allergens: List<Tag> get() = allergensTags.map(::Tag)
    val traces: List<Tag> get() = tracesTags.map(::Tag)
    val categories: List<Tag> get() = categoriesTags.map(::Tag)
    val labels: List<Tag> get() = labelsTags.map(::Tag)

    /** `true` when the product contains [allergen] (not counting traces). */
    public fun contains(allergen: Allergen): Boolean = allergen.tag in allergensTags

    /** `true` when the label warns of traces of [allergen]. */
    public fun mayContain(allergen: Allergen): Boolean = allergen.tag in tracesTags

    val ingredientsAnalysis: IngredientsAnalysis get() = IngredientsAnalysis.fromTags(ingredientsAnalysisTags)

    /** The ingredient list as written, preferring the localized text the API returned. */
    val ingredientsLabel: String? get() = listOf(ingredientsText, ingredientsTextEn).firstOrNull { !it.isNullOrBlank() }?.trim()

    /**
     * The url of an image of this product, picked as [ProductImages.url] describes: the selected image of [kind] in
     * the first of [languages] that has one (then the product's own language, English, any), at [size].
     */
    public fun imageUrl(
        kind: ImageKind = ImageKind.Front,
        size: ImageSize = ImageSize.Display,
        languages: List<String> = emptyList(),
    ): String? = ProductImages.url(this, kind, size, languages)
}
