package com.sage.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sage.app.data.remote.ApiResult
import com.sage.app.data.repository.ChatRepository
import com.sage.app.data.repository.SettingsRepository
import com.sage.app.ui.components.KeyTestState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val userName: String = "",
    val geminiKey: String = "",
    val grokKey: String = "",
    val geminiTestState: KeyTestState = KeyTestState.Idle,
    val grokTestState: KeyTestState = KeyTestState.Idle,
    val isSavedSuccess: Boolean = false,
    val isWiped: Boolean = false,
    val isChatCleared: Boolean = false,
    val showWipeConfirmDialog: Boolean = false,
    val showClearChatDialog: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            userName = settingsRepository.getUserName(),
            geminiKey = settingsRepository.getGeminiApiKey(),
            grokKey = settingsRepository.getGrokApiKey(),
            geminiTestState = if (settingsRepository.isGeminiKeyVerified()) {
                KeyTestState.Success("Stored key verified")
            } else KeyTestState.Idle,
            grokTestState = if (settingsRepository.isGrokKeyVerified()) {
                KeyTestState.Success("Stored key verified")
            } else KeyTestState.Idle
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onUserNameChanged(name: String) {
        _uiState.update { it.copy(userName = name, isSavedSuccess = false) }
    }

    fun onGeminiKeyChanged(key: String) {
        _uiState.update {
            it.copy(
                geminiKey = key,
                geminiTestState = KeyTestState.Idle,
                isSavedSuccess = false
            )
        }
    }

    fun onGrokKeyChanged(key: String) {
        _uiState.update {
            it.copy(
                grokKey = key,
                grokTestState = KeyTestState.Idle,
                isSavedSuccess = false
            )
        }
    }

    fun testGeminiKey() {
        val key = _uiState.value.geminiKey.trim()
        if (key.isBlank()) {
            _uiState.update {
                it.copy(geminiTestState = KeyTestState.Error("API key cannot be empty"))
            }
            return
        }

        _uiState.update { it.copy(geminiTestState = KeyTestState.Testing) }

        viewModelScope.launch {
            when (val result = settingsRepository.testGeminiKey(key)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(geminiTestState = KeyTestState.Success("Valid key — Gemini ready"))
                    }
                }
                is ApiResult.Error -> {
                    val message = when {
                        result.isInvalidKey -> "Invalid Gemini key"
                        result.isQuotaExceeded -> "Quota exceeded — add another key or wait"
                        result.isNetworkError -> "No internet connection. Please check your network and try again."
                        else -> result.message
                    }
                    _uiState.update {
                        it.copy(geminiTestState = KeyTestState.Error(message))
                    }
                }
            }
        }
    }

    fun testGrokKey() {
        val key = _uiState.value.grokKey.trim()
        if (key.isBlank()) {
            _uiState.update {
                it.copy(grokTestState = KeyTestState.Error("API key cannot be empty"))
            }
            return
        }

        _uiState.update { it.copy(grokTestState = KeyTestState.Testing) }

        viewModelScope.launch {
            when (val result = settingsRepository.testGrokKey(key)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(grokTestState = KeyTestState.Success("Valid key — Grok ready"))
                    }
                }
                is ApiResult.Error -> {
                    val message = when {
                        result.isInvalidKey -> "Invalid Grok key"
                        result.isQuotaExceeded -> "Quota exceeded — add another key or wait"
                        result.isNetworkError -> "No internet connection. Please check your network and try again."
                        else -> result.message
                    }
                    _uiState.update {
                        it.copy(grokTestState = KeyTestState.Error(message))
                    }
                }
            }
        }
    }

    fun saveSettings() {
        val state = _uiState.value
        viewModelScope.launch {
            settingsRepository.saveProfile(
                userName = state.userName.trim(),
                geminiKey = state.geminiKey.trim(),
                grokKey = state.grokKey.trim()
            )
            _uiState.update { it.copy(isSavedSuccess = true) }
        }
    }

    fun showWipeDialog(show: Boolean) {
        _uiState.update { it.copy(showWipeConfirmDialog = show) }
    }

    fun showClearChatDialog(show: Boolean) {
        _uiState.update { it.copy(showClearChatDialog = show) }
    }

    fun wipeKeys() {
        settingsRepository.wipeAllKeys()
        _uiState.update {
            it.copy(
                geminiKey = "",
                grokKey = "",
                geminiTestState = KeyTestState.Idle,
                grokTestState = KeyTestState.Idle,
                isWiped = true,
                showWipeConfirmDialog = false
            )
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.clearChat()
            _uiState.update {
                it.copy(
                    isChatCleared = true,
                    showClearChatDialog = false
                )
            }
        }
    }

    fun hasValidGeminiKey(): Boolean {
        return settingsRepository.hasValidGeminiKey()
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val chatRepository: ChatRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository, chatRepository) as T
        }
    }
}
