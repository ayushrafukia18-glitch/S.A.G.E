package com.sage.app.data.repository

import com.sage.app.data.local.SecurityPreferences
import com.sage.app.data.remote.ApiResult
import com.sage.app.data.remote.GeminiApiService
import com.sage.app.data.remote.GrokApiService

/**
 * SettingsRepository manages user profile data and API key testing/saving.
 * Direct encrypted local storage only.
 */
class SettingsRepository(
    private val securityPreferences: SecurityPreferences,
    private val geminiApiService: GeminiApiService,
    private val grokApiService: GrokApiService
) {

    fun getUserName(): String = securityPreferences.getUserName()

    fun getGeminiApiKey(): String = securityPreferences.getGeminiApiKey()

    fun getGrokApiKey(): String = securityPreferences.getGrokApiKey()

    fun isGeminiKeyVerified(): Boolean = securityPreferences.isGeminiKeyVerified()

    fun isGrokKeyVerified(): Boolean = securityPreferences.isGrokKeyVerified()

    fun hasValidGeminiKey(): Boolean = securityPreferences.hasValidGeminiKey()

    suspend fun testGeminiKey(apiKey: String): ApiResult<Unit> {
        val result = geminiApiService.verifyKey(apiKey)
        if (result is ApiResult.Success) {
            securityPreferences.setGeminiKeyVerified(true)
        } else {
            securityPreferences.setGeminiKeyVerified(false)
        }
        return result
    }

    suspend fun testGrokKey(apiKey: String): ApiResult<Unit> {
        val result = grokApiService.verifyKey(apiKey)
        if (result is ApiResult.Success) {
            securityPreferences.setGrokKeyVerified(true)
        } else {
            securityPreferences.setGrokKeyVerified(false)
        }
        return result
    }

    fun saveProfile(userName: String, geminiKey: String, grokKey: String) {
        securityPreferences.setUserName(userName)
        securityPreferences.setGeminiApiKey(geminiKey)
        securityPreferences.setGrokApiKey(grokKey)
    }

    fun wipeAllKeys() {
        securityPreferences.wipeAllKeys()
    }
}
