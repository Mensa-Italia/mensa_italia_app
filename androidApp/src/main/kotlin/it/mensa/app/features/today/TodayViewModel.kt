package it.mensa.app.features.today

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.services.location.LocationProvider
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.DealModel
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.NotificationModel
import it.mensa.shared.model.SigModel
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Calendar
import java.util.Locale

// ─── UI state ────────────────────────────────────────────────────────────────

sealed class TodayPhase {
    object Loading : TodayPhase()
    object Ready : TodayPhase()
    data class Error(val message: String) : TodayPhase()
}

data class TodayUiState(
    val phase: TodayPhase = TodayPhase.Loading,
    val user: UserModel? = null,
    val nextEvent: EventModel? = null,
    val upcomingEvents: List<EventModel> = emptyList(),
    val notifications: List<NotificationModel> = emptyList(),
    val sigsPreview: List<SigModel> = emptyList(),
    val dealsPreview: List<DealModel> = emptyList(),
    /** Derived — formatted date string e.g. "MARTEDÌ 16 MAGGIO" */
    val formattedDate: String = "",
)

// ─── ViewModel ───────────────────────────────────────────────────────────────

class TodayViewModel(
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val koin = koinAccess()

    private val _uiState = MutableStateFlow(
        TodayUiState(formattedDate = buildFormattedDate()),
    )
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private var lastLocation: Location? = null
    private var upcomingSnapshot: List<EventModel> = emptyList()

    init {
        observeCacheFlows()

        viewModelScope.launch {
            val loc = locationProvider.requestOnce()
            if (loc != null) {
                lastLocation = loc
                recomputeNextEvent(loc)
            }
        }

        viewModelScope.launch {
            runCatching {
                launch { koin.events.refresh(filter = null, sort = "when_end") }
                launch { koin.notifications.refresh() }
            }
        }
    }

    // ─── Cache observation ────────────────────────────────────────────────────

    private fun observeCacheFlows() {
        viewModelScope.launch {
            val contentFlow = combine(
                koin.events.observeAll(),
                koin.notifications.observeAll(),
            ) { events, notifications -> events to notifications }

            val previewFlow = combine(
                koin.sigs.observeAll(),
                koin.deals.observeAll(),
            ) { sigs, deals -> sigs to deals }

            combine(
                koin.auth.currentUser,
                contentFlow,
                previewFlow,
            ) { user, (events, notifications), (sigs, deals) ->
                val nowMs = Clock.System.now().toEpochMilliseconds()
                val upcoming = events
                    .filter { it.whenStart.toEpochMilliseconds() >= nowMs }
                    .sortedBy { it.whenStart.toEpochMilliseconds() }

                upcomingSnapshot = upcoming

                val nextEvent = computeNextEvent(upcoming, events, lastLocation)

                TodayUiState(
                    phase = TodayPhase.Ready,
                    user = user,
                    nextEvent = nextEvent,
                    upcomingEvents = upcoming,
                    notifications = notifications,
                    sigsPreview = sigs.take(5),
                    dealsPreview = deals.filter { it.isActive }.take(5),
                    formattedDate = buildFormattedDate(),
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    // ─── Pull-to-refresh ─────────────────────────────────────────────────────

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                launch { koin.events.refresh(filter = null, sort = "when_end") }
                launch { koin.notifications.refresh() }
            }
        }
    }

    // ─── Geo-proximity ───────────────────────────────────────────────────────

    private fun computeNextEvent(
        upcoming: List<EventModel>,
        allEvents: List<EventModel>,
        userLocation: Location?,
    ): EventModel? {
        if (userLocation == null) return upcoming.firstOrNull() ?: allEvents.firstOrNull()

        val horizonMs = Clock.System.now().toEpochMilliseconds() + 90L * 86_400_000L
        val candidates = upcoming
            .filter { it.whenStart.toEpochMilliseconds() <= horizonMs }
            .mapNotNull { event ->
                val pos = event.position ?: return@mapNotNull null
                val results = FloatArray(1)
                Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    pos.lat, pos.lon,
                    results,
                )
                event to results[0]
            }

        return candidates.minByOrNull { it.second }?.first
            ?: upcoming.firstOrNull()
            ?: allEvents.firstOrNull()
    }

    private fun recomputeNextEvent(userLocation: Location) {
        val state = _uiState.value
        val nextEvent = computeNextEvent(upcomingSnapshot, state.upcomingEvents, userLocation)
        _uiState.update { it.copy(nextEvent = nextEvent) }
    }

    // ─── Notification actions ─────────────────────────────────────────────────

    fun markNotificationSeen(id: String) {
        viewModelScope.launch {
            runCatching { koin.notifications.markSeen(id) }
        }
    }
}

// ─── Date/greeting helpers ────────────────────────────────────────────────────

private fun buildFormattedDate(): String {
    return try {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        // ordinal: Mon=0, Tue=1, Wed=2, Thu=3, Fri=4, Sat=5, Sun=6
        val dayNames = listOf(
            "LUNEDÌ", "MARTEDÌ", "MERCOLEDÌ",
            "GIOVEDÌ", "VENERDÌ", "SABATO", "DOMENICA",
        )
        val monthNames = listOf(
            "GENNAIO", "FEBBRAIO", "MARZO", "APRILE", "MAGGIO", "GIUGNO",
            "LUGLIO", "AGOSTO", "SETTEMBRE", "OTTOBRE", "NOVEMBRE", "DICEMBRE",
        )
        val dayName = dayNames.getOrElse(now.dayOfWeek.ordinal) { "" }
        val monthName = monthNames.getOrElse(now.monthNumber - 1) { "" }
        "$dayName ${now.dayOfMonth} $monthName"
    } catch (e: Exception) {
        ""
    }
}
