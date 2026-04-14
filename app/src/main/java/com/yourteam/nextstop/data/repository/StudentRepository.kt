package com.yourteam.nextstop.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.yourteam.nextstop.models.Bus
import com.yourteam.nextstop.models.LiveLocation
import com.yourteam.nextstop.models.Route
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.snapshots
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val realtimeDb: FirebaseDatabase
) {

    fun getCurrentUid(): String? = auth.currentUser?.uid

    /**
     * Reads the student's assignedRouteId from their user document.
     */
    suspend fun getAssignedRouteId(uid: String): String? {
        val snapshot = firestore.collection("users").document(uid).get().await()
        return snapshot.getString("assignedRouteId")
    }

    /**
     * Reads the student's assignedStopId from their user document.
     */
    suspend fun getAssignedStopId(uid: String): String? {
        val snapshot = firestore.collection("users").document(uid).get().await()
        return snapshot.getString("assignedStopId")
    }

    /**
     * Updates the student's assigned stop.
     */
    suspend fun updateAssignedStopId(uid: String, stopId: String) {
        firestore.collection("users").document(uid).update("assignedStopId", stopId).await()
    }

    /**
     * Fetches the Route document with its list of stops.
     */
    suspend fun getRoute(routeId: String): Route? {
        val snapshot = firestore.collection("routes").document(routeId).get().await()
        return if (snapshot.exists()) snapshot.toObject(Route::class.java) else null
    }

    /**
     * Finds the bus assigned to the given route.
     * Queries the buses collection where routeId matches.
     */
    suspend fun getBusForRoute(routeId: String): Bus? {
        val querySnapshot = firestore.collection("buses")
            .whereEqualTo("routeId", routeId)
            .limit(1)
            .get()
            .await()

        return if (querySnapshot.documents.isNotEmpty()) {
            querySnapshot.documents[0].toObject(Bus::class.java)
        } else null
    }

    /**
     * Observes the live location of a bus in the Realtime Database
     * using a ValueEventListener wrapped in a callbackFlow.
     *
     * Emits a new [LiveLocation] every time the RTDB value changes.
     * Automatically removes the listener when the Flow is cancelled.
     */
    fun observeLiveLocation(busId: String): Flow<LiveLocation?> = callbackFlow {
        val ref = realtimeDb.reference.child("live_location").child(busId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val location = snapshot.getValue(LiveLocation::class.java)
                trySend(location)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
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
     * Observe all live locations in real-time.
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
}
