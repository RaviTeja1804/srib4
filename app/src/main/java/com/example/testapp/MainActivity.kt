package com.example.testapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.testapp.data.model.User
import com.example.testapp.ui.screens.HomeScreen
import com.example.testapp.ui.screens.AuthScreen
import com.example.testapp.ui.theme.TestAppTheme
import com.example.testapp.viewmodel.AuthViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val systemUiController = rememberSystemUiController()
            val useDarkIcons = true

            DisposableEffect(systemUiController, useDarkIcons) {
                systemUiController.setStatusBarColor(
                    color = Color.Transparent,
                    darkIcons = useDarkIcons
                )
                systemUiController.setNavigationBarColor(
                    color = Color.Transparent,
                    darkIcons = useDarkIcons
                )
                onDispose {}
            }

            TestAppTheme {
                var isLoggedIn by remember { mutableStateOf(false) }
                var currentUser by remember { mutableStateOf<User?>(null) }

                if (isLoggedIn && currentUser != null) {
                    HomeScreen(
                        user = currentUser!!,
                        onLogoutClick = {
                            currentUser = null
                            isLoggedIn = false
                        }
                    )
                } else {
                    AuthScreen(
                        onLoginClick = { username, password, result ->
                            authViewModel.login(username, password) { success, user, message ->
                                runOnUiThread {
                                    if (success && user != null) {
                                        currentUser = user
                                        isLoggedIn = true
                                    } else {
                                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onSignupClick = { username, fullName, password, result ->
                            authViewModel.signup(username, fullName, password) { success, user, message ->
                                runOnUiThread {
                                    if (success && user != null) {
                                        currentUser = user
                                        isLoggedIn = true
                                    } else {
                                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}