package com.example.thehub

import android.content.Context
import android.content.SharedPreferences

object UserPreferences {
    private const val PREF_NAME = "thehub_preferences"
    private const val KEY_REMEMBER_LOGIN = "remember_login"
    private const val KEY_SAVED_USERNAME = "saved_username"
    private const val KEY_SAVED_PASSWORD = "saved_password"
    private const val KEY_AUTO_LOGIN = "auto_login"

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
}
