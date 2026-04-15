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
    viewModel: StudentViewModel = hiltViewModel()
) {
    val selectedRoute by viewModel.selectedRoute.collectAsState()

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
        topBar = { NextStopTopBar(title = if (selectedRoute != null) "${selectedRoute!!.name}" else "Fleet Tracker", onLogout = onLogout) }
    ) { innerPadding ->
        if (selectedRoute == null) {
            StudentFleetDashboard(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
        } else {
            StudentTrackerScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun StudentFleetDashboard(viewModel: StudentViewModel, modifier: Modifier = Modifier) {
    val routes by viewModel.routes.collectAsState()
    val buses by viewModel.buses.collectAsState()
    val liveLocations by viewModel.liveLocations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    if (isLoading || routes.isEmpty()) {
        if (isLoading) {
            ShimmerListSkeleton(modifier = modifier)
        } else {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No routes available.", color = MaterialTheme.colorScheme.outline)
            }
        }
        return
    }

    // Split routes into From College / To College
    val fromCollege = routes.filter { it.direction == "from_college" }.sortedBy { it.departureTime }
    val toCollege = routes.filter { it.direction == "to_college" }.sortedBy { it.departureTime }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (fromCollege.isNotEmpty()) {
            item {
                Text("From College", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            items(fromCollege, key = { it.routeId }) { route ->
                val liveLocation = liveLocations[route.routeId]
                RouteTrackingCard(
                    route = route,
                    bus = buses.find { it.busId == route.busId },
                    liveLocation = liveLocation,
                    onClick = { viewModel.selectRoute(route) }
                )
            }
        }
        
        if (toCollege.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("To College", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            items(toCollege, key = { it.routeId }) { route ->
                val liveLocation = liveLocations[route.routeId]
                RouteTrackingCard(
                    route = route,
                    bus = buses.find { it.busId == route.busId },
                    liveLocation = liveLocation,
                    onClick = { viewModel.selectRoute(route) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteTrackingCard(
    route: Route,
    bus: com.yourteam.nextstop.models.Bus?,
    liveLocation: com.yourteam.nextstop.models.LiveLocation?,
    onClick: () -> Unit
) {
    val (statusText, statusColor) = when {
        liveLocation?.active == true -> "Live" to MaterialTheme.colorScheme.tertiary
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
                        text = "Bus ${bus?.busNumber ?: "?"} @ ${route.departureTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isBusActive by viewModel.isBusActive.collectAsState()
    val assignedStopId by viewModel.assignedStopId.collectAsState()
    val boardingEtaMinutes by viewModel.boardingEtaMinutes.collectAsState()
    val passedStopIds by viewModel.passedStopIds.collectAsState()
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
        connectionStatus = connectionStatus,
        isBusActive = isBusActive,
        assignedStopId = assignedStopId,
        boardingEtaMinutes = boardingEtaMinutes,
        passedStopIds = passedStopIds,
        onStopSelected = { selectedId, selectedName -> 
            viewModel.updateAssignedStop(selectedId)
            Toast.makeText(context, "Alert set for $selectedName.", Toast.LENGTH_SHORT).show()
        },
        modifier = modifier
    )
}
