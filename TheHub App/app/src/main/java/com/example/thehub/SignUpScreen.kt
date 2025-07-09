package com.example.thehub

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SignUpScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val auth: FirebaseAuth = Firebase.auth
    val db = Firebase.firestore

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logothehub),
            contentDescription = "App Logo",
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Sign up for TheHub",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Your password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm your new password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password != confirmPassword) {
                    Toast.makeText(context, "Mật khẩu xác nhận không khớp.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                coroutineScope.launch {
                    try {
                        val trimmedUsername = username.trim()

                        //  Kiểm tra xem username đã tồn tại trong Firestore chưa
                        val usersRef = db.collection("users")
                        val usernameQuery = usersRef.whereEqualTo("username", trimmedUsername).limit(1).get().await()
                        if (!usernameQuery.isEmpty) {
                            Toast.makeText(context, "Username này đã tồn tại.", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        // Xử lý thông minh: Nếu username là email, dùng nó. Nếu không, tạo email giả.
                        val emailForAuth = if (android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedUsername).matches()) {
                            trimmedUsername
                        } else {
                            "$trimmedUsername@example.com"
                        }

                        //  Tạo tài khoản trong Firebase Authentication
                        val authResult = auth.createUserWithEmailAndPassword(emailForAuth, password.trim()).await()
                        val firebaseUser = authResult.user

                        //  Lưu thông tin người dùng vào Firestore
                        if (firebaseUser != null) {
                            val userMap = hashMapOf(
                                "uid" to firebaseUser.uid,
                                "username" to trimmedUsername,
                                "emailForAuth" to emailForAuth // Lưu email đã dùng để xác thực
                            )
                            db.collection("users").document(firebaseUser.uid).set(userMap).await()
                        }

                        Toast.makeText(context, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show()
                        navController.navigate("login") { popUpTo("login") { inclusive = true } }
                    } catch (e: Exception) {
                        Log.e("SignUp", "Đăng ký thất bại", e)
                        if (e is FirebaseAuthUserCollisionException) {
                            Toast.makeText(context, "Username hoặc email tương ứng đã được sử dụng.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Đăng ký thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text(text = "SIGN UP", color = Color.White, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        LoginText(navController)
    }
}

@Composable
fun LoginText(navController: NavController) {
    val annotatedText = buildAnnotatedString {
        withStyle(style = SpanStyle(color = Color.Gray, fontSize = 14.sp)) {
            append("Already have an account? ")
        }
        pushStringAnnotation(tag = "Login", annotation = "Login")
        withStyle(style = SpanStyle(color = Color(0xFF0052D4), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)) {
            append("Login")
        }
        pop()
    }
    ClickableText(
        text = annotatedText,
        onClick = { offset ->
            annotatedText.getStringAnnotations(tag = "Login", start = offset, end = offset)
                .firstOrNull()?.let {
                    navController.navigate("login")
                }
        }
    )
}
