package com.sage.app.agent

import android.content.Context
import com.sage.app.actions.ActionResult
import com.sage.app.actions.ActionToolExecutor
import com.sage.app.actions.PendingAction
import com.sage.app.data.local.AppDatabase
import com.sage.app.data.local.SecurityPreferences
import com.sage.app.data.remote.ApiResult
import com.sage.app.data.remote.GeminiApiService
import com.sage.app.data.remote.GrokApiService
import com.sage.app.needle.NeedleRouter
import com.sage.app.needle.NeedleToolCatalog

sealed interface OrchestratorResult {
    data class Reply(val text: String): OrchestratorResult
    data class Confirmation(val action: PendingAction): OrchestratorResult
}

class SageOrchestrator(
    context: Context,
    database: AppDatabase,
    security: SecurityPreferences,
    gemini: GeminiApiService,
    grok: GrokApiService,
) {
    private val needle = NeedleRouter(context.applicationContext)
    private val tools = ActionToolExecutor(context.applicationContext, database)
    private val llm = LlmGateway(security, gemini, grok)

    suspend fun handle(userText: String, history: List<Pair<String, String>>): OrchestratorResult {
        val route = needle.route(userText, NeedleToolCatalog.coreSchemas())
        val call = route.functionCalls.firstOrNull()
        if (route.success && call != null && route.confidence.orDefault() >= NeedleRouter.CONFIRM_BELOW_CONFIDENCE) {
            if (call.name == "web_search_answer") {
                val query = call.arguments["query"]?.toString()?.takeIf { it.isNotBlank() } ?: userText
                return when (val result = llm.answer(query, history, webSearch = true)) {
                    is ApiResult.Success -> OrchestratorResult.Reply(result.data)
                    is ApiResult.Error -> OrchestratorResult.Reply(result.message)
                }
            }
            return when (val result = tools.prepare(call.name, call.arguments)) {
                is ActionResult.Reply -> OrchestratorResult.Reply(result.text)
                is ActionResult.Confirmation -> OrchestratorResult.Confirmation(result.action)
            }
        }
        if (route.suppressedCalls?.isNotEmpty() == true || (call != null && route.confidence.orDefault() < NeedleRouter.CONFIRM_BELOW_CONFIDENCE)) {
            return OrchestratorResult.Reply("I’m not confident enough to perform that action. Please rephrase it.")
        }
        return when (val result = llm.answer(userText, history)) {
            is ApiResult.Success -> OrchestratorResult.Reply(result.data)
            is ApiResult.Error -> OrchestratorResult.Reply(result.message)
        }
    }

    suspend fun confirm(action: PendingAction): Result<String> = tools.confirm(action)
    fun close() = needle.close()
    private fun Double?.orDefault() = this ?: 0.0
}
