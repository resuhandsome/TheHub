package com.example.thehub.chat


data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)