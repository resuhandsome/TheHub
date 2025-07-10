package com.example.thehub

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object ThemeManager {
    private const val PREF_NAME = "theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode"

    var isDarkMode by mutableStateOf(false)
        private set

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun toggleDarkMode(context: Context) {
        isDarkMode = !isDarkMode
        saveDarkModePreference(context)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        isDarkMode = enabled
        saveDarkModePreference(context)
    }

    private fun saveDarkModePreference(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply()
    }

    // ===== UTILITY FUNCTIONS =====
    @Composable
    fun getBackgroundColor(): Color =
        if (isDarkMode) Color(0xFF121212) else Color(0xFFFAFAFA)

    @Composable
    fun getSurfaceColor(): Color =
        if (isDarkMode) Color(0xFF1E1E1E) else Color.White

    @Composable
    fun getCardColor(): Color =
        if (isDarkMode) Color(0xFF1E1E1E) else Color.White

    @Composable
    fun getTextColor(): Color =
        if (isDarkMode) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)

    @Composable
    fun getSecondaryTextColor(): Color =
        if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    @Composable
    fun getIconColor(): Color =
        if (isDarkMode) Color(0xFFE0E0E0) else Color.Gray

    @Composable
    fun getDividerColor(): Color =
        if (isDarkMode) Color.Gray.copy(alpha = 0.3f) else Color(0xFFE0E0E0)

    @Composable
    fun getTopBarColor(): Color =
        if (isDarkMode) Color(0xFF1A1A1A) else Color.White

    @Composable
    fun getBottomBarColor(): Color =
        if (isDarkMode) Color(0xFF1A1A1A) else Color.White

    // Accent colors remain consistent
    @Composable
    fun getAccentColor(): Color = Color(0xFF007AFF)

    @Composable
    fun getErrorColor(): Color = Color(0xFFFF5252)
}
