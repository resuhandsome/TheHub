package com.example.thehub

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
            .addSnapshotListener { snapshot, _ ->
                val messageList = snapshot?.documents?.mapNotNull { doc ->
                    Message(
                        id = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        text = doc.getString("text") ?: "",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
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
            // Add message to subcollection
            msgsRef.add(messageData).await()

            // Update conversation metadata
            convoRef.update(
                mapOf(
                    "lastMessage" to text,
                    "lastUpdate" to now
                )
            ).await()
        } catch (e: Exception) {
            // Handle error silently for now
        }
    }

    fun stopListening() {
        listener?.remove()
    }
}
