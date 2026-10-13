package io.github.halilozel1903.openfoodfacts.images

import io.github.halilozel1903.openfoodfacts.Barcode
import io.github.halilozel1903.openfoodfacts.OpenFoodFactsClient
import io.github.halilozel1903.openfoodfacts.model.Product
import io.github.halilozel1903.openfoodfacts.model.lenientString
import kotlinx.serialization.json.JsonObject

/** Which photo of a product. */
public enum class ImageKind(public val key: String) {
    Front("front"),
    Ingredients("ingredients"),
    Nutrition("nutrition"),
    Packaging("packaging"),
}

/**
 * Image sizes Open Food Facts serves. [selectedKey] is the size's name in `selected_images`; [fileSuffix] is the
 * part of the file name (`front_en.12.400.jpg`).
 */
public enum class ImageSize(public val selectedKey: String?, public val fileSuffix: String, public val maxPixels: Int?) {
    Thumb("thumb", "100", 100),
    Small("small", "200", 200),
    Display("display", "400", 400),
    Full(null, "full", null),
    ;

    public companion object {
        /** The smallest size at least [pixels] tall or wide: pick by the size you draw at. */
        public fun atLeast(pixels: Int): ImageSize = entries.firstOrNull { it.maxPixels != null && it.maxPixels >= pixels } ?: Full
    }
}

/** Picks image urls from a product response, so you do not have to know the API's three ways of listing images. */
public object ProductImages {

    /**
     * The url of [kind] at [size], or `null` when the product has no such image:
     *
     * 1. `selected_images[kind][size][lang]` for the first of [languages], the product's `lang`, `en`, then any
     *    language;
     * 2. else a url built from `images["<kind>_<lang>"].rev` in the same language order (the only way to get
     *    [ImageSize.Full]);
     * 3. else the flat `image_<kind>_url` fields (`image_url` for the front).
     *
     * Ask for the `selected_images` field (it is in `ProductFields.Detail`) to get every language.
     */
    public fun url(
        product: Product,
        kind: ImageKind,
        size: ImageSize,
        languages: List<String> = emptyList(),
        imagesBaseUrl: String = OpenFoodFactsClient.DEFAULT_IMAGES_BASE_URL,
    ): String? {
        val order = languageOrder(product, languages)
        val fromSelected = size.selectedKey?.let { key ->
            product.selectedImages[kind.key]?.get(key)?.let { byLang -> pick(byLang, order) }
        }
        return fromSelected
            ?: built(product, kind, size, order, imagesBaseUrl)
            ?: flat(product, kind, size)
    }

    /** Every language that has a selected image of [kind]. */
    public fun languages(product: Product, kind: ImageKind): Set<String> {
        val selected = product.selectedImages[kind.key]?.values?.flatMap { it.keys }.orEmpty()
        val built = product.images.keys.filter { it.startsWith("${kind.key}_") }.map { it.removePrefix("${kind.key}_") }
        return (selected + built).toSortedSet()
    }

    /** The url of an image file: `<base>/images/products/<folder>/<name>.<rev>.<size>.jpg`. */
    public fun fileUrl(
        code: String,
        imageName: String,
        rev: String,
        size: ImageSize,
        imagesBaseUrl: String = OpenFoodFactsClient.DEFAULT_IMAGES_BASE_URL,
    ): String = "${imagesBaseUrl.trimEnd('/')}/images/products/${Barcode.imageFolder(code)}/$imageName.$rev.${size.fileSuffix}.jpg"

    private fun languageOrder(product: Product, languages: List<String>): List<String> =
        (languages + listOfNotNull(product.lang) + "en").map { it.lowercase() }.distinct()

    private fun pick(byLang: Map<String, String>, order: List<String>): String? =
        order.firstNotNullOfOrNull { byLang[it] }?.takeIf { it.isNotBlank() }
            ?: byLang.entries.sortedBy { it.key }.firstOrNull { it.value.isNotBlank() }?.value

    private fun built(product: Product, kind: ImageKind, size: ImageSize, order: List<String>, base: String): String? {
        if (product.code.isEmpty()) return null
        val prefix = "${kind.key}_"
        val name = order.map { prefix + it }.firstOrNull { it in product.images }
            ?: product.images.keys.filter { it.startsWith(prefix) }.sorted().firstOrNull()
            ?: return null
        val rev = (product.images[name] as? JsonObject)?.get("rev").lenientString() ?: return null
        return fileUrl(product.code, name, rev, size, base)
    }

    private fun flat(product: Product, kind: ImageKind, size: ImageSize): String? {
        val (display, small, thumb) = when (kind) {
            ImageKind.Front -> Triple(product.imageFrontUrl ?: product.imageUrlField, product.imageFrontSmallUrl ?: product.imageSmallUrl, product.imageFrontThumbUrl ?: product.imageThumbUrl)
            ImageKind.Ingredients -> Triple(product.imageIngredientsUrl, product.imageIngredientsSmallUrl, null)
            ImageKind.Nutrition -> Triple(product.imageNutritionUrl, product.imageNutritionSmallUrl, null)
            ImageKind.Packaging -> Triple(product.imagePackagingUrl, null, null)
        }
        val url = when (size) {
            ImageSize.Thumb -> thumb ?: small ?: display
            ImageSize.Small -> small ?: display ?: thumb
            ImageSize.Display -> display ?: small ?: thumb
            ImageSize.Full -> display?.let { Regex("\\.(100|200|400)\\.jpg$").replace(it, ".full.jpg") } ?: small ?: thumb
        }
        return url?.takeIf { it.isNotBlank() }
    }
}
