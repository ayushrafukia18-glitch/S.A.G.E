package com.sage.app.ui.publicapi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sage.app.publicapi.PublicApiEntry
import com.sage.app.publicapi.PublicApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PublicApiUiState(
    val query: String = "", val category: String? = null, val categories: List<String> = emptyList(),
    val entries: List<PublicApiEntry> = emptyList(), val loading: Boolean = false, val error: String? = null
)
class PublicApiViewModel(private val repository: PublicApiRepository): ViewModel() {
    private val _state = MutableStateFlow(PublicApiUiState())
    val state = _state.asStateFlow()
    init { loadCategories(); search() }
    fun query(value: String) { _state.update { it.copy(query = value) } }
    fun category(value: String?) { _state.update { it.copy(category = value) }; search() }
    fun search() { viewModelScope.launch { _state.update { it.copy(loading = true, error = null) }; repository.search(_state.value.query, _state.value.category).fold({ data -> _state.update { it.copy(entries = data, loading = false) } }, { e -> _state.update { it.copy(loading = false, error = e.message) } }) } }
    private fun loadCategories() { viewModelScope.launch { repository.categories().onSuccess { cats -> _state.update { it.copy(categories = cats) } } } }
    class Factory(private val repo: PublicApiRepository): ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T: ViewModel> create(c: Class<T>): T = PublicApiViewModel(repo) as T }
}
