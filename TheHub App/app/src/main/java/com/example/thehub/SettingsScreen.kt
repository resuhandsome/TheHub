package com.example.thehub

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val scrollState = rememberScrollState()

    // Load user profile
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                currentUserProfile = UserRepository.getCurrentUserProfile()
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi tải thông tin người dùng", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    fun logout() {
        coroutineScope.launch {
            try {
                Firebase.auth.signOut()
                UserPreferences.clearSavedCredentials(context)
                Toast.makeText(context, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show()
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi đăng xuất: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Xác nhận đăng xuất",
                    fontWeight = FontWeight.Bold,
                    color = if (ThemeManager.isDarkMode) Color.White else Color.Black
                )
            },
            text = {
                Text(
                    "Bạn có chắc chắn muốn đăng xuất khỏi TheHub?",
                    color = if (ThemeManager.isDarkMode) Color.Gray else Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        logout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252)
                    )
                ) {
                    Text("Đăng xuất", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(
                        "Hủy",
                        color = if (ThemeManager.isDarkMode) Color.White else Color.Black
                    )
                }
            },
            containerColor = if (ThemeManager.isDarkMode) Color(0xFF2A2A2A) else Color.White
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cài đặt",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (ThemeManager.isDarkMode) Color.White else Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (ThemeManager.isDarkMode) Color.White else Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (ThemeManager.isDarkMode) Color(0xFF1A1A1A) else Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(if (ThemeManager.isDarkMode) Color(0xFF121212) else Color(0xFFFAFAFA))
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // User Profile Section
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF007AFF))
                }
            } else {
                UserProfileSection(
                    userProfile = currentUserProfile,
                    isDarkMode = ThemeManager.isDarkMode,
                    onEditClick = { navController.navigate("edit_profile") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Settings Section
            SettingsSection(
                title = "Cài đặt chính",
                isDarkMode = ThemeManager.isDarkMode
            ) {
                SettingItem(
                    icon = Icons.Default.Favorite,
                    title = "Favourites",
                    subtitle = "Xem các bài viết đã thích",
                    isDarkMode = ThemeManager.isDarkMode,
                    onClick = { navController.navigate("favourites") }
                )

                Divider(color = if (ThemeManager.isDarkMode) Color.Gray.copy(alpha = 0.3f) else Color(0xFFE0E0E0))

                SettingItem(
                    icon = Icons.Default.Person,
                    title = "Chỉnh sửa hồ sơ",
                    subtitle = "Thay đổi thông tin cá nhân",
                    isDarkMode = ThemeManager.isDarkMode,
                    onClick = { navController.navigate("edit_profile") }
                )

                Divider(color = if (ThemeManager.isDarkMode) Color.Gray.copy(alpha = 0.3f) else Color(0xFFE0E0E0))

                ThemeSettingItem(
                    isDarkMode = ThemeManager.isDarkMode,
                    onThemeChange = {
                        ThemeManager.setDarkMode(context, it)
                        Toast.makeText(
                            context,
                            if (it) "Đã bật chế độ tối" else "Đã tắt chế độ tối",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // quyền riêng tư
            SettingsSection(
                title = "Quyền riêng tư & Bảo mật",
                isDarkMode = ThemeManager.isDarkMode
            ) {
                SettingItem(
                    icon = Icons.Default.Lock,
                    title = "Quyền riêng tư",
                    subtitle = "Quản lý quyền riêng tư tài khoản",
                    isDarkMode = ThemeManager.isDarkMode,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://policies.google.com/privacy"))
                        context.startActivity(intent)
                    }
                )

                Divider(color = if (ThemeManager.isDarkMode) Color.Gray.copy(alpha = 0.3f) else Color(0xFFE0E0E0))

                SettingItem(
                    icon = Icons.Default.Notifications,
                    title = "Thông báo",
                    subtitle = "Cài đặt thông báo",
                    isDarkMode = ThemeManager.isDarkMode,
                    onClick = { navController.navigate("notifications") }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // đăng xuất
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5252)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Đăng xuất",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun UserProfileSection(
    userProfile: UserProfile?,
    isDarkMode: Boolean,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model = userProfile?.avatarUrl?.takeIf { it.isNotEmpty() },
                contentDescription = "Profile Avatar",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.3f)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.logomacdinh)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // User Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userProfile?.username ?: "Unknown User",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
                )

                if (!userProfile?.bio.isNullOrBlank()) {
                    Text(
                        text = userProfile?.bio ?: "",
                        fontSize = 14.sp,
                        color = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = userProfile?.email ?: "",
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color(0xFF888888) else Color(0xFF999999),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Edit Button
            IconButton(onClick = onEditClick) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = Color(0xFF007AFF)
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else Color(0xFF1A1A1A),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            content()
        }
    }
}

@Composable
fun ThemeSettingItem(
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
            contentDescription = "Theme",
            tint = Color(0xFF007AFF),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isDarkMode) "Dark Mode" else "Light Mode",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
            )
            Text(
                text = if (isDarkMode) "Chế độ tối đang bật" else "Chế độ sáng đang bật",
                fontSize = 14.sp,
                color = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
            )
        }

        Switch(
            checked = isDarkMode,
            onCheckedChange = onThemeChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF007AFF),
                checkedTrackColor = Color(0xFF007AFF).copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF007AFF),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Arrow",
            tint = if (isDarkMode) Color(0xFF666666) else Color(0xFFCCCCCC),
            modifier = Modifier.size(20.dp)
        )
    }
}