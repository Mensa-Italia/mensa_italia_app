package it.mensa.app.features.localoffices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.LocalOfficeAdminModel
import it.mensa.shared.model.LocalOfficeAssistantModel
import it.mensa.shared.model.LocalOfficeLinktreeRowModel
import it.mensa.shared.model.LocalOfficeModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocalOfficeLinktreeUiState(
    val office: LocalOfficeModel? = null,
    val linktree: List<LocalOfficeLinktreeRowModel> = emptyList(),
    val admins: List<LocalOfficeAdminModel> = emptyList(),
    val assistants: List<LocalOfficeAssistantModel> = emptyList(),
    val currentUserId: String = "",
    val loading: Boolean = true,
    val error: String? = null,
)

class LocalOfficeLinktreeViewModel(private val officeId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(LocalOfficeLinktreeUiState())
    val uiState: StateFlow<LocalOfficeLinktreeUiState> = _uiState.asStateFlow()

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
            .onEach { list -> _uiState.update { it.copy(linktree = list, loading = false) } }
            .catch { e -> _uiState.update { it.copy(error = e.message, loading = false) } }
            .launchIn(viewModelScope)

        jobs += repo.observeAdmins(officeId)
            .onEach { list -> _uiState.update { it.copy(admins = list) } }
            .catch { }.launchIn(viewModelScope)

        jobs += repo.observeAssistants(officeId)
            .onEach { list -> _uiState.update { it.copy(assistants = list) } }
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
                _uiState.update { it.copy(office = office) }
            } catch (_: Exception) {}
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                repo.refreshLinktreeByOffice(officeId)
                repo.refreshAdmins(officeId)
                repo.refreshAssistants(officeId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun canEdit(state: LocalOfficeLinktreeUiState): Boolean {
        if (state.currentUserId.isEmpty()) return false
        return state.admins.any { it.user == state.currentUserId } ||
            state.assistants.any { it.user == state.currentUserId }
    }

    fun deleteLink(id: String) {
        viewModelScope.launch {
            try {
                repo.deleteLink(officeId, id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // Sorted helpers
    fun sorted(state: LocalOfficeLinktreeUiState) =
        state.linktree.sortedBy { it.sortOrder }

    fun rootLinks(state: LocalOfficeLinktreeUiState) =
        sorted(state).filter { it.parent.isEmpty() && it.kind == "link" }

    fun sections(state: LocalOfficeLinktreeUiState) =
        sorted(state).filter { it.parent.isEmpty() && it.kind == "section" }

    fun children(sectionId: String, state: LocalOfficeLinktreeUiState) =
        sorted(state).filter { it.parent == sectionId }

    fun clearError() = _uiState.update { it.copy(error = null) }

    override fun onCleared() {
        super.onCleared()
        jobs.forEach { it.cancel() }
    }
}
