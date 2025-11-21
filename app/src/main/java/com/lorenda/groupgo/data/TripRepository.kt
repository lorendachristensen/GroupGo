package com.lorenda.groupgo.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
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
        numberOfPeople: String,
    ): Result<String> {
        return try {
            val currentUser = auth.currentUser
            val userId = currentUser?.uid ?: throw Exception("User not logged in")

            val trip = Trip(
                id = tripsCollection.document().id,
                name = name,
                destination = destination,
                startDate = startDate,
                endDate = endDate,
                budget = budget,
                numberOfPeople = numberOfPeople,
                createdBy = userId,
                createdByEmail = currentUser?.email ?: "",
                createdAt = System.currentTimeMillis(),
                participants = listOf(userId), // creator is a participant
                participantsEmails = listOf(currentUser?.email ?: "")
            )

            tripsCollection.document(trip.id).set(trip).await()
            Result.success(trip.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateExploratoryTrip(
        tripId: String,
        name: String
    ): Result<Unit> {
        return try {
            tripsCollection.document(tripId).update(mapOf("name" to name)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createExploratoryTrip(
        name: String
    ): Result<String> {
        return try {
            val currentUser = auth.currentUser
            val userId = currentUser?.uid ?: throw Exception("User not logged in")

            val trip = Trip(
                id = tripsCollection.document().id,
                name = name,
                destination = "",
                startDate = "TBD",
                endDate = "TBD",
                budget = "",
                numberOfPeople = "1",
                createdBy = userId,
                createdByEmail = currentUser?.email ?: "",
                createdAt = System.currentTimeMillis(),
                participants = listOf(userId),
                participantsEmails = listOf(currentUser?.email ?: "")
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

        var createdTrips: List<Trip> = emptyList()
        var participantTrips: List<Trip> = emptyList()

        fun emitCombined() {
            val combined = (createdTrips + participantTrips)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }
            trySend(combined)
        }

        val createdSubscription = tripsCollection
            .whereEqualTo("createdBy", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                createdTrips = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Trip::class.java)
                } ?: emptyList()
                emitCombined()
            }

        val participantSubscription = tripsCollection
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                participantTrips = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Trip::class.java)
                } ?: emptyList()
                emitCombined()
            }

        awaitClose {
            createdSubscription.remove()
            participantSubscription.remove()
        }
    }
    suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            tripsCollection.document(tripId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeParticipant(
        tripId: String,
        participantUid: String,
        participantEmail: String
    ): Result<Unit> {
        return try {
            val doc = tripsCollection.document(tripId)
            firestore.runTransaction { tx ->
                val snapshot = tx.get(doc)
                val createdBy = snapshot.getString("createdBy")
                if (participantUid.isNotBlank() && participantUid == createdBy) {
                    throw IllegalArgumentException("Cannot remove organizer")
                }
                val participants = (snapshot.get("participants") as? List<String>).orEmpty()
                val emails = (snapshot.get("participantsEmails") as? List<String>).orEmpty()
                val updatedParticipants = participants.filterNot { it == participantUid }
                val updatedEmails = emails.filterNot { it.equals(participantEmail, ignoreCase = true) }
                val newCount = updatedParticipants.size.coerceAtLeast(1)
                tx.update(
                    doc,
                    mapOf(
                        "participants" to updatedParticipants,
                        "participantsEmails" to updatedEmails,
                        "numberOfPeople" to newCount.toString()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

