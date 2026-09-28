package com.sage.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sage.app.data.remote.ApiResult
import com.sage.app.data.repository.SettingsRepository
import com.sage.app.ui.components.KeyTestState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val userName: String = "",
    val geminiKey: String = "",
    val grokKey: String = "",
    val geminiTestState: KeyTestState = KeyTestState.Idle,
    val grokTestState: KeyTestState = KeyTestState.Idle,
    val isGeminiKeyVerified: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            userName = settingsRepository.getUserName(),
            geminiKey = settingsRepository.getGeminiApiKey(),
            grokKey = settingsRepository.getGrokApiKey(),
            isGeminiKeyVerified = settingsRepository.isGeminiKeyVerified()
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onUserNameChanged(name: String) {
        _uiState.update { it.copy(userName = name, errorMessage = null) }
    }

    fun onGeminiKeyChanged(key: String) {
        _uiState.update {
            it.copy(
                geminiKey = key,
                geminiTestState = KeyTestState.Idle,
                isGeminiKeyVerified = false,
                errorMessage = null
            )
        }
    }

    fun onGrokKeyChanged(key: String) {
        _uiState.update {
            it.copy(
                grokKey = key,
                grokTestState = KeyTestState.Idle,
                errorMessage = null
            )
        }
    }

    fun testGeminiKey() {
        val key = _uiState.value.geminiKey.trim()
        if (key.isBlank()) {
            _uiState.update {
                it.copy(
                    geminiTestState = KeyTestState.Error("Please enter a Gemini API key first"),
                    isGeminiKeyVerified = false
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                geminiTestState = KeyTestState.Testing,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            when (val result = settingsRepository.testGeminiKey(key)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            geminiTestState = KeyTestState.Success("Valid key — Gemini ready"),
                            isGeminiKeyVerified = true,
                            errorMessage = null
                        )
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
                        it.copy(
                            geminiTestState = KeyTestState.Error(message),
                            isGeminiKeyVerified = false
                        )
                    }
                }
            }
        }
    }

    fun testGrokKey() {
        val key = _uiState.value.grokKey.trim()
        if (key.isBlank()) {
            _uiState.update {
                it.copy(grokTestState = KeyTestState.Error("Please enter a Grok API key first"))
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

    fun completeOnboarding(onSuccess: () -> Unit) {
        val state = _uiState.value
        val trimmedGeminiKey = state.geminiKey.trim()

        if (trimmedGeminiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "A valid Gemini API key is required to proceed",
                    geminiTestState = KeyTestState.Error("Gemini API key is required")
                )
            }
            return
        }

        if (!state.isGeminiKeyVerified) {
            _uiState.update {
                it.copy(
                    geminiTestState = KeyTestState.Testing,
                    isSaving = true,
                    errorMessage = null
                )
            }

            viewModelScope.launch {
                when (val result = settingsRepository.testGeminiKey(trimmedGeminiKey)) {
                    is ApiResult.Success -> {
                        settingsRepository.saveProfile(
                            userName = state.userName.trim().ifBlank { "User" },
                            geminiKey = trimmedGeminiKey,
                            grokKey = state.grokKey.trim()
                        )
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                isGeminiKeyVerified = true,
                                geminiTestState = KeyTestState.Success("Valid key — Gemini ready")
                            )
                        }
                        onSuccess()
                    }
                    is ApiResult.Error -> {
                        val message = when {
                            result.isInvalidKey -> "Invalid Gemini key — update it to proceed"
                            result.isQuotaExceeded -> "Quota exceeded — add another key or wait"
                            result.isNetworkError -> "No internet connection. Please check your network and try again."
                            else -> result.message
                        }
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                isGeminiKeyVerified = false,
                                geminiTestState = KeyTestState.Error(message),
                                errorMessage = "Cannot proceed: $message"
                            )
                        }
                    }
                }
            }
        } else {
            settingsRepository.saveProfile(
                userName = state.userName.trim().ifBlank { "User" },
                geminiKey = trimmedGeminiKey,
                grokKey = state.grokKey.trim()
            )
            onSuccess()
        }
    }

    class Factory(private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(settingsRepository) as T
        }
    }
}
