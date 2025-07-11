package com.example.thehub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel(private val conversationId: String) : ViewModel() {
    private val repo = ChatRepository(conversationId, Firebase.auth.currentUser!!.uid)

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _otherUserName = MutableStateFlow("")
    val otherUserName: StateFlow<String> = _otherUserName

    val messages = repo.messages

    init {
        repo.startListening()
        _isLoading.value = false
    }

    fun loadConversation() {
        viewModelScope.launch {
            try {
                val db = Firebase.firestore
                val currentUserId = Firebase.auth.currentUser!!.uid

                val conversationDoc = db.collection("conversations")
                    .document(conversationId)
                    .get()
                    .await()

                if (conversationDoc.exists()) {
                    val participants = conversationDoc.get("participants") as? List<String> ?: emptyList()
                    val otherUserId = participants.firstOrNull { it != currentUserId }

                    if (otherUserId != null) {
                        val userDoc = db.collection("users")
                            .document(otherUserId)
                            .get()
                            .await()

                        _otherUserName.value = userDoc.getString("username") ?: "Người dùng"
                    }
                }
            } catch (e: Exception) {
                _otherUserName.value = "Người dùng"
            }
        }
    }

    fun updateDraft(text: String) {
        _draft.value = text
    }

    fun sendMessage() {
        val text = _draft.value.trim()
        if (text.isNotBlank()) {
            viewModelScope.launch {
                repo.sendMessage(text)
                _draft.value = ""
            }
        }
    }

    // (PHẦN THÊM MỚI) Hàm để gọi Repository xóa tin nhắn
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repo.deleteMessage(messageId)
        }
    }


    override fun onCleared() {
        super.onCleared()
        repo.stopListening()
    }

    companion object {
        fun factory(conversationId: String) = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(conversationId) as T
            }
        }
    }
}