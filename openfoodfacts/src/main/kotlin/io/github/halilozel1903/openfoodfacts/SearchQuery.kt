package io.github.halilozel1903.openfoodfacts

import io.github.halilozel1903.openfoodfacts.model.NutriScore

/** Sort orders of `/api/v2/search` (`sort_by`). */
public enum class SearchSort(public val apiValue: String) {
    /** Most scanned first. */
    Popularity("popularity_key"),
    ProductName("product_name"),
    /** Newest first. */
    Created("created_t"),
    /** Recently edited first. */
    LastModified("last_modified_t"),
    /** Best Nutri-Score first. */
    NutriScore("nutriscore_score"),
    /** Best Green-Score first. */
    GreenScore("ecoscore_score"),
    /** Most complete data first. */
    Completeness("completeness"),
}

/**
 * A structured search (`GET /api/v2/search`): products in categories, with a brand or label, in a country, with
 * some Nutri-Score grades, sorted. Every filter is optional; values in one list are combined with AND, except
 * [nutritionGrades], which matches any of the grades.
 *
 * Tags may be given with or without their language prefix: `"breakfast-cereals"` means `"en:breakfast-cereals"`.
 *
 * ```kotlin
 * SearchQuery(
 *     categories = listOf("breakfast-cereals"),
 *     nutritionGrades = setOf(NutriScore.A, NutriScore.B),
 *     sortBy = SearchSort.Popularity,
 *     pageSize = 20,
 * )
 * ```
 */
public data class SearchQuery(
    val categories: List<String> = emptyList(),
    val brands: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val nutritionGrades: Set<NutriScore> = emptySet(),
    val sortBy: SearchSort? = SearchSort.Popularity,
    /** The page, from 1. */
    val page: Int = 1,
    /** Products per page, at most 100. */
    val pageSize: Int = 24,
    /** The fields of each product; `null` for all of them. */
    val fields: List<String>? = ProductFields.Summary,
    /** Any other `/api/v2/search` parameter, sent as is. */
    val extraParameters: Map<String, String> = emptyMap(),
) {
    init {
        require(page >= 1) { "page starts at 1" }
        require(pageSize in 1..100) { "pageSize must be 1 to 100" }
        require(nutritionGrades.all { it.isGraded }) { "nutritionGrades takes A to E" }
    }

    /** The same query, one page further. */
    public fun next(): SearchQuery = copy(page = page + 1)

    internal fun parameters(): List<Pair<String, String?>> = buildList<Pair<String, String?>> {
        add("categories_tags" to categories.tags())
        add("brands_tags" to brands.tags(prefix = false))
        add("labels_tags" to labels.tags())
        add("countries_tags" to countries.tags())
        add("nutrition_grades_tags" to nutritionGrades.sortedBy { it.ordinal }.joinToString("|") { it.apiValue }.ifEmpty { null })
        add("sort_by" to sortBy?.apiValue)
        add("page" to page.toString())
        add("page_size" to pageSize.toString())
        add("fields" to fields?.joinToString(","))
        extraParameters.forEach { (name, value) -> add(name to value) }
    }

    private fun List<String>.tags(prefix: Boolean = true): String? =
        map { it.trim() }.filter { it.isNotEmpty() }
            .map { if (prefix && ':' !in it) "en:$it" else it }
            .joinToString(",").ifEmpty { null }
}
