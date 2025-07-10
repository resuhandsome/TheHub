package com.example.thehub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun TheHubBottomBar(nav: NavController, current: String) {
    val accent = ThemeManager.getAccentColor()
    val iconCol = ThemeManager.getIconColor()
    Surface(
        shadowElevation = 8.dp,
        color = ThemeManager.getSurfaceColor()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 10.dp),
            Arrangement.SpaceEvenly,
            Alignment.CenterVertically
        ) {
            BottomItem(Icons.Default.Home, "home", current == "home") {
                nav.navigate("home")
            }
            BottomItem(Icons.Default.Message, "Tin nhắn", current == "messages") {
                nav.navigate("messages")
            }

            // Floating Add Post
            Box(
                Modifier
                    .size(56.dp)
                    .background(accent, CircleShape)
                    .clickable { nav.navigate("compose_post") },
                Alignment.Center
            ) { Icon(Icons.Default.Add, null, tint = Color.White) }

            BottomItem(Icons.Default.Notifications, "Thông báo",
                current == "notifications") { nav.navigate("notifications") }
            BottomItem(Icons.Default.Person, "Profile", current == "profile") {
                nav.navigate("profile")
            }
        }
    }
}

@Composable
private fun BottomItem(icon: ImageVector, label: String,
                       selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp)
    ) {
        Icon(icon, label,
            tint = if (selected) ThemeManager.getAccentColor() else ThemeManager.getIconColor(),
            modifier = Modifier.size(24.dp))
        Text(label, fontSize = 10.sp,
            color = if (selected) ThemeManager.getAccentColor() else ThemeManager.getSecondaryTextColor())
    }
}
