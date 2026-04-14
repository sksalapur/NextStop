package com.yourteam.nextstop.models

import com.google.firebase.firestore.DocumentId

/**
 * Represents a bus in the fleet.
 * Status can be: "active" or "inactive"
 */
data class Bus(
    @DocumentId
    val busId: String = "",
    val busNumber: String = "",
    val routeId: String = "",
    val assignedDriverId: String? = null,
    val status: String = "inactive"
)
