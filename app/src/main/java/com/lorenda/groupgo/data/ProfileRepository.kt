package com.lorenda.groupgo.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.tasks.await

class ProfileRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore
    ) {

    private val profiles = firestore.collection("profiles")

    suspend fun upsertProfile(profile: UserProfile): Result<Unit> {
        return try {
            profiles.document(profile.uid).set(
                profile.copy(updatedAt = System.currentTimeMillis())
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(uid: String): Result<UserProfile?> {
        return try {
            val snapshot = profiles.document(uid).get().await()
            Result.success(snapshot.toObject(UserProfile::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
