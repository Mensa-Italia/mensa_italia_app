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
    /**
     * Token FCM di questo telefono. La riga che ce l'ha e' "questo
     * dispositivo" e non si puo' rimuovere: sganciarsi da soli lascerebbe
     * l'utente senza notifiche senza motivo, e il record tornerebbe comunque
     * al primo `ensureRegistered`.
     */
    val currentFirebaseId: String? = null,
) {
    fun isCurrent(device: DeviceModel): Boolean =
        currentFirebaseId != null && device.firebaseId == currentFirebaseId
}

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
        viewModelScope.launch {
            val current = runCatching { devices.currentFirebaseId() }.getOrNull()
            _uiState.update { it.copy(currentFirebaseId = current) }
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
            // `delete` risponde false sul dispositivo in uso, e la UI per
            // quello non mostra nemmeno il cestino: qui interessa solo
            // l'errore di rete.
            runCatching { devices.delete(id) }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
}
