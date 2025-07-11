package com.example.thehub

import android.content.Context
import android.content.SharedPreferences
import com.example.thehub.chat.Message
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object UserPreferences {
    private const val PREF_NAME = "thehub_preferences"
    private const val KEY_REMEMBER_LOGIN = "remember_login"
    private const val KEY_SAVED_USERNAME = "saved_username"
    private const val KEY_SAVED_PASSWORD = "saved_password"
    private const val KEY_AUTO_LOGIN = "auto_login"
    private const val KEY_SEARCH_HISTORY = "search_history"
    private const val KEY_CHAT_HISTORY = "chat_history"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveLoginCredentials(context: Context, username: String, password: String, remember: Boolean) {
        val prefs = getPreferences(context)
        prefs.edit().apply {
            putBoolean(KEY_REMEMBER_LOGIN, remember)
            if (remember) {
                putString(KEY_SAVED_USERNAME, username)
                putString(KEY_SAVED_PASSWORD, password)
            } else {
                remove(KEY_SAVED_USERNAME)
                remove(KEY_SAVED_PASSWORD)
            }
            apply()
        }
    }

    fun getSavedCredentials(context: Context): Pair<String, String>? {
        val prefs = getPreferences(context)
        val remember = prefs.getBoolean(KEY_REMEMBER_LOGIN, false)

        return if (remember) {
            val username = prefs.getString(KEY_SAVED_USERNAME, "") ?: ""
            val password = prefs.getString(KEY_SAVED_PASSWORD, "") ?: ""
            if (username.isNotEmpty() && password.isNotEmpty()) {
                Pair(username, password)
            } else null
        } else null
    }

    fun isRememberLoginEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_REMEMBER_LOGIN, false)
    }

    fun setAutoLogin(context: Context, enabled: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_AUTO_LOGIN, enabled).apply()
    }

    fun isAutoLoginEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_AUTO_LOGIN, false)
    }

    fun clearSavedCredentials(context: Context) {
        val prefs = getPreferences(context)
        prefs.edit().apply {
            remove(KEY_REMEMBER_LOGIN)
            remove(KEY_SAVED_USERNAME)
            remove(KEY_SAVED_PASSWORD)
            remove(KEY_AUTO_LOGIN)
            apply()
        }
    }

    fun saveSearchHistory(context: Context, searches: List<String>) {
        val prefs = getPreferences(context)
        val gson = Gson()
        val json = gson.toJson(searches.take(10))
        prefs.edit().putString(KEY_SEARCH_HISTORY, json).apply()
    }

    fun getSearchHistory(context: Context): List<String> {
        val prefs = getPreferences(context)
        val json = prefs.getString(KEY_SEARCH_HISTORY, "") ?: ""
        return if (json.isNotEmpty()) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                Gson().fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun saveChatHistory(context: Context, conversationId: String, messages: List<Message>) {
        val prefs = getPreferences(context)
        val gson = Gson()
        val json = gson.toJson(messages.takeLast(100))
        prefs.edit().putString("$KEY_CHAT_HISTORY$conversationId", json).apply()
    }

    fun getChatHistory(context: Context, conversationId: String): List<Message> {
        val prefs = getPreferences(context)
        val json = prefs.getString("$KEY_CHAT_HISTORY$conversationId", "") ?: ""
        return if (json.isNotEmpty()) {
            try {
                val type = object : TypeToken<List<Message>>() {}.type
                Gson().fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}