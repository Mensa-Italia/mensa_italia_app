package it.mensa.shared.config

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import it.mensa.shared.api.endpoints.SettingsApi
import it.mensa.shared.db.MensaDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    )

    private val _flags = MutableStateFlow(Flags())
    val flags: StateFlow<Flags> = _flags.asStateFlow()

    val publicAreaEnabled: Boolean get() = _flags.value.publicArea
    val orgChartEnabled: Boolean get() = _flags.value.orgChart
    val localGroupsEnabled: Boolean get() = _flags.value.localGroups

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
        if (parts.size != 3) return null
        return Flags(
            publicArea = parts[0] == "1",
            orgChart = parts[1] == "1",
            localGroups = parts[2] == "1",
        )
    }

    private suspend fun writeCached(flags: Flags) {
        val encoded = listOf(flags.publicArea, flags.orgChart, flags.localGroups)
            .joinToString(",") { if (it) "1" else "0" }
        try {
            db.keyValueQueries.insertOrReplace(key = CACHE_KEY, value_ = encoded)
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
        const val CACHE_KEY = "config.feature_flags"
    }
}
