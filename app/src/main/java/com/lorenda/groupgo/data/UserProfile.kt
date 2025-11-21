package com.lorenda.groupgo.data

data class UserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val displayName: String = "",
    val profilePic: String = "",
    val shortBio: String = "",
    val homeAirport: String = "",
    val passportId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
