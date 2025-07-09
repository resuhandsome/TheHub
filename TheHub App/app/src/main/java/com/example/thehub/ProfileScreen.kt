package com.example.thehub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.thehub.ui.theme.TheHubTheme
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val photoUrl = currentUser?.photoUrl

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            ProfileTopAppBar(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("setting") }
            )
        },
        bottomBar = { ProfileBottomNavBar() }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(2.dp)
        ) {
            item(span = { GridItemSpan(3) }) {
                ProfileHeader(photoUrl = photoUrl)
            }
            item(span = { GridItemSpan(3) }) {
                EditProfileButton()
            }
            item(span = { GridItemSpan(3) }) {
                YourPostsTab()
            }
            items(9) {
                PostItemPlaceholder()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar(onBackClick: () -> Unit, onSettingsClick: () -> Unit) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        actions = {
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.Default.Star, contentDescription = "Custom Logo", tint = Color(0xFF00BFFF))
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
    )
}

@Composable
fun ProfileHeader(photoUrl: Any?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Profile Picture",
            placeholder = painterResource(id = R.drawable.logomacdinh),
            error = painterResource(id = R.drawable.logomacdinh),
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(number = "9", label = "post")
            StatItem(number = "67.8k", label = "followers")
            StatItem(number = "1", label = "following")
        }
    }
}

@Composable
fun StatItem(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = number, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun EditProfileButton() {
    Button(
        onClick = { /* TODO */ },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
    ) {
        Text("Edit profile", color = Color.White)
    }
}

@Composable
fun YourPostsTab() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.GridOn, contentDescription = "Posts Icon", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Your Posts", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Divider(color = Color.DarkGray, thickness = 1.dp)
    }
}

@Composable
fun PostItemPlaceholder() {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {

    }
}
@Composable
fun ProfileBottomNavBar() {
    NavigationBar(
        containerColor = Color.Black,
    ) {
        NavigationBarItem(selected = false, onClick = { /*TODO*/ }, icon = { Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White) })
        NavigationBarItem(selected = false, onClick = { /*TODO*/ }, icon = { Icon(Icons.Default.AddCircle, contentDescription = "Add Post", modifier = Modifier.size(32.dp), tint = Color.White) })
        NavigationBarItem(selected = false, onClick = { /*TODO*/ }, icon = { Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White) })
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    TheHubTheme {
        ProfileScreen(navController = rememberNavController())
    }
}