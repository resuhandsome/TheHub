package com.example.thehub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentSection(postId: String) {
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var newComment by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore
    val coroutineScope = rememberCoroutineScope()

    // Load comments
    LaunchedEffect(postId) {
        try {
            val commentsSnapshot = db.collection("comments")
                .whereEqualTo("postId", postId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val commentsList = mutableListOf<Comment>()

            for (doc in commentsSnapshot.documents) {
                val authorId = doc.getString("authorId") ?: ""
                var authorName = doc.getString("authorName") ?: "Unknown User"
                var authorAvatarUrl = doc.getString("authorAvatarUrl") ?: ""

                // Load author profile if needed
                if (authorName == "Unknown User" && authorId.isNotEmpty()) {
                    try {
                        val authorProfile = UserRepository.getUserProfile(authorId)
                        if (authorProfile != null) {
                            authorName = authorProfile.username
                            authorAvatarUrl = authorProfile.avatarUrl
                        }
                    } catch (e: Exception) {
                        // Keep default values
                    }
                }

                val comment = Comment(
                    id = doc.id,
                    postId = doc.getString("postId") ?: "",
                    authorId = authorId,
                    authorName = authorName,
                    authorAvatarUrl = authorAvatarUrl,
                    content = doc.getString("content") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L
                )

                commentsList.add(comment)
            }

            comments = commentsList

        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Comments list
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else if (comments.isEmpty()) {
            Text(
                text = "Chưa có bình luận nào",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(comments) { comment ->
                    CommentItem(comment = comment)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Add comment input
        if (currentUser != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                TextField(
                    value = newComment,
                    onValueChange = { newComment = it },
                    placeholder = {
                        Text(
                            "Viết bình luận...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (newComment.isNotBlank() && !isSubmitting) {
                            coroutineScope.launch {
                                isSubmitting = true
                                try {
                                    val currentUserProfile = UserRepository.getCurrentUserProfile()

                                    val commentData = hashMapOf(
                                        "postId" to postId,
                                        "authorId" to currentUser.uid,
                                        "authorName" to (currentUserProfile?.username ?: currentUser.displayName ?: "Unknown User"),
                                        "authorAvatarUrl" to (currentUserProfile?.avatarUrl ?: currentUser.photoUrl?.toString() ?: ""),
                                        "content" to newComment.trim(),
                                        "timestamp" to System.currentTimeMillis()
                                    )

                                    db.collection("comments").add(commentData).await()

                                    // Update post comment count
                                    val postRef = db.collection("posts").document(postId)
                                    db.runTransaction { transaction ->
                                        val post = transaction.get(postRef)
                                        val currentComments = post.getLong("comments") ?: 0
                                        transaction.update(postRef, "comments", currentComments + 1)
                                    }.await()

                                    newComment = ""

                                    // Reload comments
                                    val updatedCommentsSnapshot = db.collection("comments")
                                        .whereEqualTo("postId", postId)
                                        .orderBy("timestamp", Query.Direction.DESCENDING)
                                        .get()
                                        .await()

                                    val updatedCommentsList = mutableListOf<Comment>()
                                    for (doc in updatedCommentsSnapshot.documents) {
                                        val comment = Comment(
                                            id = doc.id,
                                            postId = doc.getString("postId") ?: "",
                                            authorId = doc.getString("authorId") ?: "",
                                            authorName = doc.getString("authorName") ?: "Unknown User",
                                            authorAvatarUrl = doc.getString("authorAvatarUrl") ?: "",
                                            content = doc.getString("content") ?: "",
                                            timestamp = doc.getLong("timestamp") ?: 0L
                                        )
                                        updatedCommentsList.add(comment)
                                    }

                                    comments = updatedCommentsList

                                } catch (e: Exception) {
                                    // Handle error
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        }
                    },
                    enabled = newComment.isNotBlank() && !isSubmitting,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (newComment.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (newComment.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = comment.authorAvatarUrl,
            contentDescription = "Author Avatar",
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.logomacdinh)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.authorName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatTime(comment.timestamp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = comment.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )
        }
    }
}
