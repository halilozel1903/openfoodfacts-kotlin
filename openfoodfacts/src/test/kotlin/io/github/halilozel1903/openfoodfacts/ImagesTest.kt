package io.github.halilozel1903.openfoodfacts

import io.github.halilozel1903.openfoodfacts.images.ImageKind
import io.github.halilozel1903.openfoodfacts.images.ImageSize
import io.github.halilozel1903.openfoodfacts.images.ProductImages
import io.github.halilozel1903.openfoodfacts.model.Product
import io.github.halilozel1903.openfoodfacts.model.RawProductResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImagesTest {
    private val product: Product =
        OpenFoodFactsClient.json.decodeFromString(RawProductResponse.serializer(), Fixtures.read("product-3017620422003.json")).product!!
    private val base = "https://images.openfoodfacts.org/images/products/301/762/042/2003"

    @Test
    fun selectedImagePrefersProductLanguage() {
        // No languages asked: the product's own language (fr) comes first.
        assertEquals("$base/front_fr.633.400.jpg", product.imageUrl())
        assertEquals("$base/front_en.427.200.jpg", product.imageUrl(size = ImageSize.Small, languages = listOf("en")))
        assertEquals("$base/front_fr.633.100.jpg", product.imageUrl(size = ImageSize.Thumb, languages = listOf("de")))
    }

    @Test
    fun fullSizeIsBuiltFromRevision() {
        assertEquals("$base/front_fr.633.full.jpg", product.imageUrl(size = ImageSize.Full))
        assertEquals("$base/front_en.427.full.jpg", product.imageUrl(size = ImageSize.Full, languages = listOf("en")))
    }

    @Test
    fun builtWhenNotSelected() {
        // ingredients_fr is only in `images`.
        assertEquals("$base/ingredients_fr.212.400.jpg", product.imageUrl(ImageKind.Ingredients))
        assertEquals(setOf("en", "fr"), ProductImages.languages(product, ImageKind.Front))
        assertNull(product.imageUrl(ImageKind.Packaging))
    }

    @Test
    fun flatFieldsAsLastResort() {
        val p = Product(code = "5449000000996", imageFrontSmallUrl = "https://images.openfoodfacts.org/images/products/544/900/000/0996/front_en.1.200.jpg")
        assertEquals(p.imageFrontSmallUrl, p.imageUrl(size = ImageSize.Display))
        assertEquals(p.imageFrontSmallUrl, p.imageUrl(size = ImageSize.Thumb))
        val q = Product(code = "1", imageFrontUrl = "https://x/front_en.3.400.jpg")
        assertEquals("https://x/front_en.3.full.jpg", q.imageUrl(size = ImageSize.Full))
    }

    @Test
    fun sizes() {
        assertEquals(ImageSize.Small, ImageSize.atLeast(150))
        assertEquals(ImageSize.Display, ImageSize.atLeast(400))
        assertEquals(ImageSize.Full, ImageSize.atLeast(900))
    }
}
