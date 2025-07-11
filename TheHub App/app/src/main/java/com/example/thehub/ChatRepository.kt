package com.example.thehub

import android.util.Log
import com.example.thehub.chat.Message
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(private val conversationId: String, private val myUid: String) {
    private val db = Firebase.firestore
    private val convoRef = db.collection("conversations").document(conversationId)
    private val msgsRef = convoRef.collection("messages")

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private var listener: com.google.firebase.firestore.ListenerRegistration? = null

    fun startListening() {
        listener = msgsRef.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("ChatRepository", "Listen failed.", error)
                    return@addSnapshotListener
                }

                val messageList = snapshot?.documents?.mapNotNull { doc ->
                    Message(
                        id = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        text = doc.getString("text") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()
                _messages.value = messageList
            }
    }

    suspend fun sendMessage(text: String) {
        if (text.isBlank()) return

        val now = System.currentTimeMillis()
        val messageData = mapOf(
            "senderId" to myUid,
            "text" to text,
            "timestamp" to now
        )

        try {
            msgsRef.add(messageData).await()
            convoRef.update(
                mapOf(
                    "lastMessage" to text,
                    "lastUpdate" to now
                )
            ).await()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message", e)
        }
    }

    // xóa tin nhắn
    suspend fun deleteMessage(messageId: String) {
        try {
            msgsRef.document(messageId).delete().await()

            // cập nhật lastMessage nếu cần
            val lastMessageSnapshot = msgsRef
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val lastMessageText = if (lastMessageSnapshot.isEmpty) {
                "Cuộc trò chuyện đã bắt đầu"
            } else {
                lastMessageSnapshot.documents.first().getString("text") ?: ""
            }

            convoRef.update("lastMessage", lastMessageText).await()

        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting message", e)
        }
    }

    fun stopListening() {
        listener?.remove()
    }
}