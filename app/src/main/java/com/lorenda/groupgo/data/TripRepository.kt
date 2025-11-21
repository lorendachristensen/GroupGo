package com.lorenda.groupgo.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class TripRepository {
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val tripsCollection = firestore.collection("trips")
    private val tag = "TripRepository"

    suspend fun createTrip(
        name: String,
        destination: String,
        startDate: String,
        endDate: String,
        budget: String,
        numberOfPeople: String,

    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")

            val trip = Trip(
                id = tripsCollection.document().id,
                name = name,
                destination = destination,
                startDate = startDate,
                endDate = endDate,
                budget = budget,
                numberOfPeople = numberOfPeople,
                createdBy = userId,
                createdAt = System.currentTimeMillis(),
                participants = emptyList()
            )

            tripsCollection.document(trip.id).set(trip).await()
            Log.d(tag, "Created trip ${trip.id} for user $userId")
            Result.success(trip.id)
        } catch (e: Exception) {
            Log.e(tag, "Failed to create trip", e)
            Result.failure(e)
        }
    }

    fun getUserTrips(userId: String?): Flow<List<Trip>> {
        if (userId.isNullOrBlank()) return flowOf(emptyList())

        val ownedFlow = callbackFlow {
            val subscription = tripsCollection
                .whereEqualTo("createdBy", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val trips = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Trip::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    trySend(trips)
                }
            awaitClose { subscription.remove() }
        }

        val participantFlow = callbackFlow {
            val subscription = tripsCollection
                .whereArrayContains("participants", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val trips = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Trip::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    trySend(trips)
                }
            awaitClose { subscription.remove() }
        }

        return ownedFlow.combine(participantFlow) { owned, participating ->
            (owned + participating)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }
        }
    }
    suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            tripsCollection.document(tripId).delete().await()
            Log.d(tag, "Deleted trip $tripId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete trip $tripId", e)
            Result.failure(e)
        }
    }
    suspend fun updateTrip(
        tripId: String,
        name: String,
        destination: String,
        budget: String,
        numberOfPeople: String,
        startDate: String,
        endDate: String
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "name" to name,
                "destination" to destination,
                "budget" to budget,
                "numberOfPeople" to numberOfPeople,
                "startDate" to startDate,
                "endDate" to endDate
            )
            tripsCollection.document(tripId).update(updates).await()
            Log.d(tag, "Updated trip $tripId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update trip $tripId", e)
            Result.failure(e)
        }
    }

    suspend fun removeParticipant(tripId: String, userId: String): Result<Unit> {
        return try {
            tripsCollection.document(tripId)
                .update("participants", FieldValue.arrayRemove(userId))
                .await()
            Log.d(tag, "Removed participant $userId from trip $tripId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to remove participant $userId from trip $tripId", e)
            Result.failure(e)
        }
    }
}
