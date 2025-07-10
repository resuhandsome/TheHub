package com.example.thehub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Data class cho Message
data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, conversationId: String) {
    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.factory(conversationId)
    )

    val draft by viewModel.draft.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val otherUserName by viewModel.otherUserName.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Theme-aware colors
    val backgroundColor = ThemeManager.getBackgroundColor()
    val surfaceColor = ThemeManager.getSurfaceColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()
    val accentColor = ThemeManager.getAccentColor()

    // Load conversation data on first launch
    LaunchedEffect(conversationId) {
        viewModel.loadConversation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = otherUserName.takeIf { it.isNotEmpty() } ?: "Đang tải...",
                            color = textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isLoading) {
                            Text(
                                text = "Đang kết nối...",
                                color = secondaryTextColor,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor
                )
            )
        },
        bottomBar = {
            MessageInputBar(
                draft = draft,
                onDraftChange = viewModel::updateDraft,
                onSendMessage = viewModel::sendMessage,
                enabled = !isLoading
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
        ) {
            if (isLoading && messages.isEmpty()) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = accentColor,
                        strokeWidth = 3.dp
                    )
                }
            } else {
                // Messages list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages.reversed()) { message ->
                        MessageBubble(message = message)
                    }

                    // Empty state
                    if (messages.isEmpty() && !isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "💬",
                                        fontSize = 48.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Chưa có tin nhắn nào",
                                        color = secondaryTextColor,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Hãy bắt đầu cuộc trò chuyện!",
                                        color = secondaryTextColor,
                                        fontSize = 14.sp
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

@Composable
private fun MessageInputBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    enabled: Boolean
) {
    val surfaceColor = ThemeManager.getSurfaceColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()
    val accentColor = ThemeManager.getAccentColor()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surfaceColor,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            TextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 120.dp),
                placeholder = {
                    Text(
                        "Nhập tin nhắn...",
                        color = secondaryTextColor
                    )
                },
                enabled = enabled,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSendMessage() }
                ),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (ThemeManager.isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF5F5F5),
                    unfocusedContainerColor = if (ThemeManager.isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF5F5F5),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    disabledContainerColor = if (ThemeManager.isDarkMode) Color(0xFF1A1A1A) else Color(0xFFE0E0E0)
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = onSendMessage,
                modifier = Modifier.size(48.dp),
                containerColor = if (draft.isNotBlank() && enabled) accentColor else secondaryTextColor,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp
                )
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Gửi tin nhắn",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val currentUser = Firebase.auth.currentUser
    val isMyMessage = message.senderId == currentUser?.uid

    // Theme-aware colors
    val myMessageColor = ThemeManager.getAccentColor()
    val otherMessageColor = ThemeManager.getSurfaceColor()
    val myTextColor = Color.White
    val otherTextColor = ThemeManager.getTextColor()
    val timestampColor = ThemeManager.getSecondaryTextColor()

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMyMessage)
            Alignment.CenterEnd
        else
            Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isMyMessage)
                Alignment.End
            else
                Alignment.Start
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 280.dp),
                color = if (isMyMessage) myMessageColor else otherMessageColor,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMyMessage) 16.dp else 4.dp,
                    bottomEnd = if (isMyMessage) 4.dp else 16.dp
                ),
                tonalElevation = if (isMyMessage) 0.dp else 2.dp,
                shadowElevation = if (isMyMessage) 2.dp else 1.dp
            ) {
                Text(
                    text = message.text,
                    color = if (isMyMessage) myTextColor else otherTextColor,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatMessageTime(message.timestamp),
                color = timestampColor,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// Utility function để format thời gian tin nhắn
private fun formatMessageTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Vừa xong" // < 1 minute
        diff < 3600000 -> "${diff / 60000} phút trước" // < 1 hour
        diff < 86400000 -> "${diff / 3600000} giờ trước" // < 1 day
        else -> {
            val date = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
            date.format(java.util.Date(timestamp))
        }
    }
}
