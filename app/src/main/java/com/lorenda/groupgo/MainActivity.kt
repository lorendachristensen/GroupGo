package com.lorenda.groupgo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.lorenda.groupgo.ui.auth.LoginScreen
import com.lorenda.groupgo.ui.auth.SignUpScreen
import com.lorenda.groupgo.ui.theme.GroupGoTheme
import com.lorenda.groupgo.ui.home.HomeScreen
import com.lorenda.groupgo.ui.trips.CreateTripScreen
import com.lorenda.groupgo.data.TripRepository
import kotlinx.coroutines.launch

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

    // Trip repository and trips list
    val tripRepository = remember { TripRepository() }
    val trips by tripRepository.getUserTrips().collectAsState(initial = emptyList())

    // Track which screen to show
    var showSignUp by remember { mutableStateOf(false) }
    var showCreateTrip by remember { mutableStateOf(false) }

    // Coroutine scope for async operations
    val scope = rememberCoroutineScope()

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
            showCreateTrip -> {
                CreateTripScreen(
                    onBackClick = {
                        showCreateTrip = false
                    },
                    onCreateClick = { name, destination, budget, people ->
                        scope.launch {
                            val result = tripRepository.createTrip(
                                name = name,
                                destination = destination,
                                startDate = "TBD",  // We'll improve date handling later
                                endDate = "TBD",
                                budget = budget,
                                numberOfPeople = people
                            )

                            if (result.isSuccess) {
                                Toast.makeText(
                                    context,
                                    "Trip '$name' created successfully!",
                                    Toast.LENGTH_LONG
                                ).show()
                                showCreateTrip = false
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error creating trip: ${result.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
            }
            isLoggedIn -> {
                HomeScreen(
                    userEmail = authViewModel.auth.currentUser?.email ?: "User",
                    trips = trips,  // Pass the trips list
                    onCreateTripClick = {
                        showCreateTrip = true
                    },
                    onLogoutClick = {
                        authViewModel.signOut()
                        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            showSignUp -> {
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