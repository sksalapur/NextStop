package com.yourteam.nextstop.ui.auth

/**
 * Represents the authentication state of the app.
 */
sealed class AuthState {
    /** No authentication action in progress. */
    data object Idle : AuthState()

    /** Authentication or role-fetch is in progress. */
    data object Loading : AuthState()

    /** Successfully authenticated and role resolved. */
    data class Success(val role: String) : AuthState()

    /** An error occurred during auth or role-fetch. */
    data class Error(val message: String) : AuthState()
}
