package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.StampModel
import it.mensa.shared.model.StampUserModel
import kotlinx.serialization.Serializable

/** Minimal body for creating a user_stamp record (stamp + code + user id). */
@Serializable
data class AddUserStampBody(
    val stamp: String,
    val code: String,
    val user: String
)

class StampsApi(private val pb: PocketBaseClient) {

    /**
     * Catalogo dei francobolli.
     *
     * La collection si chiama `stamp`, al singolare. Qui c'era scritto
     * `stamps` in tutti e cinque i metodi, e `stamps` non esiste: PocketBase
     * risponde 404 con "Missing collection context". Non se n'era accorto
     * nessuno perche' il passaporto non passa da qui — legge `stamp_users` con
     * `expand=stamp` — e l'unico chiamante vero era [getStamp], cioe' la
     * verifica dopo una scansione, che infatti falliva sempre.
     */
    suspend fun list(
        filter: String? = null,
        sort: String = "-created"
    ): List<StampModel> =
        pb.fullList(COLLECTION, filter = filter, sort = sort)

    suspend fun get(id: String): StampModel =
        pb.getOne(COLLECTION, id)

    suspend fun create(body: StampModel): StampModel =
        pb.create(COLLECTION, body)

    suspend fun update(id: String, body: StampModel): StampModel =
        pb.update(COLLECTION, id, body)

    suspend fun delete(id: String) =
        pb.delete(COLLECTION, id)

    /**
     * Add a user_stamp record — POST /api/collections/stamp_users/records.
     * Flutter: pb.collection('stamp_users').create(body: { "stamp": id, "code": code, "user": userId })
     */
    suspend fun addUserStamp(stampId: String, code: String, userId: String): StampUserModel =
        pb.create("stamp_users", AddUserStampBody(stamp = stampId, code = code, user = userId))

    /**
     * Fetch user stamps for the authenticated user — expand=stamp, sort=-created.
     */
    suspend fun getUserStamps(
        filter: String? = null
    ): List<StampUserModel> =
        pb.fullList("stamp_users", filter = filter, sort = "-created", expand = "stamp")

    /**
     * Recupera il francobollo che sta dietro a un QR appena scansionato, per
     * mostrarlo nel foglio di conferma prima di riscattarlo.
     *
     * Il codice NON si controlla qui, e non e' una svista. Prima questo metodo
     * filtrava per `id = '...' && code = '...'`, ma `stamp` un campo `code`
     * non ce l'ha: il codice sta in `stamp_secret`, una collection a parte con
     * `listRule` nulla, cioe' leggibile solo dai superuser. Quel filtro
     * rispondeva 400 a ogni scansione — ed e' giusto che sia cosi', un segreto
     * che il client puo' leggere per confrontarlo non e' piu' un segreto.
     *
     * A validarlo e' il server, quando si crea il record in `stamp_users`:
     *
     *     user.id = @request.auth.id &&
     *     stamp.stamp_secret_via_stamp.code = @request.body.code
     *
     * Quindi un codice sbagliato non passa comunque: fallisce [addStamp], che
     * e' il punto in cui il francobollo viene davvero assegnato.
     *
     * Torna null se il francobollo non esiste, perche' chi chiama distingue
     * "non trovato" da un errore di rete mostrando due stati diversi.
     */
    suspend fun getStamp(id: String, @Suppress("UNUSED_PARAMETER") code: String): StampModel? =
        runCatching { pb.getOne<StampModel>(COLLECTION, id) }.getOrNull()

    /**
     * Shorthand for [addUserStamp] — the user id comes from the caller context.
     */
    suspend fun addStamp(stampId: String, code: String, userId: String): StampUserModel =
        addUserStamp(stampId, code, userId)

    private companion object {
        /** Singolare, come nello schema. Vedi il kdoc di [list]. */
        const val COLLECTION = "stamp"
    }
}
