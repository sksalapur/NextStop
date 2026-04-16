package com.yourteam.nextstop.navigation

/**
 * Defines all navigation route constants for the app.
 */
object NavRoutes {
    const val LOGIN = "login"

    // Nested graph routes
    const val ADMIN_GRAPH = "admin_graph"
    const val DRIVER_GRAPH = "driver_graph"
    const val STUDENT_GRAPH = "student_graph"

    // Screen routes within each graph
    const val ADMIN_HOME = "admin_home"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_ASSIGNMENTS = "admin_assignments"
    const val ADMIN_MANAGE = "admin_manage"
    
    const val DRIVER_HOME = "driver_home"
    const val STUDENT_HOME = "student_home"
    const val STUDENT_HOME_STOP_SETUP = "student_home_stop_setup"
}
