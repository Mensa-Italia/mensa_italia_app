package it.mensa.app.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class NotificationManagerUiState(
    val notifyEvents: Boolean = true,
    val notifyMessages: Boolean = true,
    val notifyGeneral: Boolean = true,
    val selectedRegions: Set<String> = emptySet(),
    val loading: Boolean = true,
    val saving: Boolean = false,
)

val italianRegions: List<String> = listOf(
    "Abruzzo", "Basilicata", "Calabria", "Campania", "Emilia-Romagna",
    "Friuli-Venezia Giulia", "Lazio", "Liguria", "Lombardia", "Marche",
    "Molise", "Piemonte", "Puglia", "Sardegna", "Sicilia", "Toscana",
    "Trentino-Alto Adige", "Umbria", "Valle d'Aosta", "Veneto",
)

class NotificationManagerViewModel : ViewModel() {

    private val auth = koinAccess().auth
    private val metadata = koinAccess().metadata

    private val _uiState = MutableStateFlow(NotificationManagerUiState())
    val uiState: StateFlow<NotificationManagerUiState> = _uiState.asStateFlow()

    private var userId: String = ""

    init {
        // Observe current user for userId
        auth.currentUser
            .onEach { user ->
                userId = user?.id ?: ""
                if (userId.isNotEmpty()) loadMetadata()
            }
            .launchIn(viewModelScope)
    }

    private fun loadMetadata() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            runCatching { metadata.refresh(userId) }
            _uiState.update {
                it.copy(
                    notifyEvents = (metadata.get("notify_events") ?: "true") == "true",
                    notifyMessages = (metadata.get("notify_messages") ?: "true") == "true",
                    notifyGeneral = (metadata.get("notify_general") ?: "true") == "true",
                    selectedRegions = parseRegions(metadata.get("notify_me_events")),
                    loading = false,
                )
            }
        }
    }

    /** Flutter persists the chosen regions as a JSON string array under
     *  `notify_me_events`, e.g. `["Lazio","Toscana"]`. We mirror that
     *  exact shape so both apps read the same metadata. */
    private fun parseRegions(raw: String?): Set<String> {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return emptySet()
        return runCatching {
            Json.decodeFromString<List<String>>(text).toSet()
        }.getOrElse { emptySet() }
    }

    fun setNotifyEvents(value: Boolean) {
        _uiState.update { it.copy(notifyEvents = value) }
        save("notify_events", value.toString())
    }

    fun setNotifyMessages(value: Boolean) {
        _uiState.update { it.copy(notifyMessages = value) }
        save("notify_messages", value.toString())
    }

    fun setNotifyGeneral(value: Boolean) {
        _uiState.update { it.copy(notifyGeneral = value) }
        save("notify_general", value.toString())
    }

    fun toggleRegion(region: String, enabled: Boolean) {
        val updated = _uiState.value.selectedRegions.toMutableSet().apply {
            if (enabled) add(region) else remove(region)
        }
        _uiState.update { it.copy(selectedRegions = updated) }
        // Persist as a JSON array string — matches Flutter's contract on the
        // same backend key (`notify_me_events`).
        save("notify_me_events", Json.encodeToString(updated.toList()))
    }

    private fun save(key: String, value: String) {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            runCatching { metadata.set(userId, key, value) }
            _uiState.update { it.copy(saving = false) }
        }
    }
}
