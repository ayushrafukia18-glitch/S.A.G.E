package com.sage.app.agent

import com.sage.app.data.local.SecurityPreferences
import com.sage.app.data.remote.ApiResult
import com.sage.app.data.remote.GeminiApiService
import com.sage.app.data.remote.GrokApiService

class LlmGateway(
    private val security: SecurityPreferences,
    private val gemini: GeminiApiService,
    private val grok: GrokApiService,
) {
    suspend fun answer(prompt: String, history: List<Pair<String, String>>, webSearch: Boolean = false): ApiResult<String> {
        val geminiKey = security.getGeminiApiKey()
        if (geminiKey.isNotBlank()) {
            when (val result = gemini.generateContent(geminiKey, prompt, history, webSearch)) {
                is ApiResult.Success -> return result
                is ApiResult.Error -> if (!result.isInvalidKey && !result.isQuotaExceeded && !result.isNetworkError) return result
            }
        }
        val grokKey = security.getGrokApiKey()
        if (grokKey.isNotBlank()) {
            when (val result = grok.generateContent(grokKey, prompt, history)) {
                is ApiResult.Success -> return if (webSearch) ApiResult.Success("(Live web search wasn't available, so this comes from the model's own knowledge.)\n\n" + result.data) else result
                is ApiResult.Error -> return result
            }
        }
        return ApiResult.Error("No usable AI provider is available. Check your API keys in Settings.")
    }
}
