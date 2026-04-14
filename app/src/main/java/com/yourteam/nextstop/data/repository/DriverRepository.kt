package com.yourteam.nextstop.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.yourteam.nextstop.models.Bus
import com.yourteam.nextstop.models.LiveLocation
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
     * Reads the driver's user document and returns their assignedBusId.
     */
    suspend fun getAssignedBusId(uid: String): String? {
        val snapshot = firestore.collection("users").document(uid).get().await()
        return snapshot.getString("assignedBusId")
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
    suspend fun updateLiveLocation(busId: String, location: LiveLocation) {
        realtimeDb.reference
            .child("live_location")
            .child(busId)
            .setValue(location)
            .await()
    }

    /**
     * Sets active = false in the RTDB live_location node.
     */
    suspend fun clearLiveLocation(busId: String) {
        realtimeDb.reference
            .child("live_location")
            .child(busId)
            .child("active")
            .setValue(false)
            .await()
    }

    /**
     * Updates `status` field on the Firestore bus document.
     */
    suspend fun setBusStatus(busId: String, active: Boolean) {
        firestore.collection("buses")
            .document(busId)
            .update("status", if (active) "active" else "inactive")
            .await()
    }
}
