package com.yourteam.nextstop.models

import com.google.firebase.firestore.DocumentId

/**
 * Represents a route with an ordered list of stops.
 */
data class Route(
    @DocumentId
    val routeId: String = "",
    val name: String = "",
    val startName: String = "",
    val endName: String = "",
    val stops: List<Stop> = emptyList()
)

data class Stop(
    val stopId: String = "",
    val stopName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val order: Int = 0
)
