package com.example.thehub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// Simple Message data class
data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(nav: NavController, conversationId: String) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chat",
                        color = ThemeManager.getTextColor()
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
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ThemeManager.getSurfaceColor())
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    placeholder = { Text("Nhập tin nhắn…") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = if (ThemeManager.isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF5F5F5),
                        unfocusedContainerColor = if (ThemeManager.isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF5F5F5),
                        focusedTextColor = ThemeManager.getTextColor(),
                        unfocusedTextColor = ThemeManager.getTextColor()
                    )
                )
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            // Add send logic here
                            messageText = ""
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = ThemeManager.getAccentColor()
                    )
                }
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .background(ThemeManager.getBackgroundColor()),
            reverseLayout = true
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isMyMessage = message.senderId == Firebase.auth.currentUser?.uid
    val bubbleColor = if (isMyMessage)
        ThemeManager.getAccentColor()
    else
        ThemeManager.getSurfaceColor()
    val textColor = if (isMyMessage)
        Color.White
    else
        ThemeManager.getTextColor()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp),
        contentAlignment = if (isMyMessage)
            Alignment.CenterEnd
        else
            Alignment.CenterStart
    ) {
        Surface(
            color = bubbleColor,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp
        ) {
            Text(
                message.text,
                color = textColor,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}
