package it.mensa.shared.config

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import it.mensa.shared.api.endpoints.SettingsApi
import it.mensa.shared.db.MensaDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import it.mensa.shared.model.search.SearchType

/**
 * Server-driven feature switches, from the PocketBase `configs` collection.
 *
 * These gate whole sections of the app, so the default when we know nothing is
 * **off**: a flag that is missing, unparseable or not yet fetched hides its
 * feature. Shipping a section the association has switched off is the failure
 * that matters here; briefly hiding one that is on is not.
 *
 * Values are cached in SQLDelight so a cold start offline still honours the
 * last-known switches instead of falling back to "everything hidden".
 */
class FeatureFlags(
    private val settings: SettingsApi,
    private val db: MensaDatabase,
) {
    data class Flags(
        val publicArea: Boolean = false,
        val orgChart: Boolean = false,
        val localGroups: Boolean = false,
        val passkeys: Boolean = false,
        val donationLinkOnIos: Boolean = false,
        val donationLinkOnAndroid: Boolean = false,
        val donationStripeLink: String = "",
    ) {
        /**
         * Link esterno da aprire al posto della pagina di donazione nativa,
         * oppure null per usare quella nativa.
         *
         * Serve perche' Apple pretende l'iscrizione a un circuito per le
         * donazioni in-app che l'associazione non ha: dove il flag e' acceso,
         * la donazione esce dall'app invece di passare da Stripe dentro
         * l'app.
         *
         * Il link vuoto vale come flag spento e non e' prudenza: un flag
         * acceso senza link darebbe un pulsante che non apre niente, che e'
         * peggio della pagina nativa. La regola sta qui e non nelle due UI
         * perche' altrimenti andrebbe ricordata due volte.
         */
        val donationLinkIos: String? get() = donationStripeLink.takeIf { donationLinkOnIos && it.isNotBlank() }
        val donationLinkAndroid: String? get() = donationStripeLink.takeIf { donationLinkOnAndroid && it.isNotBlank() }

        /**
         * True se i risultati di ricerca di [type] vanno mostrati.
         *
         * I flag spegnevano solo le voci di menu, non la ricerca: con
         * `org_chart_enabled = false` l'organigramma spariva dal profilo ma i
         * suoi risultati continuavano a uscire nella ricerca globale, e
         * aprirli portava a una schermata che non doveva essere
         * raggiungibile. Stessa cosa per i gruppi locali.
         *
         * I tipi che non dipendono da nessun flag passano sempre.
         */
        fun allowsSearchType(type: String): Boolean = when (type) {
            SearchType.ORG -> orgChart
            SearchType.LINKTREE_LINK -> localGroups
            else -> true
        }
    }

    private val _flags = MutableStateFlow(Flags())
    val flags: StateFlow<Flags> = _flags.asStateFlow()

    val publicAreaEnabled: Boolean get() = _flags.value.publicArea
    val orgChartEnabled: Boolean get() = _flags.value.orgChart
    val localGroupsEnabled: Boolean get() = _flags.value.localGroups
    val passkeysEnabled: Boolean get() = _flags.value.passkeys

    /** Vedi [Flags.donationLinkIos]. */
    val donationLinkIos: String? get() = _flags.value.donationLinkIos
    /** Vedi [Flags.donationLinkAndroid]. */
    val donationLinkAndroid: String? get() = _flags.value.donationLinkAndroid

    /** Vedi [Flags.allowsSearchType]. */
    fun allowsSearchType(type: String): Boolean = _flags.value.allowsSearchType(type)

    /**
     * Restores the cached values, then refreshes from the backend. Safe to call
     * on every launch and after login; never throws — a failure leaves whatever
     * was already loaded in place.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun bootstrap() {
        try {
            readCached()?.let { _flags.value = it }
        } catch (_: Throwable) {
            // Cache miss or unreadable DB — the network pass below still runs.
        }
        try {
            val configs = settings.configs()
            val fresh = Flags(
                publicArea = configs[KEY_PUBLIC_AREA].toFlag(),
                orgChart = configs[KEY_ORG_CHART].toFlag(),
                localGroups = configs[KEY_LOCAL_GROUPS].toFlag(),
                passkeys = configs[KEY_PASSKEYS].toFlag(),
                donationLinkOnIos = configs[KEY_DONATION_LINK_IOS].toFlag(),
                donationLinkOnAndroid = configs[KEY_DONATION_LINK_ANDROID].toFlag(),
                donationStripeLink = configs[KEY_DONATION_STRIPE_LINK]?.trim().orEmpty(),
            )
            _flags.value = fresh
            writeCached(fresh)
        } catch (_: Throwable) {
            // Offline or backend down: keep the cached values.
        }
    }

    private suspend fun readCached(): Flags? {
        val raw = db.keyValueQueries.selectById(CACHE_KEY).awaitAsOneOrNull()?.value_ ?: return null
        val parts = raw.split(',')
        // Length check doubles as a version guard: a cache written by an older
        // build with fewer flags is discarded rather than read short.
        if (parts.size != 6) return null
        return Flags(
            publicArea = parts[0] == "1",
            orgChart = parts[1] == "1",
            localGroups = parts[2] == "1",
            passkeys = parts[3] == "1",
            donationLinkOnIos = parts[4] == "1",
            donationLinkOnAndroid = parts[5] == "1",
            donationStripeLink = db.keyValueQueries
                .selectById(CACHE_KEY_DONATION_LINK).awaitAsOneOrNull()?.value_.orEmpty(),
        )
    }

    private suspend fun writeCached(flags: Flags) {
        val encoded = listOf(
            flags.publicArea,
            flags.orgChart,
            flags.localGroups,
            flags.passkeys,
            flags.donationLinkOnIos,
            flags.donationLinkOnAndroid,
        ).joinToString(",") { if (it) "1" else "0" }
        try {
            db.keyValueQueries.insertOrReplace(key = CACHE_KEY, value_ = encoded)
            db.keyValueQueries.insertOrReplace(
                key = CACHE_KEY_DONATION_LINK,
                value_ = flags.donationStripeLink,
            )
        } catch (_: Throwable) {
            // Cache write is best-effort; the in-memory value is already set.
        }
    }

    /**
     * PocketBase stores config values as free text, so accept the spellings a
     * human might type in the admin UI. Anything else — including null — is off.
     */
    private fun String?.toFlag(): Boolean =
        this?.trim()?.lowercase() in setOf("true", "1", "yes", "on")

    private companion object {
        const val KEY_PUBLIC_AREA = "public_area_enabled"
        const val KEY_ORG_CHART = "org_chart_enabled"
        const val KEY_LOCAL_GROUPS = "local_groups_enabled"
        const val KEY_PASSKEYS = "enable_passkeys"
        const val KEY_DONATION_LINK_IOS = "donation_link_on_ios"
        const val KEY_DONATION_LINK_ANDROID = "donation_link_on_android"
        const val KEY_DONATION_STRIPE_LINK = "donation_stripe_link"
        const val CACHE_KEY = "config.feature_flags"
        // Il link sta per conto suo e non nella lista separata da virgole:
        // un URL puo' contenerne una, e la spezzerebbe in due campi.
        const val CACHE_KEY_DONATION_LINK = "config.donation_stripe_link"
    }
}
