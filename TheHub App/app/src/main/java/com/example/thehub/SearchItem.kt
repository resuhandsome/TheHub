package com.example.thehub

sealed class SearchItem {
    data class User(
        val uid: String = "",
        val username: String = "",
        val avatar: String = "",
        val email: String = ""
    ) : SearchItem()

    data class Post(
        val id: String = "",
        val snippet: String = "",
        val authorId: String = "",
        val authorName: String = "",
        val timestamp: Long = 0L
    ) : SearchItem()
}
