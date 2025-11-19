package com.lorenda.groupgo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lorenda.groupgo.ui.auth.LoginScreen
import com.lorenda.groupgo.ui.auth.SignUpScreen
import com.lorenda.groupgo.ui.theme.GroupGoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GroupGoTheme {
                GroupGoApp()
            }
        }
    }
}

@Composable
fun GroupGoApp() {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val context = LocalContext.current

    // Track which screen to show
    var showSignUp by remember { mutableStateOf(false) }

    // Show messages based on auth state
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                authViewModel.resetState()
                // Reset to login screen after successful signup
                if (state.message.contains("created")) {
                    showSignUp = false
                }
            }
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when {
            isLoggedIn -> {
                // Main app content when logged in - ADD LOGOUT BUTTON
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Welcome to GroupGo!",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You're logged in!")
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            authViewModel.signOut()
                            Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Log Out")
                    }
                }
            }
            showSignUp -> {
                // Show sign up screen
                SignUpScreen(
                    modifier = Modifier.padding(innerPadding),
                    onSignUpClick = { email, password ->
                        authViewModel.signUp(email, password)
                    },
                    onLoginClick = {
                        showSignUp = false
                    },
                    isLoading = authState is AuthState.Loading
                )
            }
            else -> {
                // Show login screen
                LoginScreen(
                    modifier = Modifier.padding(innerPadding),
                    onLoginClick = { email, password ->
                        authViewModel.login(email, password)
                    },
                    onSignUpClick = {
                        showSignUp = true
                    },
                    isLoading = authState is AuthState.Loading
                )
            }
        }
    }
}