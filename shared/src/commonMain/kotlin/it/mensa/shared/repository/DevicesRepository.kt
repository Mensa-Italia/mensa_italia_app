package it.mensa.shared.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import it.mensa.shared.api.endpoints.DevicesApi
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.DeviceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class DevicesRepository(
    private val api: DevicesApi,
    private val db: MensaDatabase,
) {
    fun observeAll(): Flow<List<DeviceModel>> =
        db.deviceQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel() } }

    suspend fun refresh() {
        val items = api.list()
        val now = Clock.System.now().toEpochMilliseconds()
        db.transaction {
            db.deviceQueries.deleteAll()
            items.forEach { d ->
                db.deviceQueries.insertOrReplace(
                    id = d.id,
                    user = d.user,
                    firebaseId = d.firebaseId,
                    deviceName = d.deviceName,
                    language = "",
                    createdAt = d.created.toEpochMilliseconds(),
                    updatedAt = now,
                )
            }
        }
    }

    suspend fun firstSnapshot(): List<DeviceModel> = observeAll().first()

    suspend fun register(
        userId: String,
        firebaseToken: String,
        deviceName: String,
        language: String = "",
    ): DeviceModel {
        val created = api.register(userId, firebaseToken, deviceName, language)
        db.deviceQueries.insertOrReplace(
            id = created.id,
            user = created.user,
            firebaseId = created.firebaseId,
            deviceName = created.deviceName,
            language = language,
            createdAt = created.created.toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
        return created
    }

    suspend fun delete(id: String) {
        api.delete(id)
        db.deviceQueries.deleteById(id)
    }
}
