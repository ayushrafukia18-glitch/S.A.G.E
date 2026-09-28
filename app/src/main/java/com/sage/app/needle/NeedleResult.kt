package com.sage.app.needle

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Mirrors the exact response dict documented in needle-main/llms.txt under
 * "Response shape". Field names and nullability follow that section verbatim:
 *
 * {
 *   "type": "call",
 *   "success": true,
 *   "error": null,
 *   "error_code": null,
 *   "function_calls": [{"name": "set_lights", "arguments": {...}}],
 *   "reasoning": "...",
 *   "confidence": 0.94,
 *   "prefill_tps": 4300.0,
 *   "decode_tps": 850.0
 * }
 *
 * Notes carried over from the source docs, kept here so callers don't have to
 * re-derive them:
 * - `type` is "call" when the model wants tool calls (an empty [functionCalls]
 *   is the model's refusal for off-topic input) or "respond" when the loop is
 *   finished.
 * - `suppressedCalls` holds a call the engine withheld (confidence below 0.1,
 *   or a grounding gate fired); when it's non-null, [functionCalls] is empty.
 *   The documented options are: show it to the user to confirm, or treat the
 *   turn as a refusal. SAGE's confirmation system (see confirmation/) is the
 *   natural place to do the former.
 * - `validation.ungrounded`, when present, lists "tool.field" names whose
 *   value wasn't grounded in the input text.
 */
@JsonClass(generateAdapter = true)
data class NeedleResult(
    @Json(name = "type") val type: String, // "call" | "respond"
    @Json(name = "success") val success: Boolean,
    @Json(name = "error") val error: String? = null,
    @Json(name = "error_code") val errorCode: String? = null,
    @Json(name = "function_calls") val functionCalls: List<NeedleFunctionCall> = emptyList(),
    @Json(name = "reasoning") val reasoning: String? = null,
    @Json(name = "confidence") val confidence: Double? = null,
    @Json(name = "suppressed_calls") val suppressedCalls: List<NeedleFunctionCall>? = null,
    @Json(name = "validation") val validation: NeedleValidation? = null,
    @Json(name = "prefill_tps") val prefillTps: Double? = null,
    @Json(name = "decode_tps") val decodeTps: Double? = null,
)

@JsonClass(generateAdapter = true)
data class NeedleFunctionCall(
    @Json(name = "name") val name: String,
    @Json(name = "arguments") val arguments: Map<String, Any?>,
)

@JsonClass(generateAdapter = true)
data class NeedleValidation(
    @Json(name = "ungrounded") val ungrounded: List<String>? = null,
)

/**
 * Local, app-defined error codes for failures that happen *before* or *around*
 * the engine call (engine unavailable, artifacts missing, JNI not wired up
 * yet). These are NOT part of Needle's own `error_code` vocabulary — keep them
 * clearly distinct so a caller never confuses "Needle refused/errored" with
 * "Needle isn't integrated in this build".
 */
enum class NeedleIntegrationErrorCode {
    NEEDLE_NOT_INTEGRATED,
    NEEDLE_ARTIFACTS_MISSING,
    NEEDLE_ENGINE_LOAD_FAILED,
}
