package com.yourteam.nextstop.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.nextstop.data.repository.StudentRepository
import com.yourteam.nextstop.models.Stop
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SetupUiState {
    data object Loading : SetupUiState()
    data class Ready(val stops: List<Stop>) : SetupUiState()
    data class Error(val message: String) : SetupUiState()
    data object Saved : SetupUiState()
}

@HiltViewModel
class HomeStopSetupViewModel @Inject constructor(
    private val repository: StudentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SetupUiState>(SetupUiState.Loading)
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadAllStops()
    }

    private fun loadAllStops() {
        viewModelScope.launch {
            try {
                val routes = repository.getAllRoutes()
                // Flatten all stops, deduplicate by stopId
                val stops = routes
                    .flatMap { it.stops }
                    .distinctBy { it.stopName.trim().lowercase() }
                    .sortedBy { it.stopName }
                _uiState.value = SetupUiState.Ready(stops)
            } catch (e: Exception) {
                _uiState.value = SetupUiState.Error(e.message ?: "Failed to load stops")
            }
        }
    }

    fun saveHomeStop(stopId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val uid = repository.getCurrentUid() ?: return@launch
                repository.setHomeStopId(uid, stopId)
                _uiState.value = SetupUiState.Saved
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = SetupUiState.Error(e.message ?: "Failed to save stop")
            } finally {
                _isSaving.value = false
            }
        }
    }
}
