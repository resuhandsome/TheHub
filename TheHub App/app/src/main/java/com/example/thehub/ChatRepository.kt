package com.example.thehub.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(private val cid: String, private val myUid: String) {
    private val db = Firebase.firestore
    private val convoRef = db.collection("conversations").document(cid)
    private val msgsRef = convoRef.collection("messages")

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    suspend fun listen() {
        msgsRef.orderBy("created", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                _messages.value = snap?.documents?.map {
                    Message(
                        it.id,
                        it.getString("senderId") ?: "",
                        it.getString("text") ?: "",
                        it.getTimestamp("created") ?: Timestamp.now()
                    )
                } ?: emptyList()
            }
    }

    suspend fun send(text: String) {
        if (text.isBlank()) return
        val now = Timestamp.now()
        msgsRef.add(
            mapOf(
                "senderId" to myUid,
                "text" to text,
                "created" to now
            )
        ).await()
        convoRef.update(
            mapOf(
                "lastMessage" to text,
                "lastUpdate" to now
            )
        )
    }
}
