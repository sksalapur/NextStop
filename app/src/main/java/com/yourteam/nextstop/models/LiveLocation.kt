package com.yourteam.nextstop.models

/**
 * Represents the live location of a bus.
 * Stored in the Firebase Realtime Database.
 */
data class LiveLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val timestamp: Long = 0L,
    val active: Boolean = false
)
