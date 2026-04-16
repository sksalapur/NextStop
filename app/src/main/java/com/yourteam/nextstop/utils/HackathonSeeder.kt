package com.yourteam.nextstop.utils

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.yourteam.nextstop.models.Bus
import com.yourteam.nextstop.models.Route
import com.yourteam.nextstop.models.Stop
import com.yourteam.nextstop.models.User
import com.yourteam.nextstop.models.LiveLocation
import kotlinx.coroutines.tasks.await
import java.util.UUID

object HackathonSeeder {
    suspend fun seedDatabase(firestore: FirebaseFirestore) {
        val sdmcetLat = 15.421
        val sdmcetLng = 75.023

        // Generating 10 mock drivers and 10 buses mapped to Hubballi-Dharwad Twin Cities
        val mainNodes = listOf(
            Pair("New Bus Stand Hubli", Pair(15.362, 75.143)),
            Pair("Navanagar", Pair(15.394, 75.088)),
            Pair("Rayapur", Pair(15.405, 75.068)),
            Pair("SDMCET", Pair(sdmcetLat, sdmcetLng)),
            Pair("KCD Circle", Pair(15.441, 75.011)),
            Pair("Toll Naka", Pair(15.448, 75.021)),
            Pair("Jubilee Circle", Pair(15.452, 75.008)),
            Pair("Kittur Rani Channamma Circle", Pair(15.353, 75.138)),
            Pair("Unkal Lake", Pair(15.372, 75.127)),
            Pair("Vidyanagar", Pair(15.366, 75.130))
        )

        for (i in 1..10) {
            val driverId = "mock_driver_$i"
            val busId = "bus_$i"
            
            // Create user
            val driverUser = User(
                uid = driverId,
                email = "driver$i@sdmcet.edu.in",
                name = "Mock Driver $i",
                role = "driver"
            )
            firestore.collection("users").document(driverId).set(driverUser).await()

            // Create bus
            val bus = Bus(
                busId = busId,
                busNumber = "KA-25-A-10${if(i < 10) "0$i" else i}"
            )
            firestore.collection("buses").document(busId).set(bus).await()

            // Shuffle nodes to create random valid routes
            val routeNodes = mainNodes.shuffled().take(4)
            
            // TO_COLLEGE ROUTE
            val toCollegeStops = (routeNodes.dropLast(1) + Pair("SDMCET", Pair(sdmcetLat, sdmcetLng))).mapIndexed { idx, pair ->
                Stop(stopId = UUID.randomUUID().toString(), stopName = pair.first, latitude = pair.second.first, longitude = pair.second.second, order = idx)
            }
            val toCollegeRoute = Route(
                routeId = "route_to_college_$i",
                name = "To College $i",
                direction = "to_college",
                busId = busId,
                departureTime = "07:30 AM",
                assignedDriverId = driverId,
                startName = toCollegeStops.first().stopName,
                endName = toCollegeStops.last().stopName,
                endLat = toCollegeStops.last().latitude,
                endLng = toCollegeStops.last().longitude,
                stops = toCollegeStops
            )
            firestore.collection("routes").document(toCollegeRoute.routeId).set(toCollegeRoute).await()

            // FROM_COLLEGE ROUTE
            val fromCollegeStops = (listOf(Pair("SDMCET", Pair(sdmcetLat, sdmcetLng))) + routeNodes.drop(1)).mapIndexed { idx, pair ->
                Stop(stopId = UUID.randomUUID().toString(), stopName = pair.first, latitude = pair.second.first, longitude = pair.second.second, order = idx)
            }
            val fromCollegeRoute = Route(
                routeId = "route_to_home_$i",
                name = "To Home $i",
                direction = "from_college",
                busId = busId,
                departureTime = "04:30 PM",
                assignedDriverId = driverId,
                startName = fromCollegeStops.first().stopName,
                endName = fromCollegeStops.last().stopName,
                endLat = fromCollegeStops.last().latitude,
                endLng = fromCollegeStops.last().longitude,
                stops = fromCollegeStops
            )
            firestore.collection("routes").document(fromCollegeRoute.routeId).set(fromCollegeRoute).await()
        }
    }

    suspend fun unseedDatabase(firestore: FirebaseFirestore) {
        for (i in 1..10) {
            val driverId = "mock_driver_$i"
            val busId = "bus_$i"
            val toCollegeRouteId = "route_to_college_$i"
            val toHomeRouteId = "route_to_home_$i"
            
            firestore.collection("users").document(driverId).delete().await()
            firestore.collection("buses").document(busId).delete().await()
            firestore.collection("routes").document(toCollegeRouteId).delete().await()
            firestore.collection("routes").document(toHomeRouteId).delete().await()
            // Legacy clean up for tests that might have run before rename
            firestore.collection("routes").document("route_from_college_$i").delete().await()
        }
    }
}
