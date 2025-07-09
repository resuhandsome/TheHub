package com.example.thehub

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
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
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// Lớp dữ liệu đại diện cho một bài đăng
data class Post(
    val author: String,
    val authorAvatarUrl: String,
    val time: String,
    val content: String,
    val imageUrl: String? = null
)

// bài đăng mẫu
val samplePosts = listOf(
    Post(
        author = "deanobeidallah",
        authorAvatarUrl = "https://i.pravatar.cc/150?u=deano",
        time = "1 ngày",
        content = "Trump started the fire with his erratic tariffs now he surrenders. Yet media claims Trump is a savior for making a trade deal with China. There is NO deal. Trump surrendered but not before he caused much pain to small and mid-sized businesses, dock workers, truckers and people with 401ks/stocks"
    ),
    Post(
        author = "walsh_freedom",
        authorAvatarUrl = "https://i.pravatar.cc/150?u=walsh",
        time = "1 ngày",
        content = "So economically, the next four years are gonna be just an endless cycle of 90 day “pauses” on all the bad shit Trump tries to do, huh?"
    ),
    Post(
        author = "viktoraxelsen",
        authorAvatarUrl = "https://i.pravatar.cc/150?u=viktor",
        time = "7 giờ",
        content = "A good day of filming for my fantastic partner HELM, together with The Company Film, Aarhus 😊🤝🎬",
        imageUrl = "https://i.imgur.com/8a3n2fC.jpeg"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    // Lấy thông tin người dùng hiện tại từ Firebase Auth
    val currentUser = Firebase.auth.currentUser

    // Xác định avatar để hiển thị
    val avatarUrl = currentUser?.photoUrl
    val defaultAvatar = R.drawable.logomacdinh

    Scaffold(
        topBar = {
            // Sử dụng Column để xếp chồng các thành phần theo chiều dọc
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8F8F8))
                    .padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 8.dp)
            ) {
                //Logo và Tên logo
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logothehub),
                        contentDescription = "App Logo",
                        modifier = Modifier.height(60.dp)
                    )
                    Spacer(modifier = Modifier.width(15.dp))
                    Text(
                        text = "TheHub",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                //  Thanh tìm kiếm và Avatar
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var searchText by remember { mutableStateOf("") }
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Avatar người dùng
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = defaultAvatar),
                            contentDescription = "Default Avatar",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Thanh điều hướng dưới cùng
            BottomAppBar(
                containerColor = Color.White,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                // thông báo
                IconButton(onClick = { /*TODO*/ }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }
                // thêm bài
                IconButton(onClick = { /*TODO*/ }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = "Add Post")
                }
                // ngôi nhà
                IconButton(onClick = { /*TODO*/ }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Home, contentDescription = "Home")
                }
            }
        }
    ) { paddingValues ->
        // Nội dung chính - Danh sách các bài đăng
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            items(samplePosts) { post ->
                PostItem(post = post)
                Divider(color = Color.LightGray, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun PostItem(post: Post) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Phần đầu của bài đăng (avatar, tên tác giả, thời gian)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = post.authorAvatarUrl,
                contentDescription = "Author Avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = post.author, fontWeight = FontWeight.Bold)
                Text(text = post.time, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Nội dung bài đăng
        Text(text = post.content, fontSize = 14.sp)

        // Hình ảnh của bài đăng (nếu có)
        if (post.imageUrl != null) {
            Spacer(modifier = Modifier.height(12.dp))
            AsyncImage(
                model = post.imageUrl,
                contentDescription = "Post Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Các nút hành động (Thích, Bình luận, Chia sẻ)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.icontim),
                contentDescription = "Like",
                modifier = Modifier.size(24.dp) // Chỉnh kích thước icon
            )
            Icon(
                painter = painterResource(id = R.drawable.iconbinhluan),
                contentDescription = "Comment",
                modifier = Modifier.size(24.dp) // Chỉnh kích thước icon
            )
            Icon(
                painter = painterResource(id = R.drawable.iconchiase),
                contentDescription = "Share",
                modifier = Modifier.size(24.dp) // Chỉnh kích thước icon
            )
        }
    }
}
