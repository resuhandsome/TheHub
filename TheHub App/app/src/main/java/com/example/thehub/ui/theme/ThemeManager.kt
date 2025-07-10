package com.example.thehub

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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
}
