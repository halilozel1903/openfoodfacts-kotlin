package io.github.halilozel1903.openfoodfacts.internal

import java.net.URLEncoder

/** `a=1&b=x%2Cy`, skipping `null` values, in the given order. */
internal fun queryString(params: List<Pair<String, String?>>): String =
    params.filter { it.second != null }.joinToString("&") { (name, value) -> "${encode(name)}=${encode(value!!)}" }

internal fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")
