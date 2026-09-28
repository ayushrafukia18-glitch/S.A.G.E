package com.sage.app.needle

/**
 * The abstraction boundary between SAGE's Kotlin layer and the Needle 3 native
 * router. Exactly one implementation is active at a time, selected at startup
 * by [NeedleRouter]:
 *
 * - [NeedleNativeEngine] — real native routing, once the platform engine +
 *   header have been vendored in per docs/NEEDLE_INTEGRATION.md. Currently a
 *   documented non-functional boundary (see that class's KDoc).
 * - [NeedleUnavailableEngine] — always-available fallback used today, and
 *   whenever the native artifacts aren't present on a given build/device.
 *
 * [NeedleRouter] depends only on this interface, never on a concrete engine,
 * so SageOrchestrator's control flow doesn't change when native integration
 * lands.
 */
interface NeedleEngine {

    /**
     * Prepares the engine for use (loading the native library / model weights
     * where applicable). Must be safe to call once per process lifetime.
     * Returns failure rather than throwing when the engine can't be prepared;
     * callers must not crash the app on a Needle initialization failure.
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Whether this engine can currently route requests. False for
     * [NeedleUnavailableEngine] always; false for [NeedleNativeEngine] until
     * the native artifacts are present and [initialize] has succeeded.
     */
    fun isAvailable(): Boolean

    /**
     * Routes a single user utterance against the given tool schemas.
     *
     * @param request the raw user utterance (already transcribed, if it came
     *   from voice).
     * @param tools schemas pulled from [com.sage.app.tools.ToolRegistry],
     *   already in Needle's raw JSON-schema shape.
     * @param system optional environment-facts string, per llms.txt's "system
     *   facts" — e.g. current date/time. Pass null to let the engine's own
     *   `auto_date` behavior apply, per the documented default.
     */
    suspend fun route(
        request: String,
        tools: List<NeedleToolSchema>,
        system: String? = null,
    ): NeedleResult

    /** Releases any native resources. Safe to call multiple times. */
    fun close()
}
