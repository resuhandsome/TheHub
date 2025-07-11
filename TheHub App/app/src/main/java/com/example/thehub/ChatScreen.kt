package com.example.thehub

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.thehub.chat.Message
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(nav: NavController, conversationId: String) {
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.factory(conversationId))
    val messages by viewModel.messages.collectAsState()
    val otherUserName by viewModel.otherUserName.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.loadConversation()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (isLoading) "Đang tải..." else otherUserName, color = ThemeManager.getTextColor()) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ThemeManager.getTextColor())
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeManager.getSurfaceColor())
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(ThemeManager.getSurfaceColor()).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = draft,
                    onValueChange = { viewModel.updateDraft(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Nhập tin nhắn…") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = if (ThemeManager.isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF5F5F5),
                        unfocusedContainerColor = if (ThemeManager.isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF5F5F5),
                        focusedTextColor = ThemeManager.getTextColor(),
                        unfocusedTextColor = ThemeManager.getTextColor(),
                        cursorColor = ThemeManager.getAccentColor(),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                IconButton(
                    onClick = { if (draft.isNotBlank()) viewModel.sendMessage() },
                    enabled = draft.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = if (draft.isNotBlank()) ThemeManager.getAccentColor() else Color.Gray)
                }
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize().background(ThemeManager.getBackgroundColor()),
            state = lazyListState
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    onDelete = { viewModel.deleteMessage(message.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(message: Message, onDelete: () -> Unit) {
    val isMyMessage = message.senderId == Firebase.auth.currentUser?.uid
    var showMenu by remember { mutableStateOf(false) }

    val bubbleColor = if (isMyMessage) ThemeManager.getAccentColor() else ThemeManager.getSurfaceColor()
    val textColor = if (isMyMessage) Color.White else ThemeManager.getTextColor()

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
        contentAlignment = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { if (isMyMessage) showMenu = true }
            )
        ) {
            Text(
                text = message.text,
                color = textColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Xóa tin nhắn") },
                onClick = {
                    onDelete()
                    showMenu = false
                }
            )
        }
    }
}