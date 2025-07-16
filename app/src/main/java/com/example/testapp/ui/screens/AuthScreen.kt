package com.example.testapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons // Keep this import
import androidx.compose.material.icons.filled.Visibility // Add this import
import androidx.compose.material.icons.filled.VisibilityOff // Add this import
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testapp.data.model.User // Assuming this is your User data class


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLoginClick: (String, String, (Boolean, User?, String) -> Unit) -> Unit,
    onSignupClick: (String, String, String, (Boolean, User?, String) -> Unit) -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) } // State for password visibility

    // Background uses a subtle vertical gradient from light to even lighter
    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(LightBackground, Color(0xFFE0E6EB)) // Consistent with HomeScreen
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush) // Apply light background gradient
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White) // Pure white for card background
        ) {
            Column(
                modifier = Modifier
                    .padding(36.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = isLogin,
                    transitionSpec = {
                        (slideInVertically { height -> -height / 2 } + fadeIn())
                            .togetherWith(slideOutVertically { height -> height / 2 } + fadeOut())
                            .using(SizeTransform(clip = false))
                    }, label = "Auth Title Animation"
                ) { targetIsLogin ->
                    Text(
                        text = if (targetIsLogin) "Welcome Back!" else "Unleash Your Power!",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                        fontWeight = FontWeight.Black,
                        color = SecondaryAccent // Deep blue for title
                    )
                }

                if (!isLogin) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = BorderLight,
                            focusedLabelColor = PrimaryAccent,
                            unfocusedLabelColor = TextLight,
                            cursorColor = PrimaryAccent,
                            focusedTextColor = TextDark, // Ensure typed text is dark
                            unfocusedTextColor = TextDark // Ensure typed text is dark
                        ),
                        textStyle = LocalTextStyle.current.copy(color = TextDark) // Fallback for text color
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = BorderLight,
                        focusedLabelColor = PrimaryAccent,
                        unfocusedLabelColor = TextLight,
                        cursorColor = PrimaryAccent,
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark
                    ),
                    textStyle = LocalTextStyle.current.copy(color = TextDark)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), // Toggle visibility
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = { // Add the trailing icon for password visibility
                        val imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = imageVector, contentDescription = description, tint = TextLight) // Icon color
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = BorderLight,
                        focusedLabelColor = PrimaryAccent,
                        unfocusedLabelColor = TextLight,
                        cursorColor = PrimaryAccent,
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark
                    ),
                    textStyle = LocalTextStyle.current.copy(color = TextDark)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (isLogin) {
                            onLoginClick(username, password) { success, user, message -> }
                        } else {
                            onSignupClick(username, fullName, password) { success, user, message -> }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent), // Vibrant accent for button
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text(
                        text = if (isLogin) "Login Securely" else "Join the Future",
                        color = Color.White, // White text on vibrant blue
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                TextButton(onClick = {
                    isLogin = !isLogin
                    username = ""
                    password = ""
                    fullName = ""
                    passwordVisible = false // Reset password visibility when switching forms
                }) {
                    Text(
                        text = if (isLogin) "New here? Sign up for Free!" else "Already a member? Login Now!",
                        color = SecondaryAccent, // Deep blue for the switch text
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}