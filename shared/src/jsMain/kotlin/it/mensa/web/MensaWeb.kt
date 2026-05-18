@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.MensaSdk
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.auth.AuthState
import it.mensa.shared.db.DriverFactory
import it.mensa.shared.di.initializeMensaDatabase
import it.mensa.shared.i18n.I18n
import kotlinx.browser.window
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

/**
 * Public JS-facing entry point. The Astro+React frontend imports this
 * package via the emitted npm artifact (kotlin js(IR) library distribution,
 * `outputModuleName = "shared"`) and constructs a single [MensaWebSdk]
 * once at app boot, then accesses feature roots from it.
 *
 * Constraints — all `@JsExport`-annotated members live in this file:
 *  - No `Long` in the public surface (Kotlin/JS exports it as `BigInt`).
 *    Use `Int` for small values, `Double` for epoch ms.
 *  - No `Char`, no `Throwable` return types, no `sealed` exposed directly.
 *  - Suspend functions → `kotlin.js.Promise` via `scope.promise { }`.
 *  - Flows → callback-based `subscribe*` returning an `unsubscribe()` thunk.
 *
 * Keep this file SMALL: this is the bootstrap façade only. Each repository
 * (events, deals, sigs, members, ...) gets its own `MensaWeb<Feature>` class
 * in a sibling file when added.
 */
@JsExport
class MensaWebSdk {
    // Default dispatcher (browser microtask + event loop) suffices: every
    // suspend the façade calls is I/O-bound (HTTP, IDB-backed Worker DB) and
    // browsers have no real thread-pool semantics. SupervisorJob isolates
    // failures so a crashing subscribe-Flow won't tear down init().
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // initialize() is idempotent at the JS bridge level (web/src/lib/mensa.ts
    // memoizes the first Promise). We additionally cache the Deferred here so
    // every per-feature `awaitReady()` can `await` the same Promise instance
    // without re-triggering Koin.startKoin (which throws on second invocation).
    private var initOnce: Deferred<Unit>? = null

    /**
     * Idempotency: [MensaSdk.initKoin] uses Koin's `startKoin` which throws
     * if invoked twice. Hosts must call [initialize] exactly once. The Astro
     * app shell does this in a top-level `Astro.glob`-equivalent bootstrap
     * before mounting any React island.
     */
    fun initialize(): Promise<Unit> = scope.promise { awaitReady() }

    internal suspend fun awaitReady() {
        val existing = initOnce
        if (existing != null) return existing.await()
        val d = scope.async {
            MensaSdk.initKoin()
            val factory = KoinPlatform.getKoin().get<DriverFactory>()
            initializeMensaDatabase(factory)
            KoinPlatform.getKoin().get<AuthRepository>().init()
            // Bootstrap Tolgee i18n catalog (mirrors iOS RootView.swift:20).
            // Without this the catalog never loads and every t() returns the
            // fallback (= the raw key for notifications). Failures are swallowed
            // by TranslationLoader.bootstrap so init never fails on i18n.
            val preferred = runCatching {
                window.navigator.language.ifBlank { "it" }
            }.getOrDefault("it")
            runCatching {
                KoinPlatform.getKoin().get<I18n>().bootstrap(preferred)
            }
            Unit
        }
        initOnce = d
        d.await()
    }

