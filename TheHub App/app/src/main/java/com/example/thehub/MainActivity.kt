package com.example.thehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.thehub.ui.theme.TheHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TheHubTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
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
            SettingsScreen(navController = navController)
        }

        composable("favourites") {
            FavouritesScreen(navController = navController)
        }
    }
}
