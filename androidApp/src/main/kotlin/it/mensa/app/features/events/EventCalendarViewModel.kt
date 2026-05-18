package it.mensa.app.features.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.EventModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class EventCalendarUiState(
    val events: List<EventModel> = emptyList(),
    val selectedDateMillis: Long = todayStartMillis(),
    val displayedMonthMillis: Long = todayStartMillis(),
)

fun todayStartMillis(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

class EventCalendarViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EventCalendarUiState())
    val uiState: StateFlow<EventCalendarUiState> = _uiState.asStateFlow()

    private val repo get() = koinAccess().events

    init {
        repo.observeAll()
            .onEach { list -> _uiState.update { it.copy(events = list) } }
            .catch { /* non-fatal */ }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            try { repo.refresh(sort = "when_end") } catch (_: Exception) {}
        }
    }

    fun selectDate(millis: Long) = _uiState.update { it.copy(selectedDateMillis = millis) }
    fun setDisplayedMonth(millis: Long) = _uiState.update { it.copy(displayedMonthMillis = millis) }

    fun daysWithEvents(): Set<Long> {
        val cal = Calendar.getInstance()
        val set = mutableSetOf<Long>()
        for (e in _uiState.value.events) {
            val start = e.whenStart.toEpochMilliseconds()
            val end = e.whenEnd.toEpochMilliseconds()
            cal.timeInMillis = start
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            var d = cal.timeInMillis
            while (d <= end) {
                set.add(d)
                cal.timeInMillis = d; cal.add(Calendar.DAY_OF_MONTH, 1); d = cal.timeInMillis
            }
        }
        return set
    }

    fun eventsOnDay(dayStartMillis: Long): List<EventModel> {
        val cal = Calendar.getInstance().apply { timeInMillis = dayStartMillis }
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        val dayEnd = cal.timeInMillis
        return _uiState.value.events.filter { e ->
            val s = e.whenStart.toEpochMilliseconds()
            val en = e.whenEnd.toEpochMilliseconds()
            s <= dayEnd && en >= dayStartMillis
        }.sortedBy { it.whenStart.toEpochMilliseconds() }
    }
}
