package com.lorenda.groupgo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    val auth: FirebaseAuth = Firebase.auth
    private val profileRepository = com.lorenda.groupgo.data.ProfileRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        // Check if user is already logged in
        _isLoggedIn.value = auth.currentUser != null
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Success("Login successful!")
                _isLoggedIn.value = true
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        displayName: String,
        profilePic: String,
        shortBio: String,
        homeAirport: String,
        passportId: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                val user = auth.currentUser
                if (user != null) {
                    val profileUpdates = userProfileChangeRequest {
                        this.displayName = displayName.ifBlank { "$firstName $lastName".trim() }
                        if (profilePic.isNotBlank()) {
                            photoUri = android.net.Uri.parse(profilePic)
                        }
                    }
                    user.updateProfile(profileUpdates).await()

                    val profile = com.lorenda.groupgo.data.UserProfile(
                        uid = user.uid,
                        firstName = firstName,
                        lastName = lastName,
                        displayName = displayName.ifBlank { "$firstName $lastName".trim() },
                        profilePic = profilePic,
                        shortBio = shortBio,
                        homeAirport = homeAirport,
                        passportId = passportId
                    )
                    profileRepository.upsertProfile(profile).getOrThrow()
                }
                _authState.value = AuthState.Success("Account created successfully!")
                _isLoggedIn.value = true
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _isLoggedIn.value = false
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    // NEW FUNCTION ADDED FOR PROFILE UPDATE
    fun updateProfile(displayName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = auth.currentUser
                if (user != null) {
                    val profileUpdates = userProfileChangeRequest {
                        this.displayName = displayName
                    }
                    user.updateProfile(profileUpdates).await()
                    _authState.value = AuthState.Success("Profile updated successfully!")
                } else {
                    _authState.value = AuthState.Error("No user logged in")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Profile update failed")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
