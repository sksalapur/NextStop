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
import com.yourteam.nextstop.models.Route
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

    val selectedRoute by studentViewModel.selectedRoute.collectAsState()
    val selectedBus = buses.find { it.busId == selectedRoute?.busId }

    androidx.activity.compose.BackHandler(enabled = selectedRoute != null) {
        studentViewModel.selectRoute(null)
    }

    if (selectedRoute != null && selectedBus != null) {
        val routeStops by studentViewModel.routeStops.collectAsState()
        val busLocation by studentViewModel.busLocation.collectAsState()
        val etaMinutes by studentViewModel.etaMinutes.collectAsState()
        val nextStopName by studentViewModel.nextStopName.collectAsState()
        val isBusActive by studentViewModel.isBusActive.collectAsState()

        com.yourteam.nextstop.ui.components.SharedTrackerScreen(
            busNumber = selectedBus.busNumber,
            routeStops = routeStops,
            busLocation = busLocation,
            etaMinutes = etaMinutes,
            nextStopName = nextStopName,
            isBusActive = isBusActive,
            assignedStopId = null,
            boardingEtaMinutes = null,
            passedStopIds = emptySet(),
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (routes.isEmpty()) {
        EmptyStateMessage("No routes defined.")
        return
    }

    val fromCollege = routes.filter { it.direction == "from_college" }.sortedBy { it.departureTime }
    val toCollege = routes.filter { it.direction == "to_college" }.sortedBy { it.departureTime }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (fromCollege.isNotEmpty()) {
            item {
                Text(
                    text = "From College",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(fromCollege, key = { it.routeId }) { route ->
                val bus = buses.find { it.busId == route.busId }
                val driverName = drivers.find { it.uid == route.assignedDriverId }?.name ?: "Unassigned"
                val liveLocation = liveLocations[route.routeId]

                RouteStatusCard(
                    route = route,
                    bus = bus,
                    driverName = driverName,
                    liveLocation = liveLocation,
                    onClick = { studentViewModel.selectRoute(route) }
                )
            }
        }

        if (toCollege.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To College",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(toCollege, key = { it.routeId }) { route ->
                val bus = buses.find { it.busId == route.busId }
                val driverName = drivers.find { it.uid == route.assignedDriverId }?.name ?: "Unassigned"
                val liveLocation = liveLocations[route.routeId]

                RouteStatusCard(
                    route = route,
                    bus = bus,
                    driverName = driverName,
                    liveLocation = liveLocation,
                    onClick = { studentViewModel.selectRoute(route) }
                )
            }
        }
    }
}

@Composable
fun RouteStatusCard(
    route: Route,
    bus: Bus?,
    driverName: String,
    liveLocation: LiveLocation?,
    onClick: () -> Unit
) {
    val (statusText, statusColor) = when {
        route.assignedDriverId.isNullOrEmpty() -> "No Driver" to MaterialTheme.colorScheme.error
        liveLocation?.active == true -> "Live" to MaterialTheme.colorScheme.tertiary
        else -> "Scheduled" to MaterialTheme.colorScheme.outline
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
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(100.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(statusColor)
            )

            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                // Header Row
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

                Spacer(modifier = Modifier.height(12.dp))

                // Body Row
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Bus @ ${route.departureTime}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = bus?.busNumber ?: "Unknown",
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
