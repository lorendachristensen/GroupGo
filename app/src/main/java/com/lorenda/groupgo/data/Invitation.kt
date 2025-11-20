package com.lorenda.groupgo.data

data class Invitation(
    val id: String = "",
    val tripId: String = "",
    val tripName: String = "",
    val invitedByUid: String = "",
    val invitedByEmail: String = "",
    val invitedEmail: String = "",
    val status: String = "pending", // pending, accepted, declined
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null
)

