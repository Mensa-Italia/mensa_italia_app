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

    suspend fun getExApp(appId: String): ExAppModel =
        api.getExApp(appId)

    /** Current user's consent record for [appId], or null if none exists. */
    suspend fun getGrantedPermissions(appId: String): ExGrantedPermissionsModel? {
        val userId = auth.currentUser.value?.id ?: error("Not authenticated")
        return api.getGrantedPermissions(userId, appId)
    }

    /**
     * Grant [perms] on [appId]. Creates a record if none exists; otherwise
     * PATCHes with the deduped union of existing + new permissions (matches
     * Flutter's `toSet().toList()`).
     */
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
    suspend fun removePermissions(appId: String, perms: List<String>): ExGrantedPermissionsModel? {
        val userId = auth.currentUser.value?.id ?: error("Not authenticated")
        val existing = api.getGrantedPermissions(userId, appId) ?: return null
        val filtered = existing.permissions.filter { it !in perms }
        return api.patchPermissions(existing.id, filtered)
    }

    /** Notify the third-party callback URL of the user's decision. */
    suspend fun postCallback(url: String, accepted: Boolean) =
        api.postCallback(url, accepted)
}
