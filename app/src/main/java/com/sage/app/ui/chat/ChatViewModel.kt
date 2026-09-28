package com.sage.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sage.app.actions.PendingAction
import com.sage.app.data.local.MessageEntity
import com.sage.app.data.repository.ChatDispatchResult
import com.sage.app.data.repository.ChatRepository
import com.sage.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val inputText: String = "",
    val isLoading: Boolean = false,
    val userName: String = "",
    val errorMessage: String? = null,
    val pendingAction: PendingAction? = null,
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val messages: StateFlow<List<MessageEntity>> = chatRepository.messages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _uiState = MutableStateFlow(ChatUiState(userName = settingsRepository.getUserName()))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onInputTextChanged(newText: String) = _uiState.update { it.copy(inputText = newText, errorMessage = null) }

    fun sendMessage() {
        val currentText = _uiState.value.inputText.trim()
        if (currentText.isBlank() || _uiState.value.isLoading) return
        _uiState.update { it.copy(inputText = "", isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = chatRepository.sendMessage(currentText)) {
                is ChatDispatchResult.Success -> _uiState.update { it.copy(isLoading = false) }
                is ChatDispatchResult.Confirmation -> _uiState.update { it.copy(isLoading = false, pendingAction = result.action) }
                is ChatDispatchResult.Error -> _uiState.update { it.copy(isLoading = false, inputText = currentText, errorMessage = result.message) }
            }
        }
    }

    fun confirmPendingAction() {
        val action = _uiState.value.pendingAction ?: return
        _uiState.update { it.copy(pendingAction = null, errorMessage = null, isLoading = true) }
        viewModelScope.launch {
            chatRepository.confirm(action).fold(
                onSuccess = { message -> chatRepository.addAssistantMessage(message) },
                onFailure = { error -> chatRepository.addAssistantMessage("That didn't go through: ${error.message ?: "action failed"}") }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun cancelPendingAction() { _uiState.update { it.copy(pendingAction = null) } }
    fun sendStarterQuestion(question: String) { _uiState.update { it.copy(inputText = question) }; sendMessage() }
    fun refreshUserName() { _uiState.update { it.copy(userName = settingsRepository.getUserName()) } }
    fun clearChat() { viewModelScope.launch { chatRepository.clearChat() } }

    class Factory(private val chatRepository: ChatRepository, private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(chatRepository, settingsRepository) as T
    }
}
