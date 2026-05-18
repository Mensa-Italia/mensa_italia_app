package it.mensa.shared.api.endpoints

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import it.mensa.shared.api.PocketBaseClient
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

@Serializable
internal data class CallbackBody(val accepted: Boolean)

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

    suspend fun getExApp(id: String): ExAppModel =
        pb.getOne("ex_apps", id)

    /** First record matching (user, ex_app) or null. */
    suspend fun getGrantedPermissions(userId: String, appId: String): ExGrantedPermissionsModel? =
        pb.fullList<ExGrantedPermissionsModel>(
            "ex_granted_permissions",
            filter = "user='$userId' && ex_app='$appId'",
        ).firstOrNull()

    internal suspend fun create(body: ExGrantedPermissionsCreateBody): ExGrantedPermissionsModel =
        pb.create("ex_granted_permissions", body)

    suspend fun patchPermissions(id: String, perms: List<String>): ExGrantedPermissionsModel =
        pb.update("ex_granted_permissions", id, ExGrantedPermissionsPatchBody(perms))

    /**
     * POSTs `{"accepted": <bool>}` to the third-party callback URL. We strip
     * the PocketBase Authorization header here so we don't leak the user's
     * token to an arbitrary external host.
     */
    suspend fun postCallback(url: String, accepted: Boolean) {
        client.post(url) {
            headers { remove(HttpHeaders.Authorization) }
            contentType(ContentType.Application.Json)
            setBody(CallbackBody(accepted))
        }
    }
}
