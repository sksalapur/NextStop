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
        val sdmcetLat = 15.4210
        val sdmcetLng = 75.0230
        val sdmcet = Triple("SDMCET", sdmcetLat, sdmcetLng)

        // 4 corridors — each is a straight-line path converging on SDMCET
        // Stops are ordered from farthest to nearest, then SDMCET at the end

        val corridors = listOf(
            // Route 1: Hubli City Center → SDMCET  (SE → NW)
            listOf(
                Triple("Keshwapur", 15.3580, 75.1320),
                Triple("Vidyanagar", 15.3750, 75.0950),
                Triple("Navanagar", 15.3950, 75.0600)
            ),
            // Route 2: Dharwad → SDMCET  (NE → SW)
            listOf(
                Triple("Jubilee Circle", 15.4580, 75.0060),
                Triple("Toll Naka", 15.4450, 75.0130),
                Triple("KCD Circle", 15.4350, 75.0180)
            ),
            // Route 3: Old Hubli → SDMCET  (S → NW)
            listOf(
                Triple("Old Hubli", 15.3480, 75.1420),
                Triple("Unkal Lake", 15.3720, 75.1000),
                Triple("Amargol", 15.3980, 75.0580)
            ),
            // Route 4: Gokul Road → SDMCET  (SW → N)
            listOf(
                Triple("Gokul Road", 15.3420, 75.1020),
                Triple("Rayapur", 15.3800, 75.0680),
                Triple("Tarihal", 15.4050, 75.0400)
            )
        )

        for (i in 1..4) {
            val driverId = "mock_driver_$i"
            val busId = "bus_$i"

            // Create driver user
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
                busNumber = "KA-25-A-100$i"
            )
            firestore.collection("buses").document(busId).set(bus).await()

            val corridor = corridors[i - 1]

            // TO_COLLEGE: farthest stop → mid → nearest → SDMCET
            val toCollegeStops = (corridor + sdmcet).mapIndexed { idx, node ->
                Stop(
                    stopId = UUID.randomUUID().toString(),
                    stopName = node.first,
                    latitude = node.second,
                    longitude = node.third,
                    order = idx
                )
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

            // FROM_COLLEGE: SDMCET → nearest → mid → farthest
            val fromCollegeStops = (listOf(sdmcet) + corridor.reversed()).mapIndexed { idx, node ->
                Stop(
                    stopId = UUID.randomUUID().toString(),
                    stopName = node.first,
                    latitude = node.second,
                    longitude = node.third,
                    order = idx
                )
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
        for (i in 1..4) {
            val driverId = "mock_driver_$i"
            val busId = "bus_$i"
            val toCollegeRouteId = "route_to_college_$i"
            val toHomeRouteId = "route_to_home_$i"

            firestore.collection("users").document(driverId).delete().await()
            firestore.collection("buses").document(busId).delete().await()
            firestore.collection("routes").document(toCollegeRouteId).delete().await()
            firestore.collection("routes").document(toHomeRouteId).delete().await()
            // Legacy cleanup
            firestore.collection("routes").document("route_from_college_$i").delete().await()
        }
        // Clean up old mock data (5-10) from previous seeds
        for (i in 5..10) {
            try {
                firestore.collection("users").document("mock_driver_$i").delete().await()
                firestore.collection("buses").document("bus_$i").delete().await()
                firestore.collection("routes").document("route_to_college_$i").delete().await()
                firestore.collection("routes").document("route_to_home_$i").delete().await()
                firestore.collection("routes").document("route_from_college_$i").delete().await()
            } catch (_: Exception) { /* ignore if they don't exist */ }
        }
    }
}
