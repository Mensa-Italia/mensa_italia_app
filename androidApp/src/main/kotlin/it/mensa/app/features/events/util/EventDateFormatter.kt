package it.mensa.app.features.events.util

import it.mensa.app.support.AppFormat
import kotlinx.datetime.Instant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * EventDateFormatter — Android equivalent of iOS EventDateUtil.swift.
 * Provides shared date formatting helpers used across the Events module.
 *
 * Everything delegates to [AppFormat], so the language follows the in-app
 * picker rather than the device. The formatters are functions and not `val`s
 * on purpose: held as properties they would freeze the language they were
 * first built with for the whole process.
 */
object EventDateFormatter {

    fun fullFormatter(locale: Locale = AppFormat.locale()): SimpleDateFormat =
        AppFormat.formatter(AppFormat.Skeleton.FULL_WITH_TIME, locale)

    fun mediumFormatter(locale: Locale = AppFormat.locale()): SimpleDateFormat =
        AppFormat.formatter(AppFormat.Skeleton.MEDIUM_WITH_TIME, locale)

    fun dayMonthFormatter(locale: Locale = AppFormat.locale()): SimpleDateFormat =
        AppFormat.formatter(AppFormat.Skeleton.DAY_MONTH, locale)

    fun timeFormatter(locale: Locale = AppFormat.locale()): SimpleDateFormat =
        AppFormat.formatter(AppFormat.Skeleton.TIME, locale)

    fun dayHeaderFormatter(locale: Locale = AppFormat.locale()): SimpleDateFormat =
        AppFormat.formatter(AppFormat.Skeleton.WEEKDAY_DAY_MONTH, locale)

    fun toDate(instant: Instant): Date = Date(instant.toEpochMilliseconds())

    fun formatFull(instant: Instant, locale: Locale = AppFormat.locale()): String =
        fullFormatter(locale).format(toDate(instant))

    fun formatMedium(instant: Instant, locale: Locale = AppFormat.locale()): String =
        mediumFormatter(locale).format(toDate(instant))

    fun formatDayMonth(instant: Instant, locale: Locale = AppFormat.locale()): String =
        dayMonthFormatter(locale).format(toDate(instant))

    fun formatTime(instant: Instant, locale: Locale = AppFormat.locale()): String =
        timeFormatter(locale).format(toDate(instant))

    fun isPast(event: it.mensa.shared.model.EventModel): Boolean {
        val now = System.currentTimeMillis()
        val end = event.whenEnd.toEpochMilliseconds()
        return if (end > 0) end < now else event.whenStart.toEpochMilliseconds() < now
    }
}
