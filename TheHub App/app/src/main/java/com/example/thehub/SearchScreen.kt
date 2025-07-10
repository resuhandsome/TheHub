package com.example.thehub

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

// Data classes cho Search
sealed interface SearchItem {
    data class User(val uid: String, val username: String, val avatar: String = "") : SearchItem
    data class Post(val id: String, val snippet: String) : SearchItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(nav: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchItem>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = { Text("Tìm kiếm…") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ThemeManager.getSurfaceColor(),
                            unfocusedContainerColor = ThemeManager.getSurfaceColor(),
                            focusedTextColor = ThemeManager.getTextColor(),
                            unfocusedTextColor = ThemeManager.getTextColor()
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ThemeManager.getTextColor()
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeManager.getSurfaceColor()
                )
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .background(ThemeManager.getBackgroundColor())
        ) {
            items(searchResults) { item ->
                when (item) {
                    is SearchItem.User -> SearchRow(
                        icon = R.drawable.logomacdinh,
                        title = item.username,
                        subtitle = "Người dùng",
                        onClick = { nav.navigate("profile/${item.uid}") }
                    )
                    is SearchItem.Post -> SearchRow(
                        icon = R.drawable.iconchiase,
                        title = item.snippet.take(50),
                        subtitle = "Bài viết",
                        onClick = { /* Mở post detail */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchRow(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        leadingContent = {
            Image(
                painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        },
        headlineContent = {
            Text(
                title,
                color = ThemeManager.getTextColor()
            )
        },
        supportingContent = {
            Text(
                subtitle,
                color = ThemeManager.getSecondaryTextColor()
            )
        },
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp)
    )
    HorizontalDivider(color = ThemeManager.getDividerColor())
}