    val i18n: MensaWebI18n = MensaWebI18n(scope, this)
    val auth: MensaWebAuth = MensaWebAuth(scope, this)
    val events: MensaWebEvents = MensaWebEvents(scope, this)
    val deals: MensaWebDeals = MensaWebDeals(scope, this)
    val notifications: MensaWebNotifications = MensaWebNotifications(scope, this)
    val regSoci: MensaWebRegSoci = MensaWebRegSoci(scope, this)
    val sigs: MensaWebSigs = MensaWebSigs(scope, this)
    val tickets: MensaWebTickets = MensaWebTickets(scope, this)
    val receipts: MensaWebReceipts = MensaWebReceipts(scope, this)
    val localOffices: MensaWebLocalOffices = MensaWebLocalOffices(scope, this)
    val quid: MensaWebQuid = MensaWebQuid(scope, this)
    val podcasts: MensaWebPodcasts = MensaWebPodcasts(scope, this)
    val documents: MensaWebDocuments = MensaWebDocuments(scope, this)
    val boutique: MensaWebBoutique = MensaWebBoutique(scope, this)
    val addons: MensaWebAddons = MensaWebAddons(scope, this)
    val search: MensaWebSearch = MensaWebSearch(scope, this)
    val devices: MensaWebDevices = MensaWebDevices(scope, this)
    val positions: MensaWebPositions = MensaWebPositions(scope, this)
    val metadata: MensaWebMetadata = MensaWebMetadata(scope, this)
}

@JsExport
class MensaWebAuth internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    // Lazily resolve through Koin so MensaWebSdk's field initializer doesn't
    // require Koin to be started yet (it isn't until initialize() runs).
    private val repo: AuthRepository get() = KoinPlatform.getKoin().get()

    /**
     * Mirrors [AuthRepository.login]. JS callers receive a rejected Promise on
     * failure (Kotlin's `Result.getOrThrow()` → thrown exception → rejected
     * Promise via `scope.promise { }`).
     */
    fun login(email: String, password: String): Promise<MensaWebUser> = scope.promise {
        sdk.awaitReady()
        val user = repo.login(email, password).getOrThrow()
        user.toJs()
    }

    fun logout(): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        repo.logout()
    }

    /**
     * Subscribe to the auth state as a string discriminator. We flatten the
     * sealed [AuthState] hierarchy to "Unknown" | "Anonymous" | "Authenticated"
     * because `@JsExport` of sealed classes is brittle for JS-side
     * `instanceof` checks. Returns a thunk that cancels the underlying Job.
     */
    fun subscribeAuthState(callback: (state: String) -> Unit): () -> Unit {
        val job: Job = scope.launch {
            sdk.awaitReady()
            repo.authState.collect { state ->
                callback(when (state) {
                    is AuthState.Unknown -> "Unknown"
                    is AuthState.Anonymous -> "Anonymous"
                    is AuthState.Authenticated -> "Authenticated"
                })
            }
        }
        return { job.cancel() }
    }

    /**
     * Subscribe to the current user as a POJO (or `null` when signed out).
     * Returns a thunk that cancels the underlying Job.
     */
    fun subscribeCurrentUser(callback: (user: MensaWebUser?) -> Unit): () -> Unit {
        val job: Job = scope.launch {
            sdk.awaitReady()
            repo.currentUser.collect { user ->
                callback(user?.toJs())
            }
        }
        return { job.cancel() }
    }
}

/**
 * Plain JS POJO mirror of [UserModel]. Field types deliberately narrowed:
 *  - `expireMembershipMs` is `Double` epoch milliseconds (no `Long` in JsExport).
 *  - `createdMs` mirrors `created` for the same reason.
 *  - `powers`/`addons` are `Array<String>` (not Kotlin `List`) because Kotlin/JS
 *    exports `List` as the opaque `KtList`; an `Array` is idiomatic JS.
 */
@JsExport
data class MensaWebUser(
    val id: String,
    val username: String,
    val name: String,
    val avatar: String,
    val email: String,
    val expireMembershipMs: Double,
    val powers: Array<String>,
    val addons: Array<String>,
    val isMembershipActive: Boolean,
    val createdMs: Double,
)

internal fun UserModel.toJs(): MensaWebUser = MensaWebUser(
    id = id,
    username = username,
    name = name,
    avatar = avatar,
    email = email,
    expireMembershipMs = expireMembership.toEpochMilliseconds().toDouble(),
    powers = powers.toTypedArray(),
    addons = addons.toTypedArray(),
    isMembershipActive = isMembershipActive,
    createdMs = created.toEpochMilliseconds().toDouble(),
)
