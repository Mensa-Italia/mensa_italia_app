package it.mensa.app.features.profile.sub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.CalendarLinkModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalendarLinkerUiState(
    val link: CalendarLinkModel? = null,
    val loading: Boolean = true,
    val copied: Boolean = false,
    val errorMessage: String? = null,
)

class CalendarLinkerViewModel : ViewModel() {

    private val calendarLinks = koinAccess().calendarLinks

    private val _uiState = MutableStateFlow(CalendarLinkerUiState())
    val uiState: StateFlow<CalendarLinkerUiState> = _uiState.asStateFlow()

    val availableRegions = listOf(
        "Abruzzo", "Basilicata", "Calabria", "Campania", "Emilia-Romagna",
        "Friuli-Venezia Giulia", "Lazio", "Liguria", "Lombardia", "Marche",
        "Molise", "Piemonte", "Puglia", "Sardegna", "Sicilia", "Toscana",
        "Trentino-Alto Adige", "Umbria", "Valle d'Aosta", "Veneto",
    )

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            calendarLinks.observeCurrent().collect { link ->
                _uiState.update { it.copy(link = link, loading = false) }
            }
        }
        viewModelScope.launch { refresh() }
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { calendarLinks.refresh() }.onFailure { e ->
                _uiState.update { it.copy(loading = false, errorMessage = e.message) }
            }
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun toggleRegion(region: String) {
        val current = _uiState.value.link ?: return
        val newState = if (current.state.any { it.equals(region, ignoreCase = true) }) {
            current.state.filter { !it.equals(region, ignoreCase = true) }
        } else {
            current.state + region
        }
        // Optimistic update
        _uiState.update { it.copy(link = current.copy(state = newState)) }
        viewModelScope.launch {
            runCatching {
                calendarLinks.changeState(current.id, newState)
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message) }
                refresh() // revert
            }
        }
    }

    fun onCopied() {
        _uiState.update { it.copy(copied = true) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            _uiState.update { it.copy(copied = false) }
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    fun webcalUrl(link: CalendarLinkModel) = "webcal://svc.mensa.it/ical/${link.hash}"
    fun httpsUrl(link: CalendarLinkModel) = "https://svc.mensa.it/ical/${link.hash}"
}
