package com.sage.app

import android.app.Application
import android.util.Log
import com.sage.app.data.local.AppDatabase
import com.sage.app.data.local.SecurityPreferences
import com.sage.app.data.remote.GeminiApiService
import com.sage.app.data.remote.GrokApiService
import com.sage.app.data.repository.ChatRepository
import com.sage.app.data.repository.SettingsRepository
import com.sage.app.agent.SageOrchestrator

class SageApplication : Application() {

    var securityPreferences: SecurityPreferences? = null
        private set

    var secureStorageError: String? = null
        private set

    lateinit var appDatabase: AppDatabase
        private set

    lateinit var geminiApiService: GeminiApiService
        private set

    lateinit var grokApiService: GrokApiService
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var sageOrchestrator: SageOrchestrator
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        appDatabase = AppDatabase.getInstance(this)
        geminiApiService = GeminiApiService()
        grokApiService = GrokApiService()

        try {
            securityPreferences = SecurityPreferences(this)
        } catch (e: Exception) {
            Log.e("SageApplication", "Failed to initialize secure storage", e)
            secureStorageError = "Secure storage unavailable on this device."
        }

        val prefs = securityPreferences
        if (prefs != null) {
            sageOrchestrator = SageOrchestrator(this, appDatabase, prefs, geminiApiService, grokApiService)
            chatRepository = ChatRepository(
                messageDao = appDatabase.messageDao(),
                orchestrator = sageOrchestrator
            )

            settingsRepository = SettingsRepository(
                securityPreferences = prefs,
                geminiApiService = geminiApiService,
                grokApiService = grokApiService
            )
        }
    }

    companion object {
        lateinit var instance: SageApplication
            private set
    }
}
