package com.yourteam.nextstop.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourteam.nextstop.models.Bus
import com.yourteam.nextstop.models.LiveLocation
import com.yourteam.nextstop.ui.components.ShimmerListSkeleton

@Composable
fun AdminDashboardTab(
    viewModel: AdminViewModel,
    studentViewModel: com.yourteam.nextstop.ui.student.StudentViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val buses by viewModel.buses.collectAsState()
    val routes by viewModel.routes.collectAsState()
    val drivers by viewModel.drivers.collectAsState()
    val liveLocations by viewModel.liveLocations.collectAsState()

    // Show shimmer while initial data hasn't arrived yet
    if (buses.isEmpty() && routes.isEmpty()) {
        ShimmerListSkeleton()
        return
    }

    val selectedBus by studentViewModel.selectedTrackingBus.collectAsState()

    androidx.activity.compose.BackHandler(enabled = selectedBus != null) {
        studentViewModel.selectBus(null)
    }

    if (selectedBus != null) {
        val routeStops by studentViewModel.routeStops.collectAsState()
        val busLocation by studentViewModel.busLocation.collectAsState()
        val etaMinutes by studentViewModel.etaMinutes.collectAsState()
        val nextStopName by studentViewModel.nextStopName.collectAsState()
        val connectionStatus by studentViewModel.connectionStatus.collectAsState()
        val isBusActive by studentViewModel.isBusActive.collectAsState()

        com.yourteam.nextstop.ui.components.SharedTrackerScreen(
            busNumber = selectedBus!!.busNumber,
            routeStops = routeStops,
            busLocation = busLocation,
            etaMinutes = etaMinutes,
            nextStopName = nextStopName,
            connectionStatus = connectionStatus,
            isBusActive = isBusActive,
            assignedStopId = null,
            boardingEtaMinutes = null,
            onStopSelected = null,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (buses.isEmpty()) {
        EmptyStateMessage("No buses found in the fleet.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(buses, key = { it.busId }) { bus ->
            val routeName = routes.find { it.routeId == bus.routeId }?.name ?: "Unknown Route"
            val driverName = drivers.find { it.uid == bus.assignedDriverId }?.name ?: "Unassigned"
            val liveLocation = liveLocations[bus.busId]

            BusStatusCard(
                bus = bus,
                routeName = routeName,
                driverName = driverName,
                liveLocation = liveLocation,
                onClick = { studentViewModel.selectBus(bus) }
            )
        }
    }
}

@Composable
fun BusStatusCard(
    bus: Bus,
    routeName: String,
    driverName: String,
    liveLocation: LiveLocation?,
    onClick: () -> Unit
) {
    val (statusText, statusColor) = when {
        bus.assignedDriverId.isNullOrEmpty() -> "No Driver" to MaterialTheme.colorScheme.error
        liveLocation?.active == true -> "Live" to MaterialTheme.colorScheme.tertiary
        else -> "Inactive" to MaterialTheme.colorScheme.outline
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Colored left border strip
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(100.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(statusColor)
            )

            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                // Header Row: Bus Number + Status
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

                    // Status chip
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

                Spacer(modifier = Modifier.height(12.dp))

                // Body Row: Route + Driver
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Route",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = routeName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 18.dp, top = 2.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Driver",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = driverName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 18.dp, top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
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
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
