package com.sage.app.needle

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Mirrors the "raw JSON schema" tool format documented in needle-main/llms.txt
 * ("Defining tools (three equivalent ways)" -> "Raw JSON schema (what the engine
 * actually consumes)"). This is the exact shape Needle's engine expects — do not
 * add fields beyond what's documented there.
 *
 * Example from the Needle source:
 * {
 *   "name": "set_lights",
 *   "description": "Turn a room's lights on/off and set brightness",
 *   "parameters": {
 *     "type": "object",
 *     "properties": { "room": {"type": "string"}, ... },
 *     "required": ["room", "on"]
 *   }
 * }
 *
 * SAGE's own [com.sage.app.tools.Tool] schemas already use this exact shape
 * (see docs/TOOLS.md), so ToolRegistry -> NeedleToolSchema is a direct mapping,
 * not a translation.
 */
@JsonClass(generateAdapter = true)
data class NeedleToolSchema(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "parameters") val parameters: NeedleJsonSchemaObject,
    /**
     * Optional. Needle's source documents `"triggers"` as a list of case-insensitive
     * regexes that force a tool call even below the confidence floor. Only set this
     * if a specific tool genuinely needs a hard trigger (e.g. an explicit safety word);
     * do not add triggers speculatively.
     */
    @Json(name = "triggers") val triggers: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class NeedleJsonSchemaObject(
    @Json(name = "type") val type: String = "object",
    @Json(name = "properties") val properties: Map<String, NeedleJsonSchemaProperty>,
    @Json(name = "required") val required: List<String> = emptyList(),
)

/**
 * A single property in a tool's parameter schema. Only the constraint keywords
 * Needle's source explicitly documents (needle.Field: enum, const, ge/le/gt/lt,
 * multiple_of, min_length/max_length, pattern, format, min_items/max_items,
 * unique_items) are modeled here. Everything is nullable because a given
 * property only uses the constraints relevant to its type.
 */
@JsonClass(generateAdapter = true)
data class NeedleJsonSchemaProperty(
    @Json(name = "type") val type: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "enum") val enum: List<String>? = null,
    @Json(name = "minimum") val minimum: Double? = null,
    @Json(name = "maximum") val maximum: Double? = null,
    @Json(name = "exclusiveMinimum") val exclusiveMinimum: Double? = null,
    @Json(name = "exclusiveMaximum") val exclusiveMaximum: Double? = null,
    @Json(name = "multipleOf") val multipleOf: Double? = null,
    @Json(name = "minLength") val minLength: Int? = null,
    @Json(name = "maxLength") val maxLength: Int? = null,
    @Json(name = "pattern") val pattern: String? = null,
    @Json(name = "format") val format: String? = null,
    @Json(name = "minItems") val minItems: Int? = null,
    @Json(name = "maxItems") val maxItems: Int? = null,
    @Json(name = "uniqueItems") val uniqueItems: Boolean? = null,
)
