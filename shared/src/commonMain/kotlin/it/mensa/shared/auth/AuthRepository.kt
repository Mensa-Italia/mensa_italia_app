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
    private val credentials: ICredentialStore,
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

    /**
     * Rifa' il login per intero con le credenziali salvate: rimanda email e
     * password a `/api/cs/auth-with-zitadel`, esattamente come farebbe l'utente
     * uscendo e rientrando.
     *
     * Esiste perche' un `/api/cs/me` non e' bastato: con la tessera rinnovata,
     * il tasto "Ho rinnovato" non sbloccava niente mentre un logout seguito da
     * un login sbloccava. Il record allegato alla risposta di login e' quindi
     * piu' fresco di quello che torna da `/me`.
     *
     * Torna null quando non c'e' niente da rimandare (accesso con passkey,
     * credenziali mai salvate) o quando il login non riesce: in entrambi i casi
     * chi chiama ricade su [refreshCurrentUser].
     *
     * `method = null` di proposito: questo non e' un accesso fatto dall'utente,
     * e marcarlo come PASSWORD farebbe comparire la proposta di attivazione
     * della passkey a ogni avvio (vedi [it.mensa.shared.auth.passkey.PasskeyEnrollmentGate]).
     */
    suspend fun reloginWithStoredCredentials(): UserModel? {
        val stored = runCatching { credentials.read() }.getOrNull() ?: return null
        return runCatching {
            adoptTokens(
                tokens = api.loginWithZitadel(stored.email, stored.password),
                method = null,
                source = "/auth-with-zitadel (relogin)",
            )
        }.getOrNull()
    }

    /**
     * Rilegge il socio nel modo piu' forte disponibile: rifa' il login se le
     * credenziali ci sono, altrimenti si limita a `/api/cs/me`.
     *
     * E' il punto d'ingresso per il tasto "Ho rinnovato" e per qualunque
     * pull-to-refresh che mostri dati della tessera. Torna null solo quando
     * nessuna delle due strade ha prodotto un record.
     */
    suspend fun reloadUser(): UserModel? =
        reloginWithStoredCredentials() ?: refreshCurrentUser()

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
        // Login vero a ogni avvio, quando le credenziali ci sono. E' la
        // richiesta esplicita: la sessione ripristinata da disco piu' un
        // `/api/cs/me` non bastava a far arrivare la scadenza tessera
        // aggiornata. Se il login non riesce — offline, password cambiata,
        // server giu' — si prosegue con la sessione persistita di sempre, che
        // resta la strada normale per chi entra con la passkey.
        if (reloginWithStoredCredentials() != null) return

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
        val decoded = cachedJson?.let {
            runCatching { json.decodeFromString<UserModel>(it) }.getOrNull()
        }
        val cachedUser = decoded.takeIfUsable()
        if (cachedJson != null && cachedUser == null) {
            // Cache inservibile: buttala, o tornerebbe identica al prossimo
            // avvio. Il caso vero e' l'aggiornamento dalla vecchia app
            // Flutter, che ha lasciato qui un record con altre chiavi.
            runCatching { db.keyValueQueries.deleteById(KEY_CURRENT_USER) }
        }
        _currentUser.value = DemoIdentity.redact(cachedUser)
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
     * Ritorna il record aggiornato — e non null — quando il server ha
     * risposto, *anche se* quel record dice ancora tessera scaduta. Torna null
     * solo quando la lettura non e' avvenuta: nessuna sessione, rete muta, o
     * sessione morta (in quest'ultimo caso lo stato e' gia' passato ad
     * anonimo e chi chiama si ritrova sulla login).
     *
     * La differenza fra i due casi non e' un dettaglio: e' cio' che permette a
     * chi chiama di distinguere "ho chiesto, il rinnovo non risulta ancora" da
     * "non sono riuscito a chiedere". Il muro del rinnovo la buttava via e il
     * suo tasto "Ho rinnovato" sembrava morto in entrambi i casi.
     */
    suspend fun refreshCurrentUser(): UserModel? {
        if (_authState.value !is AuthState.Authenticated) return null
        return when (val outcome = fetchMeOrWipe()) {
            is MeOutcome.Ok -> {
                // Un record senza `id` non lo si pubblica e non lo si mette in
                // cache, nemmeno se la chiamata e' andata a buon fine: questo
                // gira a ogni ritorno in foreground, quindi pubblicarlo
                // vorrebbe dire sostituire un socio buono con uno vuoto.
                val user = outcome.user.takeIfUsable() ?: return null
                db.keyValueQueries.insertOrReplace(
                    key = KEY_CURRENT_USER,
                    value_ = json.encodeToString(UserModel.serializer(), user),
                )
                DemoIdentity.redact(user).also { _currentUser.value = it }
            }
            MeOutcome.Offline -> null
            MeOutcome.Wiped -> null
        }
    }

    /**
     * Il record, oppure null se non e' utilizzabile.
     *
     * Un `UserModel` senza `id` non e' un socio con qualche campo mancante: e'
     * un decode fallito in silenzio. Ogni campo del modello ha un default
     * vuoto e il parser gira con `ignoreUnknownKeys` e `coerceInputValues`,
     * quindi un JSON con altre chiavi — quello lasciato dalla vecchia app
     * Flutter — non solleva nessuna eccezione: produce un oggetto con tutti i
     * campi vuoti, che `runCatching { }.getOrNull()` considera un successo.
     *
     * Il risultato lo ha visto un socio: "Buonasera Socio" (il fallback di
     * Today per nome vuoto) e la tessera senza dati, finche' non ha fatto
     * logout e login. Il logout risolveva perche' riscriveva il record nel
     * formato giusto.
     *
     * Il controllo e' su `id` e non su `name` di proposito: `id` e' cio' su
     * cui si costruiscono il QR della tessera e ogni lookup, quindi vuoto vuol
     * dire inservibile in senso proprio. Non e' un fallback difensivo su un
     * campo che il backend riempie gia': e' il rifiuto di pubblicare qualcosa
     * che non e' mai stato letto davvero.
     */
    private fun UserModel?.takeIfUsable(): UserModel? = this?.takeIf { it.id.isNotBlank() }

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
        runCatching { credentials.clear() }
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
        ).also {
            // Da qui in poi l'app sa rifare questo stesso login da sola.
            // Vedi [CredentialStore] per dove finiscono e perche'.
            runCatching { credentials.save(email, password) }
        }
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
        method: LoginMethod?,
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
        runCatching { credentials.clear() }
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
        // Le credenziali NON si toccano qui. Questo e' il ramo "non c'e' una
        // sessione su disco", che si imbocca anche dopo un avvio offline in cui
        // il relogin non e' riuscito: cancellarle li' vorrebbe dire perderle per
        // sempre al primo avvio senza rete. Si cancellano solo con un logout
        // esplicito e in wipeAndGoAnonymous, cioe' quando e' il server a dire
        // che quell'identita' non e' piu' valida.
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
