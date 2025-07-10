package com.example.thehub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// Simple conversation data class
data class Conversation(
    val id: String = "",
    val title: String = "",
    val lastMessage: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(nav: NavController) {
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    val currentUser = Firebase.auth.currentUser

    LaunchedEffect(currentUser) {
        // Mock data for demonstration
        conversations = listOf(
            Conversation("1", "John Doe", "Hello there!", System.currentTimeMillis()),
            Conversation("2", "Jane Smith", "How are you?", System.currentTimeMillis() - 3600000)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tin nhắn",
                        color = ThemeManager.getTextColor()
                    )
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
            items(conversations) { conversation ->
                ListItem(
                    headlineContent = {
                        Text(
                            conversation.title,
                            color = ThemeManager.getTextColor()
                        )
                    },
                    supportingContent = {
                        Text(
                            conversation.lastMessage,
                            color = ThemeManager.getSecondaryTextColor()
                        )
                    },
                    modifier = Modifier
                        .clickable {
                            nav.navigate("chat/${conversation.id}")
                        }
                        .padding(horizontal = 8.dp)
                )
                HorizontalDivider(color = ThemeManager.getDividerColor())
            }
        }
    }
}
