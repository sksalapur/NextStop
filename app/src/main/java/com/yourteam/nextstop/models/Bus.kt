package com.yourteam.nextstop.models

import com.google.firebase.firestore.DocumentId

/**
 * Registry of physical buses in the fleet.
 */
data class Bus(
    @DocumentId
    val busId: String = "",
    val busNumber: String = ""
)
