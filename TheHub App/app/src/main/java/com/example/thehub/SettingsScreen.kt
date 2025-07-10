package com.example.thehub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser
    val coroutineScope = rememberCoroutineScope()

    // Dialog states
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        TopAppBar(
            title = {
                Text(
                    "Cài đặt",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // User Info Section
            item {
                if (currentUser != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = currentUser.displayName ?: "Người dùng",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentUser.email ?: "",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Appearance Section
            item {
                SettingsSection(title = "Giao diện") {
                    SettingsItem(
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
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )

                    SettingsItem(
                        icon = Icons.Filled.Language,
                        title = "Ngôn ngữ",
                        subtitle = "Tiếng Việt",
                        onClick = {
                            Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Account Section
            item {
                SettingsSection(title = "Tài khoản") {
                    SettingsItem(
                        icon = Icons.Filled.Person,
                        title = "Thông tin cá nhân",
                        subtitle = "Chỉnh sửa hồ sơ và thông tin cá nhân",
                        onClick = {
                            Toast.makeText(context, "Chuyển đến trang hồ sơ", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingsItem(
                        icon = Icons.Filled.Security,
                        title = "Bảo mật",
                        subtitle = "Mật khẩu và xác thực hai yếu tố",
                        onClick = {
                            Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingsItem(
                        icon = Icons.Filled.Notifications,
                        title = "Thông báo",
                        subtitle = "Quản lý thông báo và âm thanh",
                        onClick = {
                            Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Privacy Section
            item {
                SettingsSection(title = "Quyền riêng tư") {
                    SettingsItem(
                        icon = Icons.Filled.Lock,
                        title = "Quyền riêng tư",
                        subtitle = "Kiểm soát ai có thể xem nội dung của bạn",
                        onClick = {
                            Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingsItem(
                        icon = Icons.Filled.Block,
                        title = "Người dùng bị chặn",
                        subtitle = "Quản lý danh sách người dùng bị chặn",
                        onClick = {
                            Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingsItem(
                        icon = Icons.Filled.Visibility,
                        title = "Hoạt động",
                        subtitle = "Kiểm soát ai có thể thấy hoạt động của bạn",
                        onClick = {
                            Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Data & Storage Section
            item {
                SettingsSection(title = "Dữ liệu & Lưu trữ") {
                    SettingsItem(
                        icon = Icons.Filled.Storage,
                        title = "Quản lý dữ liệu",
                        subtitle = "Xóa cache và dữ liệu tạm thời",
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    // Clear app cache logic here
                                    Toast.makeText(context, "Đã xóa dữ liệu tạm thời", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Lỗi khi xóa dữ liệu", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    SettingsItem(
                        icon = Icons.Filled.Download,
                        title = "Tải xuống",
                        subtitle = "Quản lý file đã tải xuống",
                        onClick = {
                            Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Support Section
            item {
                SettingsSection(title = "Hỗ trợ") {
                    SettingsItem(
                        icon = Icons.Filled.Help,
                        title = "Trung tâm trợ giúp",
                        subtitle = "Câu hỏi thường gặp và hướng dẫn",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://help.thehub.com"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Không thể mở liên kết", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    SettingsItem(
                        icon = Icons.Filled.Feedback,
                        title = "Phản hồi",
                        subtitle = "Gửi ý kiến và báo cáo lỗi",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@thehub.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Phản hồi từ ứng dụng TheHub")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Không thể mở ứng dụng email", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    SettingsItem(
                        icon = Icons.Filled.Info,
                        title = "Về ứng dụng",
                        subtitle = "Phiên bản 1.0.0",
                        onClick = { showAboutDialog = true }
                    )

                    SettingsItem(
                        icon = Icons.Filled.Policy,
                        title = "Điều khoản dịch vụ",
                        subtitle = "Xem điều khoản sử dụng",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://thehub.com/terms"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Không thể mở liên kết", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // Logout Section
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    SettingsItem(
                        icon = Icons.Filled.Logout,
                        title = "Đăng xuất",
                        subtitle = "Đăng xuất khỏi tài khoản hiện tại",
                        iconTint = MaterialTheme.colorScheme.error,
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showLogoutDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Logout Confirmation Dialog
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
                                // Navigate to login screen
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
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // About Dialog
    if (showAboutDialog) {
        Dialog(onDismissRequest = { showAboutDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "TheHub",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Phiên bản 1.0.0",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Ứng dụng mạng xã hội kết nối mọi người. Được phát triển với ❤️ bằng Kotlin & Jetpack Compose.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showAboutDialog = false },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Đóng")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    } else {
        Modifier.fillMaxWidth()
    }

    Row(
        modifier = modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
