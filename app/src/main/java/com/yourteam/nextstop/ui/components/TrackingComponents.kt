package com.yourteam.nextstop.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.yourteam.nextstop.R
import com.yourteam.nextstop.models.LiveLocation
import com.yourteam.nextstop.models.Stop
import com.yourteam.nextstop.util.LocationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTrackerScreen(
    busNumber: String,
    routeStops: List<Stop>,
    busLocation: LiveLocation?,
    etaMinutes: Int,
    nextStopName: String,
    connectionStatus: String,
    isBusActive: Boolean,
    assignedStopId: String? = null,
    boardingEtaMinutes: Int? = null,
    passedStopIds: Set<String> = emptySet(),
    onStopSelected: ((String, String) -> Unit)? = null, // Passes ID and Name
    modifier: Modifier = Modifier
) {
    if (!isBusActive) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Bus has not started yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = if (onStopSelected != null) 320.dp else 120.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShadowElevation = 16.dp,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Next Stop",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = nextStopName.ifEmpty { "—" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = if (etaMinutes > 0) "$etaMinutes min" else "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Distinct My Stop ETA Display Block
                if (assignedStopId != null) {
                    val assignedStopName = routeStops.find { it.stopId == assignedStopId }?.stopName ?: "Unknown"
                    val isPassed = passedStopIds.contains(assignedStopId)

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isPassed) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isPassed) MaterialTheme.colorScheme.outline
                                   else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "My Stop",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPassed) MaterialTheme.colorScheme.outline
                                        else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = assignedStopName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPassed) MaterialTheme.colorScheme.outline
                                        else MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = when {
                                isPassed || boardingEtaMinutes == -1 -> "Passed"
                                boardingEtaMinutes != null && boardingEtaMinutes >= 0 -> "$boardingEtaMinutes min"
                                else -> "—"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            color = when {
                                isPassed || boardingEtaMinutes == -1 -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                }

                if (onStopSelected != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val selectedStopName = routeStops.find { it.stopId == assignedStopId }?.stopName ?: "Select your stop (alerts)"
                    
                    Text(
                        text = "Set Stop Alerts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    BoardingStopSelector(
                        routeStops = routeStops,
                        selectedStopName = selectedStopName,
                        passedStopIds = passedStopIds,
                        onStopSelected = onStopSelected
                    )
                }
            }
        },
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            BusTrackingMap(
                routeStops = routeStops,
                busLocation = busLocation,
                nextStopName = nextStopName,
                modifier = Modifier.fillMaxSize()
            )

            ConnectionStatusChip(
                status = connectionStatus,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoardingStopSelector(
    routeStops: List<Stop>,
    selectedStopName: String,
    passedStopIds: Set<String>,
    onStopSelected: (String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Only show upcoming stops that the bus hasn't passed yet
    val upcomingStops = routeStops.sortedBy { it.order }.filter { it.stopId !in passedStopIds }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedStopName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (upcomingStops.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("All stops have been passed", color = MaterialTheme.colorScheme.outline) },
                    onClick = { expanded = false },
                    enabled = false
                )
            } else {
                upcomingStops.forEach { stop ->
                    DropdownMenuItem(
                        text = { Text(stop.stopName) },
                        onClick = {
                            onStopSelected(stop.stopId, stop.stopName)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BusTrackingMap(
    routeStops: List<Stop>,
    busLocation: LiveLocation?,
    nextStopName: String,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState()
    var hasInitializedCamera by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(busLocation) {
        if (busLocation != null && !hasInitializedCamera) {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(
                        LatLng(busLocation.latitude, busLocation.longitude),
                        15f
                    )
                )
            )
            hasInitializedCamera = true
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState
    ) {
        if (routeStops.isNotEmpty()) {
            val orderedStops = routeStops.sortedBy { it.order }
            
            // Determine split index for polylines using nextStopName mapping
            val nextStopIndex = orderedStops.indexOfFirst { it.stopName == nextStopName }.takeIf { it != -1 } ?: orderedStops.size

            val passedStops = orderedStops.take(nextStopIndex)
            val upcomingStops = orderedStops.drop(nextStopIndex)

            // Include current bus location in paths so it bridges natively
            val passedPath = mutableListOf<LatLng>()
            passedPath.addAll(passedStops.map { LatLng(it.latitude, it.longitude) })
            if (busLocation != null) passedPath.add(LatLng(busLocation.latitude, busLocation.longitude))

            val upcomingPath = mutableListOf<LatLng>()
            if (busLocation != null) upcomingPath.add(LatLng(busLocation.latitude, busLocation.longitude))
            upcomingPath.addAll(upcomingStops.map { LatLng(it.latitude, it.longitude) })

            // Draw Passed Route (Gray)
            if (passedPath.size > 1) {
                Polyline(
                    points = passedPath,
                    color = Color.Gray.copy(alpha = 0.5f),
                    width = 8f
                )
            }

            // Draw Upcoming Route (Primary Color)
            if (upcomingPath.size > 1) {
                Polyline(
                    points = upcomingPath,
                    color = MaterialTheme.colorScheme.primary,
                    width = 10f
                )
            }
        }

        // Draw Stop Pins
        routeStops.forEach { stop ->
            val isEndpoint = stop.stopId.startsWith("endpoint_")
            Marker(
                state = MarkerState(
                    position = LatLng(stop.latitude, stop.longitude)
                ),
                title = if (isEndpoint) "Final Destination: ${stop.stopName}" else stop.stopName,
                snippet = if (isEndpoint) "Drop off point" else "Stop #${stop.order}",
                icon = BitmapDescriptorFactory.defaultMarker(
                    if (isEndpoint) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
                )
            )
        }

        // Draw custom Bus Vector
        if (busLocation != null) {
            val customBusIcon = remember(context) { 
                LocationUtils.bitmapDescriptorFromVector(context, R.drawable.ic_bus_marker) 
            }
            
            Marker(
                state = MarkerState(
                    position = LatLng(busLocation.latitude, busLocation.longitude)
                ),
                title = "Bus Location",
                snippet = "Speed: ${"%.1f".format(busLocation.speed * 3.6f)} km/h",
                icon = customBusIcon ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f) // Center the bus icon natively
            )
        }
    }
}

@Composable
fun ConnectionStatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val isLive = status == "Live"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (isLive) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FiberManualRecord,
                contentDescription = null,
                modifier = Modifier.size(8.dp),
                tint = if (isLive) MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isLive) MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
