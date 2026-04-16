package com.yourteam.nextstop.ui.student

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourteam.nextstop.models.Route
import com.yourteam.nextstop.ui.components.NextStopTopBar
import com.yourteam.nextstop.ui.components.SharedTrackerScreen
import com.yourteam.nextstop.ui.components.ShimmerListSkeleton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    onLogout: () -> Unit,
    onNavigateToSetup: () -> Unit = {},
    viewModel: StudentViewModel = hiltViewModel()
) {
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val buses by viewModel.buses.collectAsState()

    BackHandler(enabled = selectedRoute != null) {
        viewModel.selectRoute(null)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled naturally */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = { 
            if (selectedRoute != null) {
                // Tracking Title
                val activeBus = buses.find { it.busId == selectedRoute!!.busId }
                NextStopTopBar(
                    title = "Live Tracking",
                    subtitle = "Bus ${activeBus?.busNumber ?: "?"} • ${selectedRoute!!.name}",
                    onLogout = null
                )
            } else {
                // Home Title
                NextStopTopBar(title = "My Bus", onLogout = onLogout) 
            }
        }
    ) { innerPadding ->
        if (selectedRoute == null) {
            StudentFleetDashboard(
                viewModel = viewModel, 
                onNavigateToSetup = onNavigateToSetup,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            StudentTrackerScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFleetDashboard(
    viewModel: StudentViewModel,
    onNavigateToSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val direction by viewModel.direction.collectAsState()
    val homeStop by viewModel.homeStop.collectAsState()
    val needsSetup by viewModel.needsHomeStopSetup.collectAsState()
    val filteredBuses by viewModel.filteredBuses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val availableStops by viewModel.availableStops.collectAsState()

    var stopDropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            ShimmerListSkeleton(modifier = Modifier.fillMaxSize())
            return@Column
        }

        // Inline boarding stop selector — works for both first-time and edit
        if (needsSetup == true || homeStop == null) {
            // First time: prominent card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Where do you board the bus?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select your stop to see relevant buses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ExposedDropdownMenuBox(
                        expanded = stopDropdownExpanded,
                        onExpandedChange = { stopDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Select your boarding stop") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stopDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = stopDropdownExpanded,
                            onDismissRequest = { stopDropdownExpanded = false }
                        ) {
                            availableStops.forEach { stop ->
                                DropdownMenuItem(
                                    text = { Text(stop.stopName) },
                                    onClick = {
                                        viewModel.changeHomeStop(stop)
                                        stopDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Compact inline selector for editing
            ExposedDropdownMenuBox(
                expanded = stopDropdownExpanded,
                onExpandedChange = { stopDropdownExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .menuAnchor()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Boarding at ${homeStop!!.stopName}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ExpandMore, contentDescription = "Change Stop", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                ExposedDropdownMenu(
                    expanded = stopDropdownExpanded,
                    onDismissRequest = { stopDropdownExpanded = false }
                ) {
                    availableStops.forEach { stop ->
                        DropdownMenuItem(
                            text = { Text(stop.stopName) },
                            onClick = {
                                viewModel.changeHomeStop(stop)
                                stopDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Direction Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { viewModel.setDirection(Direction.TO_COLLEGE) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (direction == Direction.TO_COLLEGE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (direction == Direction.TO_COLLEGE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("🏫 To College", fontWeight = if (direction == Direction.TO_COLLEGE) FontWeight.Bold else FontWeight.Normal)
            }
            FilledTonalButton(
                onClick = { viewModel.setDirection(Direction.TO_HOME) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (direction == Direction.TO_HOME) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (direction == Direction.TO_HOME) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("🏠 To Home", fontWeight = if (direction == Direction.TO_HOME) FontWeight.Bold else FontWeight.Normal)
            }
            FilledTonalButton(
                onClick = { viewModel.setDirection(Direction.ALL_SCHEDULED) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (direction == Direction.ALL_SCHEDULED) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (direction == Direction.ALL_SCHEDULED) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("🚌 All Scheduled", fontWeight = if (direction == Direction.ALL_SCHEDULED) FontWeight.Bold else FontWeight.Normal)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredBuses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No buses on this route right now", color = MaterialTheme.colorScheme.outline)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredBuses, key = { it.route.routeId }) { busData ->
                val passesStop = busData.route.stops.any { it.stopName == homeStop?.stopName }
                val isOffRoute = direction == Direction.ALL_SCHEDULED && !passesStop
                val etaText = if (isOffRoute) {
                    "Does not pass your stop"
                } else if (!busData.isLive) {
                    "Scheduled – Waiting for departure"
                } else {
                    val rawEta = com.yourteam.nextstop.ui.utils.formatEta(busData.etaMinutes)
                    when (rawEta) {
                        "Calculating..." -> "Calculating..."
                        "Bus is far away" -> "Bus is far away"
                        else -> "Arrives at ${homeStop!!.stopName} in $rawEta"
                    }
                }
                val etaColor = if (isOffRoute || etaText == "Bus is far away") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                
                RouteTrackingCard(
                    route = busData.route,
                    bus = busData.bus,
                    isLive = busData.isLive,
                    etaText = etaText,
                    etaColor = etaColor,
                    onClick = { viewModel.selectRoute(busData.route) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteTrackingCard(
    route: Route,
    bus: com.yourteam.nextstop.models.Bus,
    isLive: Boolean,
    etaText: String,
    etaColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    val (statusText, statusColor) = when {
        isLive -> "Live" to MaterialTheme.colorScheme.tertiary
        else -> "Inactive" to MaterialTheme.colorScheme.outline
    }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(statusColor)
            )

            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = route.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.12f),
                        contentColor = statusColor
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Bus ${bus.busNumber} @ ${route.departureTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = etaText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = etaColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTrackerScreen(viewModel: StudentViewModel, modifier: Modifier = Modifier) {
    val routeStops by viewModel.routeStops.collectAsState()
    val busLocation by viewModel.busLocation.collectAsState()
    val etaMinutes by viewModel.etaMinutes.collectAsState()
    val nextStopName by viewModel.nextStopName.collectAsState()
    val isBusActive by viewModel.isBusActive.collectAsState()
    val assignedStopId by viewModel.assignedStopId.collectAsState()
    val boardingEtaMinutes by viewModel.boardingEtaMinutes.collectAsState()
    val passedStopIds by viewModel.passedStopIds.collectAsState()
    val locationFreshness by viewModel.locationFreshness.collectAsState()
    val alertEnabled by viewModel.alertEnabled.collectAsState()
    val context = LocalContext.current
    
    val buses by viewModel.buses.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val activeBus = buses.find { it.busId == selectedRoute?.busId }

    SharedTrackerScreen(
        busNumber = activeBus?.busNumber ?: "?",
        routeStops = routeStops,
        busLocation = busLocation,
        etaMinutes = etaMinutes,
        nextStopName = nextStopName,
        isBusActive = isBusActive,
        assignedStopId = assignedStopId,
        boardingEtaMinutes = boardingEtaMinutes,
        passedStopIds = passedStopIds,
        locationFreshness = locationFreshness,
        alertEnabled = alertEnabled,
        onToggleAlert = { 
            viewModel.toggleAlertEnabled()
            val newStatus = if (!alertEnabled) "on" else "off"
            Toast.makeText(context, "Stop alerts turned $newStatus.", Toast.LENGTH_SHORT).show()
        },
        modifier = modifier
    )
}
