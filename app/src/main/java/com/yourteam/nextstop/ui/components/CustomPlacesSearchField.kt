package com.yourteam.nextstop.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.delay

data class PlaceResult(val name: String, val latLng: LatLng)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPlacesSearchField(
    value: String,
    onPlaceSelected: (PlaceResult) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Search location..."
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf(value) }
    var expanded by remember { mutableStateOf(false) }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }

    // Ensures we only query Google Places after the user stops typing for 600ms
    LaunchedEffect(query) {
        if (query.isBlank()) {
            predictions = emptyList()
            expanded = false
            return@LaunchedEffect
        }
        
        delay(600) // Debounce so we don't spam Google APIs
        
        try {
            if (Places.isInitialized()) {
                val placesClient = Places.createClient(context)
                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(query)
                    .setCountries("IN")
                    .build()
                
                placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener { response ->
                        predictions = response.autocompletePredictions
                        expanded = predictions.isNotEmpty()
                    }
                    .addOnFailureListener {
                        // Rate limit / Quota error / Unauthenticated
                        predictions = emptyList()
                        expanded = false
                    }
            }
        } catch (e: Exception) {
            predictions = emptyList()
            expanded = false
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true
        )
        
        if (predictions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                predictions.forEach { prediction ->
                    DropdownMenuItem(
                        text = { Text(prediction.getFullText(null).toString()) },
                        onClick = {
                            expanded = false
                            query = "" // Clear the search bar after picking
                            fetchPlaceDetails(context, prediction.placeId, prediction.getFullText(null).toString()) { placeResult ->
                                if (placeResult != null) {
                                    onPlaceSelected(placeResult)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun fetchPlaceDetails(
    context: Context,
    placeId: String,
    name: String,
    onResult: (PlaceResult?) -> Unit
) {
    if (!Places.isInitialized()) {
        onResult(null)
        return
    }

    val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
    val request = FetchPlaceRequest.newInstance(placeId, placeFields)
    val placesClient = Places.createClient(context)

    placesClient.fetchPlace(request)
        .addOnSuccessListener { response ->
            val latLng = response.place.latLng
            if (latLng != null) {
                onResult(PlaceResult(name = name, latLng = latLng))
            } else {
                onResult(null)
            }
        }
        .addOnFailureListener {
            onResult(null)
        }
}
