package com.example.thehub

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class NotificationItem(
    val id: String = "",
    val type: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val fromUserAvatar: String = "",
    val toUserId: String = "",
    val message: String = "",
    val postId: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController) {
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf("All") }
    var errorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore

    // Theme-aware colors
    val backgroundColor = ThemeManager.getBackgroundColor()
    val surfaceColor = ThemeManager.getSurfaceColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()
    val accentColor = ThemeManager.getAccentColor()
    val dividerColor = ThemeManager.getDividerColor()

    // Load notifications với error handling
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                isLoading = true
                errorMessage = ""

                println("DEBUG: Loading notifications for user: ${currentUser.uid}")

                // Query đơn giản không dùng orderBy để tránh index
                val notificationsSnapshot = db.collection("notifications")
                    .whereEqualTo("toUserId", currentUser.uid)
                    .get()
                    .await()

                println("DEBUG: Found ${notificationsSnapshot.size()} notifications")

                notifications = notificationsSnapshot.documents.mapNotNull { doc ->
                    try {
                        NotificationItem(
                            id = doc.id,
                            type = doc.getString("type") ?: "",
                            fromUserId = doc.getString("fromUserId") ?: "",
                            fromUsername = doc.getString("fromUsername") ?: "",
                            fromUserAvatar = doc.getString("fromUserAvatar") ?: "",
                            toUserId = doc.getString("toUserId") ?: "",
                            message = doc.getString("message") ?: "",
                            postId = doc.getString("postId") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            isRead = doc.getBoolean("isRead") ?: false
                        )
                    } catch (e: Exception) {
                        println("DEBUG: Error parsing notification - ${e.message}")
                        null
                    }
                }.sortedByDescending { it.timestamp }

                println("DEBUG: Successfully loaded ${notifications.size} notifications")

            } catch (e: Exception) {
                errorMessage = "Lỗi khi tải thông báo: ${e.message}"
                println("DEBUG: Error loading notifications - ${e.message}")

                // Tạo mock notifications
                if (notifications.isEmpty()) {
                    notifications = createMockNotifications(currentUser.uid)
                }
            } finally {
                isLoading = false
            }
        } else {
            errorMessage = "Chưa đăng nhập"
            isLoading = false
        }
    }

    fun retryLoadNotifications() {
        coroutineScope.launch {
            if (currentUser != null) {
                try {
                    isLoading = true
                    errorMessage = ""

                    val notificationsSnapshot = db.collection("notifications")
                        .whereEqualTo("toUserId", currentUser.uid)
                        .get()
                        .await()

                    notifications = notificationsSnapshot.documents.mapNotNull { doc ->
                        try {
                            NotificationItem(
                                id = doc.id,
                                type = doc.getString("type") ?: "",
                                fromUserId = doc.getString("fromUserId") ?: "",
                                fromUsername = doc.getString("fromUsername") ?: "",
                                fromUserAvatar = doc.getString("fromUserAvatar") ?: "",
                                toUserId = doc.getString("toUserId") ?: "",
                                message = doc.getString("message") ?: "",
                                postId = doc.getString("postId") ?: "",
                                timestamp = doc.getLong("timestamp") ?: 0L,
                                isRead = doc.getBoolean("isRead") ?: false
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.timestamp }

                } catch (e: Exception) {
                    errorMessage = "Lỗi khi tải thông báo: ${e.message}"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    fun toggleFollow(userId: String) {
        coroutineScope.launch {
            try {
                if (currentUser != null) {
                    val followDoc = db.collection("follows")
                        .whereEqualTo("followerId", currentUser.uid)
                        .whereEqualTo("followingId", userId)
                        .get()
                        .await()

                    if (followDoc.isEmpty) {
                        // Follow
                        val followData = hashMapOf(
                            "followerId" to currentUser.uid,
                            "followingId" to userId,
                            "timestamp" to System.currentTimeMillis()
                        )

                        db.collection("follows").add(followData).await()
                        Toast.makeText(context, "Đã theo dõi", Toast.LENGTH_SHORT).show()
                    } else {
                        // Unfollow
                        followDoc.documents.forEach { doc ->
                            db.collection("follows").document(doc.id).delete().await()
                        }
                        Toast.makeText(context, "Đã hủy theo dõi", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        fontSize = 20.sp,
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
        ) {
            // Tab buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("All", "Comment reply", "Followers", "Likes").forEach { tab ->
                    FilterChip(
                        onClick = { selectedTab = tab },
                        label = {
                            Text(
                                tab,
                                fontSize = 14.sp,
                                color = if (selectedTab == tab) Color.White else secondaryTextColor
                            )
                        },
                        selected = selectedTab == tab,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            containerColor = if (ThemeManager.isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF0F0F0)
                        )
                    )
                }
            }

            Divider(color = dividerColor, thickness = 0.5.dp)

            // Content area
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }

                errorMessage.isNotEmpty() && notifications.isEmpty() -> {
                    // Error state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "⚠️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Không thể tải thông báo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            fontSize = 14.sp,
                            color = secondaryTextColor
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { retryLoadNotifications() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor
                            )
                        ) {
                            Text("Thử lại")
                        }
                    }
                }

                notifications.isEmpty() -> {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🔔", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Chưa có thông báo nào",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                            Text(
                                text = "Thông báo sẽ xuất hiện ở đây",
                                fontSize = 14.sp,
                                color = secondaryTextColor
                            )
                        }
                    }
                }

                else -> {
                    // Notifications list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        val filteredNotifications = notifications.filter {
                            when (selectedTab) {
                                "Comment reply" -> it.type == "comment"
                                "Followers" -> it.type == "follow"
                                "Likes" -> it.type == "like"
                                else -> true
                            }
                        }

                        items(filteredNotifications) { notification ->
                            NotificationItemView(
                                notification = notification,
                                onFollowClick = { userId ->
                                    toggleFollow(userId)
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
fun NotificationItemView(
    notification: NotificationItem,
    onFollowClick: (String) -> Unit
) {
    var isFollowing by remember { mutableStateOf(false) }
    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore
    val coroutineScope = rememberCoroutineScope()

    // Theme-aware colors
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()
    val accentColor = ThemeManager.getAccentColor()

    // Check if already following
    LaunchedEffect(notification.fromUserId) {
        if (currentUser != null && notification.type == "follow") {
            try {
                val followDoc = db.collection("follows")
                    .whereEqualTo("followerId", currentUser.uid)
                    .whereEqualTo("followingId", notification.fromUserId)
                    .get()
                    .await()

                isFollowing = !followDoc.isEmpty
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        AsyncImage(
            model = notification.fromUserAvatar.takeIf { it.isNotEmpty() },
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.logomacdinh)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Content
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
                    color = textColor
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = getNotificationMessage(notification.type),
                    fontSize = 14.sp,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatTime(notification.timestamp),
                fontSize = 12.sp,
                color = secondaryTextColor
            )
        }

        // Action button
        when (notification.type) {
            "follow" -> {
                Button(
                    onClick = {
                        onFollowClick(notification.fromUserId)
                        isFollowing = !isFollowing
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing)
                            if (ThemeManager.isDarkMode) Color(0xFF2A2A2A) else Color(0xFFE0E0E0)
                        else accentColor
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .widthIn(min = 80.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isFollowing) "Đang theo dõi" else "Theo dõi",
                        fontSize = 12.sp,
                        color = if (isFollowing) secondaryTextColor else Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            "like", "comment" -> {
                // Post thumbnail
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (ThemeManager.isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF0F0F0),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (notification.type == "like")
                                R.drawable.iconbinhluan else R.drawable.iconchiase
                        ),
                        contentDescription = "Post",
                        modifier = Modifier.size(20.dp),
                        tint = secondaryTextColor
                    )
                }
            }
        }
    }
}

fun getNotificationMessage(type: String): String {
    return when (type) {
        "like" -> "đã thích bài viết của bạn"
        "comment" -> "đã bình luận bài viết của bạn"
        "follow" -> "đã theo dõi bạn"
        "mention" -> "đã nhắc đến bạn"
        else -> "có hoạt động mới"
    }
}

// Function tạo mock notifications cho testing
fun createMockNotifications(currentUserId: String): List<NotificationItem> {
    return listOf(
        NotificationItem(
            id = "mock1",
            type = "follow",
            fromUserId = "user123",
            fromUsername = "john_doe",
            fromUserAvatar = "",
            toUserId = currentUserId,
            message = "đã theo dõi bạn",
            timestamp = System.currentTimeMillis() - 3600000,
            isRead = false
        ),
        NotificationItem(
            id = "mock2",
            type = "like",
            fromUserId = "user456",
            fromUsername = "jane_smith",
            fromUserAvatar = "",
            toUserId = currentUserId,
            message = "đã thích bài viết của bạn",
            timestamp = System.currentTimeMillis() - 7200000,
            isRead = false
        )
    )
}
