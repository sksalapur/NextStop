package com.yourteam.nextstop.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.nextstop.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentUser()
    }

    /**
     * On app launch, if a user is already signed in,
     * fetch their role and emit Success so the NavHost
     * can skip the login screen.
     */
    private fun checkCurrentUser() {
        val user = repository.currentUser ?: return
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val role = repository.handleUserRoleInitialization(user)
                _authState.value = AuthState.Success(role)
            } catch (e: Exception) {
                // If role fetch fails for a cached user, force re-login
                repository.logout()
                _authState.value = AuthState.Error(
                    e.message ?: "Failed to restore session"
                )
            }
        }
    }

    /**
     * Authenticate with Google Credential Manager
     */
    fun loginWithGoogle(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = repository.signInWithGoogle(context)
                val role = repository.handleUserRoleInitialization(user)
                _authState.value = AuthState.Success(role)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    e.message ?: "Google Sign In failed"
                )
            }
        }
    }

    /**
     * Sign out and reset to Idle so the NavHost
     * navigates back to LoginScreen.
     * Note: The StudentViewModel (and its Direction state) is intrinsically 
     * flushed to default (TO_COLLEGE) because popUpTo(0) rips the graph out
     * and guarantees fresh ViewModel instantiation on next login.
     */
    fun logout() {
        repository.logout()
        _authState.value = AuthState.Idle
    }

    /**
     * Clear a transient error so the user can retry.
     */
    fun clearError() {
        _authState.value = AuthState.Idle
    }
}
