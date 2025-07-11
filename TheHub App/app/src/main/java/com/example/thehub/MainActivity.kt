package com.example.thehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.thehub.ui.theme.TheHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.initialize(this)

        setContent {
            val ctx = LocalContext.current
            LaunchedEffect(Unit) { ThemeManager.initialize(ctx) }

            TheHubTheme(darkTheme = ThemeManager.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ThemeManager.getBackgroundColor()
                ) {
                    AppNav()
                }
            }
        }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "login") {
        composable("login")           { LoginScreen(nav) }
        composable("signup")          { SignUpScreen(nav) }
        composable("home")            { HomeScreen(nav) }
        composable("compose_post")    { ComposePostScreen(nav) }
        composable("search")          { SearchScreen(nav) }
        composable("notifications")   { NotificationsScreen(nav) }
        composable("messages")        { ConversationsScreen(nav) }
        composable("chat/{cid}")      { back ->
            ChatScreen(nav, back.arguments?.getString("cid") ?: "")
        }
        composable("profile")         { ProfileScreen(nav) }
        composable("profile/{uid}")   { back ->
            ProfileScreen(nav, back.arguments?.getString("uid"))
        }
        composable("edit_profile")    { EditProfileScreen(nav) }
        composable("favourites")      { FavouritesScreen(nav) }
        composable("settings")        { SettingsScreen(nav) }
//        composable("about")           { AboutScreen(nav) }
//        composable("privacy")         { PrivacyScreen(nav) }
//        composable("terms")           { TermsScreen(nav) }
//        composable("help")            { HelpScreen(nav) }

    }
}
