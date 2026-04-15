package com.yourteam.nextstop.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.yourteam.nextstop.models.Bus
import com.yourteam.nextstop.models.CollegeLocation
import com.yourteam.nextstop.models.LiveLocation
import com.yourteam.nextstop.models.Route
import com.yourteam.nextstop.models.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val realtimeDb: FirebaseDatabase
) {
    /**
     * Observe central college location.
     */
    fun observeCollegeLocation(): Flow<CollegeLocation?> {
        return firestore.collection("settings").document("college_location")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.toObject(CollegeLocation::class.java) else null
            }
    }

    /**
     * Update the central college location.
     */
    suspend fun setCollegeLocation(location: CollegeLocation) {
        firestore.collection("settings").document("college_location").set(location).await()
    }

    /**
     * Observe all buses in real-time.
     */
    fun observeBuses(): Flow<List<Bus>> {
        return firestore.collection("buses").snapshots().map { snapshot ->
            snapshot.toObjects(Bus::class.java)
        }
    }

    /**
     * Observe all routes in real-time.
     */
    fun observeRoutes(): Flow<List<Route>> {
        return firestore.collection("routes").snapshots().map { snapshot ->
            snapshot.toObjects(Route::class.java)
        }
    }

    /**
     * Observe all drivers in real-time.
     */
    fun observeDrivers(): Flow<List<User>> {
        return firestore.collection("users")
            .whereEqualTo("role", "driver")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(User::class.java)
            }
    }

    /**
     * Observe the entire live_location node to get a map of busId -> LiveLocation.
     */
    fun observeAllLiveLocations(): Flow<Map<String, LiveLocation>> = callbackFlow {
        val ref = realtimeDb.reference.child("live_location")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val locations = mutableMapOf<String, LiveLocation>()
                for (child in snapshot.children) {
                    val busId = child.key ?: continue
                    val location = child.getValue(LiveLocation::class.java)
                    if (location != null) {
                        locations[busId] = location
                    }
                }
                trySend(locations)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Assigns a driver to a specific route.
     * Optionally clears the driver from another route if they were reassigned, or updates directly.
     */
    suspend fun assignDriverToRoute(routeId: String, newDriverUid: String) {
        firestore.collection("routes").document(routeId)
            .update("assignedDriverId", newDriverUid)
            .await()
    }

    /**
     * Unassign a driver from a route.
     */
    suspend fun unassignDriverFromRoute(routeId: String) {
        firestore.collection("routes").document(routeId)
            .update("assignedDriverId", null)
            .await()
    }

    /**
     * Add a new bus to Firestore.
     */
    suspend fun addBus(bus: Bus) {
        val ref = firestore.collection("buses").document()
        val newBus = bus.copy(busId = ref.id)
        ref.set(newBus).await()
    }

    /**
     * Add a new route to Firestore.
     */
    suspend fun addRoute(route: Route) {
        val ref = firestore.collection("routes").document()
        val newRoute = route.copy(routeId = ref.id)
        ref.set(newRoute).await()
    }

    /**
     * Delete existing route.
     */
    suspend fun deleteRoute(routeId: String) {
        firestore.collection("routes").document(routeId).delete().await()
    }

    /**
     * Update an existing bus in Firestore.
     */
    suspend fun updateBus(busId: String, busNumber: String) {
        val updates = mapOf(
            "busNumber" to busNumber
        )
        firestore.collection("buses").document(busId).update(updates).await()
    }

    /**
     * Delete existing bus.
     */
    suspend fun deleteBus(busId: String) {
        firestore.collection("buses").document(busId).delete().await()
    }

    /**
     * Update an existing route in Firestore.
     */
    suspend fun updateRoute(route: Route) {
        val ref = firestore.collection("routes").document(route.routeId)
        ref.set(route).await()
    }

    /**
     * Invite a new driver by adding their email to the invited_drivers collection.
     */
    suspend fun inviteDriver(email: String) {
        val docRef = firestore.collection("invited_drivers").document(email.lowercase())
        val data = hashMapOf("email" to email.lowercase(), "createdAt" to System.currentTimeMillis())
        docRef.set(data).await()
    }

    /**
     * Delete a driver (removes user document).
     */
    suspend fun deleteDriver(driverUid: String) {
        firestore.collection("users").document(driverUid).delete().await()
    }

    /**
     * Update driver's name.
     */
    suspend fun updateDriverName(driverUid: String, newName: String) {
        firestore.collection("users").document(driverUid).update("name", newName).await()
    }
}
