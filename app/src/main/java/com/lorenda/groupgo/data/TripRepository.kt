package com.lorenda.groupgo.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TripRepository {
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val tripsCollection = firestore.collection("trips")

    suspend fun createTrip(
        name: String,
        destination: String,
        startDate: String,
        endDate: String,
        budget: String,
        numberOfPeople: String
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
                createdAt = System.currentTimeMillis()
            )

            tripsCollection.document(trip.id).set(trip).await()
            Result.success(trip.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserTrips(): Flow<List<Trip>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = tripsCollection
            .whereEqualTo("createdBy", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val trips = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Trip::class.java)
                } ?: emptyList()

                trySend(trips)
            }

        awaitClose { subscription.remove() }
    }
}

