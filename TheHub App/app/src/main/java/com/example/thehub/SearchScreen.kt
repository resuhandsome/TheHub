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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(nav: NavController) {
    val viewModel: SearchViewModel = viewModel()
    val searchQuery by viewModel.query.collectAsState()
    val searchResults by viewModel.results.collectAsState()
    var recentSearches by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Theme colors
    val backgroundColor = ThemeManager.getBackgroundColor()
    val surfaceColor = ThemeManager.getSurfaceColor()
    val cardColor = ThemeManager.getCardColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()
    val accentColor = ThemeManager.getAccentColor()

    // Load recent searches from preferences
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("search_history", 0)
        recentSearches = prefs.getString("recent_searches", "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.take(5) ?: emptyList()
    }

    // Save search query to recent searches
    fun saveSearchQuery(query: String) {
        if (query.isBlank()) return
        val prefs = context.getSharedPreferences("search_history", 0)
        val updatedSearches = listOf(query) + recentSearches.filter { it != query }
        recentSearches = updatedSearches.take(5)
        prefs.edit().putString("recent_searches", recentSearches.joinToString(",")).apply()
    }

    // Debounced search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isSearching = true
            delay(500) // Debounce for 500ms
            viewModel.updateQuery(searchQuery)
            isSearching = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.updateQuery(it) },
                        onSearch = {
                            saveSearchQuery(it)
                            viewModel.updateQuery(it)
                        },
                        active = false,
                        onActiveChange = { },
                        placeholder = {
                            Text(
                                "Tìm kiếm người dùng, bài viết...",
                                color = secondaryTextColor
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = secondaryTextColor
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.updateQuery("") }
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = secondaryTextColor
                                    )
                                }
                            }
                        },
                        colors = SearchBarDefaults.colors(
                            containerColor = cardColor,
                            inputFieldColors = TextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                cursorColor = accentColor
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Search suggestions content
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Recent searches section
                if (searchQuery.isEmpty() && recentSearches.isNotEmpty()) {
                    item {
                        Text(
                            text = "Tìm kiếm gần đây",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(recentSearches) { recentSearch ->
                        RecentSearchItem(
                            query = recentSearch,
                            onClick = {
                                viewModel.updateQuery(recentSearch)
                                saveSearchQuery(recentSearch)
                            }
                        )
                    }
                }

                // Loading state
                if (isSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = accentColor,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                // Search results
                if (searchQuery.isNotEmpty() && !isSearching) {
                    if (searchResults.isEmpty()) {
                        item {
                            EmptySearchResults(query = searchQuery)
                        }
                    } else {
                        items(searchResults) { item ->
                            when (item) {
                                is SearchItem.User -> UserSearchResultItem(
                                    user = item,
                                    onClick = {
                                        nav.navigate("profile/${item.uid}")
                                    }
                                )
                                is SearchItem.Post -> PostSearchResultItem(
                                    post = item,
                                    onClick = {
                                        // TODO: Navigate to post detail
                                    }
                                )
                            }
                        }
                    }
                }

                // Popular searches when no query
                if (searchQuery.isEmpty() && recentSearches.isEmpty()) {
                    item {
                        PopularSearches(
                            onSearchClick = { query ->
                                viewModel.updateQuery(query)
                                saveSearchQuery(query)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSearchItem(
    query: String,
    onClick: () -> Unit
) {
    val cardColor = ThemeManager.getCardColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = "Recent search",
                tint = secondaryTextColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = query,
                color = textColor,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun UserSearchResultItem(
    user: SearchItem.User,
    onClick: () -> Unit
) {
    val cardColor = ThemeManager.getCardColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.avatar.takeIf { it.isNotEmpty() },
                contentDescription = "User avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.logomacdinh)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Người dùng",
                    color = secondaryTextColor,
                    fontSize = 14.sp
                )
            }
            Icon(
                Icons.Default.Person,
                contentDescription = "User",
                tint = ThemeManager.getAccentColor(),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PostSearchResultItem(
    post: SearchItem.Post,
    onClick: () -> Unit
) {
    val cardColor = ThemeManager.getCardColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(id = R.drawable.iconchiase),
                contentDescription = "Post",
                tint = secondaryTextColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.snippet.take(60) + if (post.snippet.length > 60) "..." else "",
                    color = textColor,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Bài viết",
                    color = secondaryTextColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EmptySearchResults(query: String) {
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🔍",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Không tìm thấy kết quả cho \"$query\"",
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Hãy thử từ khóa khác hoặc kiểm tra chính tả",
            color = secondaryTextColor,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun PopularSearches(
    onSearchClick: (String) -> Unit
) {
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()

    Column {
        Text(
            text = "Tìm kiếm phổ biến",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val popularSearches = listOf(
            "Công nghệ",
            "Du lịch",
            "Ẩm thực",
            "Thể thao",
            "Âm nhạc"
        )

        popularSearches.forEach { search ->
            RecentSearchItem(
                query = search,
                onClick = { onSearchClick(search) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
