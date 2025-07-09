package com.example.thehub

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(navController: NavController) { // Đổi tên hàm thành PostScreen
    // Lấy thông tin người dùng hiện tại
    val currentUser = Firebase.auth.currentUser
    val avatarUrl = currentUser?.photoUrl
    val userName = currentUser?.displayName ?: "Username"
    val defaultAvatar = R.drawable.logomacdinh

    var postText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "New post",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    Button(
                        onClick = { /* TODO: Xử lý đăng bài */ },
                        enabled = postText.isNotBlank() // Nút chỉ bật khi có nội dung
                    ) {
                        Text("Post")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF0F2F5))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF0F2F5))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                // Avatar người dùng
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = defaultAvatar),
                        contentDescription = "Default Avatar",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Tên người dùng và ô nhập liệu
                Column {
                    Text(text = userName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextField(
                        value = postText,
                        onValueChange = { postText = it },
                        placeholder = { Text("What to write.......") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nút thêm ảnh
            IconButton(onClick = { /* TODO: Mở thư viện ảnh */ }) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Add Photo",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Gray
                )
            }
        }
    }
}


