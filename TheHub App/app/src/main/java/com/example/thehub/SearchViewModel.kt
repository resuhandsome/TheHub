package com.example.thehub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val repo = SearchRepository()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<SearchItem>>(emptyList())
    val results: StateFlow<List<SearchItem>> = _results

    fun updateQuery(q: String) {
        _query.value = q
        if (q.isNotBlank()) {
            search(q)
        } else {
            _results.value = emptyList()
        }
    }

    private fun search(q: String) = viewModelScope.launch {
        _results.value = repo.search(q)
    }
}
