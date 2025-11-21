package com.lorenda.groupgo.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val displayName: String = ""
)

class UserRepository {
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val usersCollection = firestore.collection("users")

    suspend fun saveUserProfile(
        uid: String,
        firstName: String,
        lastName: String,
        email: String
    ): Result<Unit> {
        return try {
            val profile = UserProfile(
                uid = uid,
                firstName = firstName,
                lastName = lastName,
                email = email,
                displayName = "$firstName $lastName".trim()
            )
            usersCollection.document(uid).set(profile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserNames(uids: List<String>): Flow<Map<String, String>> {
        if (uids.isEmpty()) return flowOf(emptyMap())

        // Firestore whereIn supports up to 10 items; chunk if more.
        val chunks = uids.chunked(10)
        return callbackFlow {
            val listeners = chunks.map { chunk ->
                usersCollection
                    .whereIn("uid", chunk)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val map = snapshot?.documents?.associate { doc ->
                            val profile = doc.toObject(UserProfile::class.java)
                            val uid = profile?.uid ?: doc.id
                            uid to (profile?.displayName?.ifBlank { profile?.email ?: uid } ?: uid)
                        } ?: emptyMap()
                        trySend(map)
                    }
            }

            awaitClose { listeners.forEach { it.remove() } }
        }
    }
}

