package com.yourteam.nextstop.ui.admin

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yourteam.nextstop.navigation.NavRoutes
import com.yourteam.nextstop.ui.components.NextStopTopBar

@Composable
fun AdminHomeScreen(
    onLogout: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val bottomNavController = rememberNavController()

    val tabs = listOf(
        Triple(NavRoutes.ADMIN_DASHBOARD, "Dashboard", Icons.Default.Dashboard),
        Triple(NavRoutes.ADMIN_ASSIGNMENTS, "Assignments", Icons.Default.Assignment),
        Triple(NavRoutes.ADMIN_MANAGE, "Manage", Icons.Default.Settings)
    )

    Scaffold(
        topBar = { NextStopTopBar(title = "Admin Panel", onLogout = onLogout) },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                tabs.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                        onClick = {
                            bottomNavController.navigate(route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = NavRoutes.ADMIN_DASHBOARD,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.ADMIN_DASHBOARD) {
                AdminDashboardTab(viewModel = viewModel)
            }
            composable(NavRoutes.ADMIN_ASSIGNMENTS) {
                AdminAssignmentsTab(viewModel = viewModel)
            }
            composable(NavRoutes.ADMIN_MANAGE) {
                AdminManageTab(viewModel = viewModel)
            }
        }
    }
}
