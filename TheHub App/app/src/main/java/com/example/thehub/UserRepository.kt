package com.example.thehub

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val createdAt: Long = 0L
)

object UserRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                UserProfile(
                    uid = doc.getString("uid") ?: "",
                    username = doc.getString("username") ?: "",
                    email = doc.getString("email") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    avatarUrl = doc.getString("avatarUrl") ?: "",
                    bio = doc.getString("bio") ?: "",
                    followersCount = doc.getLong("followersCount")?.toInt() ?: 0,
                    followingCount = doc.getLong("followingCount")?.toInt() ?: 0,
                    postsCount = doc.getLong("postsCount")?.toInt() ?: 0,
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
            } else null
        } catch (e: Exception) {
            println("DEBUG: Error loading user profile - ${e.message}")
            null
        }
    }

    suspend fun getCurrentUserProfile(): UserProfile? {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            // retry mechanism
            var profile: UserProfile? = null
            var attempts = 0

            while (profile == null && attempts < 3) {
                try {
                    profile = getUserProfile(currentUser.uid)
                    if (profile != null) break
                } catch (e: Exception) {
                    attempts++
                    if (attempts < 3) {
                        kotlinx.coroutines.delay(1000)
                    }
                }
            }

            profile
        } else null
    }

    suspend fun updateUserProfile(userProfile: UserProfile) {
        try {
            db.collection("users").document(userProfile.uid).set(userProfile).await()
        } catch (e: Exception) {
            println("DEBUG: Error updating user profile - ${e.message}")
            throw e
        }
    }

    suspend fun recalculateFollowerCounts(userId: String) {
        try {
            val followersSnapshot = db.collection("follows")
                .whereEqualTo("followingId", userId)
                .get()
                .await()

            val actualFollowersCount = followersSnapshot.size()

            val followingSnapshot = db.collection("follows")
                .whereEqualTo("followerId", userId)
                .get()
                .await()

            val actualFollowingCount = followingSnapshot.size()

            val userRef = db.collection("users").document(userId)
            userRef.update(
                mapOf(
                    "followersCount" to actualFollowersCount,
                    "followingCount" to actualFollowingCount
                )
            ).await()

        } catch (e: Exception) {
            println("DEBUG: Error recalculating follower counts - ${e.message}")
        }
    }
    suspend fun syncAllUserCounts() {
        try {
            val usersSnapshot = db.collection("users").get().await()

            for (userDoc in usersSnapshot.documents) {
                val userId = userDoc.id
                recalculateFollowerCounts(userId)
            }

        } catch (e: Exception) {
            println("DEBUG: Error syncing all user counts - ${e.message}")
        }
    }
}


