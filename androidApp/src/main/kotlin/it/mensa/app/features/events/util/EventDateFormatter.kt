package it.mensa.app.features.events.util

import kotlinx.datetime.Instant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * EventDateFormatter — Android equivalent of iOS EventDateUtil.swift.
 * Provides shared date formatting helpers used across the Events module.
 */
object EventDateFormatter {

    private val locale = Locale.ITALIAN

    val fullFormatter: SimpleDateFormat = SimpleDateFormat("EEEE d MMMM yyyy, HH:mm", locale)
    val mediumFormatter: SimpleDateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", locale)
    val dayMonthFormatter: SimpleDateFormat = SimpleDateFormat("d MMM", locale)
    val timeFormatter: SimpleDateFormat = SimpleDateFormat("HH:mm", locale)
    val dayHeaderFormatter: SimpleDateFormat = SimpleDateFormat("EEEE, d MMMM", locale)

    fun toDate(instant: Instant): Date = Date(instant.toEpochMilliseconds())

    fun formatFull(instant: Instant): String = fullFormatter.format(toDate(instant))
    fun formatMedium(instant: Instant): String = mediumFormatter.format(toDate(instant))
    fun formatDayMonth(instant: Instant): String = dayMonthFormatter.format(toDate(instant))
    fun formatTime(instant: Instant): String = timeFormatter.format(toDate(instant))

    fun isPast(event: it.mensa.shared.model.EventModel): Boolean {
        val now = System.currentTimeMillis()
        val end = event.whenEnd.toEpochMilliseconds()
        return if (end > 0) end < now else event.whenStart.toEpochMilliseconds() < now
    }
}
