package com.example.thehub

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

data class Post(
    val id: String = "",
    val authorId: String = "",
    val author: String,
    val authorAvatarUrl: String,
    val time: String,
    val content: String,
    val imageUrls: List<String> = emptyList(),
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val comments: Int = 0,
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val currentUser = Firebase.auth.currentUser
    val defaultAvatar = R.drawable.logomacdinh

    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }

    val db = Firebase.firestore
    val context = LocalContext.current

    // Load user profile
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            currentUserProfile = UserRepository.getCurrentUserProfile()
        }
    }

    // Load posts with user profiles
    LaunchedEffect(Unit) {
        try {
            val postsSnapshot = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val postsWithProfiles = mutableListOf<Post>()

            for (doc in postsSnapshot.documents) {
                val authorId = doc.getString("authorId") ?: ""
                var authorName = doc.getString("authorName") ?: "Unknown User"
                var authorAvatarUrl = doc.getString("authorAvatarUrl") ?: ""

                // Load author profile from Firestore if authorName is Unknown User
                if (authorName == "Unknown User" && authorId.isNotEmpty()) {
                    try {
                        val authorProfile = UserRepository.getUserProfile(authorId)
                        if (authorProfile != null) {
                            authorName = authorProfile.username
                            authorAvatarUrl = authorProfile.avatarUrl
                        }
                    } catch (e: Exception) {
                        // Keep default values if profile load fails
                    }
                }

                val post = Post(
                    id = doc.id,
                    authorId = authorId,
                    author = authorName,
                    authorAvatarUrl = authorAvatarUrl,
                    time = formatTime(doc.getLong("timestamp") ?: 0L),
                    content = doc.getString("content") ?: "",
                    imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList(),
                    likes = doc.getLong("likes")?.toInt() ?: 0,
                    likedBy = doc.get("likedBy") as? List<String> ?: emptyList(),
                    comments = doc.getLong("comments")?.toInt() ?: 0,
                    timestamp = doc.getLong("timestamp") ?: 0L
                )

                postsWithProfiles.add(post)
            }

            posts = postsWithProfiles

        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi khi tải bài viết: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            // Top Bar được thiết kế lại đẹp mắt
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .statusBarsPadding()
                ) {
                    // Logo và Tên logo - điều chỉnh vị trí
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logothehub),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(50.dp) // Giảm size logo một chút
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Chữ TheHub thấp xuống một chút
                        Text(
                            text = "TheHub",
                            fontSize = 24.sp, // Giảm size chữ
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A),
                            modifier = Modifier.offset(y = 2.dp) // Thấp xuống 2dp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Avatar người dùng với border đẹp - THÊM NAVIGATION
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(2.dp, CircleShape)
                                .clickable { navController.navigate("profile") } // THÊM DÒNG NÀY
                        ) {
                            AsyncImage(
                                model = currentUserProfile?.avatarUrl?.takeIf { it.isNotEmpty() }
                                    ?: currentUser?.photoUrl,
                                contentDescription = "User Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.Gray.copy(alpha = 0.2f)),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = defaultAvatar)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Thanh tìm kiếm được thiết kế lại
                    TextField(
                        value = "",
                        onValueChange = { },
                        placeholder = {
                            Text(
                                "Tìm kiếm bài viết, người dùng...",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search Icon",
                                tint = Color.Gray,
                                modifier = Modifier.clickable {
                                    navController.navigate("search")
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { navController.navigate("search") },
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledContainerColor = Color(0xFFF5F5F5),
                            disabledIndicatorColor = Color.Transparent
                        ),
                        enabled = false
                    )
                }
            }
        },
        bottomBar = {
            // Bottom Navigation được thiết kế lại đẹp mắt
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Notifications Button
                    BottomNavItem(
                        icon = Icons.Default.Notifications,
                        label = "Thông báo",
                        isSelected = false,
                        onClick = { navController.navigate("notifications") }
                    )

                    // Add Post Button - Nút chính giữa đặc biệt
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(4.dp, CircleShape)
                            .background(
                                Color(0xFF007AFF),
                                CircleShape
                            )
                            .clickable { navController.navigate("compose_post") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Post",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Home Button
                    BottomNavItem(
                        icon = Icons.Default.Home,
                        label = "Trang chủ",
                        isSelected = true,
                        onClick = { }
                    )
                }
            }
        }
    ) { paddingValues ->
        // Content area với background gradient nhẹ
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF007AFF),
                        strokeWidth = 3.dp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(posts) { post ->
                        PostItem(post = post, navController = navController) // THÊM navController
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isSelected) Color(0xFF007AFF).copy(alpha = 0.1f) else Color.Transparent,
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFF007AFF) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) Color(0xFF007AFF) else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun PostItem(post: Post, navController: NavController) { // THÊM navController parameter
    var isLiked by remember { mutableStateOf(post.likedBy.contains(Firebase.auth.currentUser?.uid)) }
    var likeCount by remember { mutableStateOf(post.likes) }
    var showComments by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore

    fun toggleLike() {
        if (currentUser == null) return

        coroutineScope.launch {
            try {
                val postRef = db.collection("posts").document(post.id)
                val newLikedBy = if (isLiked) {
                    post.likedBy.filter { it != currentUser.uid }
                } else {
                    post.likedBy + currentUser.uid
                }

                postRef.update(
                    mapOf(
                        "likedBy" to newLikedBy,
                        "likes" to newLikedBy.size
                    )
                ).await()

                // Tạo notification nếu like (không phải unlike)
                if (!isLiked && post.authorId != currentUser.uid) {
                    val currentUserProfile = UserRepository.getCurrentUserProfile()
                    if (currentUserProfile != null) {
                        val notificationData = hashMapOf(
                            "type" to "like",
                            "fromUserId" to currentUser.uid,
                            "fromUsername" to currentUserProfile.username,
                            "fromUserAvatar" to currentUserProfile.avatarUrl,
                            "toUserId" to post.authorId,
                            "message" to "đã thích bài viết của bạn",
                            "postId" to post.id,
                            "timestamp" to System.currentTimeMillis(),
                            "isRead" to false
                        )

                        db.collection("notifications").add(notificationData).await()
                    }
                }

                isLiked = !isLiked
                likeCount = newLikedBy.size

            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi thích bài viết", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sharePost() {
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, "${post.author}: ${post.content}")
            type = "text/plain"
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Chia sẻ bài viết"))
    }

    // Post Card với shadow và border radius đẹp
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Author info - THÊM NAVIGATION KHI CLICK AVATAR
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(1.dp, CircleShape)
                        .clickable {
                            navController.navigate("profile/${post.authorId}") // THÊM NAVIGATION
                        }
                ) {
                    AsyncImage(
                        model = post.authorAvatarUrl,
                        contentDescription = "Author Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.2f)),
                        placeholder = painterResource(id = R.drawable.logomacdinh)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.author,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.clickable {
                            navController.navigate("profile/${post.authorId}") // THÊM NAVIGATION
                        }
                    )
                    Text(
                        text = post.time,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            Text(
                text = post.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFF333333)
            )

            // Images
            if (post.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(post.imageUrls) { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Post Image",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons với design đẹp hơn
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { toggleLike() }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                    if (likeCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = likeCount.toString(),
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Comment button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showComments = !showComments }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.iconbinhluan),
                        contentDescription = "Comment",
                        modifier = Modifier.size(22.dp),
                        tint = Color.Gray
                    )
                    if (post.comments > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = post.comments.toString(),
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Share button
                Icon(
                    painter = painterResource(id = R.drawable.iconchiase),
                    contentDescription = "Share",
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { sharePost() },
                    tint = Color.Gray
                )
            }

            // Comments section
            if (showComments) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.Gray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                CommentSection(postId = post.id)
            }
        }
    }
}

