package com.example.thehub

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

// Lớp dữ liệu để đại diện cho các loại thông báo khác nhau
data class Notification(
    val id: Int,
    val type: NotificationType,
    val users: List<String>,
    val time: String,
    val comment: String? = null,
    val postImageUrl: String? = null
)

enum class NotificationType {
    LIKE, FOLLOW, MENTION, COMMENT
}

// Dữ liệu thông báo mẫu
val sampleNotifications = listOf(
    Notification(1, NotificationType.LIKE, listOf("_thw.nguyn", "wind_uiu"), "2 tuần", postImageUrl = "https://i.pravatar.cc/150?img=1"),
    Notification(2, NotificationType.FOLLOW, listOf("minhchau4878"), "2 tuần"),
    Notification(3, NotificationType.MENTION, listOf("nguyen.dao.hoang", "_trancamly"), "2 tuần", postImageUrl = "https://i.pravatar.cc/150?img=2"),
    Notification(4, NotificationType.LIKE, listOf("quangnl_2907", "ngdhai8805"), "2 tuần", postImageUrl = "https://i.pravatar.cc/150?img=3"),
    Notification(5, NotificationType.LIKE, listOf("drawingbymikey"), "2 tuần", postImageUrl = "https://i.pravatar.cc/150?img=4"),
    Notification(6, NotificationType.LIKE, listOf("drawingbymikey"), "2 tuần", postImageUrl = "https://i.pravatar.cc/150?img=5"),
    Notification(7, NotificationType.LIKE, listOf("drawingbymikey"), "2 tuần", postImageUrl = "https://i.pravatar.cc/150?img=6"),
    Notification(8, NotificationType.LIKE, listOf("drawingbymikey"), "2 tuần", postImageUrl = "https://i.pravatar.cc/150?img=7"),
    Notification(9, NotificationType.COMMENT, listOf("fwg_uyn17"), "2 tuần", comment = "\"@_amy1912_ cần lắm nút haha 😂\"", postImageUrl = "https://i.pravatar.cc/150?img=8"),
    Notification(10, NotificationType.COMMENT, listOf("_amy1912_"), "2 tuần", comment = "\"@anhson_01 không nhắc không đau thương 😂\"", postImageUrl = "https://i.pravatar.cc/150?img=9")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // Thanh điều hướng dưới cùng đã được cập nhật
            BottomAppBar(
                containerColor = Color.White,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                // Nút thông báo (đã ở trang thông báo nên không cần hành động)
                IconButton(onClick = { /* Đã ở đây */ }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }
                // Nút thêm bài
                IconButton(onClick = { navController.navigate("post") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = "Add Post")
                }
                // Nút về trang chủ
                IconButton(
                    onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Home")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            item {
                NotificationFilters()
            }
            items(sampleNotifications) { notification ->
                NotificationItem(notification = notification)
            }
        }
    }
}

@Composable
fun NotificationFilters() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = { /*TODO*/ }, shape = RoundedCornerShape(8.dp)) { Text("All") }
        Button(onClick = { /*TODO*/ }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) { Text("Comment reply", color = Color.Black) }
        Button(onClick = { /*TODO*/ }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) { Text("Followers", color = Color.Black) }
    }
}

@Composable
fun NotificationItem(notification: Notification) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatars của người dùng
        AsyncImage(
            model = "https://i.pravatar.cc/150?u=${notification.users.first()}",
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))

        // Nội dung văn bản của thông báo
        Column(modifier = Modifier.weight(1f)) {
            val text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(notification.users.joinToString(", "))
                }
                when (notification.type) {
                    NotificationType.LIKE -> append(" đã thích ảnh của bạn.")
                    NotificationType.FOLLOW -> append(" đã bắt đầu theo dõi bạn.")
                    NotificationType.MENTION -> append(" và những người khác đã thích bài viết của bạn.")
                    NotificationType.COMMENT -> append(" đã bình luận: ${notification.comment}")
                }
                withStyle(style = SpanStyle(color = Color.Gray)) {
                    append(" ${notification.time}")
                }
            }
            Text(text = text, fontSize = 14.sp, lineHeight = 20.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))

        // Nội dung bên phải (Ảnh bài đăng hoặc nút Theo dõi)
        when (notification.type) {
            NotificationType.FOLLOW -> {
                Button(onClick = { /*TODO*/ }, shape = RoundedCornerShape(8.dp)) {
                    Text("Theo dõi")
                }
            }
            else -> {
                if (notification.postImageUrl != null) {
                    AsyncImage(
                        model = notification.postImageUrl,
                        contentDescription = "Post Thumbnail",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
