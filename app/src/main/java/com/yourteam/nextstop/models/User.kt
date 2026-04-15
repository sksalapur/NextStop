package com.yourteam.nextstop.models

import com.google.firebase.firestore.DocumentId

/**
 * Represents a user in the system.
 * Roles can be: "admin", "driver", or "student"
 */
data class User(
    @DocumentId
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val assignedStopId: String? = null
)
