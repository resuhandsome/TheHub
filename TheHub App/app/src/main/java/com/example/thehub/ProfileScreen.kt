package com.example.thehub

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
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
import com.google.firebase.firestore.Query
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
    var isOwnProfile by remember { mutableStateOf(false) }
    var isFollowing by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Determine if this is own profile
    val targetUserId = userId ?: currentUser?.uid
    isOwnProfile = targetUserId == currentUser?.uid

    // Load user profile
    LaunchedEffect(targetUserId) {
        if (targetUserId != null) {
            try {
                if (isOwnProfile) {
                    userProfile = UserRepository.getCurrentUserProfile()
                } else {
                    userProfile = UserRepository.getUserProfile(targetUserId)
                }

                // Load user posts
                val postsSnapshot = db.collection("posts")
                    .whereEqualTo("authorId", targetUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val postsList = postsSnapshot.documents.map { doc ->
                    Post(
                        id = doc.id,
                        authorId = doc.getString("authorId") ?: "",
                        author = userProfile?.username ?: "Unknown User",
                        authorAvatarUrl = userProfile?.avatarUrl ?: "",
                        time = formatTime(doc.getLong("timestamp") ?: 0L),
                        content = doc.getString("content") ?: "",
                        imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList(),
                        likes = doc.getLong("likes")?.toInt() ?: 0,
                        likedBy = doc.get("likedBy") as? List<String> ?: emptyList(),
                        comments = doc.getLong("comments")?.toInt() ?: 0,
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }

                userPosts = postsList

            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi tải hồ sơ: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header
            item {
                ProfileHeader(
                    userProfile = userProfile,
                    isOwnProfile = isOwnProfile,
                    isFollowing = isFollowing,
                    onFollowClick = { /* Follow logic */ },
                    onEditClick = { /* Edit profile */ },
                    onSettingsClick = { showSettingsSheet = true }
                )
            }

            // Stats Section
            item {
                ProfileStats(
                    postsCount = userPosts.size,
                    followersCount = userProfile?.followersCount ?: 0,
                    followingCount = userProfile?.followingCount ?: 0
                )
            }

            // Posts Section
            item {
                Text(
                    text = "Bài viết (${userPosts.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (userPosts.isEmpty()) {
                item {
                    EmptyPostsState(isOwnProfile = isOwnProfile)
                }
            } else {
                items(userPosts) { post ->
                    PostItem(post = post, navController = navController)
                }
            }
        }
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SettingsBottomSheetContent(
                onDismiss = { showSettingsSheet = false },
                onLogout = { showLogoutDialog = true }
            )
        }
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    "Xác nhận đăng xuất",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản không?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                Firebase.auth.signOut()
                                Toast.makeText(context, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show()
                                showLogoutDialog = false
                                showSettingsSheet = false
                                // Navigate to auth screen
                                navController.navigate("auth") {
                                    popUpTo("home") { inclusive = true }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Lỗi khi đăng xuất", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Đăng xuất")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Hủy")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun ProfileHeader(
    userProfile: UserProfile?,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onEditClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape),
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
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!userProfile?.displayName.isNullOrBlank() && userProfile?.displayName != userProfile?.username) {
                Text(
                    text = userProfile?.displayName ?: "",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Bio
            if (!userProfile?.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = userProfile?.bio ?: "",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isOwnProfile) {
                    Button(
                        onClick = onEditClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chỉnh sửa")
                    }

                    OutlinedButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cài đặt")
                    }
                } else {
                    Button(
                        onClick = onFollowClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (isFollowing) "Đã theo dõi" else "Theo dõi",
                            color = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onPrimary
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                color = MaterialTheme.colorScheme.outline
            )
            StatItem(count = followersCount, label = "Người theo dõi")
            Divider(
                modifier = Modifier.height(40.dp).width(1.dp),
                color = MaterialTheme.colorScheme.outline
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
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyPostsState(isOwnProfile: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Photo,
            contentDescription = "No Posts",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isOwnProfile) "Chưa có bài viết nào" else "Người dùng chưa đăng bài viết nào",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        if (isOwnProfile) {
            Text(
                text = "Hãy chia sẻ khoảnh khắc đầu tiên của bạn!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun SettingsBottomSheetContent(
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Cài đặt",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Dark Mode Toggle
        SharedSettingsItem(
            icon = Icons.Filled.Palette,
            title = "Chế độ tối",
            subtitle = "Chuyển đổi giữa giao diện sáng và tối",
            trailing = {
                Switch(
                    checked = ThemeManager.isDarkMode,
                    onCheckedChange = {
                        ThemeManager.toggleDarkMode()
                        Toast.makeText(
                            context,
                            if (ThemeManager.isDarkMode) "Đã bật chế độ tối" else "Đã tắt chế độ tối",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        )

        Divider(color = MaterialTheme.colorScheme.outline)

        // Other Settings
        SharedSettingsItem(
            icon = Icons.Filled.Notifications,
            title = "Thông báo",
            subtitle = "Quản lý thông báo",
            onClick = {
                Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
            }
        )

        SharedSettingsItem(
            icon = Icons.Filled.Lock,
            title = "Quyền riêng tư",
            subtitle = "Cài đặt quyền riêng tư",
            onClick = {
                Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
            }
        )

        SharedSettingsItem(
            icon = Icons.Filled.Help,
            title = "Trợ giúp",
            subtitle = "Câu hỏi thường gặp",
            onClick = {
                Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
            }
        )

        Divider(color = MaterialTheme.colorScheme.outline)

        // Logout
        SharedSettingsItem(
            icon = Icons.Filled.Logout,
            title = "Đăng xuất",
            subtitle = "Đăng xuất khỏi tài khoản",
            iconTint = MaterialTheme.colorScheme.error,
            titleColor = MaterialTheme.colorScheme.error,
            onClick = onLogout
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
