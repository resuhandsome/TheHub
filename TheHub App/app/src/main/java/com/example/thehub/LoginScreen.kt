package com.example.thehub

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = Firebase.auth

    fun navigateToHome() {
        Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
        navController.navigate("home") {
            popUpTo("login") { inclusive = true }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken!!
                coroutineScope.launch {
                    try {
                        val credential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(credential).await()
                        navigateToHome()
                    } catch (e: Exception) {
                        Log.e("FirebaseAuth", "Firebase sign-in failed", e)
                    }
                }
            } catch (e: ApiException) {
                Log.w("GoogleSignIn", "Google sign in failed", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logothehub),
            contentDescription = "App Logo",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Login in to TheHub",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(40.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
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
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "or", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))

        val googleSignInClient = remember {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            GoogleSignIn.getClient(context, gso)
        }

        SocialLoginButton(
            text = "Continue with Google",
            iconResId = R.drawable.logogoogle,
            onClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        SocialLoginButton(
            text = "Continue with Facebook",
            iconResId = R.drawable.logofacebook,
            onClick = { /* TODO */ }
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Vui lòng nhập email và mật khẩu.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                coroutineScope.launch {
                    try {
                        auth.signInWithEmailAndPassword(email.trim(), password.trim()).await()
                        navigateToHome()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Đăng nhập thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text(text = "LOGIN", color = Color.White, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        SignUpText(navController = navController)
    }
}

@Composable
fun SocialLoginButton(text: String, iconResId: Int, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(8.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = "$text icon",
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, color = Color.Black, fontSize = 16.sp)
        }
    }
}

@Composable
fun SignUpText(navController: NavController) {
    val annotatedText = buildAnnotatedString {
        withStyle(style = SpanStyle(color = Color.Gray, fontSize = 14.sp)) {
            append("Don't have an account? ")
        }
        pushStringAnnotation(tag = "SignUp", annotation = "SignUp")
        withStyle(style = SpanStyle(color = Color(0xFF0052D4), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)) {
            append("Sign up")
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