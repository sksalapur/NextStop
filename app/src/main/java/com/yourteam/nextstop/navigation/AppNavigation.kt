package com.yourteam.nextstop.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.yourteam.nextstop.ui.admin.AdminHomeScreen
import com.yourteam.nextstop.ui.auth.AuthState
import com.yourteam.nextstop.ui.auth.AuthViewModel
import com.yourteam.nextstop.ui.auth.LoginScreen
import com.yourteam.nextstop.ui.driver.DriverHomeScreen
import com.yourteam.nextstop.ui.student.StudentHomeScreen

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    // React to auth state changes for navigation
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                val route = when (state.role) {
                    "admin" -> NavRoutes.ADMIN_GRAPH
                    "driver" -> NavRoutes.DRIVER_GRAPH
                    "student" -> NavRoutes.STUDENT_GRAPH
                    else -> NavRoutes.LOGIN
                }
                navController.navigate(route) {
                    popUpTo(NavRoutes.LOGIN) { inclusive = true }
                }
            }
            is AuthState.Idle -> {
                // If we were on a role screen and logged out, go back to login
                val currentRoute = navController.currentDestination?.route
                if (currentRoute != null && currentRoute != NavRoutes.LOGIN) {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> { /* Loading / Error handled in LoginScreen */ }
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.LOGIN
    ) {
        // Login screen
        composable(NavRoutes.LOGIN) {
            // If already loading from auto-login, show a centered spinner
            if (authState is AuthState.Loading && navController.previousBackStackEntry == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LoginScreen(
                    authState = authState,
                    onLoginWithGoogle = { context ->
                        authViewModel.loginWithGoogle(context)
                    },
                    onErrorShown = { authViewModel.clearError() }
                )
            }
        }

        // Nested NavGraphs for each role
        adminNavGraph(onLogout = { authViewModel.logout() })
        driverNavGraph(onLogout = { authViewModel.logout() })
        studentNavGraph(onLogout = { authViewModel.logout() })
    }
}

// ─── Admin NavGraph ──────────────────────────────────────────────────

fun NavGraphBuilder.adminNavGraph(onLogout: () -> Unit) {
    navigation(
        startDestination = NavRoutes.ADMIN_HOME,
        route = NavRoutes.ADMIN_GRAPH
    ) {
        composable(NavRoutes.ADMIN_HOME) {
            AdminHomeScreen(onLogout = onLogout)
        }
        // Future admin screens go here
    }
}

// ─── Driver NavGraph ─────────────────────────────────────────────────

fun NavGraphBuilder.driverNavGraph(onLogout: () -> Unit) {
    navigation(
        startDestination = NavRoutes.DRIVER_HOME,
        route = NavRoutes.DRIVER_GRAPH
    ) {
        composable(NavRoutes.DRIVER_HOME) {
            DriverHomeScreen(onLogout = onLogout)
        }
        // Future driver screens go here
    }
}

// ─── Student NavGraph ────────────────────────────────────────────────

fun NavGraphBuilder.studentNavGraph(onLogout: () -> Unit) {
    navigation(
        startDestination = NavRoutes.STUDENT_HOME,
        route = NavRoutes.STUDENT_GRAPH
    ) {
        composable(NavRoutes.STUDENT_HOME) {
            StudentHomeScreen(onLogout = onLogout)
        }
        // Future student screens go here
    }
}
