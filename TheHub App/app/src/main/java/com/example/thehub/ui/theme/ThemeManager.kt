package com.example.thehub

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode"

    private lateinit var sharedPreferences: SharedPreferences

    var isDarkMode by mutableStateOf(false)
        private set

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false)
    }

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply()
    }
}
