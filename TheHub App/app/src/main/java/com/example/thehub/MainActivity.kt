package com.example.thehub

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.thehub.ui.theme.TheHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var isDarkMode by remember { mutableStateOf(false) }

            // Load theme preference từ SharedPreferences
            LaunchedEffect(Unit) {
                val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
            }

            TheHubTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        isDarkMode = isDarkMode,
                        onThemeChange = { newDarkMode ->
                            isDarkMode = newDarkMode
                            // Lưu theme preference
                            val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                            sharedPrefs.edit().putBoolean("dark_mode", newDarkMode).apply()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(navController = navController)
        }

        composable("signup") {
            SignUpScreen(navController = navController)
        }

        composable("home") {
            HomeScreen(navController = navController)
        }

        composable("compose_post") {
            ComposePostScreen(navController = navController)
        }

        composable("search") {
            SearchScreen(navController = navController)
        }

        composable("notifications") {
            NotificationsScreen(navController = navController)
        }

        composable("profile") {
            ProfileScreen(navController = navController)
        }

        composable("profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            ProfileScreen(navController = navController, userId = userId)
        }

        composable("edit_profile") {
            EditProfileScreen(navController = navController)
        }


        composable("settings") {
            SettingsScreen(
                navController = navController,
                isDarkMode = isDarkMode,
                onThemeChange = onThemeChange
            )
        }
    }
}
