package com.lorenda.groupgo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.lorenda.groupgo.ui.auth.LoginScreen
import com.lorenda.groupgo.ui.theme.GroupGoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GroupGoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginScreen(
                        modifier = Modifier.padding(innerPadding),
                        onLoginClick = { email, password ->
                            // We'll add Firebase login here soon
                            println("Login attempted with: $email")
                        },
                        onSignUpClick = {
                            // We'll add navigation to signup here soon
                            println("Navigate to sign up")
                        }
                    )
                }
            }
        }
    }
}