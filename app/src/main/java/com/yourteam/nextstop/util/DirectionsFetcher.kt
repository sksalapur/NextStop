package com.yourteam.nextstop.util

import android.util.Log
import com.yourteam.nextstop.BuildConfig
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object DirectionsFetcher {

    private const val TAG = "DirectionsFetcher"
    private const val BASE_URL = "https://maps.googleapis.com/maps/api/directions/json"
    
    // Cache to avoid repeated API calls for the same route
    private val routeCache = mutableMapOf<String, List<LatLng>>()

    suspend fun getDirections(stops: List<LatLng>): List<LatLng> = withContext(Dispatchers.IO) {
        if (stops.size < 2) return@withContext emptyList()

        // Generate cache key
        val cacheKey = stops.joinToString("|") { "${it.latitude},${it.longitude}" }
        routeCache[cacheKey]?.let { return@withContext it }

        val origin = stops.first()
        val destination = stops.last()
        val waypoints = if (stops.size > 2) {
            stops.subList(1, stops.size - 1)
        } else {
            emptyList()
        }

        val urlString = buildUrl(origin, destination, waypoints)
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseString = reader.use { it.readText() }
                connection.disconnect()

                val path = parseDirectionsResponse(responseString)
                if (path.isNotEmpty()) {
                    routeCache[cacheKey] = path
                }
                return@withContext path
            } else {
                Log.e(TAG, "Directions API error: ${connection.responseCode} ${connection.responseMessage}")
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch directions", e)
        }

        return@withContext emptyList()
    }

    private fun buildUrl(origin: LatLng, destination: LatLng, waypoints: List<LatLng>): String {
        val originStr = "${origin.latitude},${origin.longitude}"
        val destStr = "${destination.latitude},${destination.longitude}"
        
        val urlBuilder = StringBuilder("$BASE_URL?")
        urlBuilder.append("origin=$originStr")
        urlBuilder.append("&destination=$destStr")
        
        if (waypoints.isNotEmpty()) {
            val waypointsStr = waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
            // Using optimize:true can reorder, but we want our specific order, so omit optimize
            urlBuilder.append("&waypoints=$waypointsStr")
        }
        
        urlBuilder.append("&key=${BuildConfig.MAPS_API_KEY}")
        return urlBuilder.toString()
    }

    private fun parseDirectionsResponse(response: String): List<LatLng> {
        val path = mutableListOf<LatLng>()
        try {
            val jsonObject = JSONObject(response)
            val routes = jsonObject.optJSONArray("routes")
            if (routes != null && routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val overviewPolyline = route.getJSONObject("overview_polyline")
                val encodedPath = overviewPolyline.getString("points")
                path.addAll(decodePolyline(encodedPath))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing directions JSON", e)
        }
        return path
    }

    /**
     * Decodes an encoded path string into a sequence of LatLngs.
     * Algorithm standard for Google Maps polylines.
     */
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }
}
