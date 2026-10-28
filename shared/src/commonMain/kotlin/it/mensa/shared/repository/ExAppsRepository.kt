package it.mensa.shared.repository

import it.mensa.shared.api.endpoints.ExAppsApi
import it.mensa.shared.api.endpoints.ExGrantedPermissionsCreateBody
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.model.ExAppModel
import it.mensa.shared.model.ExGrantedPermissionsModel

/**
 * Third-party data-access approval flow. Mirrors Flutter's
 * `getExApp` / `getExternalAppPermissions` / `addExtAppPermission` /
 * `removeExtAppPermission` in `lib/api/api.dart` plus the callback POST
 * performed by the `bottom_check_identity` widget on approve/deny.
 *
 * Network-only — no local caching. Approval is a one-shot user gesture
 * and the consent record is read fresh each time the prompt is shown.
 */
class ExAppsRepository(
    private val api: ExAppsApi,
    private val auth: AuthRepository,
) {

    @Throws(Exception::class)
    suspend fun getExApp(appId: String): ExAppModel =
        api.getExApp(appId)

    /** Current user's consent record for [appId], or null if none exists. */
    @Throws(Exception::class)
    suspend fun getGrantedPermissions(appId: String): ExGrantedPermissionsModel? {
        val userId = auth.currentUser.value?.id ?: error("Not authenticated")
        return api.getGrantedPermissions(userId, appId)
    }

    /**
     * Grant [perms] on [appId]. Creates a record if none exists; otherwise
     * PATCHes with the deduped union of existing + new permissions (matches
     * Flutter's `toSet().toList()`).
     */
    @Throws(Exception::class)
    suspend fun addPermissions(appId: String, perms: List<String>): ExGrantedPermissionsModel {
        val userId = auth.currentUser.value?.id ?: error("Not authenticated")
        val existing = api.getGrantedPermissions(userId, appId)
        return if (existing == null) {
            api.create(
                ExGrantedPermissionsCreateBody(
                    user = userId,
                    exApp = appId,
                    permissions = perms.toSet().toList(),
                )
            )
        } else {
            val merged = (existing.permissions + perms).toSet().toList()
            api.patchPermissions(existing.id, merged)
        }
    }

    /**
     * Revoke [perms] from [appId]. No-op (returns null) when no record exists,
     * mirroring Flutter which guards on `externalApp != null`.
     */
    @Throws(Exception::class)
    suspend fun removePermissions(appId: String, perms: List<String>): ExGrantedPermissionsModel? {
        val userId = auth.currentUser.value?.id ?: error("Not authenticated")
        val existing = api.getGrantedPermissions(userId, appId) ?: return null
        val filtered = existing.permissions.filter { it !in perms }
        return api.patchPermissions(existing.id, filtered)
    }

    /** Notify the third-party callback URL of the user's decision. */
    @Throws(Exception::class)
    suspend fun postCallback(url: String, accepted: Boolean) =
        api.postCallback(url, accepted)

    /**
     * Risponde a una richiesta di esportazione dati (`data_export_confirmation`).
     *
     * Prima la decisione sul nostro server, poi — solo se il server l'ha
     * registrata — l'avviso all'app esterna con `{"accepted", "request_id"}`.
     * Se la richiesta e' scaduta o gia' decisa solleva
     * [it.mensa.shared.api.endpoints.ExportRequestGoneException] e l'app esterna
     * non viene avvisata di niente.
     *
     * La decisione e' l'atto che conta: l'app esterna estrae i dati dal
     * server con la sua chiave, il callback e' solo un avviso. Per questo un
     * callback che fallisce (app esterna irraggiungibile) non annulla niente e
     * non e' un errore: torna false al posto di true. Rifare tutto non sarebbe
     * possibile comunque, la richiesta risulterebbe gia' decisa.
     *
     * @return true se l'app esterna e' stata avvisata.
     */
    @Throws(Exception::class)
    suspend fun answerDataExport(requestId: String, callbackUrl: String, accepted: Boolean): Boolean =
        api.answerDataExport(requestId, callbackUrl, accepted)
}
