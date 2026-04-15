package com.yourteam.nextstop.ui.admin

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourteam.nextstop.models.Bus
import com.yourteam.nextstop.models.CollegeLocation
import com.yourteam.nextstop.models.Route
import com.yourteam.nextstop.models.Stop
import com.yourteam.nextstop.models.User
import com.yourteam.nextstop.ui.components.CustomPlacesSearchField
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun AdminManageTab(viewModel: AdminViewModel) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Routes", "Buses", "Drivers")

    val routes by viewModel.routes.collectAsState()
    val buses by viewModel.buses.collectAsState()
    val drivers by viewModel.drivers.collectAsState()
    val collegeLocation by viewModel.collegeLocation.collectAsState()

    var showAddRouteDialog by remember { mutableStateOf(false) }
    var showAddBusDialog by remember { mutableStateOf(false) }
    var showInviteDriverDialog by remember { mutableStateOf(false) }
    var showCollegeLocationDialog by remember { mutableStateOf(false) }
    
    var editRoute by remember { mutableStateOf<Route?>(null) }
    var editBus by remember { mutableStateOf<Bus?>(null) }
    var editDriver by remember { mutableStateOf<User?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
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
            // College Location Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Central College Location", fontWeight = FontWeight.Bold)
                        Text(
                            text = collegeLocation?.name?.ifEmpty { "Not set" } ?: "Not set",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    IconButton(onClick = { showCollegeLocationDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Location")
                    }
                }
            }

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
                    buses = buses,
                    onEditClick = { editRoute = it },
                    onDeleteClick = { 
                        viewModel.deleteRoute(it.routeId) { _, msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                    }
                )
                1 -> BusesList(
                    buses = buses, 
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

    if (showCollegeLocationDialog) {
        CollegeLocationDialog(
            existingLocation = collegeLocation,
            onDismiss = { showCollegeLocationDialog = false },
            onSave = { loc ->
                showCollegeLocationDialog = false
                viewModel.updateCollegeLocation(loc) { _, msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            }
        )
    }

    if (showAddRouteDialog) {
        if (collegeLocation == null) {
            scope.launch { snackbarHostState.showSnackbar("Please set the Central College Location first.") }
            showAddRouteDialog = false
        } else {
            AddRouteDialog(
                collegeLocation = collegeLocation!!,
                buses = buses,
                onDismiss = { showAddRouteDialog = false },
                onSave = { newRoute ->
                    showAddRouteDialog = false
                    viewModel.addRoute(newRoute) { _, message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
                }
            )
        }
    }

    if (showAddBusDialog) {
        AddBusDialog(
            onDismiss = { showAddBusDialog = false },
            onSave = { busNumber ->
                showAddBusDialog = false
                viewModel.addBus(busNumber) { _, message ->
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
                viewModel.inviteDriver(email) { _, message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            }
        )
    }

    if (editBus != null) {
        AddBusDialog(
            existingBus = editBus,
            onDismiss = { editBus = null },
            onSave = { busNumber ->
                val busIdToUpdate = editBus!!.busId
                editBus = null
                viewModel.updateBus(busIdToUpdate, busNumber) { _, msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            }
        )
    }

    if (editRoute != null) {
        if (collegeLocation == null) {
            scope.launch { snackbarHostState.showSnackbar("Please set the Central College Location first.") }
            editRoute = null
        } else {
            AddRouteDialog(
                collegeLocation = collegeLocation!!,
                existingRoute = editRoute,
                buses = buses,
                onDismiss = { editRoute = null },
                onSave = { updatedRoute ->
                    editRoute = null
                    viewModel.updateRoute(updatedRoute) { _, msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                }
            )
        }
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
fun DriversList(drivers: List<User>, onEditClick: (User) -> Unit, onDeleteClick: (User) -> Unit) {
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
fun RoutesList(routes: List<Route>, buses: List<Bus>, onEditClick: (Route) -> Unit, onDeleteClick: (Route) -> Unit) {
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
        val fromCollege = routes.filter { it.direction == "from_college" }.sortedBy { it.departureTime }
        val toCollege = routes.filter { it.direction == "to_college" }.sortedBy { it.departureTime }

        if (fromCollege.isNotEmpty()) {
            item {
                Text("From College", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            items(fromCollege, key = { it.routeId }) { route ->
                RouteItemCard(route, buses.find { it.busId == route.busId }, onEditClick, onDeleteClick)
            }
        }
        
        if (toCollege.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("To College", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            items(toCollege, key = { it.routeId }) { route ->
                RouteItemCard(route, buses.find { it.busId == route.busId }, onEditClick, onDeleteClick)
            }
        }
    }
}

@Composable
private fun RouteItemCard(route: Route, bus: Bus?, onEditClick: (Route) -> Unit, onDeleteClick: (Route) -> Unit) {
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
                Text(text = "Departure: ${route.departureTime} | Bus ${bus?.busNumber ?: "?"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
fun BusesList(buses: List<Bus>, onEditClick: (Bus) -> Unit, onDeleteClick: (Bus) -> Unit) {
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
fun CollegeLocationDialog(
    existingLocation: CollegeLocation?,
    onDismiss: () -> Unit,
    onSave: (CollegeLocation) -> Unit
) {
    var locationName by remember { mutableStateOf(existingLocation?.name ?: "") }
    var lat by remember { mutableDoubleStateOf(existingLocation?.latitude ?: 0.0) }
    var lng by remember { mutableDoubleStateOf(existingLocation?.longitude ?: 0.0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("College Location") },
        text = {
            Column {
                key("college_location_search") {
                    CustomPlacesSearchField(
                        value = locationName,
                        onPlaceSelected = { placeResult ->
                            locationName = placeResult.name
                            lat = placeResult.latLng.latitude
                            lng = placeResult.latLng.longitude
                        },
                        label = "Search College Location",
                        clearAfterSelect = false,
                        onClear = { 
                            locationName = ""
                            lat = 0.0
                            lng = 0.0
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (locationName.isNotBlank()) onSave(CollegeLocation(locationName, lat, lng)) 
                },
                enabled = locationName.isNotBlank()
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
fun EditDriverDialog(
    driver: User,
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

@Composable
fun AddBusDialog(
    existingBus: Bus? = null,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var busNumber by remember { mutableStateOf(existingBus?.busNumber ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingBus != null) "Edit Bus" else "Add New Bus") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = busNumber,
                    onValueChange = { busNumber = it },
                    label = { Text("Bus Number (Plate / ID)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (busNumber.isNotBlank()) onSave(busNumber)
                },
                enabled = busNumber.isNotBlank()
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
                    text = "The driver will receive the 'driver' role upon logging in via Google.",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRouteDialog(
    collegeLocation: CollegeLocation,
    existingRoute: Route? = null,
    buses: List<Bus>,
    onDismiss: () -> Unit,
    onSave: (Route) -> Unit
) {
    var routeName by remember { mutableStateOf(existingRoute?.name ?: "") }
    var direction by remember { mutableStateOf(existingRoute?.direction ?: "from_college") }
    var busId by remember { mutableStateOf(existingRoute?.busId ?: "") }
    var departureTime by remember { mutableStateOf(existingRoute?.departureTime ?: "7:30 AM") }
    
    // Dependent names based on direction
    var externalPlaceName by remember { 
        mutableStateOf(
            if (existingRoute?.direction == "to_college") existingRoute.startName 
            else if (existingRoute?.direction == "from_college") existingRoute.endName 
            else ""
        )
    }
    
    var externalLat by remember {
        mutableDoubleStateOf(
            if (existingRoute?.direction == "from_college") existingRoute.endLat
            else 0.0
        )
    }
    var externalLng by remember {
        mutableDoubleStateOf(
            if (existingRoute?.direction == "from_college") existingRoute.endLng
            else 0.0
        )
    }

    val context = LocalContext.current
    var busDropdownExpanded by remember { mutableStateOf(false) }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = routeName,
                    onValueChange = { routeName = it },
                    label = { Text("Route Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Direction Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val fromCollegeColors = if (direction == "from_college") {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }
                    val toCollegeColors = if (direction == "to_college") {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }

                    Button(
                        onClick = { direction = "from_college" },
                        colors = fromCollegeColors,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("From College")
                    }
                    
                    Button(
                        onClick = { direction = "to_college" },
                        colors = toCollegeColors,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("To College")
                    }
                }

                // Place Field
                key("external_area_$direction") {
                    CustomPlacesSearchField(
                        value = externalPlaceName,
                        onPlaceSelected = { placeResult ->
                            externalPlaceName = placeResult.name
                            externalLat = placeResult.latLng.latitude
                            externalLng = placeResult.latLng.longitude
                        },
                        label = if (direction == "from_college") "End Area" else "Start Area",
                        clearAfterSelect = false,
                        onClear = { 
                            externalPlaceName = ""
                            externalLat = 0.0
                            externalLng = 0.0
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Time Selector
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val amPm = if (hourOfDay >= 12) "PM" else "AM"
                                    val hour12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                                    val timeStr = "%d:%02d %s".format(hour12, minute, amPm)
                                    departureTime = timeStr
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                false 
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(departureTime)
                    }
                    
                    // Bus Selector
                    ExposedDropdownMenuBox(
                        expanded = busDropdownExpanded,
                        onExpandedChange = { busDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = buses.find { it.busId == busId }?.busNumber ?: "Select Bus",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = busDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        ExposedDropdownMenu(
                            expanded = busDropdownExpanded,
                            onDismissRequest = { busDropdownExpanded = false }
                        ) {
                            buses.forEach { bus ->
                                DropdownMenuItem(
                                    text = { Text("Bus ${bus.busNumber}") },
                                    onClick = {
                                        busId = bus.busId
                                        busDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Stops (${stopsList.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier.height(150.dp).fillMaxWidth()
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
                            
                            IconButton(onClick = { stopsList.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                CustomPlacesSearchField(
                    value = "",
                    onPlaceSelected = { placeResult ->
                        val newStop = Stop(
                            stopId = java.util.UUID.randomUUID().toString(),
                            stopName = placeResult.name,
                            latitude = placeResult.latLng.latitude,
                            longitude = placeResult.latLng.longitude,
                            order = 0 
                        )
                        stopsList.add(newStop)
                    },
                    label = "Search & Add Stop...",
                    clearAfterSelect = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (routeName.isNotBlank() && externalPlaceName.isNotBlank() && stopsList.isNotEmpty() && busId.isNotBlank()) {
                        val routeId = existingRoute?.routeId ?: ""
                        val finalStops = stopsList.mapIndexed { index, stop -> stop.copy(order = index + 1) }
                        
                        val startName = if (direction == "from_college") collegeLocation.name else externalPlaceName
                        val endName = if (direction == "to_college") collegeLocation.name else externalPlaceName
                        val endLat = if (direction == "to_college") collegeLocation.latitude else externalLat
                        val endLng = if (direction == "to_college") collegeLocation.longitude else externalLng

                        val route = Route(
                            routeId = routeId, 
                            name = routeName, 
                            direction = direction,
                            busId = busId,
                            departureTime = departureTime,
                            startName = startName, 
                            endName = endName,
                            endLat = endLat,
                            endLng = endLng,
                            assignedDriverId = existingRoute?.assignedDriverId,
                            stops = finalStops
                        )
                        onSave(route)
                    }
                },
                enabled = routeName.isNotBlank() && externalPlaceName.isNotBlank() && stopsList.isNotEmpty() && busId.isNotBlank()
            ) {
                Text("Save Route")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
