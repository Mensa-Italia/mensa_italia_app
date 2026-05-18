package it.mensa.app.features.deals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.DealModel
import it.mensa.shared.model.LocationModel
import it.mensa.shared.repository.DealsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// ─── UI state ────────────────────────────────────────────────────────────────

data class AddDealUiState(
    // Form fields
    val name: String = "",
    val commercialSector: String = "",
    val vatNumber: String = "",
    val link: String = "",
    val position: LocationModel? = null,
    val hasValidity: Boolean = false,
    val startDate: Instant = Clock.System.now(),
    val endDate: Instant = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds() + 365L * 24 * 3600 * 1000),
    val details: String = "",
    val howToGet: String = "",
    val selectedEligibility: String = "active_members",
    // Contact fields
    val contactId: String? = null,
    val contactName: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val contactNote: String = "",
    // Status
    val saving: Boolean = false,
    val deleting: Boolean = false,
    val error: String? = null,
    val dismissed: Boolean = false,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && commercialSector.isNotBlank()

    val emailLooksValid: Boolean
        get() {
            val e = contactEmail.trim()
            if (e.isEmpty()) return true
            return e.contains("@") && e.contains(".")
        }

    val isEditing: Boolean = false // overridden by ViewModel
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

class AddDealViewModel(private val dealId: String?) : ViewModel() {

    private val koin = koinAccess()
    private var existingDeal: DealModel? = null

    private val _state = MutableStateFlow(AddDealUiState())
    val state: StateFlow<AddDealUiState> = _state.asStateFlow()

    val isEditing: Boolean get() = !dealId.isNullOrEmpty()

    init {
        if (!dealId.isNullOrEmpty()) {
            viewModelScope.launch { loadExistingDeal() }
        }
    }

    private suspend fun loadExistingDeal() {
        try {
            val deal = koin.deals.getById(dealId!!) ?: return
            existingDeal = deal

            val startEpoch = deal.starting
            val endEpoch = deal.ending

            _state.update {
                it.copy(
                    name = deal.name,
                    commercialSector = deal.commercialSector,
                    vatNumber = deal.vatNumber ?: "",
                    link = deal.link ?: "",
                    position = deal.position,
                    details = deal.details ?: "",
                    howToGet = deal.howToGet ?: "",
                    selectedEligibility = deal.who?.takeIf { w -> w.isNotEmpty() } ?: "active_members",
                    hasValidity = startEpoch != null && endEpoch != null,
                    startDate = startEpoch ?: Clock.System.now(),
                    endDate = endEpoch ?: Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds() + 365L * 24 * 3600 * 1000),
                )
            }
            loadContact()
        } catch (_: Exception) {
            // Non-fatal — form remains usable
        }
    }

    private suspend fun loadContact() {
        if (dealId.isNullOrEmpty()) return
        try {
            val contacts = koin.deals.contacts(dealId)
            val first = contacts.firstOrNull() ?: return
            _state.update {
                it.copy(
                    contactId = first.id,
                    contactName = first.name,
                    contactEmail = first.email,
                    contactPhone = first.phoneNumber ?: "",
                    contactNote = first.note ?: "",
                )
            }
        } catch (_: Exception) { /* Non-fatal */ }
    }

    // ─── Field setters ────────────────────────────────────────────────────────

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setCommercialSector(v: String) = _state.update { it.copy(commercialSector = v) }
    fun setVatNumber(v: String) = _state.update { it.copy(vatNumber = v) }
    fun setLink(v: String) = _state.update { it.copy(link = v) }
    fun setPosition(v: LocationModel?) = _state.update { it.copy(position = v) }
    fun setHasValidity(v: Boolean) = _state.update { it.copy(hasValidity = v) }
    fun setStartDate(v: Instant) = _state.update { it.copy(startDate = v) }
    fun setEndDate(v: Instant) = _state.update { it.copy(endDate = v) }
    fun setDetails(v: String) = _state.update { it.copy(details = v) }
    fun setHowToGet(v: String) = _state.update { it.copy(howToGet = v) }
    fun setSelectedEligibility(v: String) = _state.update { it.copy(selectedEligibility = v) }
    fun setContactName(v: String) = _state.update { it.copy(contactName = v) }
    fun setContactEmail(v: String) = _state.update { it.copy(contactEmail = v) }
    fun setContactPhone(v: String) = _state.update { it.copy(contactPhone = v) }
    fun setContactNote(v: String) = _state.update { it.copy(contactNote = v) }

    fun clearError() = _state.update { it.copy(error = null) }

    // ─── Save ─────────────────────────────────────────────────────────────────

    fun save() {
        val s = _state.value
        if (!s.canSave || s.saving) return
        if (!s.emailLooksValid) {
            _state.update { it.copy(error = "Email non valida") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val draft = DealsRepository.DealDraft(
                    name = s.name.trim(),
                    commercialSector = s.commercialSector.trim(),
                    details = s.details.trimToNull(),
                    who = s.selectedEligibility,
                    howToGet = s.howToGet.trimToNull(),
                    link = s.link.trimToNull(),
                    vatNumber = s.vatNumber.trimToNull(),
                    positionId = s.position?.id,
                    starting = if (s.hasValidity) s.startDate else null,
                    ending = if (s.hasValidity) s.endDate else null,
                )

                val cName = s.contactName.trim()
                val cEmail = s.contactEmail.trim()
                val contact: DealsRepository.ContactDraft? = if (cName.isNotEmpty() && cEmail.isNotEmpty()) {
                    DealsRepository.ContactDraft(
                        id = s.contactId,
                        name = cName,
                        email = cEmail,
                        phoneNumber = s.contactPhone.trimToNull(),
                        note = s.contactNote.trimToNull(),
                    )
                } else null

                if (isEditing && !dealId.isNullOrEmpty()) {
                    koin.deals.update(id = dealId, draft = draft, contact = contact)
                } else {
                    koin.deals.create(draft = draft, contact = contact)
                }
                _state.update { it.copy(dismissed = true) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Errore nel salvataggio") }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    fun delete() {
        val s = _state.value
        if (dealId.isNullOrEmpty() || s.deleting) return
        viewModelScope.launch {
            _state.update { it.copy(deleting = true, error = null) }
            try {
                koin.deals.delete(dealId)
                _state.update { it.copy(dismissed = true) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Errore nell'eliminazione") }
            } finally {
                _state.update { it.copy(deleting = false) }
            }
        }
    }

    private fun String.trimToNull(): String? = trim().takeIf { it.isNotEmpty() }
}
