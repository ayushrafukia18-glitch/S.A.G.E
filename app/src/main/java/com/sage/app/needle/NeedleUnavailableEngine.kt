package com.sage.app.needle

/**
 * The engine SAGE actually runs on today. It never claims to route a request
 * to a tool — [route] always returns a "respond" result with no function
 * calls, so [com.sage.app.agent.SageOrchestrator] falls through to
 * [com.sage.app.llm.LlmGateway] for every request, exactly as it should when
 * no local action router is present (see PRODUCT VISION: "If it is not an
 * action -> Gemini/Grok handles the conversational/knowledge request").
 *
 * This is not a placeholder that fakes success — [isAvailable] is honestly
 * false, and [route] is honestly a no-op. This satisfies "the app must
 * continue to compile" and "do NOT fake Needle results" without pretending
 * local routing exists.
 */
class NeedleUnavailableEngine(
    private val reason: String = "Needle native engine is not integrated in this build.",
) : NeedleEngine {

    override suspend fun initialize(): Result<Unit> = Result.success(Unit)

    override fun isAvailable(): Boolean = false

    override suspend fun route(
        request: String,
        tools: List<NeedleToolSchema>,
        system: String?,
    ): NeedleResult = NeedleResult(
        type = "respond",
        success = false,
        error = reason,
        errorCode = NeedleIntegrationErrorCode.NEEDLE_NOT_INTEGRATED.name,
        functionCalls = emptyList(),
    )

    override fun close() {
        // No native resources held.
    }
}
