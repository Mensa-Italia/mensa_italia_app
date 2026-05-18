package it.mensa.app.features.profile.sub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.DeviceModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DevicesUiState(
    val devices: List<DeviceModel> = emptyList(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
)

class DevicesViewModel : ViewModel() {

    private val devices = koinAccess().devices

    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            devices.observeAll().collect { list ->
                _uiState.update { it.copy(devices = list, loading = false) }
            }
        }
        viewModelScope.launch { refresh() }
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { devices.refresh() }.onFailure { e ->
                _uiState.update { it.copy(loading = false, errorMessage = e.message) }
            }
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { devices.delete(id) }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
}
