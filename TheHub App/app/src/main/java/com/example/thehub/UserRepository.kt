package com.example.thehub

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object UserRepository {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    suspend fun getCurrentUserProfile(): UserProfile? {
        return try {
            val currentUser = auth.currentUser ?: return null
            val document = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            if (document.exists()) {
                document.toObject(UserProfile::class.java)?.copy(id = currentUser.uid)
            } else {
                // Create default profile
                val defaultProfile = UserProfile(
                    id = currentUser.uid,
                    username = currentUser.displayName ?: "user_${currentUser.uid.take(8)}",
                    displayName = currentUser.displayName ?: "",
                    email = currentUser.email ?: "",
                    avatarUrl = currentUser.photoUrl?.toString() ?: ""
                )

                firestore.collection("users")
                    .document(currentUser.uid)
                    .set(defaultProfile)
                    .await()

                defaultProfile
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                document.toObject(UserProfile::class.java)?.copy(id = userId)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserProfile(userProfile: UserProfile): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false
            firestore.collection("users")
                .document(currentUser.uid)
                .set(userProfile)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}