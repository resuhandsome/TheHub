package com.example.thehub

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(navController: NavController, userId: String? = null) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var userPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isFollowing by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Bài viết") } // Thay đổi giá trị mặc định
    var isFollowLoading by remember { mutableStateOf(false) }

    var followersCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }


    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore
    val isOwnProfile = userId == null || userId == currentUser?.uid

    // Theme-aware colors
    val backgroundColor = ThemeManager.getBackgroundColor()
    val surfaceColor = ThemeManager.getSurfaceColor()
    val cardColor = ThemeManager.getCardColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()
    val iconColor = ThemeManager.getIconColor()
    val dividerColor = ThemeManager.getDividerColor()
    val accentColor = ThemeManager.getAccentColor()

    suspend fun loadProfileData() {
        try {
            val targetUserId = userId ?: currentUser?.uid
            if (targetUserId != null) {
                userProfile = UserRepository.getUserProfile(targetUserId)

                val followersSnapshot = db.collection("follows")
                    .whereEqualTo("followingId", targetUserId)
                    .get()
                    .await()

                val followingSnapshot = db.collection("follows")
                    .whereEqualTo("followerId", targetUserId)
                    .get()
                    .await()

                followersCount = followersSnapshot.size()
                followingCount = followingSnapshot.size()

                if (userProfile != null) {
                    val updatedProfile = userProfile!!.copy(
                        followersCount = followersCount,
                        followingCount = followingCount
                    )
                    UserRepository.updateUserProfile(updatedProfile)
                    userProfile = updatedProfile
                }

                val postsSnapshot = db.collection("posts")
                    .whereEqualTo("authorId", targetUserId)
                    .get()
                    .await()

                userPosts = postsSnapshot.documents.mapNotNull { doc ->
                    try {
                        Post(
                            id = doc.id,
                            authorId = doc.getString("authorId") ?: "",
                            author = doc.getString("authorName") ?: "",
                            authorAvatarUrl = doc.getString("authorAvatarUrl") ?: "",
                            time = formatTime(doc.getLong("timestamp") ?: 0L),
                            content = doc.getString("content") ?: "",
                            imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList(),
                            likes = doc.getLong("likes")?.toInt() ?: 0,
                            likedBy = doc.get("likedBy") as? List<String> ?: emptyList(),
                            comments = doc.getLong("comments")?.toInt() ?: 0,
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.timestamp }

                if (!isOwnProfile && currentUser != null && targetUserId != currentUser.uid) {
                    val followDoc = db.collection("follows")
                        .whereEqualTo("followerId", currentUser.uid)
                        .whereEqualTo("followingId", targetUserId)
                        .get()
                        .await()

                    isFollowing = !followDoc.isEmpty
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi khi tải profile: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(userId, currentUser) {
        isLoading = true
        loadProfileData()
        isLoading = false
    }
    fun deletePost(postId: String) {
        coroutineScope.launch {
            try {
                db.collection("posts").document(postId).delete().await()
                userPosts = userPosts.filterNot { it.id == postId }
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


    fun toggleFollow() {
        if (currentUser == null || userProfile == null || isFollowLoading) return

        coroutineScope.launch {
            isFollowLoading = true
            try {
                val targetUserId = userProfile!!.uid

                if (isFollowing) {
                    val followDocs = db.collection("follows")
                        .whereEqualTo("followerId", currentUser.uid)
                        .whereEqualTo("followingId", targetUserId)
                        .get()
                        .await()

                    followDocs.documents.forEach { doc ->
                        db.collection("follows").document(doc.id).delete().await()
                    }
                    isFollowing = false
                    Toast.makeText(context, "Đã hủy theo dõi", Toast.LENGTH_SHORT).show()

                } else {
                    val followData = hashMapOf(
                        "followerId" to currentUser.uid,
                        "followingId" to targetUserId,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("follows").add(followData).await()

                    val currentUserProfile = UserRepository.getCurrentUserProfile()
                    if (currentUserProfile != null) {
                        val notificationData = hashMapOf(
                            "type" to "follow",
                            "fromUserId" to currentUser.uid,
                            "fromUsername" to currentUserProfile.username,
                            "fromUserAvatar" to currentUserProfile.avatarUrl,
                            "toUserId" to targetUserId,
                            "message" to "đã theo dõi bạn",
                            "timestamp" to System.currentTimeMillis(),
                            "isRead" to false
                        )

                        db.collection("notifications").add(notificationData).await()
                    }

                    isFollowing = true
                    Toast.makeText(context, "Đã theo dõi", Toast.LENGTH_SHORT).show()
                }

                // Recalculate counts for both users
                UserRepository.recalculateFollowerCounts(currentUser.uid)
                UserRepository.recalculateFollowerCounts(targetUserId)
                loadProfileData()

            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isFollowLoading = false
            }
        }
    }

    // (PHẦN THÊM MỚI) Logic để bắt đầu cuộc trò chuyện
    fun startOrNavigateToChat() {
        if (currentUser == null || userProfile == null) return

        coroutineScope.launch {
            try {
                val targetUserId = userProfile!!.uid
                val participantIds = listOf(currentUser.uid, targetUserId).sorted()

                // 1. Tìm cuộc trò chuyện hiện có
                val existingConvo = db.collection("conversations")
                    .whereEqualTo("participants", participantIds)
                    .limit(1)
                    .get()
                    .await()

                if (existingConvo.isEmpty) {
                    // 2. Nếu không có, tạo cuộc trò chuyện mới
                    val newConvoData = hashMapOf(
                        "participants" to participantIds,
                        "lastMessage" to "Bắt đầu cuộc trò chuyện",
                        "lastUpdate" to System.currentTimeMillis()
                    )
                    val newConvoRef = db.collection("conversations").add(newConvoData).await()
                    navController.navigate("chat/${newConvoRef.id}")
                } else {
                    // 3. Nếu có, điều hướng đến nó
                    val conversationId = existingConvo.documents.first().id
                    navController.navigate("chat/${conversationId}")
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Không thể bắt đầu trò chuyện: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = userProfile?.username ?: "Profile",
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
                actions = {
                    if (isOwnProfile) {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = textColor
                            )
                        }
                    } else {
                        IconButton(onClick = { /* TODO: More options */ }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = textColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(backgroundColor)
            ) {
                item {
                    ProfileHeader(
                        userProfile = userProfile,
                        isOwnProfile = isOwnProfile,
                        isFollowing = isFollowing,
                        isFollowLoading = isFollowLoading,
                        onFollowClick = { toggleFollow() },
                        onEditClick = {
                            navController.navigate("edit_profile")
                        },
                        // (PHẦN THÊM MỚI)
                        onMessageClick = { startOrNavigateToChat() }
                    )
                }

                item {
                    ProfileStats(
                        postsCount = userPosts.size,
                        followersCount = followersCount,
                        followingCount = followingCount
                    )
                }

                item {
                    if (!userProfile?.bio.isNullOrBlank()) {
                        ProfileBio(bio = userProfile?.bio ?: "")
                    }
                }

                item {
                    ProfileTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }

                item {
                    when (selectedTab) {
                        "Bài viết" -> {
                            if (userPosts.isEmpty()) {
                                EmptyPostsSection(isOwnProfile = isOwnProfile)
                            } else {
                                PostsGrid(
                                    posts = userPosts,
                                    onLongPress = { post ->
                                        if (isOwnProfile) {
                                            postToDelete = post
                                            showDeleteDialog = true
                                        }
                                    }
                                )
                            }
                        }
                        "Ảnh" -> {
                            MediaGrid(posts = userPosts.filter { it.imageUrls.isNotEmpty() })
                        }
                        "Được gắn thẻ" -> {
                            TaggedSection()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    userProfile: UserProfile?,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    isFollowLoading: Boolean,
    onFollowClick: () -> Unit,
    onEditClick: () -> Unit,
    onMessageClick: () -> Unit // (PHẦN THÊM MỚI)
) {
    val cardColor = ThemeManager.getCardColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()
    val accentColor = ThemeManager.getAccentColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(8.dp, CircleShape)
            ) {
                AsyncImage(
                    model = userProfile?.avatarUrl?.takeIf { it.isNotEmpty() },
                    contentDescription = "Profile Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.3f))
                        .border(4.dp, cardColor, CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.logomacdinh)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = userProfile?.username ?: "Unknown User",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            if (!userProfile?.displayName.isNullOrBlank() && userProfile?.displayName != userProfile?.username) {
                Text(
                    text = userProfile?.displayName ?: "",
                    fontSize = 16.sp,
                    color = secondaryTextColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isOwnProfile) {
                    Button(
                        onClick = onEditClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Chỉnh sửa",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Button(
                        onClick = onFollowClick,
                        enabled = !isFollowLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) Color(0xFFE0E0E0) else accentColor,
                            contentColor = if (isFollowing) secondaryTextColor else Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1f)
                    ) {
                        if (isFollowLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = if (isFollowing) Color.Gray else Color.White
                            )
                        } else {
                            Text(
                                text = if (isFollowing) "Đang theo dõi" else "Theo dõi",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onMessageClick() },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1f),
                        border = BorderStroke(1.dp, accentColor)
                    ) {
                        Text(
                            "Nhắn tin",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStats(
    postsCount: Int,
    followersCount: Int,
    followingCount: Int
) {
    val cardColor = ThemeManager.getCardColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()
    val dividerColor = ThemeManager.getDividerColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(count = postsCount, label = "Bài viết")
            Divider(
                modifier = Modifier.height(40.dp).width(1.dp),
                color = dividerColor
            )
            StatItem(count = followersCount, label = "Người theo dõi")
            Divider(
                modifier = Modifier.height(40.dp).width(1.dp),
                color = dividerColor
            )
            StatItem(count = followingCount, label = "Đang theo dõi")
        }
    }
}

@Composable
fun StatItem(count: Int, label: String) {
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = secondaryTextColor
        )
    }
}

@Composable
fun ProfileBio(bio: String) {
    val cardColor = ThemeManager.getCardColor()
    val textColor = ThemeManager.getTextColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = bio,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = textColor,
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Composable
fun ProfileTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf("Bài viết", "Ảnh", "Được gắn thẻ")
    val cardColor = ThemeManager.getCardColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()
    val accentColor = ThemeManager.getAccentColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) accentColor else Color.Transparent
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else secondaryTextColor
                    )
                }
            }
        }
    }
}

@Composable
fun PostsGrid(posts: List<Post>, onLongPress: (Post) -> Unit) {
    val cardColor = ThemeManager.getCardColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(400.dp)
        ) {
            items(posts) { post ->
                PostGridItem(post = post, onLongPress = { onLongPress(post) })
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PostGridItem(post: Post, onLongPress: () -> Unit) {
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = { /* TODO: Open post detail */ },
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        if (post.imageUrls.isNotEmpty()) {
            AsyncImage(
                model = post.imageUrls.first(),
                contentDescription = "Post Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (ThemeManager.isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = post.content.take(50) + if (post.content.length > 50) "..." else "",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = secondaryTextColor,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun MediaGrid(posts: List<Post>) {
    val cardColor = ThemeManager.getCardColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (posts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có ảnh nào",
                    fontSize = 16.sp,
                    color = secondaryTextColor
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(400.dp)
            ) {
                items(posts.flatMap { it.imageUrls }) { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Media",
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun TaggedSection() {
    val cardColor = ThemeManager.getCardColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Chưa có bài viết được gắn thẻ",
                fontSize = 16.sp,
                color = secondaryTextColor
            )
        }
    }
}

@Composable
fun EmptyPostsSection(isOwnProfile: Boolean) {
    val cardColor = ThemeManager.getCardColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "📝", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isOwnProfile) "Chưa có bài viết nào" else "Người dùng chưa có bài viết",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isOwnProfile) "Hãy tạo bài viết đầu tiên của bạn!" else "Hãy quay lại sau nhé!",
                fontSize = 14.sp,
                color = secondaryTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}