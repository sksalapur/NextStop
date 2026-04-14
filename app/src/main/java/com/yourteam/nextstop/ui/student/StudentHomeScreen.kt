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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LocationOn
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.yourteam.nextstop.ui.components.NextStopTopBar
import com.yourteam.nextstop.ui.components.SharedTrackerScreen
import com.yourteam.nextstop.ui.components.ShimmerListSkeleton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    onLogout: () -> Unit,
    viewModel: StudentViewModel = hiltViewModel()
) {
    val selectedBus by viewModel.selectedTrackingBus.collectAsState()

    BackHandler(enabled = selectedBus != null) {
        viewModel.selectBus(null)
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
        topBar = { NextStopTopBar(title = if (selectedBus != null) "Bus ${selectedBus!!.busNumber}" else "Fleet Tracker", onLogout = onLogout) }
    ) { innerPadding ->
        if (selectedBus == null) {
            StudentFleetDashboard(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
        } else {
            StudentTrackerScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun StudentFleetDashboard(viewModel: StudentViewModel, modifier: Modifier = Modifier) {
    val buses by viewModel.buses.collectAsState()
    val routes by viewModel.routes.collectAsState()
    val liveLocations by viewModel.liveLocations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    if (isLoading || (buses.isEmpty() && routes.isEmpty())) {
        ShimmerListSkeleton(modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(buses, key = { it.busId }) { bus ->
            val routeName = routes.find { it.routeId == bus.routeId }?.name ?: "Unknown Route"
            val liveLocation = liveLocations[bus.busId]
            
            BusTrackingCard(
                bus = bus,
                routeName = routeName,
                liveLocation = liveLocation,
                onClick = { viewModel.selectBus(bus) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusTrackingCard(
    bus: com.yourteam.nextstop.models.Bus,
    routeName: String,
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
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bus ${bus.busNumber}",
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
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = routeName,
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
    val context = LocalContext.current

    val busNumber = viewModel.selectedTrackingBus.collectAsState().value?.busNumber ?: ""

    SharedTrackerScreen(
        busNumber = busNumber,
        routeStops = routeStops,
        busLocation = busLocation,
        etaMinutes = etaMinutes,
        nextStopName = nextStopName,
        connectionStatus = connectionStatus,
        isBusActive = isBusActive,
        assignedStopId = assignedStopId,
        boardingEtaMinutes = boardingEtaMinutes,
        onStopSelected = { selectedId, selectedName -> 
            viewModel.updateAssignedStop(selectedId)
            Toast.makeText(context, "Alert set for $selectedName.", Toast.LENGTH_SHORT).show()
        },
        modifier = modifier
    )
}
