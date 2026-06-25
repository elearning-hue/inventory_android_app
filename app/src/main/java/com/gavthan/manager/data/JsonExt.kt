package com.gavthan.manager.data

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Tolerant accessors that mirror the web app's dynamic JS handling: a column
 * may arrive as a number, a string or null and we never want a hard crash.
 */

fun JsonElement?.asStringOrNull(): String? = when (this) {
    null, is JsonNull -> null
    is JsonPrimitive -> content
    else -> toString()
}

fun JsonObject.string(key: String): String? = this[key].asStringOrNull()

fun JsonObject.double(key: String): Double? = when (val e = this[key]) {
    null, is JsonNull -> null
    is JsonPrimitive -> e.content.toDoubleOrNull()
    else -> null
}

fun JsonObject.bool(key: String): Boolean? = when (val e = this[key]) {
    null, is JsonNull -> null
    is JsonPrimitive -> e.booleanOrNull ?: when (e.content.trim().lowercase()) {
        "true", "t", "1", "yes" -> true
        "false", "f", "0", "no" -> false
        else -> null
    }
    else -> null
}

/** Mirrors JS `num(v)` — coerce anything to a finite double, defaulting to 0. */
fun num(v: Any?): Double = when (v) {
    null -> 0.0
    is Number -> v.toDouble()
    is JsonPrimitive -> v.content.toDoubleOrNull() ?: 0.0
    is String -> v.toDoubleOrNull() ?: 0.0
    else -> 0.0
}
