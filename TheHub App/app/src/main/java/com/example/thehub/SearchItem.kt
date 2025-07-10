package com.example.thehub.search

sealed interface SearchItem {
    data class User(val uid: String, val username: String, val avatar: String) : SearchItem
    data class Post(val id: String, val snippet: String) : SearchItem
}
