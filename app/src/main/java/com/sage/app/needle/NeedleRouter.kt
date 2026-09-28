package com.sage.app.needle

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Selects the active [NeedleEngine] and exposes local-action routing to
 * [com.sage.app.agent.SageOrchestrator]. This is the only place that decides
 * which concrete engine backs Needle at runtime, so the rest of the app never
 * branches on "is native available".
 *
 * Today [activeEngine] always resolves to [NeedleUnavailableEngine] because
 * [NeedleNativeEngine.initialize] always fails (honestly — see its KDoc).
 * Once native integration is completed, this class needs no changes: it
 * already prefers the native engine whenever `initialize()` succeeds.
 */
class NeedleRouter(context: Context) {

    private val nativeEngine: NeedleEngine = NeedleNativeEngine(context)
    private val fallbackEngine: NeedleEngine = NeedleUnavailableEngine()

    private val initLock = Mutex()
    private var resolvedEngine: NeedleEngine? = null

    /** Lazily resolves and caches the engine to use for this process. */
    private suspend fun activeEngine(): NeedleEngine {
        resolvedEngine?.let { return it }
        return initLock.withLock {
            resolvedEngine?.let { return it }
            val chosen = if (nativeEngine.initialize().isSuccess) nativeEngine else fallbackEngine
            resolvedEngine = chosen
            chosen
        }
    }

    /** True only when local, on-device action routing is actually usable. */
    suspend fun isLocalRoutingAvailable(): Boolean = activeEngine().isAvailable()

    /**
     * Routes [request] against [tools]. Never throws — engine failures come
     * back as a [NeedleResult] with `success = false`, so
     * [com.sage.app.agent.SageOrchestrator] can uniformly fall through to
     * [com.sage.app.llm.LlmGateway] on any non-success result.
     */
    suspend fun route(
        request: String,
        tools: List<NeedleToolSchema>,
        system: String? = null,
    ): NeedleResult = activeEngine().route(request, tools, system)

    fun close() {
        nativeEngine.close()
        fallbackEngine.close()
    }

    companion object {
        /**
         * App-level policy, NOT part of Needle's own behavior: Needle's
         * documented confidence floor (below which it withholds a call into
         * `suppressed_calls`) is 0.1. SAGE additionally requires confirmation
         * — rather than silent execution — for any call at or below this
         * higher bar, on top of the mandatory confirmation for call/SMS tools
         * (see confirmation/ConfirmationManager.kt). Tune per product
         * decision; this is ours, not Needle's.
         */
        const val CONFIRM_BELOW_CONFIDENCE = 0.55
    }
}
