package com.sage.app.data.repository

import com.sage.app.agent.OrchestratorResult
import com.sage.app.agent.SageOrchestrator
import com.sage.app.actions.PendingAction
import com.sage.app.data.local.MessageDao
import com.sage.app.data.local.MessageEntity
import com.sage.app.data.remote.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ChatRepository(
    private val messageDao: MessageDao,
    private val orchestrator: SageOrchestrator,
) {
    val messages: Flow<List<MessageEntity>> = messageDao.getAllMessages()

    suspend fun sendMessage(userText: String): ChatDispatchResult {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return ChatDispatchResult.Error("Message cannot be empty")
        messageDao.insertMessage(MessageEntity(sender = "user", text = trimmed))
        val history = messageDao.getAllMessages().firstOrNull().orEmpty()
            .dropLast(1).takeLast(10).map { it.sender to it.text }
        return when (val result = orchestrator.handle(trimmed, history)) {
            is OrchestratorResult.Reply -> {
                messageDao.insertMessage(MessageEntity(sender = "sage", text = result.text))
                ChatDispatchResult.Success(result.text)
            }
            is OrchestratorResult.Confirmation -> ChatDispatchResult.Confirmation(result.action)
        }
    }

    suspend fun confirm(action: PendingAction): Result<String> = orchestrator.confirm(action)

    suspend fun addAssistantMessage(text: String) = messageDao.insertMessage(MessageEntity(sender = "sage", text = text))

    suspend fun clearChat() = messageDao.clearAllMessages()
}

sealed interface ChatDispatchResult {
    data class Success(val text: String): ChatDispatchResult
    data class Confirmation(val action: PendingAction): ChatDispatchResult
    data class Error(val message: String): ChatDispatchResult
}
