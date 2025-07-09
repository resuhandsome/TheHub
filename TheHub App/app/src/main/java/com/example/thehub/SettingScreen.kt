package com.example.thehub

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(navController: NavController) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Thêm Modifier.clickable vào Row này
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            navController.navigate("home") {
                                // Xóa các màn hình phía trên để không quay lại được
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logothehub),
                            contentDescription = "App Logo",
                            modifier = Modifier.height(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TheHub", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        // Bạn có thể giữ lại BottomAppBar nếu muốn nó hiển thị ở đây
        // bottomBar = { ProfileBottomNavBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingItem(
                icon = Icons.Default.FavoriteBorder,
                text = "Favourite",
                onClick = {
                    Toast.makeText(context, "Chức năng Favourite chưa được cài đặt", Toast.LENGTH_SHORT).show()
                }
            )
            Divider()
            SettingItem(
                icon = Icons.Default.Settings,
                text = "Setting",
                onClick = {
                    Toast.makeText(context, "Chức năng Setting chưa được cài đặt", Toast.LENGTH_SHORT).show()
                }
            )
            Divider()
            SettingItem(
                icon = Icons.Default.ExitToApp,
                text = "Sign out",
                onClick = {
                    // Xử lý đăng xuất
                    FirebaseAuth.getInstance().signOut()
                    // Điều hướng về màn hình login và xóa hết các màn hình cũ
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
            Divider()
        }
    }
}

@Composable
fun SettingItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, fontSize = 18.sp)
    }
}