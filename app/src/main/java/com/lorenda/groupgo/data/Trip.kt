package com.lorenda.groupgo.data

data class Trip(
    val id: String = "",
    val name: String = "",
    val destination: String = "",
    val startDate: String = "TBD",
    val endDate: String = "TBD",
    val budget: String = "",
    val numberOfPeople: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),

)

