package io.github.halilozel1903.openfoodfacts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BarcodeTest {
    @Test
    fun normalize() {
        assertEquals("3017620422003", Barcode.normalize(" 3017 6204-22003 "))
        assertNull(Barcode.normalize("30176abc"))
        assertNull(Barcode.normalize("  "))
    }

    @Test
    fun checkDigits() {
        assertTrue(Barcode.isValidGtin("3017620422003"))
        assertTrue(Barcode.isValidGtin("5449000000996"))
        assertTrue(Barcode.isValidGtin("96385074"))
        assertTrue(Barcode.isValidGtin("036000291452"))
        assertFalse(Barcode.isValidGtin("3017620422004"))
        assertFalse(Barcode.isValidGtin("12345"))
        assertEquals(3, Barcode.checkDigit("301762042200"))
    }

    @Test
    fun imageFolder() {
        assertEquals("301/762/042/2003", Barcode.imageFolder("3017620422003"))
        assertEquals("003/600/029/1452", Barcode.imageFolder("036000291452"))
        assertEquals("96385074", Barcode.imageFolder("96385074"))
    }
}
