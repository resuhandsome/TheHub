package com.example.thehub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val repo = SearchRepository()
    private var searchJob: Job? = null

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<SearchItem>>(emptyList())
    val results: StateFlow<List<SearchItem>> = _results

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun updateQuery(q: String) {
        _query.value = q

        // Cancel previous search
        searchJob?.cancel()

        if (q.isBlank()) {
            _results.value = emptyList()
            return
        }

        // Debounce search
        searchJob = viewModelScope.launch {
            delay(300) // delay time searching
            search(q)
        }
    }

    private fun search(q: String) = viewModelScope.launch {
        _isLoading.value = true
        try {
            _results.value = repo.search(q)
        } catch (e: Exception) {
            _results.value = emptyList()
        } finally {
            _isLoading.value = false
        }
    }
}
