package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * PocketBase serializes timestamps as "2026-01-02 18:03:59.507Z" — a space between
 * date and time instead of the ISO-8601 'T'. kotlinx.datetime.Instant.parse only
 * accepts the strict ISO form, so we normalize the space to 'T' before parsing.
 *
 * Empty strings (PocketBase sends "" for unset relations/timestamps) decode to
 * epoch zero so callers can still treat the value as a sentinel.
 */
object PbInstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PbInstant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant {
        val raw = decoder.decodeString().trim()
        if (raw.isEmpty()) return Instant.fromEpochMilliseconds(0)
        val normalized = if (raw.length > 10 && raw[10] == ' ') {
            raw.replaceFirst(' ', 'T')
        } else raw
        return runCatching { Instant.parse(normalized) }
            .getOrElse { Instant.fromEpochMilliseconds(0) }
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }
}
