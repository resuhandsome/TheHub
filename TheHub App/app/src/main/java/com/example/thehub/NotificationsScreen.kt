package com.example.thehub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Notification(
    val id: String = "",
    val type: String = "", // "like", "comment", "follow", "mention"
    val fromUserId: String = "",
    val fromUsername: String = "",
    val fromUserAvatar: String = "",
    val toUserId: String = "",
    val message: String = "",
    val postId: String = "",
    val timestamp: Long = 0L,
    var isRead: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController) {
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasUnread by remember { mutableStateOf(false) }

    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                val notificationsSnapshot = db.collection("notifications")
                    .whereEqualTo("toUserId", currentUser.uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()

                val notificationsList = notificationsSnapshot.documents.map { doc ->
                    Notification(
                        id = doc.id,
                        type = doc.getString("type") ?: "",
                        fromUserId = doc.getString("fromUserId") ?: "",
                        fromUsername = doc.getString("fromUsername") ?: "Unknown User",
                        fromUserAvatar = doc.getString("fromUserAvatar") ?: "",
                        toUserId = doc.getString("toUserId") ?: "",
                        message = doc.getString("message") ?: "",
                        postId = doc.getString("postId") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        isRead = doc.getBoolean("isRead") ?: false
                    )
                }

                notifications = notificationsList
                hasUnread = notificationsList.any { !it.isRead }

            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    fun markAsRead(notification: Notification) {
        if (!notification.isRead) {
            coroutineScope.launch {
                try {
                    db.collection("notifications")
                        .document(notification.id)
                        .update("isRead", true)
                        .await()

                    notifications = notifications.map {
                        if (it.id == notification.id) it.copy(isRead = true) else it
                    }
                    hasUnread = notifications.any { !it.isRead }

                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    fun markAllAsRead() {
        coroutineScope.launch {
            try {
                val batch = db.batch()

                notifications.filter { !it.isRead }.forEach { notification ->
                    val docRef = db.collection("notifications").document(notification.id)
                    batch.update(docRef, "isRead", true)
                }

                batch.commit().await()

                notifications = notifications.map { it.copy(isRead = true) }
                hasUnread = false

            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Thông báo",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    if (hasUnread) {
                        TextButton(
                            onClick = { markAllAsRead() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Đọc tất cả")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                notifications.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "No Notifications",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Chưa có thông báo nào",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "Thông báo về lượt thích, bình luận và người theo dõi mới sẽ xuất hiện ở đây",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        items(notifications) { notification ->
                            NotificationItem(
                                notification = notification,
                                onClick = {
                                    markAsRead(notification)

                                    when (notification.type) {
                                        "like", "comment" -> {
                                            if (notification.postId.isNotEmpty()) {
                                                navController.navigate("post/${notification.postId}")
                                            }
                                        }
                                        "follow" -> {
                                            navController.navigate("profile/${notification.fromUserId}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = notification.fromUserAvatar,
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.logomacdinh)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.fromUsername,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = notification.message,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatTime(notification.timestamp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val icon = when (notification.type) {
                "like" -> Icons.Default.Favorite
                "comment" -> Icons.Default.Chat
                "follow" -> Icons.Default.PersonAdd
                "mention" -> Icons.Default.AlternateEmail
                else -> Icons.Default.Notifications
            }

            val iconColor = when (notification.type) {
                "like" -> MaterialTheme.colorScheme.error
                "comment" -> MaterialTheme.colorScheme.primary
                "follow" -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Icon(
                imageVector = icon,
                contentDescription = notification.type,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )

            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                )
            }
        }
    }
}
