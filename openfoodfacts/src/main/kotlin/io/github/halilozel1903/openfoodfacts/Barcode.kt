package io.github.halilozel1903.openfoodfacts

/** Barcode helpers: clean up typed or scanned codes and check GTIN check digits. */
public object Barcode {

    /** Removes spaces and dashes. Returns `null` when anything other than digits remains. */
    public fun normalize(input: String): String? {
        val code = input.filterNot { it.isWhitespace() || it == '-' }
        return code.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
    }

    /**
     * `true` for an EAN-8, UPC-A (12), EAN-13 or GTIN-14 whose check digit is right. Open Food Facts also stores
     * shop specific codes that are not GTINs, so the client does not require this to look a product up.
     */
    public fun isValidGtin(code: String): Boolean {
        if (code.length !in setOf(8, 12, 13, 14) || !code.all(Char::isDigit)) return false
        return checkDigit(code.dropLast(1)) == code.last().digitToInt()
    }

    /** The GTIN check digit for [body], the code without its last digit. */
    public fun checkDigit(body: String): Int {
        require(body.all(Char::isDigit)) { "digits only" }
        val sum = body.reversed().mapIndexed { index, c -> c.digitToInt() * if (index % 2 == 0) 3 else 1 }.sum()
        return (10 - sum % 10) % 10
    }

    /**
     * The folder of a product's images on `images.openfoodfacts.org`: the code left padded with zeros to 13 digits
     * and split as `3/3/3/rest` (`3017620422003` becomes `301/762/042/2003`). Codes of 8 digits or fewer are used
     * as is.
     */
    public fun imageFolder(code: String): String {
        if (code.length <= 8) return code
        val padded = code.padStart(13, '0')
        return listOf(padded.substring(0, 3), padded.substring(3, 6), padded.substring(6, 9), padded.substring(9)).joinToString("/")
    }
}
