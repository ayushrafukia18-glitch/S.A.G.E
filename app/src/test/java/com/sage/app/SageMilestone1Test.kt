package com.sage.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sage.app.data.local.AppDatabase
import com.sage.app.data.local.MessageDao
import com.sage.app.data.local.MessageEntity
import com.sage.app.data.local.SecurityPreferences
import com.sage.app.data.remote.ApiResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SageMilestone1Test {

    private lateinit var database: AppDatabase
    private lateinit var messageDao: MessageDao
    private lateinit var context: Context
    private lateinit var securityPreferences: SecurityPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        messageDao = database.messageDao()
        securityPreferences = SecurityPreferences(context)
        securityPreferences.clearAll()
    }

    @After
    fun tearDown() {
        database.close()
        securityPreferences.clearAll()
    }

    @Test
    fun testRoomDatabaseMessagePersistenceAndRestoration() = runBlocking {
        // Given user question and Sage answer
        val userMsg = MessageEntity(
            sender = "user",
            text = "What is the capital of Norway?",
            timestamp = 1000L
        )
        val sageMsg = MessageEntity(
            sender = "sage",
            text = "The capital of Norway is Oslo.",
            timestamp = 2000L
        )

        messageDao.insertMessage(userMsg)
        messageDao.insertMessage(sageMsg)

        // When retrieving messages from database (simulating app restart)
        val loadedMessages = messageDao.getAllMessages().first()

        // Then verify all messages and attributes are faithfully persisted
        assertEquals(2, loadedMessages.size)
        assertEquals("user", loadedMessages[0].sender)
        assertEquals("What is the capital of Norway?", loadedMessages[0].text)
        assertTrue(loadedMessages[0].isUser)

        assertEquals("sage", loadedMessages[1].sender)
        assertEquals("The capital of Norway is Oslo.", loadedMessages[1].text)
        assertFalse(loadedMessages[1].isUser)
    }

    @Test
    fun testSecurityPreferencesKeyLifecycle() {
        // Initial state: no valid key
        assertFalse(securityPreferences.hasValidGeminiKey())
        assertEquals("", securityPreferences.getGeminiApiKey())

        // Save keys
        securityPreferences.setUserName("Alex")
        securityPreferences.setGeminiApiKey("AIzaSyTestGeminiKey123")
        securityPreferences.setGrokApiKey("xai-TestGrokKey456")

        assertTrue(securityPreferences.hasValidGeminiKey())
        assertEquals("Alex", securityPreferences.getUserName())
        assertEquals("AIzaSyTestGeminiKey123", securityPreferences.getGeminiApiKey())
        assertEquals("xai-TestGrokKey456", securityPreferences.getGrokApiKey())

        // Wipe keys
        securityPreferences.wipeAllKeys()
        assertFalse(securityPreferences.hasValidGeminiKey())
        assertEquals("", securityPreferences.getGeminiApiKey())
        assertEquals("", securityPreferences.getGrokApiKey())
    }

    @Test
    fun testApiResultErrorClassification() {
        val error401 = ApiResult.Error(
            message = "Invalid Gemini key — update it in Settings",
            statusCode = 401,
            isInvalidKey = true
        )
        assertTrue(error401.isInvalidKey)
        assertEquals("Invalid Gemini key — update it in Settings", error401.message)

        val error429 = ApiResult.Error(
            message = "Quota exceeded — add another key or wait",
            statusCode = 429,
            isQuotaExceeded = true
        )
        assertTrue(error429.isQuotaExceeded)
        assertEquals("Quota exceeded — add another key or wait", error429.message)

        val errorOffline = ApiResult.Error(
            message = "No internet connection. Please check your network and try again.",
            isNetworkError = true
        )
        assertTrue(errorOffline.isNetworkError)
        assertEquals("No internet connection. Please check your network and try again.", errorOffline.message)
    }
}
