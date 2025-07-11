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
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(navController: NavController) {
    val currentUser = Firebase.auth.currentUser
    val defaultAvatar = R.drawable.logomacdinh

    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }


    val db = Firebase.firestore
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Theme-aware colors
    val backgroundColor = ThemeManager.getBackgroundColor()
    val surfaceColor = ThemeManager.getSurfaceColor()
    val cardColor = ThemeManager.getCardColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()
    val iconColor = ThemeManager.getIconColor()
    val dividerColor = ThemeManager.getDividerColor()
    val accentColor = ThemeManager.getAccentColor()

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
    fun deletePost(postId: String) {
        coroutineScope.launch {
            try {
                // Delete the post document from Firestore
                db.collection("posts").document(postId).delete().await()

                // TODO: Delete images from Firebase Storage associated with the post
                // TODO: Delete comments subcollection for the post

                // Update the UI
                posts = posts.filterNot { it.id == postId }
                Toast.makeText(context, "Đã xóa bài viết", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi xóa bài viết: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    if (showDeleteDialog && postToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa bài viết này không?") },
            confirmButton = {
                Button(
                    onClick = {
                        deletePost(postToDelete!!.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }


    // Handle search
    fun handleSearch() {
        if (searchQuery.isNotEmpty()) {
            navController.navigate("search")
        }
    }

    Scaffold(
        topBar = {
            // Top Bar với theme-aware colors
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp,
                color = surfaceColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .statusBarsPadding()
                ) {
                    // Logo và Tên logo
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
                            color = textColor,
                            modifier = Modifier.offset(y = 2.dp)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Avatar người dùng
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
                                    .background(iconColor.copy(alpha = 0.2f)),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = defaultAvatar)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Thanh tìm kiếm - FUNCTIONAL
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Tìm kiếm bài viết, người dùng...",
                                fontSize = 14.sp,
                                color = secondaryTextColor
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search Icon",
                                tint = iconColor,
                                modifier = Modifier.clickable { handleSearch() }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = if (ThemeManager.isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF5F5F5),
                            unfocusedContainerColor = if (ThemeManager.isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF5F5F5),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        singleLine = true,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { handleSearch() }
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        )
                    )
                }
            }
        },
        bottomBar = {
            TheHubBottomBar(navController, current = "home")
        }
    ) { paddingValues ->
        // Content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = accentColor,
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
                        PostItem(
                            post = post,
                            navController = navController,
                            onLongPress = {
                                if (post.authorId == currentUser?.uid) {
                                    postToDelete = post
                                    showDeleteDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// Bottom Navigation Bar Component
@Composable
fun TheHubBottomBar(navController: NavController, current: String) {
    val accentColor = ThemeManager.getAccentColor()
    val surfaceColor = ThemeManager.getSurfaceColor()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = surfaceColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left-side items
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomNavItem(
                    icon = Icons.Default.Home,
                    label = "Trang chủ",
                    isSelected = current == "home",
                    onClick = { navController.navigate("home") }
                )
                BottomNavItem(
                    icon = Icons.Default.Message,
                    label = "Tin nhắn",
                    isSelected = current == "messages",
                    onClick = { navController.navigate("messages") }
                )
            }

            // Center "Add Post" button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(4.dp, CircleShape)
                    .background(accentColor, CircleShape)
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

            // Right-side items
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomNavItem(
                    icon = Icons.Default.Notifications,
                    label = "Thông báo",
                    isSelected = current == "notifications",
                    onClick = { navController.navigate("notifications") }
                )
                BottomNavItem(
                    icon = Icons.Default.Person,
                    label = "Trang cá nhân",
                    isSelected = current == "profile",
                    onClick = { navController.navigate("profile") }
                )
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
    val textColor = ThemeManager.getTextColor()
    val accentColor = ThemeManager.getAccentColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()

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
                    if (isSelected) accentColor.copy(alpha = 0.1f) else Color.Transparent,
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) accentColor else secondaryTextColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) accentColor else secondaryTextColor,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PostItem(post: Post, navController: NavController, onLongPress: () -> Unit) {
    var isLiked by remember { mutableStateOf(post.likedBy.contains(Firebase.auth.currentUser?.uid)) }
    var likeCount by remember { mutableStateOf(post.likes) }
    var showComments by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore

    // Theme-aware colors
    val cardColor = ThemeManager.getCardColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()
    val iconColor = ThemeManager.getIconColor()
    val dividerColor = ThemeManager.getDividerColor()

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

    // Post Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .combinedClickable(
                onClick = { /* Handle regular click if needed */ },
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
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
                            .background(iconColor.copy(alpha = 0.2f)),
                        placeholder = painterResource(id = R.drawable.logomacdinh)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.author,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textColor,
                        modifier = Modifier.clickable {
                            navController.navigate("profile/${post.authorId}")
                        }
                    )
                    Text(
                        text = post.time,
                        fontSize = 12.sp,
                        color = secondaryTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            Text(
                text = post.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = textColor
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
                // Like button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { toggleLike() }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                    if (likeCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = likeCount.toString(),
                            fontSize = 13.sp,
                            color = secondaryTextColor,
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
                        tint = iconColor
                    )
                    if (post.comments > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = post.comments.toString(),
                            fontSize = 13.sp,
                            color = secondaryTextColor,
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
                    tint = iconColor
                )
            }

            // Comments section
            if (showComments) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = dividerColor)
                Spacer(modifier = Modifier.height(8.dp))
                CommentSection(postId = post.id)
            }
        }
    }
}