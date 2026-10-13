package io.github.halilozel1903.openfoodfacts.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

// Open Food Facts returns some numbers as strings ("nova_group": "4", "page": "1") and some as "" when unknown.
// These read either and fall back to null.

internal fun JsonElement?.lenientDouble(): Double? =
    (this as? JsonPrimitive)?.content?.trim()?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it.isFinite() }

internal fun JsonElement?.lenientString(): String? =
    (this as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() && it != "null" }

internal object LenientIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("openfoodfacts.LenientInt", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Int? =
        (decoder as JsonDecoder).decodeJsonElement().lenientDouble()?.toInt()

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeString("") else encoder.encodeString(value.toString())
    }
}

internal object LenientDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("openfoodfacts.LenientDouble", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Double? =
        (decoder as JsonDecoder).decodeJsonElement().lenientDouble()

    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) encoder.encodeString("") else encoder.encodeString(value.toString())
    }
}
