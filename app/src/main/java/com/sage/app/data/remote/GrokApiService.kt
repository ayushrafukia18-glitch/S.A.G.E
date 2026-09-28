package com.sage.app.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service for xAI / Grok API (Optional secondary provider via BYOK).
 */
class GrokApiService(
    private val client: OkHttpClient = defaultClient()
) {

    companion object {
        private const val TAG = "GrokApiService"
        private const val BASE_URL = "https://api.x.ai/v1"
        private const val MODEL_NAME = "grok-2-latest"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        }
    }

    suspend fun verifyKey(apiKey: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext ApiResult.Error(
                message = "API key cannot be empty",
                isInvalidKey = true
            )
        }

        val url = "$BASE_URL/models"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $trimmedKey")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> ApiResult.Success(Unit)
                    401, 403 -> {
                        ApiResult.Error(
                            message = "Invalid Grok key",
                            statusCode = response.code,
                            isInvalidKey = true
                        )
                    }
                    429 -> {
                        ApiResult.Error(
                            message = "Quota exceeded — add another key or wait",
                            statusCode = 429,
                            isQuotaExceeded = true
                        )
                    }
                    else -> {
                        ApiResult.Error(
                            message = "Verification failed with code ${response.code}",
                            statusCode = response.code
                        )
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network failure while verifying Grok key", e)
            ApiResult.Error(
                message = "No internet connection. Please check your network and try again.",
                isNetworkError = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error verifying Grok key", e)
            ApiResult.Error(message = e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun generateContent(
        apiKey: String,
        prompt: String,
        history: List<Pair<String, String>> = emptyList()
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext ApiResult.Error(
                message = "Invalid Grok key — update it in Settings",
                statusCode = 401,
                isInvalidKey = true
            )
        }

        val url = "$BASE_URL/chat/completions"
        val messagesArray = JSONArray()

        // System prompt
        messagesArray.put(JSONObject().apply {
            put("role", "system")
            put("content", "You are SAGE, an intelligent, concise, and helpful personal AI assistant.")
        })

        // Conversation history
        for ((sender, messageText) in history.takeLast(10)) {
            val role = if (sender.equals("user", ignoreCase = true)) "user" else "assistant"
            messagesArray.put(JSONObject().apply {
                put("role", role)
                put("content", messageText)
            })
        }

        // Current prompt
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })

        val requestPayload = JSONObject().apply {
            put("model", MODEL_NAME)
            put("messages", messagesArray)
            put("temperature", 0.7)
        }

        val requestBody = requestPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $trimmedKey")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                when (response.code) {
                    200 -> {
                        val parsed = parseChatCompletion(bodyString)
                        if (parsed.isNotBlank()) {
                            ApiResult.Success(parsed)
                        } else {
                            ApiResult.Error("Empty response from Grok")
                        }
                    }
                    401, 403 -> ApiResult.Error("Invalid Grok key — update it in Settings", response.code, isInvalidKey = true)
                    429 -> ApiResult.Error("Quota exceeded — add another key or wait", 429, isQuotaExceeded = true)
                    else -> ApiResult.Error("Error from Grok: ${response.code}", response.code)
                }
            }
        } catch (e: IOException) {
            ApiResult.Error("No internet connection. Please check your network and try again.", isNetworkError = true)
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    private fun parseChatCompletion(jsonString: String): String {
        return try {
            val root = JSONObject(jsonString)
            val choices = root.optJSONArray("choices") ?: return ""
            if (choices.length() == 0) return ""
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.optJSONObject("message") ?: return ""
            message.optString("content").trim()
        } catch (_: Exception) {
            ""
        }
    }
}
