package com.yourteam.nextstop.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourteam.nextstop.models.Route
import com.yourteam.nextstop.models.Stop
import com.yourteam.nextstop.ui.components.CustomPlacesSearchField
import com.yourteam.nextstop.ui.components.PlaceResult
import kotlinx.coroutines.launch

@Composable
fun AdminManageTab(viewModel: AdminViewModel) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Routes", "Buses", "Drivers")

    val routes by viewModel.routes.collectAsState()
    val buses by viewModel.buses.collectAsState()
    val drivers by viewModel.drivers.collectAsState()

    var showAddRouteDialog by remember { mutableStateOf(false) }
    var showAddBusDialog by remember { mutableStateOf(false) }
    var showInviteDriverDialog by remember { mutableStateOf(false) }
    
    var editRoute by remember { mutableStateOf<Route?>(null) }
    var editBus by remember { mutableStateOf<com.yourteam.nextstop.models.Bus?>(null) }
    var editDriver by remember { mutableStateOf<com.yourteam.nextstop.models.User?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                onClick = { 
                    when (selectedTabIndex) {
                        0 -> showAddRouteDialog = true
                        1 -> showAddBusDialog = true
                        2 -> showInviteDriverDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> RoutesList(
                    routes = routes, 
                    onEditClick = { editRoute = it },
                    onDeleteClick = { 
                        viewModel.deleteRoute(it.routeId) { _, msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                    }
                )
                1 -> BusesList(
                    buses = buses, 
                    routes = routes, 
                    onEditClick = { editBus = it },
                    onDeleteClick = { 
                        viewModel.deleteBus(it.busId) { _, msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                    }
                )
                2 -> DriversList(
                    drivers = drivers,
                    onEditClick = { editDriver = it },
                    onDeleteClick = { 
                        viewModel.deleteDriver(it.uid) { _, msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                    }
                )
            }
        }
    }

    if (showAddRouteDialog) {
        AddRouteDialog(
            onDismiss = { showAddRouteDialog = false },
            onSave = { newRoute ->
                showAddRouteDialog = false
                viewModel.addRoute(newRoute) { success, message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            }
        )
    }

    if (showAddBusDialog) {
        AddBusDialog(
            routes = routes,
            onDismiss = { showAddBusDialog = false },
            onSave = { busNumber, routeId ->
                showAddBusDialog = false
                viewModel.addBus(busNumber, routeId) { success, message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            }
        )
    }

    if (showInviteDriverDialog) {
        InviteDriverDialog(
            onDismiss = { showInviteDriverDialog = false },
            onSave = { email ->
                showInviteDriverDialog = false
                viewModel.inviteDriver(email) { success, message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            }
        )
    }

    if (editBus != null) {
        AddBusDialog(
            existingBus = editBus,
            routes = routes,
            onDismiss = { editBus = null },
            onSave = { busNumber, routeId ->
                val busIdToUpdate = editBus!!.busId
                editBus = null
                viewModel.updateBus(busIdToUpdate, busNumber, routeId) { _, msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            }
        )
    }

    if (editRoute != null) {
        AddRouteDialog(
            existingRoute = editRoute,
            onDismiss = { editRoute = null },
            onSave = { updatedRoute ->
                editRoute = null
                viewModel.updateRoute(updatedRoute) { _, msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            }
        )
    }
    
    if (editDriver != null) {
        EditDriverDialog(
            driver = editDriver!!,
            onDismiss = { editDriver = null },
            onSave = { uid, newName ->
                editDriver = null
                viewModel.updateDriverName(uid, newName) { _, msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            }
        )
    }
}

// ─── Lists ───────────────────────────────────────────────────────────

@Composable
fun DriversList(drivers: List<com.yourteam.nextstop.models.User>, onEditClick: (com.yourteam.nextstop.models.User) -> Unit, onDeleteClick: (com.yourteam.nextstop.models.User) -> Unit) {
    if (drivers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No drivers found.", color = MaterialTheme.colorScheme.outline)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(drivers, key = { it.uid }) { driver ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = driver.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = driver.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onEditClick(driver) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = { onDeleteClick(driver) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun RoutesList(routes: List<Route>, onEditClick: (Route) -> Unit, onDeleteClick: (Route) -> Unit) {
    if (routes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No routes defined.", color = MaterialTheme.colorScheme.outline)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(routes, key = { it.routeId }) { route ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = route.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "${route.stops.size} stops", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onEditClick(route) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Route", tint = MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = { onDeleteClick(route) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Route", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun BusesList(buses: List<com.yourteam.nextstop.models.Bus>, routes: List<Route>, onEditClick: (com.yourteam.nextstop.models.Bus) -> Unit, onDeleteClick: (com.yourteam.nextstop.models.Bus) -> Unit) {
    if (buses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No buses in the fleet.", color = MaterialTheme.colorScheme.outline)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(buses, key = { it.busId }) { bus ->
            val routeName = routes.find { it.routeId == bus.routeId }?.name ?: "Unknown Route"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Bus ${bus.busNumber}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Route: $routeName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onEditClick(bus) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Bus", tint = MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = { onDeleteClick(bus) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Bus", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// ─── Dialogs ─────────────────────────────────────────────────────────

@Composable
fun EditDriverDialog(
    driver: com.yourteam.nextstop.models.User,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(driver.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Driver Name") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(driver.uid, name)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBusDialog(
    existingBus: com.yourteam.nextstop.models.Bus? = null,
    routes: List<Route>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var busNumber by remember { mutableStateOf(existingBus?.busNumber ?: "") }
    var selectedRoute by remember { mutableStateOf<Route?>(routes.find { it.routeId == existingBus?.routeId } ?: routes.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingBus != null) "Edit Bus" else "Add New Bus") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = busNumber,
                    onValueChange = { busNumber = it },
                    label = { Text("Bus Number (Plate / ID)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (routes.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedRoute?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assign to Route") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            routes.forEach { route ->
                                DropdownMenuItem(
                                    text = { Text(route.name) },
                                    onClick = {
                                        selectedRoute = route
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text("You must create a route first.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (busNumber.isNotBlank() && selectedRoute != null) {
                        onSave(busNumber, selectedRoute!!.routeId)
                    }
                },
                enabled = busNumber.isNotBlank() && selectedRoute != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun InviteDriverDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Driver") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "The driver will automatically receive the 'driver' role upon logging in via Google for the first time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Driver Email (Gmail)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (email.isNotBlank()) onSave(email) },
                enabled = email.isNotBlank()
            ) {
                Text("Invite")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddRouteDialog(
    existingRoute: Route? = null,
    onDismiss: () -> Unit,
    onSave: (Route) -> Unit
) {
    var routeName by remember { mutableStateOf(existingRoute?.name ?: "") }
    var startName by remember { mutableStateOf(existingRoute?.startName ?: "") }
    var endName by remember { mutableStateOf(existingRoute?.endName ?: "") }
    
    // Manage stops list state
    val stopsList = remember { mutableStateListOf<Stop>().apply { 
        if (existingRoute != null) addAll(existingRoute.stops.sortedBy { it.order })
    }}

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingRoute != null) "Edit Route" else "Add New Route") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = routeName,
                    onValueChange = { routeName = it },
                    label = { Text("Route Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startName,
                        onValueChange = { startName = it },
                        label = { Text("Start Area") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endName,
                        onValueChange = { endName = it },
                        label = { Text("End Area") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Stops (${stopsList.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier.height(200.dp).fillMaxWidth()
                ) {
                    itemsIndexed(stopsList) { index, stop ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            OutlinedTextField(
                                value = stop.stopName,
                                onValueChange = { newName ->
                                    stopsList[index] = stop.copy(stopName = newName)
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            
                            IconButton(
                                onClick = { 
                                    if (index > 0) {
                                        val temp = stopsList[index]
                                        stopsList[index] = stopsList[index - 1]
                                        stopsList[index - 1] = temp
                                    }
                                },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(20.dp))
                            }
                            
                            IconButton(
                                onClick = { 
                                    if (index < stopsList.size - 1) {
                                        val temp = stopsList[index]
                                        stopsList[index] = stopsList[index + 1]
                                        stopsList[index + 1] = temp
                                    }
                                },
                                enabled = index < stopsList.size - 1
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(20.dp))
                            }
                            
                            IconButton(onClick = { stopsList.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // Google Places Autocomplete to add a stop using debounce
                CustomPlacesSearchField(
                    value = "",
                    onPlaceSelected = { placeResult ->
                        val newStop = Stop(
                            stopId = "", // Firestore will ignore/generate if needed
                            stopName = placeResult.name,
                            latitude = placeResult.latLng.latitude,
                            longitude = placeResult.latLng.longitude,
                            order = 0 // dynamically assigned on save
                        )
                        stopsList.add(newStop)
                    },
                    label = "Search & Add Stop..."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (routeName.isNotBlank() && startName.isNotBlank() && endName.isNotBlank() && stopsList.isNotEmpty()) {
                        val routeId = existingRoute?.routeId ?: ""
                        // update the actual order indices sequentially
                        val finalStops = stopsList.mapIndexed { index, stop -> stop.copy(order = index + 1) }
                        val route = Route(routeId = routeId, name = routeName, startName = startName, endName = endName, stops = finalStops)
                        onSave(route)
                    }
                },
                enabled = routeName.isNotBlank() && startName.isNotBlank() && endName.isNotBlank() && stopsList.isNotEmpty()
            ) {
                Text("Save Route")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
