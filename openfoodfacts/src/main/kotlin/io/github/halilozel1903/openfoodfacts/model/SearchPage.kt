package io.github.halilozel1903.openfoodfacts.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One page of search results. */
public data class SearchPage(
    /** Products matching the search, across all pages. */
    val count: Int,
    /** This page, from 1. */
    val page: Int,
    val pageSize: Int,
    val products: List<Product>,
) {
    val pageCount: Int get() = if (pageSize <= 0) 0 else (count + pageSize - 1) / pageSize
    val hasNextPage: Boolean get() = page < pageCount
}

@Serializable
internal data class RawSearchPage(
    @Serializable(with = LenientIntSerializer::class) val count: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val page: Int? = null,
    @SerialName("page_size") @Serializable(with = LenientIntSerializer::class) val pageSize: Int? = null,
    val products: List<Product> = emptyList(),
)

@Serializable
internal data class RawProductResponse(
    val code: String? = null,
    @Serializable(with = LenientIntSerializer::class) val status: Int? = null,
    @SerialName("status_verbose") val statusVerbose: String? = null,
    val product: Product? = null,
)
