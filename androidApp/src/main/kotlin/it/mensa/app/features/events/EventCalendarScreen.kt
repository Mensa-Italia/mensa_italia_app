package it.mensa.app.features.events

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.events._components.EventRowCard
import it.mensa.app.features.events.util.EventDateFormatter
import it.mensa.app.ui.theme.MensaCyan
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * EventCalendarScreen — Android equivalent of iOS EventCalendarView.swift.
 * Monthly calendar with event markers and day detail list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCalendarScreen(
    onBack: () -> Unit = {},
    onEventClick: (String) -> Unit = {},
    vm: EventCalendarViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val daysWithEvents = remember(state.events) { vm.daysWithEvents() }
    val selectedDayEvents = remember(state.selectedDateMillis, state.events) { vm.eventsOnDay(state.selectedDateMillis) }

    val dayFormatter = remember { SimpleDateFormat("EEEE d MMMM yyyy", Locale.ITALIAN) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario eventi") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro") } },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
        ) {
            item {
                CalendarMonthGrid(
                    displayedMonthMillis = state.displayedMonthMillis,
                    selectedDateMillis = state.selectedDateMillis,
                    daysWithEvents = daysWithEvents,
                    onDaySelected = { day -> vm.selectDate(day) },
                    onMonthChange = { delta ->
                        val cal = Calendar.getInstance().apply { timeInMillis = state.displayedMonthMillis }
                        cal.add(Calendar.MONTH, delta)
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                        vm.setDisplayedMonth(cal.timeInMillis)
                    },
                    eventsCountForDay = { day -> vm.eventsOnDay(day).size },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(dayFormatter.format(Date(state.selectedDateMillis)).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Text("${selectedDayEvents.size} eventi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (selectedDayEvents.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Nessun evento in questa giornata.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                itemsIndexed(selectedDayEvents, key = { _, e -> e.id }) { _, event ->
                    EventRowCard(
                        event = event,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().clickable { onEventClick(event.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    displayedMonthMillis: Long,
    selectedDateMillis: Long,
    daysWithEvents: Set<Long>,
    onDaySelected: (Long) -> Unit,
    onMonthChange: (Int) -> Unit,
    eventsCountForDay: (Long) -> Int,
    modifier: Modifier = Modifier,
) {
    val monthTitleFormatter = remember { SimpleDateFormat("LLLL yyyy", Locale.ITALIAN) }
    val monthTitle = remember(displayedMonthMillis) { monthTitleFormatter.format(Date(displayedMonthMillis)).replaceFirstChar { it.uppercase() } }
    val weekdays = listOf("L", "M", "M", "G", "V", "S", "D")
    val days = remember(displayedMonthMillis) { buildMonthDays(displayedMonthMillis) }

    Column(modifier = modifier) {
        // Month header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonthChange(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mese precedente") }
            Text(monthTitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            IconButton(onClick = { onMonthChange(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mese successivo") }
        }

        // Weekday labels
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { wd ->
                Text(wd, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(4.dp))

        // Grid
        AnimatedContent(
            targetState = displayedMonthMillis,
            transitionSpec = {
                slideInHorizontally { w -> w } togetherWith slideOutHorizontally { w -> -w }
            },
            label = "CalendarAnimation",
        ) { _ ->
            Column(modifier = Modifier.fillMaxWidth()) {
                days.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { (millis, inMonth) ->
                            val isSelected = millis == selectedDateMillis
                            val isToday = millis == todayStartMillis()
                            val hasEvents = daysWithEvents.contains(millis)
                            val dotCount = if (hasEvents) minOf(eventsCountForDay(millis), 3) else 0
                            val cal = Calendar.getInstance().apply { timeInMillis = millis }
                            val dayNum = cal.get(Calendar.DAY_OF_MONTH)

                            Column(
                                modifier = Modifier.weight(1f).aspectRatio(0.7f).clickable { onDaySelected(millis) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Box(
                                    modifier = Modifier.size(34.dp).clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .then(if (isToday && !isSelected) Modifier.clip(CircleShape).background(Color.Transparent) else Modifier),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isToday && !isSelected) {
                                        Box(Modifier.size(34.dp).clip(CircleShape).background(Color.Transparent).padding(1.dp)) {
                                            Surface(modifier = Modifier.fillMaxSize(), shape = CircleShape, color = Color.Transparent, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) {}
                                        }
                                    }
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal),
                                        color = when {
                                            isSelected -> Color.White
                                            inMonth -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        },
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (dotCount > 0) {
                                        repeat(dotCount) {
                                            Box(Modifier.size(4.dp).clip(CircleShape).background(if (inMonth) MensaCyan else MensaCyan.copy(alpha = 0.4f)))
                                        }
                                    } else { Box(Modifier.size(4.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildMonthDays(monthStartMillis: Long): List<Pair<Long, Boolean>> {
    val cal = Calendar.getInstance().apply { timeInMillis = monthStartMillis; set(Calendar.DAY_OF_MONTH, 1) }
    // Start of the calendar grid: Monday of the week containing the 1st
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday...
    val leadingDays = (firstDayOfWeek - Calendar.MONDAY + 7) % 7
    val gridCal = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis; add(Calendar.DAY_OF_MONTH, -leadingDays) }

    val result = mutableListOf<Pair<Long, Boolean>>()
    repeat(42) {
        gridCal.set(Calendar.HOUR_OF_DAY, 0); gridCal.set(Calendar.MINUTE, 0); gridCal.set(Calendar.SECOND, 0); gridCal.set(Calendar.MILLISECOND, 0)
        val inMonth = gridCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH) && gridCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
        result.add(Pair(gridCal.timeInMillis, inMonth))
        gridCal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return result
}
