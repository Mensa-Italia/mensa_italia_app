package it.mensa.app.features.publicarea

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.LocalOfficeAdminModel
import it.mensa.shared.model.LocalOfficeAssistantModel
import it.mensa.shared.model.LocalOfficeModel
import it.mensa.shared.model.LocalOfficeTestDateModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

// ─── List ─────────────────────────────────────────────────────────────────────

data class PublicLocalOfficesListUiState(
    val offices: List<LocalOfficeModel> = emptyList(),
    val query: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

/**
 * Public (pre-login) variant of LocalOfficesListViewModel — uses
 * `refreshAllOfficesPublic` instead of the authenticated endpoint.
 */
class PublicLocalOfficesListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PublicLocalOfficesListUiState(loading = true))
    val uiState: StateFlow<PublicLocalOfficesListUiState> = _uiState.asStateFlow()

    private val repo get() = koinAccess().localOffices
    private var observeJob: Job? = null

    init {
        startObserving()
        refresh()
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = repo.observeAllOffices()
            .onEach { list ->
                _uiState.update { it.copy(offices = list, loading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.message, loading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                repo.refreshAllOfficesPublic()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun setQuery(q: String) = _uiState.update { it.copy(query = q) }

    fun filtered(state: PublicLocalOfficesListUiState): List<LocalOfficeModel> {
        val q = state.query.trim()
        if (q.isEmpty()) return state.offices
        val needle = normalize(q)
        return state.offices.filter { o ->
            val hay = normalize("${o.name} ${o.region} ${o.bio}")
            hay.contains(needle)
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("[^\\p{ASCII}]"), "")
            .lowercase()
}

// ─── Detail ───────────────────────────────────────────────────────────────────

data class PublicLocalOfficeDetailUiState(
    val office: LocalOfficeModel? = null,
    val admins: List<LocalOfficeAdminModel> = emptyList(),
    val assistants: List<LocalOfficeAssistantModel> = emptyList(),
    val testDates: List<LocalOfficeTestDateModel> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

/**
 * Public (pre-login) variant of LocalOfficeViewModel — resolves the office via
 * `officeByIdPublic` (no auth) and subscribes to admins/assistants/testDates.
 */
class PublicLocalOfficeDetailViewModel(
    private val officeId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicLocalOfficeDetailUiState())
    val uiState: StateFlow<PublicLocalOfficeDetailUiState> = _uiState.asStateFlow()

    private val repo get() = koinAccess().localOffices
    private var adminsJob: Job? = null
    private var assistantsJob: Job? = null
    private var testDatesJob: Job? = null

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val office = repo.officeByIdPublic(officeId)
                if (office == null) {
                    _uiState.update { it.copy(loading = false, error = "Gruppo locale non trovato.") }
                    return@launch
                }
                _uiState.update { it.copy(office = office, loading = false) }
                subscribeAll()
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    private fun subscribeAll() {
        adminsJob?.cancel()
        adminsJob = repo.observeAdmins(officeId)
            .onEach { list -> _uiState.update { it.copy(admins = list) } }
            .catch { /* swallow — UI keeps last good value */ }
            .launchIn(viewModelScope)

        assistantsJob?.cancel()
        assistantsJob = repo.observeAssistants(officeId)
            .onEach { list -> _uiState.update { it.copy(assistants = list) } }
            .catch { }
            .launchIn(viewModelScope)

        testDatesJob?.cancel()
        testDatesJob = repo.observeUpcomingTestDates(officeId)
            .onEach { list -> _uiState.update { it.copy(testDates = list) } }
            .catch { }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repo.refreshAdmins(officeId) }
            runCatching { repo.refreshAssistants(officeId) }
            runCatching { repo.refreshUpcomingTestDates(officeId) }
        }
    }
}
