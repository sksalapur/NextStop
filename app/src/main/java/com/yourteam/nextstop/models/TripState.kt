package com.yourteam.nextstop.models

/**
 * Represents the state of a driver's active trip.
 */
sealed class TripState {
    data object Stopped : TripState()
    data class Running(
        val speed: Float = 0f,
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    ) : TripState()
}
