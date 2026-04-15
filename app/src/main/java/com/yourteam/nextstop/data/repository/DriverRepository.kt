package com.yourteam.nextstop.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.yourteam.nextstop.models.Bus
import com.yourteam.nextstop.models.LiveLocation
import com.yourteam.nextstop.models.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriverRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val realtimeDb: FirebaseDatabase
) {

    /**
     * Returns the currently signed-in driver's UID, or null.
     */
    fun getCurrentUid(): String? = auth.currentUser?.uid

    /**
     * Observes all routes currently assigned to the driver.
     */
    fun observeAssignedRoutes(uid: String): Flow<List<Route>> {
        return firestore.collection("routes")
            .whereEqualTo("assignedDriverId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Route::class.java)
            }
    }

    /**
     * Fetches the Bus document for the given busId.
     */
    suspend fun getBus(busId: String): Bus? {
        val snapshot = firestore.collection("buses").document(busId).get().await()
        return if (snapshot.exists()) snapshot.toObject(Bus::class.java) else null
    }

    /**
     * Writes a live location update to the Realtime Database.
     */
    suspend fun updateLiveLocation(routeId: String, location: LiveLocation) {
        realtimeDb.reference
            .child("live_location")
            .child(routeId)
            .setValue(location)
            .await()
    }

    /**
     * Sets active = false in the RTDB live_location node.
     */
    suspend fun clearLiveLocation(routeId: String) {
        realtimeDb.reference
            .child("live_location")
            .child(routeId)
            .child("active")
            .setValue(false)
            .await()
    }
}
