package com.lorenda.groupgo.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Singleton

@Singleton
class InvitationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val invitationsCollection = firestore.collection("invitations")

    // Create invitation (called by trip owner)
    suspend fun sendInvitation(
        tripId: String,
        tripName: String,
        invitedEmail: String
    ): Result<String> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))

            val invitation = Invitation(
                id = invitationsCollection.document().id,
                tripId = tripId,
                tripName = tripName,
                invitedByUid = currentUser.uid,
                invitedByEmail = currentUser.email ?: "",
                invitedEmail = invitedEmail.lowercase().trim()
            )

            invitationsCollection.document(invitation.id).set(invitation).await()
            Result.success(invitation.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all pending invitations for current user (by email)
    fun getPendingInvitations(): Flow<List<Invitation>> = callbackFlow {
        val currentUserEmail = auth.currentUser?.email?.lowercase()?.trim()
            ?: run {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

        val subscription = invitationsCollection
            .whereEqualTo("invitedEmail", currentUserEmail)
            .whereEqualTo("status", "pending")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val invitations = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Invitation::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(invitations)
            }

        awaitClose { subscription.remove() }
    }

    // Get invitations for a specific trip
    fun getInvitationsForTrip(tripId: String): Flow<List<Invitation>> = callbackFlow {
        val subscription = invitationsCollection
            .whereEqualTo("tripId", tripId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val invitations = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Invitation::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(invitations)
            }

        awaitClose { subscription.remove() }
    }

    // Accept invitation and add user to trip participants
    suspend fun acceptInvitation(invitationId: String, tripId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))

            firestore.runTransaction { transaction ->
                val invitationRef = invitationsCollection.document(invitationId)
                val tripRef = firestore.collection("trips").document(tripId)

                val invitationSnapshot = transaction.get(invitationRef)
                val tripSnapshot = transaction.get(tripRef)

                if (!invitationSnapshot.exists()) throw Exception("Invitation not found")
                val invitedEmail = invitationSnapshot.getString("invitedEmail").orEmpty()
                val resolvedEmail = currentUser.email ?: invitedEmail

                // Update invitation status
                transaction.update(invitationRef, mapOf(
                    "status" to "accepted",
                    "acceptedAt" to System.currentTimeMillis(),
                    "acceptedByUid" to currentUser.uid,
                    "acceptedByDisplayName" to (currentUser.displayName ?: resolvedEmail)
                ))

                // Add user UID/email to trip participants (create field if not exists)
                val currentParticipants = tripSnapshot.get("participants") as? List<String> ?: emptyList()
                val currentParticipantEmails = tripSnapshot.get("participantsEmails") as? List<String> ?: emptyList()
                if (!currentParticipants.contains(currentUser.uid)) {
                    transaction.update(tripRef, mapOf(
                        "participants" to (currentParticipants + currentUser.uid),
                        "participantsEmails" to (currentParticipantEmails + resolvedEmail)
                    ))
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineInvitation(invitationId: String): Result<Unit> {
        return try {
            invitationsCollection.document(invitationId)
                .update("status", "declined")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
