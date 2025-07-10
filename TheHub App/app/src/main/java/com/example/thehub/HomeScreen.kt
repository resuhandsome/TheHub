// GIỮ NGUYÊN TẤT CẢ IMPORTS VÀ DATA CLASS POST CỦA BẠN
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

// GIỮ NGUYÊN DATA CLASS POST CỦA BẠN
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

    // GIỮ NGUYÊN TẤT CẢ FIREBASE LOGIC CỦA BẠN
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }

    val db = Firebase.firestore
    val context = LocalContext.current

    // GIỮ NGUYÊN FIREBASE LOADING LOGIC
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            currentUserProfile = UserRepository.getCurrentUserProfile()
        }
    }

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
            // CHỈ SỬA COLORS, GIỮ NGUYÊN LAYOUT
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface // ✅ Thay Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .statusBarsPadding()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logothehub),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(50.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "TheHub",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface, // ✅ Thay Color(0xFF1A1A1A)
                            modifier = Modifier.offset(y = 2.dp)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(2.dp, CircleShape)
                                .clickable { navController.navigate("profile") }
                        ) {
                            AsyncImage(
                                model = currentUserProfile?.avatarUrl?.takeIf { it.isNotEmpty() }
                                    ?: currentUser?.photoUrl,
                                contentDescription = "User Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant), // ✅ Thay Color.Gray.copy()
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = defaultAvatar)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = "",
                        onValueChange = { },
                        placeholder = {
                            Text(
                                "Tìm kiếm bài viết, người dùng...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant // ✅ Thay Color.Gray
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search Icon",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, // ✅ Thay Color.Gray
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
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, // ✅ Thay Color(0xFFF5F5F5)
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        enabled = false
                    )
                }
            }
        },
        bottomBar = {
            // CHỈ SỬA COLORS, GIỮ NGUYÊN LAYOUT
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface // ✅ Thay Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(
                        icon = Icons.Default.Notifications,
                        label = "Thông báo",
                        isSelected = false,
                        onClick = { navController.navigate("notifications") }
                    )

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(4.dp, CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primary, // ✅ Thay Color(0xFF007AFF)
                                CircleShape
                            )
                            .clickable { navController.navigate("compose_post") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Post",
                            tint = MaterialTheme.colorScheme.onPrimary, // ✅ Thay Color.White
                            modifier = Modifier.size(28.dp)
                        )
                    }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // ✅ Thay Color(0xFFFAFAFA)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary, // ✅ Thay Color(0xFF007AFF)
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
                        PostItem(post = post, navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer // ✅ Thay Color(0xFF007AFF).copy()
                    else androidx.compose.ui.graphics.Color.Transparent,
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary // ✅ Thay Color(0xFF007AFF)
                else MaterialTheme.colorScheme.onSurfaceVariant, // ✅ Thay Color.Gray
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary // ✅ Thay Color(0xFF007AFF)
            else MaterialTheme.colorScheme.onSurfaceVariant, // ✅ Thay Color.Gray
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun PostItem(post: Post, navController: NavController) {
    // GIỮ NGUYÊN TẤT CẢ FIREBASE LOGIC LIKE/UNLIKE CỦA BẠN
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

    // CHỈ SỬA COLORS, GIỮ NGUYÊN LAYOUT
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // ✅ Thay Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Author info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(1.dp, CircleShape)
                        .clickable {
                            navController.navigate("profile/${post.authorId}")
                        }
                ) {
                    AsyncImage(
                        model = post.authorAvatarUrl,
                        contentDescription = "Author Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant), // ✅ Thay Color.Gray.copy()
                        placeholder = painterResource(id = R.drawable.logomacdinh)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.author,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface, // ✅ Thay Color(0xFF1A1A1A)
                        modifier = Modifier.clickable {
                            navController.navigate("profile/${post.authorId}")
                        }
                    )
                    Text(
                        text = post.time,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // ✅ Thay Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = post.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface // ✅ Thay Color(0xFF333333)
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

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { toggleLike() }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.error // ✅ Thay Color.Red
                        else MaterialTheme.colorScheme.onSurfaceVariant, // ✅ Thay Color.Gray
                        modifier = Modifier.size(22.dp)
                    )
                    if (likeCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = likeCount.toString(),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, // ✅ Thay Color.Gray
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showComments = !showComments }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.iconbinhluan),
                        contentDescription = "Comment",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant // ✅ Thay Color.Gray
                    )
                    if (post.comments > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = post.comments.toString(),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, // ✅ Thay Color.Gray
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Icon(
                    painter = painterResource(id = R.drawable.iconchiase),
                    contentDescription = "Share",
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { sharePost() },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant // ✅ Thay Color.Gray
                )
            }

            if (showComments) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline) // ✅ Thay Color.Gray.copy()
                Spacer(modifier = Modifier.height(8.dp))
                CommentSection(postId = post.id)
            }
        }
    }
}
