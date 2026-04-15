package com.yourteam.nextstop.ui.admin

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourteam.nextstop.models.Bus
import com.yourteam.nextstop.models.Route
import com.yourteam.nextstop.models.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAssignmentsTab(viewModel: AdminViewModel) {
    val routes by viewModel.routes.collectAsState()
    val buses by viewModel.buses.collectAsState()
    val drivers by viewModel.drivers.collectAsState()
    val liveLocations by viewModel.liveLocations.collectAsState()
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedRouteToAssign by remember { mutableStateOf<Route?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (routes.isEmpty()) {
            EmptyStateMessage("No routes found. Add a route in the Manage tab.")
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val fromCollege = routes.filter { it.direction == "from_college" }.sortedBy { it.departureTime }
            val toCollege = routes.filter { it.direction == "to_college" }.sortedBy { it.departureTime }

            if (fromCollege.isNotEmpty()) {
                item {
                    Text("From College", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                items(fromCollege, key = { it.routeId }) { route ->
                    val currentDriver = drivers.find { it.uid == route.assignedDriverId }
                    val busAssigned = buses.find { it.busId == route.busId }
                    val isActive = liveLocations[route.routeId]?.active == true
                    
                    AssignmentCard(
                        route = route,
                        bus = busAssigned,
                        currentDriver = currentDriver,
                        isActive = isActive,
                        onAssignClick = {
                            selectedRouteToAssign = route
                            showBottomSheet = true
                        }
                    )
                }
            }
            
            if (toCollege.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("To College", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                items(toCollege, key = { it.routeId }) { route ->
                    val currentDriver = drivers.find { it.uid == route.assignedDriverId }
                    val busAssigned = buses.find { it.busId == route.busId }
                    val isActive = liveLocations[route.routeId]?.active == true
                    
                    AssignmentCard(
                        route = route,
                        bus = busAssigned,
                        currentDriver = currentDriver,
                        isActive = isActive,
                        onAssignClick = {
                            selectedRouteToAssign = route
                            showBottomSheet = true
                        }
                    )
                }
            }
        }
    }

    if (showBottomSheet) {
        val routeTitle = selectedRouteToAssign?.name ?: ""
        val currentRouteDriverId = selectedRouteToAssign?.assignedDriverId
        
        // Find drivers available to take this specific route (they aren't busy at the exact same specific departure time)
        val targetDepartureTime = selectedRouteToAssign?.departureTime
        val availableDrivers = drivers.filter { driver ->
            // A driver is available if they are the current driver for this very route,
            // or if they do not have any other route assigned to them at the SAME departureTime.
            driver.uid == currentRouteDriverId || routes.none { it.assignedDriverId == driver.uid && it.departureTime == targetDepartureTime }
        }

        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Assign Driver to Route: $routeTitle",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (availableDrivers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No free drivers available for this time slot.", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn {
                        item {
                            if (currentRouteDriverId != null) {
                                ListItem(
                                    headlineContent = { Text("Unassign Current Driver", color = MaterialTheme.colorScheme.error) },
                                    leadingContent = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    modifier = Modifier.clickable {
                                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                                            selectedRouteToAssign?.let { route ->
                                                viewModel.unassignDriver(route.routeId) { _, msg ->
                                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                                }
                                            }
                                            showBottomSheet = false
                                        }
                                    }
                                )
                            }
                        }
                        items(availableDrivers) { driver ->
                            ListItem(
                                headlineContent = { Text(driver.name) },
                                supportingContent = {
                                    if (driver.uid == currentRouteDriverId) {
                                        Text("Currently assigned", color = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                leadingContent = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                },
                                modifier = Modifier.clickable {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            showBottomSheet = false
                                            
                                            // Perform assignment
                                            selectedRouteToAssign?.let { route ->
                                                viewModel.assignDriver(
                                                    routeId = route.routeId,
                                                    newDriverUid = driver.uid
                                                ) { success, message ->
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(message)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssignmentCard(
    route: Route,
    bus: Bus?,
    currentDriver: User?,
    isActive: Boolean,
    onAssignClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

                if (isActive) {
                    Text(
                        text = "Trip in progress",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Time: ${route.departureTime} | Bus ${bus?.busNumber ?: "?"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Current Driver",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentDriver?.name ?: "— Unassigned —",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp),
                        color = if (currentDriver == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = onAssignClick,
                    enabled = !isActive // Disable reassignment if the physical bus for this route is currently live
                ) {
                    Icon(Icons.Default.AssignmentInd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentDriver == null) "Assign" else "Reassign")
                }
            }
        }
    }
}
