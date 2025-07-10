package com.example.thehub

data class Post(
    val id: String = "",
    val authorId: String = "",
    val author: String = "",
    val authorAvatarUrl: String = "",
    val time: String = "",
    val content: String = "",
    val imageUrls: List<String> = emptyList(),
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val comments: Int = 0,
    val timestamp: Long = 0L
)
