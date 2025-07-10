package com.example.thehub

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore
    val storage = Firebase.storage
    val scrollState = rememberScrollState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    // Load current user profile
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                currentUserProfile = UserRepository.getCurrentUserProfile()
                currentUserProfile?.let { profile ->
                    username = profile.username
                    displayName = profile.displayName
                    bio = profile.bio
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi tải thông tin", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    fun saveProfile() {
        if (username.isBlank()) {
            Toast.makeText(context, "Username không được để trống", Toast.LENGTH_SHORT).show()
            return
        }

        coroutineScope.launch {
            isSaving = true
            try {
                var avatarUrl = currentUserProfile?.avatarUrl ?: ""

                // Upload new avatar if selected
                selectedImageUri?.let { uri ->
                    val imageRef = storage.reference.child("avatars/${currentUser?.uid}")
                    val uploadTask = imageRef.putFile(uri).await()
                    avatarUrl = uploadTask.storage.downloadUrl.await().toString()
                }

                // Update profile data - ✅ SỬA DỤNG 'id' THAY VÌ 'uid'
                val updatedProfile = currentUserProfile?.copy(
                    username = username,
                    displayName = displayName.ifBlank { username },
                    bio = bio,
                    avatarUrl = avatarUrl
                ) ?: UserProfile(
                    id = currentUser?.uid ?: "",  // ✅ SỬA: uid -> id
                    username = username,
                    displayName = displayName.ifBlank { username },
                    bio = bio,
                    avatarUrl = avatarUrl,
                    email = currentUser?.email ?: "",
                    createdAt = System.currentTimeMillis()
                )

                // Save to Firestore
                val success = UserRepository.updateUserProfile(updatedProfile)

                if (success) {
                    // Update Firebase Auth displayName
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()
                    currentUser?.updateProfile(profileUpdates)?.await()

                    Toast.makeText(context, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                } else {
                    Toast.makeText(context, "Lỗi khi cập nhật profile", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi cập nhật: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chỉnh sửa profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface // ✅ Dynamic color
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface // ✅ Dynamic color
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { saveProfile() },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary // ✅ Dynamic color
                            )
                        } else {
                            Text(
                                "Lưu",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary // ✅ Dynamic color
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface // ✅ Dynamic color
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary // ✅ Dynamic color
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background) // ✅ Dynamic color
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface // ✅ Dynamic color
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(120.dp)
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shadow(8.dp, CircleShape)
                            ) {
                                AsyncImage(
                                    model = selectedImageUri ?: currentUserProfile?.avatarUrl?.takeIf { it.isNotEmpty() },
                                    contentDescription = "Profile Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant) // ✅ Dynamic color
                                        .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape), // ✅ Dynamic color
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(id = R.drawable.logomacdinh)
                                )
                            }

                            // Camera button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape) // ✅ Dynamic color
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Change Avatar",
                                    tint = MaterialTheme.colorScheme.onPrimary, // ✅ Dynamic color
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Nhấn để thay đổi ảnh đại diện",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant // ✅ Dynamic color
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Form Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface // ✅ Dynamic color
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Username field
                        Text(
                            text = "Username",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface // ✅ Dynamic color
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = username,
                            onValueChange = { username = it },
                            placeholder = {
                                Text(
                                    "Nhập username",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, // ✅ Dynamic color
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Display Name field
                        Text(
                            text = "Tên hiển thị",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface // ✅ Dynamic color
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            placeholder = {
                                Text(
                                    "Nhập tên hiển thị",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, // ✅ Dynamic color
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Bio field
                        Text(
                            text = "Tiểu sử",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface // ✅ Dynamic color
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = bio,
                            onValueChange = { bio = it },
                            placeholder = {
                                Text(
                                    "Viết gì đó về bản thân...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, // ✅ Dynamic color
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            maxLines = 5
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
