package it.mensa.app.features.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.LocationModel
import it.mensa.shared.repository.EventDraft
import it.mensa.shared.repository.ScheduleDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

data class ScheduleDraftUi(
    val stableId: String = java.util.UUID.randomUUID().toString(),
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val infoLink: String = "",
    val whenStart: Long = System.currentTimeMillis(),
    val whenEnd: Long = System.currentTimeMillis() + 3600_000,
    val maxExternalGuests: Int = 0,
    val price: Double = 0.0,
    val isSubscriptable: Boolean = false,
) {
    val isDeleted: Boolean get() = id?.startsWith("DELETE:") == true
}

data class AddEventUiState(
    val name: String = "",
    val description: String = "",
    val infoLink: String = "",
    val startDateMillis: Long = System.currentTimeMillis() + 86400_000,
    val endDateMillis: Long = System.currentTimeMillis() + 86400_000 + 7200_000,
    val position: LocationModel? = null,
    val isOnline: Boolean = false,
    val isNational: Boolean = false,
    val isSpot: Boolean = false,
    val imageBytes: ByteArray? = null,
    val imageFilename: String? = null,
    val imageContentType: String? = null,
    val schedules: List<ScheduleDraftUi> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null,
    val dismissed: Boolean = false,
    val canControlEvents: Boolean = false,
    val ownerId: String = "",
)

class AddEventViewModel(private val eventId: String? = null) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEventUiState())
    val uiState: StateFlow<AddEventUiState> = _uiState.asStateFlow()

    private val eventsRepo get() = koinAccess().events
    private val schedulesRepo get() = koinAccess().eventSchedules
    private val auth get() = koinAccess().auth

    val isEditing: Boolean get() = eventId != null

    init {
        initUser()
        if (eventId != null) loadEvent(eventId)
    }

    private fun initUser() {
        val user = auth.currentUser.value
        val powers = user?.powers ?: emptyList()
        val canControl = powers.contains("super") || powers.contains("events") || powers.contains("events_helper")
        _uiState.update { it.copy(canControlEvents = canControl, ownerId = user?.id ?: "") }
    }

    private fun loadEvent(id: String) {
        viewModelScope.launch {
            val event = eventsRepo.getById(id) ?: return@launch
            _uiState.update { it.copy(
                name = event.name,
                description = event.description,
                infoLink = event.infoLink,
                startDateMillis = event.whenStart.toEpochMilliseconds(),
                endDateMillis = event.whenEnd.toEpochMilliseconds(),
                position = event.position,
                isNational = event.isNational,
                isSpot = event.isSpot,
                isOnline = event.position == null,
            )}
            loadSchedules(id)
        }
    }

    private fun loadSchedules(eventId: String) {
        viewModelScope.launch {
            try {
                schedulesRepo.refresh(eventId)
                val list = schedulesRepo.firstSnapshot(eventId)
                _uiState.update { it.copy(
                    schedules = list.map { s -> ScheduleDraftUi(
                        id = s.id,
                        title = s.title,
                        description = s.description,
                        infoLink = s.infoLink,
                        whenStart = s.whenStart.toEpochMilliseconds(),
                        whenEnd = s.whenEnd.toEpochMilliseconds(),
                        maxExternalGuests = s.maxExternalGuests,
                        price = s.price,
                        isSubscriptable = s.isSubscriptable,
                    )}
                )}
            } catch (_: Exception) {}
        }
    }

    fun updateName(v: String) = _uiState.update { it.copy(name = v) }
    fun updateDescription(v: String) = _uiState.update { it.copy(description = v) }
    fun updateInfoLink(v: String) = _uiState.update { it.copy(infoLink = v) }
    fun updateStartDate(v: Long) = _uiState.update { it.copy(startDateMillis = v) }
    fun updateEndDate(v: Long) = _uiState.update { it.copy(endDateMillis = v) }
    fun updatePosition(v: LocationModel?) = _uiState.update { it.copy(position = v) }
    fun updateIsOnline(v: Boolean) = _uiState.update { it.copy(isOnline = v) }
    fun updateIsNational(v: Boolean) = _uiState.update { it.copy(isNational = v) }
    fun updateIsSpot(v: Boolean) = _uiState.update { it.copy(isSpot = v) }
    fun updateImage(bytes: ByteArray, filename: String, contentType: String) = _uiState.update { it.copy(imageBytes = bytes, imageFilename = filename, imageContentType = contentType) }
    fun updateSchedules(list: List<ScheduleDraftUi>) = _uiState.update { it.copy(schedules = list) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun save() {
        viewModelScope.launch {
            val s = _uiState.value
            val name = s.name.trim()
            val desc = s.description.trim()
            if (name.isBlank()) { _uiState.update { it.copy(error = "Il nome è obbligatorio") }; return@launch }
            if (desc.isBlank()) { _uiState.update { it.copy(error = "La descrizione è obbligatoria") }; return@launch }
            if (s.endDateMillis < s.startDateMillis) { _uiState.update { it.copy(error = "La fine deve essere dopo l'inizio") }; return@launch }

            var finalIsOnline = s.isOnline
            var finalIsNational = s.isNational
            var finalIsSpot = s.isSpot
            if (!s.canControlEvents) { finalIsOnline = false; finalIsNational = false; finalIsSpot = true }

            if (!finalIsOnline && s.position == null) {
                _uiState.update { it.copy(error = "Seleziona una posizione o segna l'evento come online") }
                return@launch
            }

            _uiState.update { it.copy(saving = true) }
            try {
                val kSchedules = s.schedules.map { sd -> ScheduleDraft(
                    id = sd.id,
                    title = sd.title,
                    description = sd.description,
                    infoLink = sd.infoLink,
                    whenStart = Instant.fromEpochMilliseconds(sd.whenStart),
                    whenEnd = Instant.fromEpochMilliseconds(sd.whenEnd),
                    maxExternalGuests = sd.maxExternalGuests,
                    price = sd.price,
                    isSubscriptable = sd.isSubscriptable,
                )}
                val draft = EventDraft(
                    name = name, description = desc, infoLink = s.infoLink.trim(),
                    whenStart = Instant.fromEpochMilliseconds(s.startDateMillis),
                    whenEnd = Instant.fromEpochMilliseconds(s.endDateMillis),
                    isNational = finalIsNational, isSpot = finalIsSpot,
                    ownerId = s.ownerId,
                    positionId = if (finalIsOnline) null else s.position?.id,
                    imageBytes = s.imageBytes,
                    imageFilename = s.imageBytes?.let { s.imageFilename ?: "cover.jpg" },
                    imageContentType = s.imageContentType ?: "image/jpeg",
                    schedules = kSchedules,
                )
                if (eventId != null) eventsRepo.update(eventId, draft) else eventsRepo.create(draft)
                _uiState.update { it.copy(dismissed = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(saving = false) }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            if (eventId == null) return@launch
            _uiState.update { it.copy(saving = true) }
            try {
                eventsRepo.delete(eventId)
                _uiState.update { it.copy(dismissed = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(saving = false) }
            }
        }
    }
}
