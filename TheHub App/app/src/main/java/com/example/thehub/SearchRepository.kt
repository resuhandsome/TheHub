package com.example.thehub.search

import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class SearchRepository {
    private val db = Firebase.firestore

    suspend fun search(q: String): List<SearchItem> = coroutineScope {
        if (q.isBlank()) return@coroutineScope emptyList()

        val users = async {
            db.collection("users")
                .whereGreaterThanOrEqualTo("username", q)
                .whereLessThanOrEqualTo("username", q + '\uf8ff')
                .get().await()
                .map {
                    SearchItem.User(
                        it.id,
                        it.getString("username") ?: "",
                        it.getString("avatarUrl") ?: ""
                    )
                }
        }
        val posts = async {
            db.collection("posts")
                .whereArrayContains("keywords", q.lowercase())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get().await()
                .map {
                    SearchItem.Post(it.id, it.getString("content") ?: "")
                }
        }
        users.await() + posts.await()
    }
}
