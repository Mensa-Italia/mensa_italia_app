package it.mensa.shared.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import it.mensa.shared.api.endpoints.LocationsApi
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.LocationModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

class LocationsRepository(
    private val api: LocationsApi,
    private val db: MensaDatabase,
    @Suppress("unused") private val json: Json,
) {
    fun observeAll(): Flow<List<LocationModel>> =
        db.locationQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel() } }

    suspend fun refresh() {
        val items = api.list()
        db.transaction {
            db.locationQueries.deleteAll()
            items.forEach { l ->
                db.locationQueries.insertOrReplace(
                    id = l.id,
                    name = l.name,
                    lat = l.lat,
                    lon = l.lon,
                    address = l.address,
                    state = l.state,
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                )
            }
        }
    }

    /**
     * Creates a new location via the API and persists it locally.
     */
    suspend fun createAndAddLocal(
        name: String,
        address: String,
        lat: Double,
        lon: Double,
        createdBy: String = "",
    ): LocationModel {
        val created = api.create(name = name, address = address, lat = lat, lon = lon, createdBy = createdBy)
        db.locationQueries.insertOrReplace(
            id = created.id,
            name = created.name,
            lat = created.lat,
            lon = created.lon,
            address = created.address,
            state = created.state,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
        return created
    }

    /**
     * Soft-deletes a location via the API (sets saved=false) and removes it from local cache.
     */
    suspend fun deleteOne(id: String) {
        api.delete(id)
        db.locationQueries.deleteById(id)
    }
}
