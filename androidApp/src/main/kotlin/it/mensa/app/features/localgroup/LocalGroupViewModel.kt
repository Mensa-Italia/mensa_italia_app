package it.mensa.app.features.localgroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.localgroup.ExportKind
import it.mensa.shared.localgroup.LocalGroupExportFile
import it.mensa.shared.localgroup.LocalGroupPeriod
import it.mensa.shared.localgroup.LocalGroupRepository
import it.mensa.shared.localgroup.LocalGroupSnapshot
import it.mensa.shared.localgroup.LocalGroupState
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.LocalOfficeMonthlyStatsModel
import it.mensa.shared.model.MemberChangeModel
import it.mensa.shared.model.RegSociModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Something read on demand by an admin page: still loading, failed, or read. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data object Failed : Loadable<Nothing>
    data class Loaded<T>(val value: T) : Loadable<T>
}

/**
 * The admin area's data ("Area segreteria"): each page makes its own
 * instance and asks only for what it shows. The office comes from the tab's
 * snapshot ([LocalGroupRepository.observe], also when a refresh failed but a
 * snapshot is cached); with none yet the tab is refreshed once.
 */
class LocalGroupViewModel(
    private val repository: LocalGroupRepository = koinAccess().localGroup,
) : ViewModel() {

    val snapshot: StateFlow<LocalGroupSnapshot?> = repository.observe()
        .map(::snapshotOf)
        .stateIn(viewModelScope, SharingStarted.Eagerly, snapshotOf(repository.observe().value))

    private val _period = MutableStateFlow(LocalGroupPeriod.DAYS_30)
    val period: StateFlow<LocalGroupPeriod> = _period.asStateFlow()

    private val _changes = MutableStateFlow<Loadable<List<MemberChangeModel>>>(Loadable.Loading)
    val changes: StateFlow<Loadable<List<MemberChangeModel>>> = _changes.asStateFlow()

    private val _monthly = MutableStateFlow<Loadable<List<LocalOfficeMonthlyStatsModel>>>(Loadable.Loading)
    val monthly: StateFlow<Loadable<List<LocalOfficeMonthlyStatsModel>>> = _monthly.asStateFlow()

    private val _newMembers = MutableStateFlow<Loadable<List<RegSociModel>>>(Loadable.Loading)
    val newMembers: StateFlow<Loadable<List<RegSociModel>>> = _newMembers.asStateFlow()

    private val _events = MutableStateFlow<Loadable<List<EventModel>>>(Loadable.Loading)
    val events: StateFlow<Loadable<List<EventModel>>> = _events.asStateFlow()

    /** The running read of each target: a new one (another period) cancels the old. */
    private val jobs = mutableMapOf<MutableStateFlow<*>, Job>()

    init {
        if (snapshot.value == null) viewModelScope.launch { repository.refresh() }
    }

    fun selectPeriod(value: LocalGroupPeriod) {
        if (value == _period.value) return
        _period.value = value
        snapshot.value?.let { loadChanges(it.office.id) }
    }

    fun loadChanges(officeId: String) {
        val range = _period.value.range()
        load(_changes) { repository.loadChanges(officeId, range.from, range.to) }
    }

    fun loadMonthly(officeId: String) = load(_monthly) { repository.loadMonthly(officeId) }

    fun loadNewMembers(officeId: String) = load(_newMembers) { repository.loadNewMembers(officeId) }

    fun loadEvents(officeId: String) = load(_events) { repository.loadOfficeEvents(officeId) }

    /**
     * Downloads the .xlsx of [kind] and writes it in [directory] (the cache,
     * which the FileProvider shares). The movements' files take the selected
     * period; the active members' one has no dates. Throws on failure.
     */
    suspend fun export(kind: ExportKind, directory: File): File {
        val office = checkNotNull(snapshot.value).office
        val range = _period.value.range().takeIf { kind != ExportKind.ACTIVE }
        val bytes = repository.exportMembers(office.id, kind, range?.from, range?.to)
        val name = LocalGroupExportFile.fileName(office, kind, range?.from, range?.to)
        return withContext(Dispatchers.IO) { File(directory, name).apply { writeBytes(bytes) } }
    }

    private fun <T> load(target: MutableStateFlow<Loadable<T>>, read: suspend () -> T) {
        jobs.remove(target)?.cancel()
        target.value = Loadable.Loading
        jobs[target] = viewModelScope.launch {
            target.value = try {
                Loadable.Loaded(read())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Loadable.Failed
            }
        }
    }

    private companion object {
        fun snapshotOf(state: LocalGroupState): LocalGroupSnapshot? = when (state) {
            is LocalGroupState.Ready -> state.snapshot
            is LocalGroupState.Failed -> state.cached
            else -> null
        }
    }
}
