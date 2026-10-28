package it.mensa.shared.api.endpoints

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.api.SkipAuthAttribute
import it.mensa.shared.model.ExAppModel
import it.mensa.shared.model.ExGrantedPermissionsModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ExGrantedPermissionsCreateBody(
    val user: String,
    @SerialName("ex_app") val exApp: String,
    val permissions: List<String>,
)

@Serializable
internal data class ExGrantedPermissionsPatchBody(
    val permissions: List<String>,
)

/**
 * `request_id` c'e' solo nell'esportazione dei dati (`data_export_confirmation`);
 * per la conferma d'identita' resta null e, non essendo `encodeDefaults`, non
 * finisce nel JSON: il corpo e' ancora `{"accepted": bool}`.
 */
@Serializable
internal data class CallbackBody(
    val accepted: Boolean,
    @SerialName("request_id") val requestId: String? = null,
)

@Serializable
internal data class ExportDecisionBody(
    @SerialName("request_id") val requestId: String,
    val accepted: Boolean,
)

/**
 * La richiesta di esportazione non c'e' piu' da decidere: scaduta, gia'
 * decisa, o non dell'utente (`POST /api/cs/exapp/export/decide` risponde 404 o
 * 409). Non si riprova: serve una richiesta nuova dall'app esterna.
 */
class ExportRequestGoneException(val status: Int) :
    Exception("Richiesta di esportazione scaduta o gia' gestita (HTTP $status)")

/**
 * Third-party app approval endpoints. Mirrors Flutter `getExApp`,
 * `getExternalAppPermissions`, `addExtAppPermission`, `removeExtAppPermission`
 * in `lib/api/api.dart` (~lines 1120–1174).
 *
 * The `image` field on [ExAppModel] is the raw PocketBase file token; callers
 * are responsible for building the absolute URL (Flutter does this inline via
 * `pb.files.getUrl` — KMP keeps the model wire-shaped so SwiftUI/Android can
 * build the URL using their own base URL helpers).
 */
class ExAppsApi(
    private val pb: PocketBaseClient,
    private val client: HttpClient,
) {

    @Throws(Exception::class)
    suspend fun getExApp(id: String): ExAppModel =
        pb.getOne("ex_apps", id)

    /** First record matching (user, ex_app) or null. */
    @Throws(Exception::class)
    suspend fun getGrantedPermissions(userId: String, appId: String): ExGrantedPermissionsModel? =
        pb.fullList<ExGrantedPermissionsModel>(
            "ex_granted_permissions",
            filter = "user='$userId' && ex_app='$appId'",
        ).firstOrNull()

    internal suspend fun create(body: ExGrantedPermissionsCreateBody): ExGrantedPermissionsModel =
        pb.create("ex_granted_permissions", body)

    @Throws(Exception::class)
    suspend fun patchPermissions(id: String, perms: List<String>): ExGrantedPermissionsModel =
        pb.update("ex_granted_permissions", id, ExGrantedPermissionsPatchBody(perms))

    /**
     * `POST /api/cs/exapp/export/decide`: il socio approva o rifiuta
     * l'esportazione dei suoi dati. Passa dal client condiviso, quindi per un
     * utente `watched` porta la prova di integrita' come ogni altra API; ai
     * `blocked` e' consentita (sta nell'allowlist del server).
     *
     * 404 e 409 (scaduta, gia' decisa) diventano [ExportRequestGoneException]:
     * il client non usa `expectSuccess`, quindi senza questo controllo il
     * JSON d'errore passerebbe per una risposta riuscita.
     */
    @Throws(Exception::class)
    suspend fun decideExport(requestId: String, accepted: Boolean) {
        val response = client.post("/api/cs/exapp/export/decide") {
            contentType(ContentType.Application.Json)
            setBody(ExportDecisionBody(requestId, accepted))
        }
        val status = response.status.value
        if (status == 404 || status == 409) throw ExportRequestGoneException(status)
        if (status !in 200..299) error("export/decide: HTTP $status")
    }

    /**
     * Risponde a una richiesta di esportazione: prima la decisione sul nostro
     * server, poi — solo se l'ha registrata — l'avviso all'app esterna con
     * `{"accepted", "request_id"}`. Vedi `ExAppsRepository.answerDataExport`.
     *
     * @return true se l'app esterna e' stata avvisata.
     */
    @Throws(Exception::class)
    suspend fun answerDataExport(requestId: String, callbackUrl: String, accepted: Boolean): Boolean {
        decideExport(requestId, accepted)
        return runCatching { postCallback(callbackUrl, accepted, requestId) }.isSuccess
    }

    /**
     * POSTs `{"accepted": <bool>}` to the third-party callback URL, plus
     * `request_id` when it's a data export.
     *
     * L'host e' di un terzo: niente `Authorization` (nemmeno quello che
     * [it.mensa.shared.api.AuthPlugin] metterebbe a un sottodominio di
     * mensa.it, da cui [SkipAuthAttribute]) e niente prova di integrita'
     * (l'`IntegrityGuard` agisce solo sul nostro server). Il token non deve
     * uscire verso un host arbitrario.
     */
    @Throws(Exception::class)
    suspend fun postCallback(url: String, accepted: Boolean, requestId: String? = null) {
        client.post(url) {
            attributes.put(SkipAuthAttribute, true)
            headers { remove(HttpHeaders.Authorization) }
            contentType(ContentType.Application.Json)
            setBody(CallbackBody(accepted, requestId))
        }
    }
}
