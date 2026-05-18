package it.mensa.app.features.localoffices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.LocalOfficeAdminModel
import it.mensa.shared.model.LocalOfficeAssistantModel
import it.mensa.shared.model.LocalOfficeModel
import it.mensa.shared.model.LocalOfficeTestDateModel
import it.mensa.shared.model.LocalOfficeLinktreeRowModel
import it.mensa.shared.model.SigModel
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocalOfficeUiState(
    val office: LocalOfficeModel? = null,
    val linktree: List<LocalOfficeLinktreeRowModel> = emptyList(),
    val admins: List<LocalOfficeAdminModel> = emptyList(),
    val assistants: List<LocalOfficeAssistantModel> = emptyList(),
    val testDates: List<LocalOfficeTestDateModel> = emptyList(),
    val events: List<EventModel> = emptyList(),
    val sigs: List<SigModel> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val currentUserId: String = "",
)

class LocalOfficeViewModel(private val officeId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(LocalOfficeUiState())
    val uiState: StateFlow<LocalOfficeUiState> = _uiState.asStateFlow()

    private val repo get() = koinAccess().localOffices
    private val auth get() = koinAccess().auth
    private val jobs = mutableListOf<Job>()

    init {
        startObserving()
        observeUser()
        loadOffice()
        refresh()
    }

    private fun startObserving() {
        jobs += repo.observeLinktree(officeId)
            .onEach { list -> _uiState.update { it.copy(linktree = list) } }
            .catch { }.launchIn(viewModelScope)

        jobs += repo.observeAdmins(officeId)
            .onEach { list -> _uiState.update { it.copy(admins = list) } }
            .catch { }.launchIn(viewModelScope)

        jobs += repo.observeAssistants(officeId)
            .onEach { list -> _uiState.update { it.copy(assistants = list) } }
            .catch { }.launchIn(viewModelScope)

        jobs += repo.observeUpcomingTestDates(officeId)
            .onEach { list -> _uiState.update { it.copy(testDates = list) } }
            .catch { }.launchIn(viewModelScope)

        jobs += repo.observeEvents(officeId)
            .onEach { list -> _uiState.update { it.copy(events = list) } }
            .catch { }.launchIn(viewModelScope)

        jobs += repo.observeSigs(officeId)
            .onEach { list -> _uiState.update { it.copy(sigs = list) } }
            .catch { }.launchIn(viewModelScope)
    }

    private fun observeUser() {
        auth.currentUser
            .onEach { user -> _uiState.update { it.copy(currentUserId = user?.id ?: "") } }
            .catch { }
            .launchIn(viewModelScope)
    }

    private fun loadOffice() {
        viewModelScope.launch {
            try {
                val office = repo.officeById(officeId)
                _uiState.update { it.copy(office = office, loading = office == null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, loading = false) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                repo.refreshAllForOffice(officeId)
                // Also reload the office itself
                val office = repo.officeById(officeId)
                _uiState.update { it.copy(office = office, loading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, loading = false) }
            }
        }
    }

    fun canEdit(state: LocalOfficeUiState): Boolean {
        if (state.currentUserId.isEmpty()) return false
        val adminIds = state.admins.map { it.user }
        val assistantIds = state.assistants.map { it.user }
        return adminIds.contains(state.currentUserId) || assistantIds.contains(state.currentUserId)
    }

    // ─── Test date CRUD ──────────────────────────────────────────────────────

    fun deleteTestDate(id: String) {
        viewModelScope.launch {
            try {
                repo.deleteTestDate(officeId, id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // ─── Link CRUD ───────────────────────────────────────────────────────────

    fun deleteLink(id: String) {
        viewModelScope.launch {
            try {
                repo.deleteLink(officeId, id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    override fun onCleared() {
        super.onCleared()
        jobs.forEach { it.cancel() }
    }
}
