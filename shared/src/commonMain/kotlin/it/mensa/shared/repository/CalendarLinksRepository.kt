package it.mensa.shared.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import it.mensa.shared.api.endpoints.CalendarLinksApi
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.CalendarLinkModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class CalendarLinksRepository(
    private val api: CalendarLinksApi,
    private val db: MensaDatabase,
    private val json: Json,
) {
    fun observeCurrent(): Flow<CalendarLinkModel?> =
        // Pick first row, if any
        db.calendarQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.firstOrNull()?.toModel(json) }

    suspend fun refresh() {
        val link = api.current() ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        db.transaction {
            db.calendarQueries.deleteAll()
            db.calendarQueries.insertOrReplace(
                id = link.id,
                user = link.user,
                hash = link.hash,
                stateJson = json.encodeToString(
                    ListSerializer(String.serializer()), link.state,
                ),
                updatedAt = now,
            )
        }
    }

    suspend fun firstSnapshot(): CalendarLinkModel? = observeCurrent().first()

    /** Update the included regions for the given calendar link and mirror to DB. */
    suspend fun changeState(id: String, state: List<String>): CalendarLinkModel {
        val updated = api.updateState(id, state)
        val now = Clock.System.now().toEpochMilliseconds()
        db.transaction {
            db.calendarQueries.insertOrReplace(
                id = updated.id,
                user = updated.user,
                hash = updated.hash,
                stateJson = json.encodeToString(
                    ListSerializer(String.serializer()), updated.state,
                ),
                updatedAt = now,
            )
        }
        return updated
    }
}
