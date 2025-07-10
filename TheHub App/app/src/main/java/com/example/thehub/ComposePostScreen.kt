package com.example.thehub

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
fun ComposePostScreen(navController: NavController) {
    var content by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isPosting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore
    val storage = Firebase.storage
    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImages = uris.take(4) // Limit to 4 images
    }

    fun uploadImages(uris: List<Uri>): List<String> {
        // In real implementation, upload to Firebase Storage
        // For now, return empty list
        return emptyList()
    }

    fun createPost() {
        if (content.isBlank()) {
            Toast.makeText(context, "Vui lòng nhập nội dung bài viết", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUser == null) {
            Toast.makeText(context, "Bạn cần đăng nhập để đăng bài", Toast.LENGTH_SHORT).show()
            return
        }

        isPosting = true
        coroutineScope.launch {
            try {
                // Get current user profile
                val userProfile = UserRepository.getCurrentUserProfile()

                // Upload images if any
                val imageUrls = if (selectedImages.isNotEmpty()) {
                    uploadImages(selectedImages)
                } else {
                    emptyList()
                }

                // Create post data
                val postData = hashMapOf(
                    "authorId" to currentUser.uid,
                    "authorName" to (userProfile?.username ?: currentUser.displayName ?: "Unknown User"),
                    "authorAvatarUrl" to (userProfile?.avatarUrl ?: currentUser.photoUrl?.toString() ?: ""),
                    "content" to content.trim(),
                    "imageUrls" to imageUrls,
                    "likes" to 0,
                    "likedBy" to emptyList<String>(),
                    "comments" to 0,
                    "timestamp" to System.currentTimeMillis()
                )

                // Save to Firestore
                db.collection("posts").add(postData).await()

                Toast.makeText(context, "Đăng bài thành công!", Toast.LENGTH_SHORT).show()
                navController.popBackStack()

            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi đăng bài: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isPosting = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo bài viết", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { createPost() },
                        enabled = content.isNotBlank() && !isPosting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Đăng")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Content Input
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = {
                    Text(
                        "Bạn đang nghĩ gì?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Image Section
            if (selectedImages.isNotEmpty()) {
                Text(
                    text = "Ảnh đã chọn (${selectedImages.size}/4)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImages) { uri ->
                        Box {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected Image",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            IconButton(
                                onClick = {
                                    selectedImages = selectedImages.filter { it != uri }
                                },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(50)
                                        )
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Add Images Button
            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedImages.size < 4
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Images")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Thêm ảnh (${selectedImages.size}/4)")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Character Count
            Text(
                text = "${content.length}/1000",
                fontSize = 12.sp,
                color = if (content.length > 1000) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
