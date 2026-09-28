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
 * Client for Gemini REST API using the user's BYOK (Bring Your Own Key) directly.
 */
class GeminiApiService(
    private val client: OkHttpClient = defaultClient()
) {

    companion object {
        private const val TAG = "GeminiApiService"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val MODEL_NAME = "gemini-2.5-flash"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        }
    }

    /**
     * Verifies a Gemini API key using the listModels endpoint.
     */
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
            .header("x-goog-api-key", trimmedKey)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                when (response.code) {
                    200 -> ApiResult.Success(Unit)
                    400, 401, 403 -> {
                        Log.w(TAG, "Gemini verify failed: code ${response.code}, body: $bodyString")
                        ApiResult.Error(
                            message = "Invalid Gemini key",
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
            Log.e(TAG, "Network failure while verifying Gemini key", e)
            ApiResult.Error(
                message = "No internet connection. Please check your network and try again.",
                isNetworkError = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error verifying Gemini key", e)
            ApiResult.Error(message = e.localizedMessage ?: "Unknown error")
        }
    }

    /**
     * Sends prompt to Gemini API and returns the generated content.
     */
    suspend fun generateContent(
        apiKey: String,
        prompt: String,
        history: List<Pair<String, String>> = emptyList(),
        webSearch: Boolean = false
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext ApiResult.Error(
                message = "Invalid Gemini key — update it in Settings",
                statusCode = 401,
                isInvalidKey = true
            )
        }

        val url = "$BASE_URL/models/$MODEL_NAME:generateContent"

        // Build contents array with conversation history and latest prompt
        val contentsArray = JSONArray()

        // Include prior message turns if available (up to last 10 messages for context)
        val recentHistory = history.takeLast(10)
        for ((sender, messageText) in recentHistory) {
            val role = if (sender.equals("user", ignoreCase = true)) "user" else "model"
            val turnObj = JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", messageText) })
                })
            }
            contentsArray.put(turnObj)
        }

        // Add the current prompt
        val currentPromptObj = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", prompt) })
            })
        }
        contentsArray.put(currentPromptObj)

        val requestPayload = JSONObject().apply {
            put("contents", contentsArray)
            if (webSearch) put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "You are SAGE, an intelligent, concise, and helpful personal AI assistant. Provide direct, accurate, and friendly answers.")
                    })
                })
            })
        }

        val requestBody = requestPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", trimmedKey)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                when (response.code) {
                    200 -> {
                        val parsedText = parseCandidateText(bodyString)
                        if (parsedText.isNotBlank()) {
                            ApiResult.Success(parsedText)
                        } else {
                            ApiResult.Error(message = "Received empty response from SAGE")
                        }
                    }
                    401 -> {
                        ApiResult.Error(
                            message = "Invalid Gemini key — update it in Settings",
                            statusCode = 401,
                            isInvalidKey = true
                        )
                    }
                    400 -> {
                        if (bodyString.contains("API_KEY_INVALID", ignoreCase = true) ||
                            bodyString.contains("API key not valid", ignoreCase = true)
                        ) {
                            ApiResult.Error(
                                message = "Invalid Gemini key — update it in Settings",
                                statusCode = 400,
                                isInvalidKey = true
                            )
                        } else {
                            ApiResult.Error(
                                message = extractErrorMessage(bodyString, "Bad request to Gemini API"),
                                statusCode = 400
                            )
                        }
                    }
                    403 -> {
                        ApiResult.Error(
                            message = "Invalid Gemini key — update it in Settings",
                            statusCode = 403,
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
                        val errorMsg = extractErrorMessage(bodyString, "Error from Gemini (${response.code})")
                        ApiResult.Error(
                            message = errorMsg,
                            statusCode = response.code
                        )
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network failure calling Gemini", e)
            ApiResult.Error(
                message = "No internet connection. Please check your network and try again.",
                isNetworkError = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error generating content", e)
            ApiResult.Error(message = e.localizedMessage ?: "Unknown error")
        }
    }

    private fun parseCandidateText(jsonString: String): String {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val text = part.optString("text")
                if (text.isNotEmpty()) {
                    sb.append(text)
                }
            }
            sb.toString().trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing candidate text", e)
            ""
        }
    }

    private fun extractErrorMessage(jsonString: String, fallback: String): String {
        return try {
            val root = JSONObject(jsonString)
            val error = root.optJSONObject("error")
            error?.optString("message")?.takeIf { it.isNotBlank() } ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }
}
