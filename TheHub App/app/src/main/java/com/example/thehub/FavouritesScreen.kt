package com.example.thehub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                val likedPostsSnapshot = db.collection("posts")
                    .whereArrayContains("likedBy", currentUser.uid)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                val likedPostsList = mutableListOf<Post>()

                for (doc in likedPostsSnapshot.documents) {
                    val authorId = doc.getString("authorId") ?: ""
                    var authorName = doc.getString("authorName") ?: "Unknown User"
                    var authorAvatarUrl = doc.getString("authorAvatarUrl") ?: ""

                    if (authorName == "Unknown User" && authorId.isNotEmpty()) {
                        try {
                            val authorProfile = UserRepository.getUserProfile(authorId)
                            if (authorProfile != null) {
                                authorName = authorProfile.username
                                authorAvatarUrl = authorProfile.avatarUrl
                            }
                        } catch (e: Exception) {
                            // Keep default values
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

                    likedPostsList.add(post)
                }

                likedPosts = likedPostsList

            } catch (e: Exception) {
                errorMessage = e.message ?: "Lỗi không xác định"
            } finally {
                isLoading = false
            }
        }
    }

    fun retryLoadLikedPosts() {
        isLoading = true
        errorMessage = ""
        coroutineScope.launch {
            // Retry logic
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
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
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = errorMessage,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { retryLoadLikedPosts() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
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
                            tint = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Chưa có bài viết nào được thích",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Hãy thích những bài viết yêu thích để xem lại sau",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
