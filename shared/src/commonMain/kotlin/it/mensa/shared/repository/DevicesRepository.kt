package it.mensa.shared.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
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

    /**
     * Token FCM di *questo* dispositivo, per come lo ha visto l'ultima
     * [ensureRegistered]. Serve alle UI per riconoscere la propria riga nella
     * lista e non offrirne la rimozione.
     *
     * Sta in KeyValue e non in memoria perche' deve sopravvivere al riavvio
     * dell'app; sparisce al logout insieme al resto della cache
     * (`wipeAllUserData`), che e' esattamente cio' che vogliamo.
     */
    suspend fun currentFirebaseId(): String? =
        db.keyValueQueries.selectById(KEY_CURRENT_FIREBASE_ID)
            .awaitAsOneOrNull()
            ?.value_
            ?.takeIf { it.isNotBlank() }

    /** True se [device] e' il dispositivo su cui sta girando l'app. */
    suspend fun isCurrent(device: DeviceModel): Boolean {
        val mine = currentFirebaseId() ?: return false
        return device.firebaseId == mine
    }

    /**
     * Si assicura che questo dispositivo risulti registrato sul server, e lo
     * registra se non c'e'.
     *
     * Sostituisce la vecchia coppia "registra una volta sola + ricorda di
     * averlo fatto in un flag locale". Quel flag era il bug: cancellando il
     * device dal server e rifacendo login, l'app trovava il flag ancora li' e
     * non ripubblicava niente, quindi il telefono restava fuori dall'elenco e
     * senza notifiche fino a una reinstallazione. La verita' su chi e'
     * registrato ce l'ha il server, quindi la si chiede al server: una GET
     * filtrata per `firebase_id`, e la POST solo se manca davvero.
     *
     * Idempotente e sicura da chiamare a ogni avvio, a ogni login e a ogni
     * rotazione del token FCM. Ritorna il record — nuovo o gia' esistente —
     * oppure null se non c'e' niente da registrare (nessun token, nessun
     * utente) o se la rete non risponde.
     */
    suspend fun ensureRegistered(
        userId: String,
        firebaseToken: String,
        deviceName: String,
        language: String = "",
    ): DeviceModel? {
        if (userId.isBlank() || firebaseToken.isBlank()) return null

        // Prima di tutto: da qui in poi questo e' "il mio device", anche se la
        // chiamata di rete fallisce. Serve a proteggerne la riga in lista.
        db.keyValueQueries.insertOrReplace(
            key = KEY_CURRENT_FIREBASE_ID,
            value_ = firebaseToken,
        )

        val existing = runCatching { api.findByFirebaseId(firebaseToken).items }
            .getOrElse { return null }
            .firstOrNull { it.user == userId }

        if (existing != null) {
            upsertRow(existing, language)
            return existing
        }

        return runCatching { register(userId, firebaseToken, deviceName, language) }.getOrNull()
    }

    suspend fun register(
        userId: String,
        firebaseToken: String,
        deviceName: String,
        language: String = "",
    ): DeviceModel {
        val created = api.register(userId, firebaseToken, deviceName, language)
        upsertRow(created, language)
        return created
    }

    /**
     * Cancella un device registrato. Ritorna false — senza cancellare niente —
     * se [id] e' il dispositivo su cui gira l'app.
     *
     * Sganciarsi da soli non ha senso: il record tornerebbe al primo
     * [ensureRegistered] utile, e nel frattempo l'utente resta senza notifiche
     * senza capire perche'. Per smettere di ricevere push ci sono le
     * impostazioni di sistema; per sganciare un telefono, lo si fa dall'altro
     * telefono.
     *
     * Il controllo sta qui e non solo nella UI perche' e' una regola del
     * dominio. E' un valore di ritorno e non un'eccezione di proposito: una
     * `suspend fun` che lancia verso Swift e' il caso descritto in
     * [it.mensa.shared.auth.AuthRepository.init], cioe' un modo per far
     * terminare il processo invece di mostrare un errore.
     */
    suspend fun delete(id: String): Boolean {
        val row = db.deviceQueries.selectById(id).awaitAsOneOrNull()
        val mine = currentFirebaseId()
        if (row != null && mine != null && row.firebaseId == mine) return false
        api.delete(id)
        db.deviceQueries.deleteById(id)
        return true
    }

    private suspend fun upsertRow(device: DeviceModel, language: String) {
        db.deviceQueries.insertOrReplace(
            id = device.id,
            user = device.user,
            firebaseId = device.firebaseId,
            deviceName = device.deviceName,
            language = language,
            createdAt = device.created.toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    private companion object {
        const val KEY_CURRENT_FIREBASE_ID = "devices.current_firebase_id"
    }
}
