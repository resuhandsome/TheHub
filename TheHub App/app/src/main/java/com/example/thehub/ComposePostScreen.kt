package com.example.thehub

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
fun ComposePostScreen(navController: NavController) {
    var postContent by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isPosting by remember { mutableStateOf(false) }
    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore
    val storage = Firebase.storage
    val scrollState = rememberScrollState()

    // load user profile
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            currentUserProfile = UserRepository.getCurrentUserProfile()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImages = uris.take(5)
    }

    fun uploadPost() {
        if (postContent.isBlank() && selectedImages.isEmpty()) {
            Toast.makeText(context, "Vui lòng nhập nội dung hoặc chọn ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        coroutineScope.launch {
            isPosting = true
            try {
                val imageUrls = mutableListOf<String>()

                // upload images to Firebase Storage
                for (imageUri in selectedImages) {
                    val imageRef = storage.reference.child("posts/${UUID.randomUUID()}")
                    val uploadTask = imageRef.putFile(imageUri).await()
                    val downloadUrl = uploadTask.storage.downloadUrl.await()
                    imageUrls.add(downloadUrl.toString())
                }

                // create post data
                val postData = hashMapOf(
                    "authorId" to currentUser?.uid,
                    "authorName" to (currentUserProfile?.username ?: "Unknown User"),
                    "authorAvatarUrl" to (currentUserProfile?.avatarUrl ?: ""),
                    "content" to postContent,
                    "imageUrls" to imageUrls,
                    "timestamp" to System.currentTimeMillis(),
                    "likes" to 0,
                    "likedBy" to emptyList<String>(),
                    "comments" to 0
                )

                // save to Firestore
                val documentRef = db.collection("posts").add(postData).await()

                // debug log
                println("DEBUG: Created post with ID: ${documentRef.id}")

                // update user's posts count
                if (currentUser != null && currentUserProfile != null) {
                    try {
                        val currentPostsSnapshot = db.collection("posts")
                            .whereEqualTo("authorId", currentUser.uid)
                            .get()
                            .await()

                        val newPostsCount = currentPostsSnapshot.size()
                        val updatedProfile = currentUserProfile!!.copy(postsCount = newPostsCount)
                        UserRepository.updateUserProfile(updatedProfile)
                    } catch (e: Exception) {
                        println("DEBUG: Error updating posts count - ${e.message}")
                    }
                }

                Toast.makeText(context, "Đăng bài thành công!", Toast.LENGTH_SHORT).show()
                navController.popBackStack()

            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi đăng bài: ${e.message}", Toast.LENGTH_SHORT).show()
                println("DEBUG: Error creating post - ${e.message}")
            } finally {
                isPosting = false
            }
        }
    }

    // main container với gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFAFAFA),
                        Color.White
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Modern Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // cancel button
                    TextButton(
                        onClick = { navController.popBackStack() },
                        enabled = !isPosting,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF666666)
                        )
                    ) {
                        Text(
                            text = "Hủy",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // title
                    Text(
                        text = "Tạo bài viết",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )

                    // post button
                    Button(
                        onClick = { uploadPost() },
                        enabled = !isPosting && (postContent.isNotBlank() || selectedImages.isNotEmpty()),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF),
                            disabledContainerColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .widthIn(min = 80.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        if (isPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Đăng",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // main content với scroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                // user profile section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // avatar
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .shadow(4.dp, CircleShape)
                        ) {
                            AsyncImage(
                                model = currentUserProfile?.avatarUrl?.takeIf { it.isNotEmpty() }
                                    ?: currentUser?.photoUrl,
                                contentDescription = "User Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.Gray.copy(alpha = 0.2f))
                                    .border(2.dp, Color.White, CircleShape),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = R.drawable.logomacdinh)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // user info và content
                        Column(modifier = Modifier.weight(1f)) {
                            // username
                            Text(
                                text = currentUserProfile?.username ?: "Đang tải...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF1A1A1A)
                            )

                            Text(
                                text = "Công khai",
                                fontSize = 14.sp,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // text input area
                            TextField(
                                value = postContent,
                                onValueChange = { postContent = it },
                                placeholder = {
                                    Text(
                                        "Bạn đang nghĩ gì?",
                                        color = Color(0xFF999999),
                                        fontSize = 16.sp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 300.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = Color(0xFF007AFF)
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 16.sp,
                                    color = Color(0xFF1A1A1A),
                                    lineHeight = 24.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // selected images preview
                if (selectedImages.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Ảnh đã chọn (${selectedImages.size}/5)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF666666)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(selectedImages) { imageUri ->
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        AsyncImage(
                                            model = imageUri,
                                            contentDescription = "Selected Image",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )

                                        // remove button
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 6.dp, y = (-6).dp)
                                                .size(24.dp)
                                                .background(
                                                    Color.Black.copy(alpha = 0.7f),
                                                    CircleShape
                                                )
                                                .clickable {
                                                    selectedImages = selectedImages.filter { it != imageUri }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // action buttons section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Thêm vào bài viết",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // add photo button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { imagePickerLauncher.launch("image/*") }
                                .background(Color(0xFFF8F9FA))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        Color(0xFF4CAF50).copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add Image",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "Ảnh/Video",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1A1A1A)
                                )
                                Text(
                                    text = "Thêm ảnh hoặc video vào bài viết",
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            if (selectedImages.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0xFF4CAF50), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = selectedImages.size.toString(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Feeling/Activity Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8F9FA))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        Color(0xFFFF9800).copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "😊",
                                    fontSize = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "Cảm xúc/Hoạt động",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1A1A1A)
                                )
                                Text(
                                    text = "Bạn đang cảm thấy thế nào?",
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    }
                }

                // bottom spacing để tránh bị keyboard che
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
