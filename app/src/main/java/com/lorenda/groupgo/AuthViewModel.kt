package com.lorenda.groupgo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    val auth: FirebaseAuth = Firebase.auth
    private val usersCollection = Firebase.firestore.collection("users")

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
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    val displayName = user.displayName ?: ""
                    val profile = mapOf(
                        "uid" to user.uid,
                        "email" to (user.email ?: email),
                        "displayName" to displayName
                    )
                    usersCollection.document(user.uid)
                        .set(profile, SetOptions.merge())
                        .await()
                }
                _authState.value = AuthState.Success("Login successful!")
                _isLoggedIn.value = true
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun signUp(email: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("User not created")

                // Update display name
                val displayName = "$firstName $lastName".trim()
                val profileUpdates = userProfileChangeRequest { this.displayName = displayName }
                result.user?.updateProfile(profileUpdates)?.await()

                // Save profile to Firestore
                val profile = mapOf(
                    "uid" to uid,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to email,
                    "displayName" to displayName
                )
                usersCollection.document(uid).set(profile).await()

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
