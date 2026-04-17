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
import com.yourteam.nextstop.ui.student.LocationFreshness
import com.yourteam.nextstop.ui.utils.formatEta
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.SheetValue
import com.yourteam.nextstop.util.DirectionsFetcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTrackerScreen(
    busNumber: String,
    routeStops: List<Stop>,
    busLocation: LiveLocation?,
    etaMinutes: Int,
    nextStopName: String,
    isBusActive: Boolean,
    assignedStopId: String? = null,
    boardingEtaMinutes: Int? = null,
    passedStopIds: Set<String> = emptySet(),
    locationFreshness: LocationFreshness = LocationFreshness.Fresh,
    alertEnabled: Boolean = false,
    onToggleAlert: (() -> Unit)? = null,
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

    val cameraPositionState = rememberCameraPositionState()
    var hasInitializedCamera by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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

    val showCenterFab by remember {
        derivedStateOf {
            if (busLocation == null) return@derivedStateOf false
            val pos = cameraPositionState.position.target
            val distance = LocationUtils.haversineKm(
                busLocation.latitude, busLocation.longitude,
                pos.latitude, pos.longitude
            ) * 1000
            distance > 500
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 120.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShadowElevation = 16.dp,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .alpha(if (locationFreshness is LocationFreshness.Unavailable) 0.4f else 1.0f)
            ) {
                if (assignedStopId != null) {
                    val assignedStopName = routeStops.find { it.stopId == assignedStopId }?.stopName ?: "Unknown"
                    val isPassed = passedStopIds.contains(assignedStopId)

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
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isPassed) MaterialTheme.colorScheme.outline
                                   else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Your Stop",
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

                        if (locationFreshness !is LocationFreshness.Unavailable) {
                            val boardingEtaText = if (boardingEtaMinutes != null) formatEta(boardingEtaMinutes) else "—"
                            Text(
                                text = when {
                                    isPassed || boardingEtaMinutes == -1 -> "Passed"
                                    boardingEtaMinutes != null && boardingEtaMinutes >= 0 -> boardingEtaText
                                    else -> "—"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = when {
                                    isPassed || boardingEtaMinutes == -1 -> MaterialTheme.colorScheme.outline
                                    boardingEtaText == "Bus is far away" -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }

                        if (onToggleAlert != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { onToggleAlert() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (alertEnabled) Icons.Default.NotificationsActive 
                                                  else Icons.Default.NotificationsNone,
                                    contentDescription = "Toggle Alerts",
                                    tint = if (alertEnabled) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
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
                            if (locationFreshness is LocationFreshness.Stale) {
                                Text(
                                    text = "ETA may be inaccurate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        if (locationFreshness !is LocationFreshness.Unavailable) {
                            val etaText = formatEta(etaMinutes)
                            Text(
                                text = if (etaMinutes >= 0) etaText else "—",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (etaText == "Bus is far away") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                } // Closes else block

                // NEW: Divider + Scrollable Stop List
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                val orderedStops = remember(routeStops) { routeStops.sortedBy { it.order } }
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(orderedStops) { stop ->
                        val isPassed = passedStopIds.contains(stop.stopId)
                        val isNext = stop.stopName == nextStopName
                        
                        val stopEtaMinutes = if (!isPassed && busLocation != null) {
                            LocationUtils.calculateEtaMinutes(
                                busLat = busLocation.latitude, busLon = busLocation.longitude,
                                stopLat = stop.latitude, stopLon = stop.longitude,
                                speedKmh = busLocation.speed * 3.6f
                            )
                        } else null
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPassed) Icons.Default.CheckCircle else Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = when {
                                    isNext -> MaterialTheme.colorScheme.primary
                                    isPassed -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stop.stopName,
                                style = if (isNext) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                                color = if (isPassed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            
                            if (isNext && locationFreshness !is LocationFreshness.Unavailable) {
                                Text("Next", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            } else if (!isPassed && stopEtaMinutes != null && stopEtaMinutes > 0) {
                                val stopEtaText = formatEta(stopEtaMinutes)
                                val shortText = if (stopEtaText == "Bus is far away") "Far" else stopEtaText
                                Text(shortText, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            BusTrackingMap(
                routeStops = routeStops,
                busLocation = busLocation,
                nextStopName = nextStopName,
                isBusActive = isBusActive,
                locationFreshness = locationFreshness,
                cameraPositionState = cameraPositionState,
                modifier = Modifier.fillMaxSize()
            )

            // Route Details Header
            if (routeStops.isNotEmpty()) {
                val orderedStops = routeStops.sortedBy { it.order }
                val startStop = orderedStops.first().stopName
                val endStop = orderedStops.last().stopName
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 16.dp, end = 120.dp) // Avoid live chip
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = startStop,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 4.dp).size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = endStop,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }

            // Animated Connection Status Chip
            val chipColor = if (locationFreshness is LocationFreshness.Fresh) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = chipColor.copy(alpha = 0.15f),
                contentColor = chipColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                AnimatedContent(
                    targetState = locationFreshness,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "status_anim"
                ) { freshness ->
                    val text = when (freshness) {
                        is LocationFreshness.Fresh -> "● Live"
                        is LocationFreshness.Stale -> "● ${freshness.minutesAgo} min ago"
                        is LocationFreshness.Unavailable -> "● Offline"
                    }
                    Text(
                        text = text,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Re-center FAB
            AnimatedVisibility(
                visible = showCenterFab,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 184.dp, end = 16.dp), // Height above sheet + standard margin
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        busLocation?.let { loc ->
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f)
                                )
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Center Map")
                }
            }
            
            // Stop Process Bar
            val busLatLng = busLocation?.let { LatLng(it.latitude, it.longitude) }
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp) // Fixed peek height
                    .padding(top = 8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                StopProgressBar(
                    stops = routeStops,
                    busLatLng = busLatLng,
                    studentStopId = assignedStopId,
                    passedStopIds = passedStopIds
                )
            }
            
            // Unavailable Banner Below Map (Above Sheet)
            if (locationFreshness is LocationFreshness.Unavailable) {
                val copy = if (!isBusActive) "Bus trip has ended" else "Bus location unavailable — last seen ${locationFreshness.minutesAgo} min ago"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp) // Offset by sheet peek height
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = copy,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusTrackingMap(
    routeStops: List<Stop>,
    busLocation: LiveLocation?,
    nextStopName: String,
    isBusActive: Boolean = true,
    locationFreshness: LocationFreshness = LocationFreshness.Fresh,
    cameraPositionState: CameraPositionState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState
    ) {
        if (routeStops.isNotEmpty()) {
            val orderedStops = routeStops.sortedBy { it.order }
            
            // State to hold the snapped polyline path
            var snappedPath by remember { mutableStateOf<List<LatLng>>(emptyList()) }
            
            // Fetch directions async when stops change
            LaunchedEffect(orderedStops) {
                if (orderedStops.size >= 2) {
                    val latLngs = orderedStops.map { LatLng(it.latitude, it.longitude) }
                    snappedPath = DirectionsFetcher.getDirections(latLngs)
                }
            }

            // Draw single full snapped Route (Primary Color)
            if (snappedPath.isNotEmpty()) {
                Polyline(
                    points = snappedPath,
                    color = MaterialTheme.colorScheme.primary,
                    width = 12f // Slightly thicker for a premium feel
                )
            } else {
                // Fallback to straight lines if API fails or is loading
                val fallbackPath = orderedStops.map { LatLng(it.latitude, it.longitude) }
                if (fallbackPath.size > 1) {
                    Polyline(
                        points = fallbackPath,
                        color = Color.Gray.copy(alpha = 0.5f),
                        width = 8f
                    )
                }
            }
        }

        // Draw Stop Pins
        val orderedForMarkers = routeStops.sortedBy { it.order }
        val lastStopOrder = orderedForMarkers.lastOrNull()?.order ?: -1
        
        routeStops.forEach { stop ->
            val isLast = stop.order == lastStopOrder
            val isCollege = stop.stopName.contains("SDMCET", ignoreCase = true) ||
                            stop.stopName.contains("College", ignoreCase = true)
            
            val markerIcon = when {
                isCollege -> remember(context) {
                    LocationUtils.bitmapDescriptorFromVector(context, R.drawable.ic_college_marker)
                }
                isLast -> null // Will use default green marker below
                else -> null // Will use default red marker below
            }
            
            Marker(
                state = MarkerState(
                    position = LatLng(stop.latitude, stop.longitude)
                ),
                title = when {
                    isCollege -> "🏫 ${stop.stopName}"
                    isLast -> "Final Destination: ${stop.stopName}"
                    else -> stop.stopName
                },
                snippet = when {
                    isCollege -> "College Campus"
                    isLast -> "Drop off point"
                    else -> "Stop #${stop.order}"
                },
                icon = markerIcon ?: BitmapDescriptorFactory.defaultMarker(
                    if (isLast) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
                )
            )
        }

        // Draw custom Bus Vector
        if (busLocation != null) {
            val customBusIcon = remember(context) { 
                LocationUtils.bitmapDescriptorFromVector(context, R.drawable.ic_bus_marker) 
            }
            
            val busAlpha = if (locationFreshness is LocationFreshness.Unavailable || !isBusActive) 0.5f else 1.0f
            val busMarkerState = rememberMarkerState(
                position = LatLng(busLocation.latitude, busLocation.longitude)
            )
            
            // Update position reactively when busLocation changes
            LaunchedEffect(busLocation.latitude, busLocation.longitude) {
                busMarkerState.position = LatLng(busLocation.latitude, busLocation.longitude)
            }
            
            Marker(
                state = busMarkerState,
                title = "Bus Location",
                snippet = "Speed: ${"%.1f".format(busLocation.speed * 3.6f)} km/h",
                icon = customBusIcon ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                alpha = busAlpha
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

@Composable
fun StopProgressBar(
    stops: List<Stop>,
    busLatLng: LatLng?,
    studentStopId: String?,
    passedStopIds: Set<String>,
    modifier: Modifier = Modifier
) {
    if (stops.isEmpty()) return
    
    val sortedStops = remember(stops) { stops.sortedBy { it.order } }
    
    // Calculate progress fraction
    val busProgress = remember(sortedStops, busLatLng, passedStopIds) {
        if (busLatLng == null) return@remember 0f
        
        val numStops = sortedStops.size
        if (numStops <= 1) return@remember 1f
        
        val passedCount = sortedStops.count { it.stopId in passedStopIds }
        if (passedCount == 0) return@remember 0f
        if (passedCount >= numStops) return@remember 1f
        
        val lastPassedStop = sortedStops[passedCount - 1]
        val nextStop = sortedStops[passedCount]
        
        val distA = LocationUtils.haversineKm(
            lastPassedStop.latitude, lastPassedStop.longitude,
            busLatLng.latitude, busLatLng.longitude
        )
        val distB = LocationUtils.haversineKm(
            busLatLng.latitude, busLatLng.longitude,
            nextStop.latitude, nextStop.longitude
        )
        
        val segmentDist = distA + distB
        val segmentFraction = if (segmentDist > 0) (distA / segmentDist).coerceIn(0.0, 1.0) else 0.0
        
        val segmentStartFrac = (passedCount - 1).toFloat() / (numStops - 1)
        val segmentEndFrac = passedCount.toFloat() / (numStops - 1)
        
        (segmentStartFrac + (segmentFraction * (segmentEndFrac - segmentStartFrac))).toFloat()
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = busProgress,
        animationSpec = tween(durationMillis = 800),
        label = "busProgress"
    )
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = maxWidth
        val numStops = sortedStops.size
        val stopPoints = if (numStops > 1) {
            List(numStops) { index -> index.toFloat() / (numStops - 1) }
        } else listOf(0f)
        
        // Background track (surfaceVariant)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))
        )
        
        // Active filled line overlay (primary)
        Box(
            modifier = Modifier
                .width(totalWidth * animatedProgress)
                .height(4.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        
        // Render each stop circle
        sortedStops.forEachIndexed { index, stop ->
            val positionFrac = stopPoints[index]
            val xOffset = totalWidth * positionFrac
            
            val isPassed = positionFrac <= animatedProgress || stop.stopId in passedStopIds
            val isStudentStop = stop.stopId == studentStopId
            
            val circleColor = when {
                isStudentStop -> MaterialTheme.colorScheme.tertiary
                isPassed -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            
            Box(
                modifier = Modifier
                    .offset(x = xOffset - 6.dp) // center the 12dp circle
                    .size(12.dp)
                    .background(circleColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isStudentStop) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "My Stop",
                        modifier = Modifier.size(8.dp),
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                }
            }
            
            // Render text label ONLY for the student stop
            if (isStudentStop) {
                Text(
                    text = stop.stopName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .offset(x = xOffset - 30.dp, y = 14.dp) // drop text below circle
                        .width(60.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        
        // Active Bus indicator animating across
        Surface(
            modifier = Modifier
                .offset(x = (totalWidth * animatedProgress) - 9.dp) // center 18dp icon (9dp offset)
                .size(18.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = "Bus Tracker",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
