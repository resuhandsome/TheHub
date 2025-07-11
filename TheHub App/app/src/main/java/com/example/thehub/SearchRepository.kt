package com.example.thehub

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

        val query = q.lowercase().trim()

        val users = async {
            try {
                // search users by username (case insensitive)
                db.collection("users")
                    .orderBy("username")
                    .startAt(query)
                    .endAt(query + "\uf8ff")
                    .limit(10)
                    .get().await()
                    .documents.mapNotNull { doc ->
                        try {
                            SearchItem.User(
                                uid = doc.id,
                                username = doc.getString("username") ?: "",
                                avatar = doc.getString("avatarUrl") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val posts = async {
            try {
                // search posts by content
                db.collection("posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(20)
                    .get().await()
                    .documents.mapNotNull { doc ->
                        try {
                            val content = doc.getString("content") ?: ""
                            if (content.lowercase().contains(query)) {
                                SearchItem.Post(
                                    id = doc.id,
                                    snippet = content
                                )
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
            } catch (e: Exception) {
                emptyList()
            }
        }

        // combine results with users first
        val userResults = users.await()
        val postResults = posts.await()

        userResults + postResults
    }
}
