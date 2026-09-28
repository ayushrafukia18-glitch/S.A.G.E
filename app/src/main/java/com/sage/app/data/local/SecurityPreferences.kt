package com.sage.app.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages sensitive user preferences and API keys using EncryptedSharedPreferences (AES-256 GCM).
 *
 * CRITICAL SECURITY POLICY:
 * If EncryptedSharedPreferences fails to initialize on this device, it THROWS an IllegalStateException
 * and refuses to fall back to unencrypted storage.
 */
class SecurityPreferences(context: Context) {

    private val prefs: SharedPreferences = createEncryptedPreferences(context)

    companion object {
        private const val TAG = "SecurityPreferences"
        private const val PREFS_FILE_NAME = "sage_encrypted_prefs"

        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_GEMINI_API_KEY = "key_gemini_api_key"
        private const val KEY_GROK_API_KEY = "key_grok_api_key"
        private const val KEY_GEMINI_KEY_VERIFIED = "key_gemini_key_verified"
        private const val KEY_GROK_KEY_VERIFIED = "key_grok_key_verified"

        /**
         * Checks if hardware/framework encrypted preferences can be securely created.
         */
        fun isSecureStorageAvailable(context: Context): Boolean {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                true
            } catch (_: Exception) {
                false
            }
        }

        @Throws(IllegalStateException::class)
        private fun createEncryptedPreferences(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.e(TAG, "Secure storage unavailable on this device", e)
                throw IllegalStateException("Secure storage unavailable on this device.", e)
            }
        }
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "").orEmpty()
    }

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name.trim()).apply()
    }

    fun getGeminiApiKey(): String {
        return prefs.getString(KEY_GEMINI_API_KEY, "").orEmpty()
    }

    fun setGeminiApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, apiKey.trim()).apply()
    }

    fun getGrokApiKey(): String {
        return prefs.getString(KEY_GROK_API_KEY, "").orEmpty()
    }

    fun setGrokApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GROK_API_KEY, apiKey.trim()).apply()
    }

    fun isGeminiKeyVerified(): Boolean {
        return prefs.getBoolean(KEY_GEMINI_KEY_VERIFIED, false)
    }

    fun setGeminiKeyVerified(verified: Boolean) {
        prefs.edit().putBoolean(KEY_GEMINI_KEY_VERIFIED, verified).apply()
    }

    fun isGrokKeyVerified(): Boolean {
        return prefs.getBoolean(KEY_GROK_KEY_VERIFIED, false)
    }

    fun setGrokKeyVerified(verified: Boolean) {
        prefs.edit().putBoolean(KEY_GROK_KEY_VERIFIED, verified).apply()
    }

    fun hasValidGeminiKey(): Boolean {
        return getGeminiApiKey().isNotBlank()
    }

    fun wipeAllKeys() {
        prefs.edit()
            .remove(KEY_GEMINI_API_KEY)
            .remove(KEY_GROK_API_KEY)
            .remove(KEY_GEMINI_KEY_VERIFIED)
            .remove(KEY_GROK_KEY_VERIFIED)
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
