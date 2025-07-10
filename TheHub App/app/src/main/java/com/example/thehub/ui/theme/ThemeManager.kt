package com.example.thehub

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

object ThemeManager {
    var isDarkMode by mutableStateOf(false)
        private set

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
    }

    fun setThemeMode(enabled: Boolean) {
        isDarkMode = enabled
    }

    @Composable
    fun initializeWithSystem() {
        val systemDarkMode = isSystemInDarkTheme()
        LaunchedEffect(systemDarkMode) {
            if (!isDarkMode) {  // Chỉ set nếu chưa được user thay đổi
                isDarkMode = systemDarkMode
            }
        }
    }

    @Composable
    fun getColorScheme(): ColorScheme {
        return if (isDarkMode) {
            darkColorScheme(
                primary = Color(0xFF007AFF),
                background = Color(0xFF121212),
                surface = Color(0xFF1E1E1E)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF007AFF),
                background = Color.White,
                surface = Color.White
            )
        }
    }
}
