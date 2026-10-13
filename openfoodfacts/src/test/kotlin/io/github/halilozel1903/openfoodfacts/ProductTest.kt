package io.github.halilozel1903.openfoodfacts

import io.github.halilozel1903.openfoodfacts.model.Allergen
import io.github.halilozel1903.openfoodfacts.model.GreenScore
import io.github.halilozel1903.openfoodfacts.model.NovaGroup
import io.github.halilozel1903.openfoodfacts.model.Nutrient
import io.github.halilozel1903.openfoodfacts.model.NutrientLevel
import io.github.halilozel1903.openfoodfacts.model.NutriScore
import io.github.halilozel1903.openfoodfacts.model.Presence
import io.github.halilozel1903.openfoodfacts.model.Product
import io.github.halilozel1903.openfoodfacts.model.RawProductResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProductTest {
    private val product: Product =
        OpenFoodFactsClient.json.decodeFromString(RawProductResponse.serializer(), Fixtures.read("product-3017620422003.json")).product!!

    @Test
    fun namesAndBrands() {
        assertEquals("Nutella", product.displayName)
        assertEquals(listOf("Nutella", "Ferrero"), product.brandList)
        assertEquals("Nutella", product.brand)
        assertEquals("400 g", product.quantity)
    }

    @Test
    fun scores() {
        assertEquals(NutriScore.E, product.nutriScore)
        assertEquals("E", product.nutriScore.letter)
        assertEquals(26, product.nutriscoreScore)
        assertEquals(NovaGroup.UltraProcessed, product.nova)
        assertEquals(GreenScore.D, product.greenScore)
    }

    @Test
    fun nutritionPer100g() {
        val facts = product.nutriments.per100g
        assertEquals(539.0, facts.energyKcal)
        assertEquals(2252.0, facts.energyKj)
        assertEquals(30.9, facts.fat)
        assertEquals(10.6, facts.saturatedFat)
        assertEquals(56.3, facts.sugars)
        assertEquals(6.3, facts.proteins)
        assertEquals(0.107, facts.salt)
        // "" in the response means unknown.
        assertNull(facts.fiber)
        assertEquals(8.45, product.nutriments.perServing(Nutrient.Sugars))
        assertEquals("g", product.nutriments.unit("salt"))
        assertTrue("nova-group" in product.nutriments.keys)
        assertEquals(NutrientLevel.High, product.nutrientLevel(Nutrient.Sugars))
        assertEquals(NutrientLevel.Low, product.nutrientLevel(Nutrient.Salt))
        assertNull(product.nutrientLevel(Nutrient.Proteins))
    }

    @Test
    fun allergensAndTraces() {
        assertEquals(listOf("Milk", "Nuts", "Soybeans"), product.allergens.map { it.displayName })
        assertEquals(listOf(Allergen.Milk, Allergen.Nuts, Allergen.Soybeans), product.allergens.mapNotNull(Allergen::fromTag))
        assertTrue(product.contains(Allergen.Milk))
        assertFalse(product.contains(Allergen.Gluten))
        assertFalse(product.mayContain(Allergen.Peanuts))
        assertEquals("en", product.allergens.first().language)
    }

    @Test
    fun ingredients() {
        assertEquals(8, product.ingredients.size)
        val hazelnut = product.ingredients[2]
        assertEquals("NOISETTES", hazelnut.name)
        assertEquals(13.0, hazelnut.bestPercent)
        // Percent given as a string.
        assertEquals(7.4, product.ingredients[3].percent)
        assertEquals(Presence.Yes, product.ingredients[1].fromPalmOil)
        assertEquals(Presence.No, product.ingredients[4].vegan)
        val flat = product.ingredients.flatMap { it.flatten() }
        assertTrue(flat.any { it.id == "en:soya" })
        assertEquals(10, flat.size)
        val analysis = product.ingredientsAnalysis
        assertEquals(Presence.Yes, analysis.palmOil)
        assertEquals(Presence.No, analysis.vegan)
        assertEquals(Presence.Yes, analysis.vegetarian)
        assertTrue(product.ingredientsLabel!!.startsWith("Sucre"))
    }

    @Test
    fun scoreParsing() {
        assertEquals(NutriScore.Unknown, NutriScore.fromApi(null))
        assertEquals(NutriScore.NotApplicable, NutriScore.fromApi("not-applicable"))
        assertEquals(NutriScore.A, NutriScore.fromApi(" A "))
        assertFalse(NutriScore.Unknown.isGraded)
        assertEquals(GreenScore.APlus, GreenScore.fromApi("a-plus"))
        assertNull(NovaGroup.fromGroup(7))
        // environmental_score_grade wins over the legacy field, which is the fallback.
        assertEquals(GreenScore.B, Product(ecoscoreGrade = "b").greenScore)
        assertEquals(GreenScore.APlus, Product(environmentalScoreGrade = "a-plus", ecoscoreGrade = "b").greenScore)
    }

    @Test
    fun derivedEnergyAndSalt() {
        val p = Product(
            nutrimentsRaw = mapOf(
                "energy_100g" to kotlinx.serialization.json.JsonPrimitive(418.4),
                "sodium_100g" to kotlinx.serialization.json.JsonPrimitive("0.4"),
            ),
        )
        val facts = p.nutriments.per100g
        assertEquals(418.4, facts.energyKj)
        assertEquals(100.0, facts.energyKcal!!, 1e-9)
        assertEquals(1.0, facts.salt!!, 1e-9)
        assertTrue(Product().nutriments.per100g.isEmpty)
    }
}
