package com.example.thehub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ConversationItem(
    val id: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserAvatar: String = "",
    val lastMessage: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(nav: NavController) {
    var conversations by remember { mutableStateOf<List<ConversationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val currentUser = Firebase.auth.currentUser

    val backgroundColor = ThemeManager.getBackgroundColor()
    val surfaceColor = ThemeManager.getSurfaceColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()

    suspend fun loadConversations() {
        if (currentUser == null) return

        try {
            val db = Firebase.firestore
            val conversationsSnapshot = db.collection("conversations")
                .whereArrayContains("participants", currentUser.uid)
                .orderBy("lastUpdate", Query.Direction.DESCENDING)
                .get()
                .await()

            val conversationsList = mutableListOf<ConversationItem>()

            for (doc in conversationsSnapshot.documents) {
                val participants = doc.get("participants") as? List<String> ?: emptyList()
                val otherUserId = participants.firstOrNull { it != currentUser.uid }

                if (otherUserId != null) {
                    val userDoc = db.collection("users").document(otherUserId).get().await()
                    conversationsList.add(
                        ConversationItem(
                            id = doc.id,
                            otherUserId = otherUserId,
                            otherUserName = userDoc.getString("username") ?: "Người dùng",
                            otherUserAvatar = userDoc.getString("avatarUrl") ?: "",
                            lastMessage = doc.getString("lastMessage") ?: "Bắt đầu cuộc trò chuyện",
                            timestamp = doc.getLong("lastUpdate") ?: System.currentTimeMillis(),
                            isRead = true
                        )
                    )
                }
            }
            conversations = conversationsList
        } catch (e: Exception) {
            // handle error
        }
        isLoading = false
    }

    LaunchedEffect(currentUser) {
        loadConversations()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tin nhắn",
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ThemeManager.getAccentColor())
                }
            } else if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "💬", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chưa có cuộc trò chuyện nào",
                            color = secondaryTextColor,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Hãy bắt đầu nhắn tin với ai đó!",
                            color = secondaryTextColor,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(conversations) { conversation ->
                        ConversationItemView(
                            conversation = conversation,
                            onClick = {
                                nav.navigate("chat/${conversation.id}")
                            }
                        )
                        Divider(
                            color = ThemeManager.getDividerColor(),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItemView(
    conversation: ConversationItem,
    onClick: () -> Unit
) {
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()

    ListItem(
        headlineContent = {
            Text(
                text = conversation.otherUserName,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = conversation.lastMessage.ifEmpty { "Bắt đầu cuộc trò chuyện" },
                color = secondaryTextColor,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            AsyncImage(
                model = conversation.otherUserAvatar.takeIf { it.isNotEmpty() },
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.logomacdinh)
            )
        },
        trailingContent = {
            Text(
                text = formatTime(conversation.timestamp),
                color = secondaryTextColor,
                fontSize = 12.sp
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}