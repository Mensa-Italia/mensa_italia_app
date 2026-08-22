package it.mensa.shared.auth

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import it.mensa.shared.api.endpoints.AuthApi
import it.mensa.shared.auth.oidc.JwtDecoder
import it.mensa.shared.auth.oidc.OidcDiscoveryCache
import it.mensa.shared.auth.oidc.OidcSession
import it.mensa.shared.auth.oidc.OidcTokenResponse
import it.mensa.shared.auth.oidc.TokenRefresher
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.di.wipeAllUserData
import it.mensa.shared.demo.DemoIdentity
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

private const val KEY_CURRENT_USER = "auth.current_user"
private const val EXPIRES_SAFETY_MARGIN_SECONDS = 30L

class AuthRepository(
    private val api: AuthApi,
    private val tokenStore: ITokenStore,
    private val db: MensaDatabase,
    private val json: Json,
    private val discovery: OidcDiscoveryCache,
    private val tokenRefresher: TokenRefresher,
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserModel?>(null)
    val currentUser: StateFlow<UserModel?> = _currentUser.asStateFlow()

    private val _lastLoginMethod = MutableStateFlow<LoginMethod?>(null)

    /**
     * Come l'utente e' entrato *in questa esecuzione dell'app*, o null se la
     * sessione e' stata ripristinata da disco a freddo.
     *
     * Volutamente non persistito: serve solo a decidere se proporre
     * l'attivazione della passkey dopo un accesso con password, e un cold start
     * con sessione ripristinata non e' un accesso con password. Vedi
     * [it.mensa.shared.auth.passkey.PasskeyEnrollmentGate].
     */
    val lastLoginMethod: StateFlow<LoginMethod?> = _lastLoginMethod.asStateFlow()

    init {
        tokenRefresher.onSessionLost = {
            AuthHolder.session = null
            _authState.value = AuthState.Anonymous
            _currentUser.value = null
        }
    }

    /**
     * Bootstrap della sessione persistita. Una `suspend fun` esposta a Swift
     * senza `@Throws` termina il processo se lancia un'eccezione non
     * `CancellationException` (runtime K/N), quindi catchamo tutto e ricadiamo
     * su utente anonimo: l'app continua, l'utente vede la login screen.
     *
     * Caso reale che innesca il fallback: simulator/build senza code signing
     * → Keychain query torna `errSecMissingEntitlement (-34018)` →
     * `KeychainSettings` lancia `IllegalStateException`. Su device firmato
     * questo non capita, ma il fallback ci protegge anche da altre rotture
     * del Keychain (corrupt entry, accessibilità cambiata, ecc.).
     */
    suspend fun init() {
        try {
            initImpl()
        } catch (t: Throwable) {
            println("MENSA_AUTH_INIT_FAILED: ${t::class.simpleName}: ${t.message}")
            runCatching { goAnonymous() }
        }
    }

    private suspend fun initImpl() {
        val stored = readStoredSession()
        if (stored == null) {
            goAnonymous()
            return
        }
        AuthHolder.session = stored

        // Proactive refresh on cold start if the access token is dead or near-dead.
        val sessionAfterMaybeRefresh: OidcSession = if (AuthHolder.isExpiringSoon()) {
            try {
                tokenRefresher.refresh()
            } catch (e: ResponseException) {
                if (e.isAuthRejected() || e.response.status == HttpStatusCode.BadRequest) {
                    // 400 invalid_grant / 401 / 403 from the auth server →
                    // the refresh_token is dead. Session is irrecoverable.
                    wipeAndGoAnonymous()
                    return
                }
                // 5xx or other oddity: auth server hiccup, not our fault.
                // Keep the stale session and let /api/cs/me drive the retry.
                stored
            } catch (_: Throwable) {
                // Offline / transport: keep the cached session.
                stored
            }
        } else stored

        _authState.value = AuthState.Authenticated(sessionAfterMaybeRefresh.accessToken)

        // Show last-known user from cache immediately for snappy UI, then
        // canonicalise via /api/cs/me. The OIDC refresh response carries
        // only tokens, so /me is the only path that picks up server-side
        // membership / powers / addons changes.
        val cachedJson = db.keyValueQueries.selectById(KEY_CURRENT_USER).awaitAsOneOrNull()?.value_
        _currentUser.value = DemoIdentity.redact(
            cachedJson?.let { runCatching { json.decodeFromString<UserModel>(it) }.getOrNull() },
        )
        when (val outcome = fetchMeOrWipe()) {
            is MeOutcome.Ok -> {
                db.keyValueQueries.insertOrReplace(
                    key = KEY_CURRENT_USER,
                    value_ = json.encodeToString(UserModel.serializer(), outcome.user),
                )
                _currentUser.value = DemoIdentity.redact(outcome.user)
            }
            MeOutcome.Offline -> Unit   // keep cached UI, will retry next init/login
            MeOutcome.Wiped -> Unit     // wipeAndGoAnonymous already nuked state
        }
    }

    /**
     * Rilegge il record utente dal server e lo ripubblica su [currentUser].
     *
     * Serve perche' `/api/cs/me` e' l'unico posto da cui arrivano scadenza
     * tessera, `powers` e `addons`: la risposta di refresh OIDC porta solo i
     * token, e il record allegato al login e' vecchio quanto il login. Finora
     * lo si chiamava solo dentro [init], cioe' un avvio a freddo: un'app
     * rimasta aperta per giorni, o un `/me` fallito una volta perche' offline,
     * continuavano a mostrare la tessera con i dati di allora.
     *
     * Da chiamare a ogni ritorno in foreground e da ogni pull-to-refresh che
     * mostra dati del socio. E' l'alternativa a tenersi email e password sul
     * telefono per "rifare il login" a ogni avvio: quella non aggiornerebbe
     * niente di piu' (il record lo scrive comunque il server, non le
     * credenziali) e in cambio metterebbe una password in chiaro sul
     * dispositivo. Il refresh_token nel Keychain / EncryptedSharedPreferences
     * fa gia' il lavoro di tenere viva la sessione.
     *
     * Ritorna il record aggiornato, o null se non c'e' sessione, se la rete
     * non ha risposto o se la sessione era morta (in quest'ultimo caso lo
     * stato e' gia' passato ad anonimo).
     */
    suspend fun refreshCurrentUser(): UserModel? {
        if (_authState.value !is AuthState.Authenticated) return null
        return when (val outcome = fetchMeOrWipe()) {
            is MeOutcome.Ok -> {
                db.keyValueQueries.insertOrReplace(
                    key = KEY_CURRENT_USER,
                    value_ = json.encodeToString(UserModel.serializer(), outcome.user),
                )
                DemoIdentity.redact(outcome.user).also { _currentUser.value = it }
            }
            MeOutcome.Offline -> null
            MeOutcome.Wiped -> null
        }
    }

    /**
     * Strategy when /api/cs/me fails. Wipe is reserved for the single case
     * "the auth proxy itself says this identity is no longer welcome", i.e.
     * 401/403 from SVC even after a forced token refresh. Anything else —
     * 5xx from SVC, 4xx that isn't auth-related, network/transport failure —
     * is treated as transient and leaves the cached state intact.
     *
     *  - 401/403 on first /me  → forceRefresh, retry once.
     *      - retry 200          → use it
     *      - retry 401/403      → wipe (account is dead)
     *      - retry anything else→ Offline (don't punish for a hiccup)
     *      - forceRefresh dies on a ResponseException → wipe (refresh_token revoked)
     *      - forceRefresh dies on anything else (e.g. token endpoint unreachable)
     *                          → Offline
     *  - Other ResponseException (4xx ≠ 401/403, 5xx) → Offline
     *  - Throwable (IO, timeout, cancellation)        → Offline
     */
    private suspend fun fetchMeOrWipe(): MeOutcome {
        try {
            return MeOutcome.Ok(api.me())
        } catch (e: ResponseException) {
            if (!e.isAuthRejected()) return MeOutcome.Offline
        } catch (_: Throwable) {
            return MeOutcome.Offline
        }

        // /me returned 401/403: force a fresh access token and try once more.
        val refreshed = try {
            tokenRefresher.forceRefresh()
        } catch (_: ResponseException) {
            // The auth server itself rejected the refresh — refresh_token is dead.
            wipeAndGoAnonymous()
            return MeOutcome.Wiped
        } catch (_: Throwable) {
            // Network failure reaching the auth server — don't punish, retry later.
            return MeOutcome.Offline
        }
        _authState.value = AuthState.Authenticated(refreshed.accessToken)

        return try {
            MeOutcome.Ok(api.me())
        } catch (e: ResponseException) {
            if (e.isAuthRejected()) {
                wipeAndGoAnonymous()
                MeOutcome.Wiped
            } else {
                MeOutcome.Offline
            }
        } catch (_: Throwable) {
            MeOutcome.Offline
        }
    }

    private fun ResponseException.isAuthRejected(): Boolean {
        val s = response.status
        return s == HttpStatusCode.Unauthorized || s == HttpStatusCode.Forbidden
    }

    private sealed interface MeOutcome {
        data class Ok(val user: UserModel) : MeOutcome
        data object Offline : MeOutcome
        data object Wiped : MeOutcome
    }

    private suspend fun wipeAndGoAnonymous() {
        AuthHolder.session = null
        runCatching { tokenStore.clear() }
        runCatching { wipeAllUserData() }
        _authState.value = AuthState.Anonymous
        _currentUser.value = null
        _lastLoginMethod.value = null
    }

    suspend fun login(email: String, password: String): Result<UserModel> = runCatching {
        adoptTokens(
            tokens = api.loginWithZitadel(email, password),
            method = LoginMethod.PASSWORD,
            source = "/auth-with-zitadel",
        )
    }

    /**
     * Rende attiva una sessione a partire dai token restituiti dal backend.
     *
     * Condiviso fra login con password e login con passkey: i due endpoint
     * differiscono solo per come l'utente ha dimostrato la propria identita',
     * non per cosa si fa con i token che ne escono. [source] entra nei messaggi
     * d'errore per dire quale dei due ha risposto male.
     *
     * `internal` perche' il chiamante dal lato passkey e'
     * [it.mensa.shared.auth.passkey.PasskeyRepository], nello stesso modulo: la
     * proprieta' della sessione resta qui, l'HTTP resta la'.
     */
    internal suspend fun adoptTokens(
        tokens: OidcTokenResponse,
        method: LoginMethod,
        source: String,
    ): UserModel {
        require(tokens.access_token.isNotBlank()) { "Empty access_token in $source response" }
        require(!tokens.refresh_token.isNullOrBlank()) { "Empty refresh_token in $source response" }

        val session = buildSession(tokens)
        persist(session)
        _authState.value = AuthState.Authenticated(session.accessToken)

        // Entrambi gli endpoint di login allegano il record utente ai token OIDC;
        // solo la risposta di refresh lo omette (gestito in init).
        val user = requireNotNull(tokens.record) { "$source response missing user record" }
        val userJsonStr = json.encodeToString(UserModel.serializer(), user)
        db.keyValueQueries.insertOrReplace(key = KEY_CURRENT_USER, value_ = userJsonStr)
        // Il record vero e' gia' su DB: da qui in poi si espone solo cio' che
        // la UI deve mostrare, che in modalita' screenshot e' un segnaposto.
        _currentUser.value = DemoIdentity.redact(user)
        _lastLoginMethod.value = method

        return user
    }

    suspend fun logout() {
        AuthHolder.session = null
        tokenStore.clear()
        runCatching { wipeAllUserData() }
        _authState.value = AuthState.Anonymous
        _currentUser.value = null
        _lastLoginMethod.value = null
    }

    private suspend fun buildSession(tokens: OidcTokenResponse): OidcSession {
        val payload: JsonObject = JwtDecoder.payload(tokens.access_token)
        val issuer = JwtDecoder.string(payload, "iss")
            ?: error("access_token missing iss claim")
        val clientId = JwtDecoder.string(payload, "client_id")
            ?: error("access_token missing client_id claim")
        val discovered = discovery.get(issuer)
        require(discovered.token_endpoint.isNotBlank()) { "OIDC discovery for $issuer returned empty token_endpoint" }

        return OidcSession(
            accessToken = tokens.access_token,
            refreshToken = tokens.refresh_token!!,
            idToken = tokens.id_token,
            expiresAtEpochSeconds = Clock.System.now().epochSeconds + tokens.expires_in - EXPIRES_SAFETY_MARGIN_SECONDS,
            issuer = issuer,
            clientId = clientId,
            tokenEndpoint = discovered.token_endpoint,
        )
    }

    private suspend fun persist(session: OidcSession) {
        AuthHolder.session = session
        tokenStore.save(json.encodeToString(OidcSession.serializer(), session))
    }

    private suspend fun readStoredSession(): OidcSession? {
        val raw = tokenStore.read() ?: return null
        return runCatching { json.decodeFromString(OidcSession.serializer(), raw) }.getOrNull()
    }

    private suspend fun goAnonymous() {
        AuthHolder.session = null
        runCatching { tokenStore.clear() }
        _authState.value = AuthState.Anonymous
        _currentUser.value = null
        _lastLoginMethod.value = null
        runCatching { db.keyValueQueries.deleteById(KEY_CURRENT_USER) }
    }
}

sealed interface AuthState {
    data object Unknown : AuthState
    data object Anonymous : AuthState
    data class Authenticated(val token: String) : AuthState
}

/** Come e' avvenuto l'accesso corrente. Vedi [AuthRepository.lastLoginMethod]. */
enum class LoginMethod { PASSWORD, PASSKEY }
