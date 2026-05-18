package it.mensa.shared.auth.oidc

import it.mensa.shared.model.UserModel
import kotlinx.serialization.Serializable

@Serializable
data class OidcTokenResponse(
    val access_token: String = "",
    val refresh_token: String? = null,
    val id_token: String? = null,
    val expires_in: Long = 0L,
    val token_type: String = "Bearer",
    // PocketBase-style optional payload: if the /auth-with-zitadel endpoint
    // bundles the user record alongside the OIDC tokens we take it directly.
    val record: UserModel? = null,
)

@Serializable
data class OidcDiscovery(
    val issuer: String = "",
    val token_endpoint: String = "",
    val jwks_uri: String? = null,
)

@Serializable
data class OidcSession(
    val accessToken: String,
    val refreshToken: String,
    val idToken: String?,
    val expiresAtEpochSeconds: Long,
    val issuer: String,
    val clientId: String,
    val tokenEndpoint: String,
)
