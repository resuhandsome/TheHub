package com.example.thehub

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(navController: NavController) {
    var likedPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore

    // Theme-aware colors
    val backgroundColor = ThemeManager.getBackgroundColor()
    val surfaceColor = ThemeManager.getSurfaceColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()
    val iconColor = ThemeManager.getIconColor()
    val accentColor = ThemeManager.getAccentColor()

    // Load liked posts
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                isLoading = true
                errorMessage = ""

                // Lấy tất cả posts mà user đã like
                val postsSnapshot = db.collection("posts")
                    .whereArrayContains("likedBy", currentUser.uid)
                    .get()
                    .await()

                likedPosts = postsSnapshot.documents.mapNotNull { doc ->
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

            } catch (e: Exception) {
                errorMessage = "Lỗi khi tải bài viết đã thích: ${e.message}"
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        } else {
            errorMessage = "Chưa đăng nhập"
            isLoading = false
        }
    }

    fun retryLoadLikedPosts() {
        coroutineScope.launch {
            if (currentUser != null) {
                try {
                    isLoading = true
                    errorMessage = ""

                    val postsSnapshot = db.collection("posts")
                        .whereArrayContains("likedBy", currentUser.uid)
                        .get()
                        .await()

                    likedPosts = postsSnapshot.documents.mapNotNull { doc ->
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

                } catch (e: Exception) {
                    errorMessage = "Lỗi khi tải bài viết đã thích: ${e.message}"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Bài viết đã thích",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }

                errorMessage.isNotEmpty() && likedPosts.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "⚠️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Không thể tải bài viết",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = errorMessage,
                            fontSize = 14.sp,
                            color = secondaryTextColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { retryLoadLikedPosts() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor
                            )
                        ) {
                            Text("Thử lại")
                        }
                    }
                }

                likedPosts.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "No Likes",
                            modifier = Modifier.size(64.dp),
                            tint = iconColor.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Chưa có bài viết nào được thích",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Hãy thích những bài viết yêu thích để xem lại sau",
                            fontSize = 14.sp,
                            color = secondaryTextColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(likedPosts) { post ->
                            PostItem(post = post, navController = navController)
                        }
                    }
                }
            }
        }
    }
}
