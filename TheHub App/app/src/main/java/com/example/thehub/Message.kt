package com.example.thehub.chat

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val created: Timestamp = Timestamp.now()
)
