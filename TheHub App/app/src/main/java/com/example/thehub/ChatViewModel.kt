package com.example.thehub.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val cid: String) : ViewModel() {
    private val repo = ChatRepository(cid, Firebase.auth.currentUser!!.uid)

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft
    val messages = repo.messages

    init {
        viewModelScope.launch { repo.listen() }
    }

    fun updateDraft(t: String) { _draft.value = t }

    fun send() {
        val txt = draft.value.trim()
        if (txt.isNotBlank()) {
            viewModelScope.launch { repo.send(txt) }
            _draft.value = ""
        }
    }
}
