package it.mensa.app.services.calendar

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import it.mensa.app.support.Logger
import java.util.TimeZone

/**
 * CalendarHelper — wrapper over CalendarContract for adding events to the
 * system calendar. Android equivalent of iOS EventKitHelper.swift.
 *
 * Two modes:
 * 1. [insertDirectly] — silent insert into the default calendar (requires
 *    READ_CALENDAR + WRITE_CALENDAR permissions, no user confirmation).
 * 2. [openCalendarIntent] — opens the system calendar app with pre-filled
 *    fields via an Intent (no permissions required, user confirms).
 *
 * Mode 2 is generally preferred for better UX.
 *
 * TODO:
 *  1. Add a permission check wrapper and rationale UI
 *  2. Handle event update / deletion (requires storing the inserted event ID)
 */
class CalendarHelper(private val context: Context) {

    data class CalendarEvent(
        val title: String,
        val description: String?,
        val location: String?,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val allDay: Boolean = false,
    )

    /**
     * Open the system calendar app with pre-filled event data.
     * No permissions required — the system calendar app handles confirmation.
     * Returns true if the intent was resolved.
     */
    fun openCalendarIntent(event: CalendarEvent): Boolean {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, event.title)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startTimeMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endTimeMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, event.allDay)
        if (event.description != null) {
            intent.putExtra(CalendarContract.Events.DESCRIPTION, event.description)
        }
        if (event.location != null) {
            intent.putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            Logger.w("Calendar", "openIntent", "No calendar app found")
            false
        }
    }

    /**
     * Directly insert an event into the default calendar (silent, no UI).
     * Requires WRITE_CALENDAR permission.
     * Returns the URI of the created event, or null on failure.
     */
    fun insertDirectly(event: CalendarEvent): Uri? {
        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DTSTART, event.startTimeMillis)
                put(CalendarContract.Events.DTEND, event.endTimeMillis)
                put(CalendarContract.Events.ALL_DAY, if (event.allDay) 1 else 0)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.CALENDAR_ID, getDefaultCalendarId())
                if (event.description != null) {
                    put(CalendarContract.Events.DESCRIPTION, event.description)
                }
                if (event.location != null) {
                    put(CalendarContract.Events.EVENT_LOCATION, event.location)
                }
            }
            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        } catch (e: SecurityException) {
            Logger.e("Calendar", "insertDirectly", "Permission denied", e)
            null
        } catch (e: Exception) {
            Logger.e("Calendar", "insertDirectly", "Unexpected error", e)
            null
        }
    }

    private fun getDefaultCalendarId(): Long {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars.IS_PRIMARY} = 1"
        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection, selection, null, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 1L
            } ?: 1L
        } catch (e: SecurityException) {
            1L
        }
    }
}
