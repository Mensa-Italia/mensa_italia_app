package it.mensa.shared.config

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import it.mensa.shared.api.endpoints.SettingsApi
import it.mensa.shared.db.MensaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
        val quidAudio: Boolean = false,
        /** The three rankings, each on its own switch; with all three off the Home row goes too. */
        val leaderboardMembers: Boolean = false,
        val leaderboardRegions: Boolean = false,
        val leaderboardSecretaries: Boolean = false,
        /** The region's medals page in the medal cabinet. */
        val regionMedals: Boolean = false,
    ) {
        val leaderboard: Boolean get() = leaderboardMembers || leaderboardRegions || leaderboardSecretaries

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
    val quidAudioEnabled: Boolean get() = _flags.value.quidAudio
    val leaderboardMembersEnabled: Boolean get() = _flags.value.leaderboardMembers
    val leaderboardRegionsEnabled: Boolean get() = _flags.value.leaderboardRegions
    val leaderboardSecretariesEnabled: Boolean get() = _flags.value.leaderboardSecretaries
    val regionMedalsEnabled: Boolean get() = _flags.value.regionMedals
    /** Any of the three rankings on: the Home row and the page exist. */
    val leaderboardEnabled: Boolean get() = _flags.value.leaderboard

    /** Vedi [Flags.donationLinkIos]. */
    val donationLinkIos: String? get() = _flags.value.donationLinkIos
    /** Vedi [Flags.donationLinkAndroid]. */
    val donationLinkAndroid: String? get() = _flags.value.donationLinkAndroid

    /** Vedi [Flags.allowsSearchType]. */
    fun allowsSearchType(type: String): Boolean = _flags.value.allowsSearchType(type)

    /**
     * Safe to call on every launch and after login; never throws — a failure
     * leaves whatever was already loaded in place.
     *
     * I valori salvati subito; quelli del server in sottofondo, quando la
     * cache c'e'. Prima l'avvio aspettava sempre `configs` dalla rete. Senza
     * cache (primo avvio) si aspetta: i default spegnerebbero sezioni che il
     * server tiene accese.
     */
    @Throws(Exception::class)
    suspend fun bootstrap() {
        val cached = try {
            readCached()
        } catch (_: Throwable) {
            // Cache miss or unreadable DB — the network pass below still runs.
            null
        }
        if (cached != null) {
            _flags.value = cached
            backgroundScope.launch { refreshFromNetwork() }
        } else {
            refreshFromNetwork()
        }
    }

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Suppress("TooGenericExceptionCaught")
    private suspend fun refreshFromNetwork() {
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
                quidAudio = configs[KEY_QUID_AUDIO].toFlag(),
                leaderboardMembers = configs[KEY_LEADERBOARD_MEMBERS].toFlag(),
                leaderboardRegions = configs[KEY_LEADERBOARD_REGIONS].toFlag(),
                leaderboardSecretaries = configs[KEY_LEADERBOARD_SECRETARIES].toFlag(),
                regionMedals = configs[KEY_REGION_MEDALS].toFlag(),
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
        if (parts.size != 11) return null
        return Flags(
            publicArea = parts[0] == "1",
            orgChart = parts[1] == "1",
            localGroups = parts[2] == "1",
            passkeys = parts[3] == "1",
            donationLinkOnIos = parts[4] == "1",
            donationLinkOnAndroid = parts[5] == "1",
            quidAudio = parts[6] == "1",
            leaderboardMembers = parts[7] == "1",
            leaderboardRegions = parts[8] == "1",
            leaderboardSecretaries = parts[9] == "1",
            regionMedals = parts[10] == "1",
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
            flags.quidAudio,
            flags.leaderboardMembers,
            flags.leaderboardRegions,
            flags.leaderboardSecretaries,
            flags.regionMedals,
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
        const val KEY_QUID_AUDIO = "quid_audio_enabled"
        const val KEY_LEADERBOARD_MEMBERS = "leaderboard_members_enabled"
        const val KEY_LEADERBOARD_REGIONS = "leaderboard_regions_enabled"
        const val KEY_LEADERBOARD_SECRETARIES = "leaderboard_secretaries_enabled"
        const val KEY_REGION_MEDALS = "region_medals_enabled"
        const val CACHE_KEY = "config.feature_flags"
        // Il link sta per conto suo e non nella lista separata da virgole:
        // un URL puo' contenerne una, e la spezzerebbe in due campi.
        const val CACHE_KEY_DONATION_LINK = "config.donation_stripe_link"
    }
}
