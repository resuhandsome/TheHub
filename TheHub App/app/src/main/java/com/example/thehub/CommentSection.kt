package com.example.thehub

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
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
    var commentText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore

    // load user profile
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            currentUserProfile = UserRepository.getCurrentUserProfile()
        }
    }

    // load comments
    LaunchedEffect(postId) {
        try {
            val commentsSnapshot = db.collection("posts")
                .document(postId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            comments = commentsSnapshot.documents.map { doc ->
                Comment(
                    id = doc.id,
                    authorId = doc.getString("authorId") ?: "",
                    authorName = doc.getString("authorName") ?: "",
                    authorAvatarUrl = doc.getString("authorAvatarUrl") ?: "",
                    content = doc.getString("content") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L
                )
            }
        } catch (e: Exception) {
        } finally {
            isLoading = false
        }
    }

    fun addComment() {
        val profile = currentUserProfile
        //kiểm tra profile
        if (commentText.isBlank() || currentUser == null || profile == null) {
            Toast.makeText(context, "Không thể lấy thông tin người dùng, vui lòng thử lại", Toast.LENGTH_SHORT).show()
            return
        }

        coroutineScope.launch {
            try {
                val commentData = hashMapOf(
                    "authorId" to currentUser.uid,
                    "authorName" to profile.username,
                    "authorAvatarUrl" to profile.avatarUrl,
                    "content" to commentText,
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .add(commentData)
                    .await()

                // update comment count
                db.collection("posts")
                    .document(postId)
                    .update("comments", comments.size + 1)
                    .await()

                // TẠO NOTIFICATION CHO TÁC GIẢ BÀI VIẾT
                val postDoc = db.collection("posts").document(postId).get().await()
                val postAuthorId = postDoc.getString("authorId")

                if (postAuthorId != null && postAuthorId != currentUser.uid) {
                    val notificationData = hashMapOf(
                        "type" to "comment",
                        "fromUserId" to currentUser.uid,
                        "fromUsername" to profile.username,
                        "fromUserAvatar" to profile.avatarUrl,
                        "toUserId" to postAuthorId,
                        "message" to "đã bình luận bài viết của bạn",
                        "postId" to postId,
                        "timestamp" to System.currentTimeMillis(),
                        "isRead" to false
                    )

                    db.collection("notifications").add(notificationData).await()
                    println("DEBUG: Created comment notification for $postAuthorId")
                }

                commentText = ""

                // reload comments
                val updatedSnapshot = db.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .await()

                comments = updatedSnapshot.documents.map { doc ->
                    Comment(
                        id = doc.id,
                        authorId = doc.getString("authorId") ?: "",
                        authorName = doc.getString("authorName") ?: "",
                        authorAvatarUrl = doc.getString("authorAvatarUrl") ?: "",
                        content = doc.getString("content") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi thêm bình luận", Toast.LENGTH_SHORT).show()
                println("DEBUG: Error adding comment - ${e.message}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            // comments list
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(comments) { comment ->
                    CommentItem(comment = comment)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // add comment input
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Viết bình luận...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { addComment() }) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        AsyncImage(
            model = comment.authorAvatarUrl,
            contentDescription = "Comment Author Avatar",
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.logomacdinh)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.authorName,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = comment.content,
                fontSize = 14.sp
            )
        }
    }
}