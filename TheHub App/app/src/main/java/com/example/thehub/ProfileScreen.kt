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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, userId: String? = null) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var userPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isFollowing by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Posts") }
    var isFollowLoading by remember { mutableStateOf(false) }

    // STATE CHO FOLLOWER COUNTS - QUAN TRỌNG
    var followersCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore
    val isOwnProfile = userId == null || userId == currentUser?.uid

    // FUNCTION LOAD PROFILE DATA
    suspend fun loadProfileData() {
        try {
            val targetUserId = userId ?: currentUser?.uid
            if (targetUserId != null) {
                // Load user profile
                userProfile = UserRepository.getUserProfile(targetUserId)

                // Load real-time follower counts từ follows collection
                val followersSnapshot = db.collection("follows")
                    .whereEqualTo("followingId", targetUserId)
                    .get()
                    .await()

                val followingSnapshot = db.collection("follows")
                    .whereEqualTo("followerId", targetUserId)
                    .get()
                    .await()

                // CẬP NHẬT STATE VỚI SỐ THỰC TẾ
                followersCount = followersSnapshot.size()
                followingCount = followingSnapshot.size()

                // Cập nhật user profile với số thực tế
                if (userProfile != null) {
                    val updatedProfile = userProfile!!.copy(
                        followersCount = followersCount,
                        followingCount = followingCount
                    )
                    UserRepository.updateUserProfile(updatedProfile)
                    userProfile = updatedProfile
                }

                // Load posts
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

                // Check following status
                if (!isOwnProfile && currentUser != null && targetUserId != currentUser.uid) {
                    val followDoc = db.collection("follows")
                        .whereEqualTo("followerId", currentUser.uid)
                        .whereEqualTo("followingId", targetUserId)
                        .get()
                        .await()

                    isFollowing = !followDoc.isEmpty
                }

                println("DEBUG: Loaded profile - Followers: $followersCount, Following: $followingCount")
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi khi tải profile: ${e.message}", Toast.LENGTH_SHORT).show()
            println("DEBUG: Error loading profile - ${e.message}")
        }
    }

    // Load data khi component mount
    LaunchedEffect(userId, currentUser) {
        isLoading = true
        loadProfileData()
        isLoading = false
    }

    // FUNCTION TOGGLE FOLLOW VỚI LOGIC HOÀN CHỈNH
    fun toggleFollow() {
        if (currentUser == null || userProfile == null || isFollowLoading) return

        coroutineScope.launch {
            isFollowLoading = true
            try {
                val targetUserId = userProfile!!.uid

                if (isFollowing) {
                    // UNFOLLOW PROCESS
                    println("DEBUG: Starting unfollow process")

                    // 1. Xóa follow record
                    val followDocs = db.collection("follows")
                        .whereEqualTo("followerId", currentUser.uid)
                        .whereEqualTo("followingId", targetUserId)
                        .get()
                        .await()

                    followDocs.documents.forEach { doc ->
                        db.collection("follows").document(doc.id).delete().await()
                    }

                    // 2. Cập nhật counts ngay lập tức
                    followersCount = maxOf(0, followersCount - 1)

                    // 3. Cập nhật database
                    db.collection("users").document(targetUserId)
                        .update("followersCount", followersCount).await()

                    if (currentUser.uid != targetUserId) {
                        val currentUserDoc = db.collection("users").document(currentUser.uid).get().await()
                        val currentFollowing = currentUserDoc.getLong("followingCount")?.toInt() ?: 0
                        val newFollowing = maxOf(0, currentFollowing - 1)
                        db.collection("users").document(currentUser.uid)
                            .update("followingCount", newFollowing).await()
                    }

                    // 4. Cập nhật UI state
                    isFollowing = false

                    Toast.makeText(context, "Đã hủy theo dõi", Toast.LENGTH_SHORT).show()
                    println("DEBUG: Unfollow completed - New followers count: $followersCount")

                } else {
                    // FOLLOW PROCESS
                    println("DEBUG: Starting follow process")

                    // 1. Tạo follow record
                    val followData = hashMapOf(
                        "followerId" to currentUser.uid,
                        "followingId" to targetUserId,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("follows").add(followData).await()

                    // 2. Cập nhật counts ngay lập tức
                    followersCount += 1

                    // 3. Cập nhật database
                    db.collection("users").document(targetUserId)
                        .update("followersCount", followersCount).await()

                    if (currentUser.uid != targetUserId) {
                        val currentUserDoc = db.collection("users").document(currentUser.uid).get().await()
                        val currentFollowing = currentUserDoc.getLong("followingCount")?.toInt() ?: 0
                        val newFollowing = currentFollowing + 1
                        db.collection("users").document(currentUser.uid)
                            .update("followingCount", newFollowing).await()
                    }

                    // 4. Tạo notification
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

                    // 5. Cập nhật UI state
                    isFollowing = true

                    Toast.makeText(context, "Đã theo dõi", Toast.LENGTH_SHORT).show()
                    println("DEBUG: Follow completed - New followers count: $followersCount")
                }

                // 6. Refresh profile để đảm bảo consistency
                loadProfileData()

            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                println("DEBUG: Error in toggleFollow - ${e.message}")
            } finally {
                isFollowLoading = false
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
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isOwnProfile) {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    } else {
                        IconButton(onClick = { /* TODO: More options */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF007AFF))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFFAFAFA))
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
                        }
                    )
                }

                item {
                    // SỬ DỤNG STATE COUNTS THAY VÌ USERPROFILE
                    ProfileStats(
                        postsCount = userPosts.size,
                        followersCount = followersCount, // SỬ DỤNG STATE
                        followingCount = followingCount  // SỬ DỤNG STATE
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
                        "Posts" -> {
                            if (userPosts.isEmpty()) {
                                EmptyPostsSection(isOwnProfile = isOwnProfile)
                            } else {
                                PostsGrid(posts = userPosts)
                            }
                        }
                        "Media" -> {
                            MediaGrid(posts = userPosts.filter { it.imageUrls.isNotEmpty() })
                        }
                        "Tagged" -> {
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
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
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
                        .border(4.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.logomacdinh)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Username
            Text(
                text = userProfile?.username ?: "Unknown User",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )

            if (!userProfile?.displayName.isNullOrBlank() && userProfile?.displayName != userProfile?.username) {
                Text(
                    text = userProfile?.displayName ?: "",
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isOwnProfile) {
                    Button(
                        onClick = onEditClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF)
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
                            containerColor = if (isFollowing) Color(0xFFE0E0E0) else Color(0xFF007AFF)
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
                                fontWeight = FontWeight.Medium,
                                color = if (isFollowing) Color(0xFF666666) else Color.White
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { /* TODO: Message */ },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1f),
                        border = BorderStroke(1.dp, Color(0xFF007AFF))
                    ) {
                        Text(
                            "Nhắn tin",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF007AFF)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                color = Color(0xFFE0E0E0)
            )
            StatItem(count = followersCount, label = "Người theo dõi")
            Divider(
                modifier = Modifier.height(40.dp).width(1.dp),
                color = Color(0xFFE0E0E0)
            )
            StatItem(count = followingCount, label = "Đang theo dõi")
        }
    }
}

@Composable
fun StatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
    }
}

@Composable
fun ProfileBio(bio: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = bio,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Color(0xFF333333),
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Composable
fun ProfileTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf("Posts", "Media", "Tagged")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            if (isSelected) Color(0xFF007AFF) else Color.Transparent
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF666666)
                    )
                }
            }
        }
    }
}

@Composable
fun PostsGrid(posts: List<Post>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                PostGridItem(post = post)
            }
        }
    }
}

@Composable
fun PostGridItem(post: Post) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { /* TODO: Open post detail */ },
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
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = post.content.take(50) + if (post.content.length > 50) "..." else "",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun MediaGrid(posts: List<Post>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    color = Color(0xFF666666)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
fun EmptyPostsSection(isOwnProfile: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isOwnProfile) "Hãy tạo bài viết đầu tiên của bạn!" else "Hãy quay lại sau nhé!",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

