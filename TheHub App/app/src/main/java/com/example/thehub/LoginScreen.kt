package com.example.thehub

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var rememberLogin by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = Firebase.auth
    val db = Firebase.firestore
    val scrollState = rememberScrollState()

    // Theme-aware colors
    val backgroundColor = ThemeManager.getBackgroundColor()
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()
    val accentColor = ThemeManager.getAccentColor()

    // Load saved credentials khi khởi tạo
    LaunchedEffect(Unit) {
        val savedCredentials = UserPreferences.getSavedCredentials(context)
        if (savedCredentials != null) {
            username = savedCredentials.first
            password = savedCredentials.second
            rememberLogin = true
        }
    }

    // Navigation function
    val navigateToHome = {
        Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
        navController.navigate("home") {
            popUpTo("login") { inclusive = true }
        }
    }

    // Google Sign-In Client
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    // Google Sign-In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken!!
                coroutineScope.launch {
                    isGoogleLoading = true
                    try {
                        val credential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(credential).await()
                        val user = authResult.user

                        if (user != null) {
                            val userProfile = UserProfile(
                                uid = user.uid,
                                username = user.displayName ?: "User${user.uid.take(6)}",
                                email = user.email ?: "",
                                displayName = user.displayName ?: "",
                                avatarUrl = user.photoUrl?.toString() ?: "",
                                bio = "",
                                followersCount = 0,
                                followingCount = 0,
                                postsCount = 0,
                                createdAt = System.currentTimeMillis()
                            )

                            UserRepository.updateUserProfile(userProfile)
                        }

                        navigateToHome()
                    } catch (e: Exception) {
                        Log.e("FirebaseAuth", "Firebase sign-in with Google failed", e)
                        Toast.makeText(context, "Đăng nhập Google thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isGoogleLoading = false
                    }
                }
            } catch (e: ApiException) {
                Log.w("GoogleSignIn", "Google sign in failed", e)
                Toast.makeText(context, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show()
                isGoogleLoading = false
            }
        } else {
            isGoogleLoading = false
        }
    }

    //LƯU TÀI KHOẢN
    fun handleRememberLoginChange(newValue: Boolean) {
        rememberLogin = newValue
        if (!newValue) {
            // Tự động xóa credentials khi bỏ tick
            UserPreferences.clearSavedCredentials(context)
            Toast.makeText(context, "Đã xóa thông tin đã lưu", Toast.LENGTH_SHORT).show()
        }
    }

    fun login() {
        if (username.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Vui lòng nhập username và mật khẩu.", Toast.LENGTH_SHORT).show()
            return
        }

        coroutineScope.launch {
            isLoading = true
            try {
                val trimmedUsername = username.trim()

                if (Firebase.auth.app == null) {
                    Toast.makeText(context, "Lỗi kết nối Firebase", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val usersRef = db.collection("users")
                val query = usersRef.whereEqualTo("username", trimmedUsername).limit(1).get().await()

                if (query.isEmpty) {
                    Toast.makeText(context, "Username không tồn tại.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val userDoc = query.documents[0]
                val emailForAuth = userDoc.getString("email")

                if (emailForAuth != null) {
                    auth.signInWithEmailAndPassword(emailForAuth, password.trim()).await()

                    var userProfile: UserProfile? = null
                    var retryCount = 0

                    while (userProfile == null && retryCount < 3) {
                        try {
                            userProfile = UserRepository.getCurrentUserProfile()
                            break
                        } catch (e: Exception) {
                            retryCount++
                            if (retryCount < 3) {
                                delay(1000)
                            }
                        }
                    }

                    if (userProfile != null) {
                        val currentUser = auth.currentUser
                        if (currentUser != null && currentUser.displayName != userProfile.username) {
                            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(userProfile.username)
                                .build()
                            currentUser.updateProfile(profileUpdates).await()
                        }

                        // Lưu credentials chỉ khi rememberLogin = true
                        if (rememberLogin) {
                            UserPreferences.saveLoginCredentials(context, username, password, true)
                        }

                        navigateToHome()
                    } else {
                        Toast.makeText(context, "Lỗi tải dữ liệu người dùng. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Lỗi dữ liệu người dùng.", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("LoginScreen", "Đăng nhập thất bại", e)
                val errorMessage = when {
                    e is FirebaseAuthInvalidCredentialsException -> "Sai mật khẩu. Vui lòng thử lại."
                    e.message?.contains("network") == true -> "Lỗi kết nối mạng"
                    e.message?.contains("too-many-requests") == true -> "Quá nhiều lần thử. Vui lòng đợi."
                    else -> "Đăng nhập thất bại: Lỗi hệ thống."
                }
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Logo
        Image(
            painter = painterResource(id = R.drawable.logothehub),
            contentDescription = "App Logo",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "Đăng nhập vào TheHub",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Username Input
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = {
                Text(
                    "Username",
                    color = secondaryTextColor
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading && !isGoogleLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = secondaryTextColor.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Input
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = {
                Text(
                    "Mật khẩu",
                    color = secondaryTextColor
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading && !isGoogleLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = secondaryTextColor.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rememberLogin,
                onCheckedChange = { handleRememberLoginChange(it) },
                enabled = !isLoading && !isGoogleLoading,
                colors = CheckboxDefaults.colors(
                    checkedColor = accentColor
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Lưu tài khoản",
                fontSize = 14.sp,
                color = secondaryTextColor
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Login Button
        Button(
            onClick = { login() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = textColor),
            enabled = !isLoading && !isGoogleLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = backgroundColor,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "ĐĂNG NHẬP",
                    color = backgroundColor,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Divider
        Text(
            text = "hoặc",
            color = secondaryTextColor,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In Button
        SocialLoginButton(
            text = "Tiếp tục với Google",
            iconResId = R.drawable.logogoogle,
            onClick = {
                isGoogleLoading = true
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            },
            isLoading = isGoogleLoading,
            enabled = !isLoading && !isGoogleLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Up Text
        SignUpText(navController = navController)

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun SocialLoginButton(
    text: String,
    iconResId: Int,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val textColor = ThemeManager.getTextColor()
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(8.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(secondaryTextColor.copy(alpha = 0.5f))
        ),
        enabled = enabled
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = textColor
                )
            } else {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = "$text icon",
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = text,
                    color = textColor,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun SignUpText(navController: NavController) {
    val secondaryTextColor = ThemeManager.getSecondaryTextColor()
    val accentColor = ThemeManager.getAccentColor()

    val annotatedText = buildAnnotatedString {
        withStyle(style = SpanStyle(color = secondaryTextColor, fontSize = 14.sp)) {
            append("Chưa có tài khoản? ")
        }

        pushStringAnnotation(tag = "SignUp", annotation = "SignUp")
        withStyle(style = SpanStyle(color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)) {
            append("Đăng ký")
        }
        pop()
    }

    ClickableText(
        text = annotatedText,
        onClick = { offset ->
            annotatedText.getStringAnnotations(tag = "SignUp", start = offset, end = offset)
                .firstOrNull()?.let {
                    navController.navigate("signup")
                }
        }
    )
}
