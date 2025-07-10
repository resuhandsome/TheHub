package com.example.thehub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchType by remember { mutableStateOf(SearchType.ALL) }

    val db = Firebase.firestore
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun performSearch() {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            return
        }

        isLoading = true
        coroutineScope.launch {
            try {
                val results = mutableListOf<SearchResult>()

                // Search Posts
                if (searchType == SearchType.ALL || searchType == SearchType.POSTS) {
                    val postsSnapshot = db.collection("posts")
                        .orderBy("timestamp")
                        .get()
                        .await()

                    postsSnapshot.documents.forEach { doc ->
                        val content = doc.getString("content") ?: ""
                        val authorName = doc.getString("authorName") ?: ""

                        if (content.contains(searchQuery, ignoreCase = true) ||
                            authorName.contains(searchQuery, ignoreCase = true)) {

                            results.add(
                                SearchResult.PostResult(
                                    Post(
                                        id = doc.id,
                                        authorId = doc.getString("authorId") ?: "",
                                        author = authorName,
                                        authorAvatarUrl = doc.getString("authorAvatarUrl") ?: "",
                                        time = formatTime(doc.getLong("timestamp") ?: 0L),
                                        content = content,
                                        imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList(),
                                        likes = doc.getLong("likes")?.toInt() ?: 0,
                                        likedBy = doc.get("likedBy") as? List<String> ?: emptyList(),
                                        comments = doc.getLong("comments")?.toInt() ?: 0,
                                        timestamp = doc.getLong("timestamp") ?: 0L
                                    )
                                )
                            )
                        }
                    }
                }

                // Search Users
                if (searchType == SearchType.ALL || searchType == SearchType.USERS) {
                    val usersSnapshot = db.collection("users")
                        .get()
                        .await()

                    usersSnapshot.documents.forEach { doc ->
                        val username = doc.getString("username") ?: ""
                        val displayName = doc.getString("displayName") ?: ""

                        if (username.contains(searchQuery, ignoreCase = true) ||
                            displayName.contains(searchQuery, ignoreCase = true)) {

                            results.add(
                                SearchResult.UserResult(
                                    UserProfile(
                                        id = doc.id,
                                        username = username,
                                        displayName = displayName,
                                        avatarUrl = doc.getString("avatarUrl") ?: "",
                                        bio = doc.getString("bio") ?: "",
                                        followersCount = doc.getLong("followersCount")?.toInt() ?: 0,
                                        followingCount = doc.getLong("followingCount")?.toInt() ?: 0
                                    )
                                )
                            )
                        }
                    }
                }

                searchResults = results.take(50) // Limit results

            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            performSearch()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Tìm kiếm bài viết, người dùng...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Type Tabs
            ScrollableTabRow(
                selectedTabIndex = searchType.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                SearchType.values().forEach { type ->
                    Tab(
                        selected = searchType == type,
                        onClick = {
                            searchType = type
                            if (searchQuery.isNotBlank()) {
                                performSearch()
                            }
                        },
                        text = {
                            Text(
                                text = when (type) {
                                    SearchType.ALL -> "Tất cả"
                                    SearchType.POSTS -> "Bài viết"
                                    SearchType.USERS -> "Người dùng"
                                }
                            )
                        }
                    )
                }
            }

            // Search Results
            Box(modifier = Modifier.fillMaxSize()) {
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

                    searchQuery.isBlank() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Tìm kiếm bài viết và người dùng",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Text(
                                text = "Nhập từ khóa để bắt đầu tìm kiếm",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    searchResults.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Không tìm thấy kết quả",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Text(
                                text = "Thử tìm kiếm với từ khóa khác",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults) { result ->
                                when (result) {
                                    is SearchResult.PostResult -> {
                                        PostItem(post = result.post, navController = navController)
                                    }
                                    is SearchResult.UserResult -> {
                                        UserSearchItem(
                                            user = result.user,
                                            onClick = {
                                                navController.navigate("profile/${result.user.id}")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchItem(
    user: UserProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.logomacdinh)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (user.displayName.isNotEmpty() && user.displayName != user.username) {
                    Text(
                        text = user.displayName,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (user.bio.isNotEmpty()) {
                    Text(
                        text = user.bio,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = "${user.followersCount} followers",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class SearchType {
    ALL, POSTS, USERS
}

sealed class SearchResult {
    data class PostResult(val post: Post) : SearchResult()
    data class UserResult(val user: UserProfile) : SearchResult()
}
