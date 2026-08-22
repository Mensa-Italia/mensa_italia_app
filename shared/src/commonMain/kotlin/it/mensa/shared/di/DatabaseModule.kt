package it.mensa.shared.di

import app.cash.sqldelight.db.SqlDriver
import it.mensa.shared.db.DriverFactory
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.db.createDatabase
import it.mensa.shared.spotlight.SpotlightSinkRegistry
import org.koin.dsl.module

/**
 * Database wiring.
 *
 * SQLDelight gira con `generateAsync = true`, quindi
 * `DriverFactory.createDriver()` e' `suspend`. Il lambda `single { }` di Koin
 * non puo' sospendere, percio' il database e' registrato come holder pigro.
 * Chi sta in un contesto suspend usa [requireMensaDatabase] (che delega a
 * Koin) dopo che l'host ha chiamato [initializeMensaDatabase] una volta sola
 * durante il bootstrap dell'applicazione.
 *
 * Host responsibilities:
 *  - iOS e Android MUST call `initializeMensaDatabase(get())`
 *    from a suspend bootstrap path BEFORE the first repository call.
 *  - If they don't, `get<MensaDatabase>()` throws an explicit IllegalStateException
 *    rather than silently constructing on the wrong thread.
 */
val databaseModule = module {
    single<MensaDatabase> {
        MensaDatabaseHolder.requireDatabase()
    }
}

internal object MensaDatabaseHolder {
    // No @Volatile: reads/writes happen on the platform's main bootstrap path
    // (Swift main thread for iOS, Android main thread for Android) before any
    // repository call observes the field, so the visibility we need is fence-free.
    private var instance: MensaDatabase? = null
    private var driver: SqlDriver? = null

    /** Idempotent. Constructs the database via the [DriverFactory] on first call. */
    suspend fun initialize(factory: DriverFactory) {
        if (instance == null) {
            val d = factory.createDriver()
            driver = d
            instance = createDatabase(d)
        }
    }

    fun requireDatabase(): MensaDatabase = instance
        ?: error(
            "MensaDatabase not initialized. Call initializeMensaDatabase(get<DriverFactory>()) " +
                "from a suspend bootstrap path after MensaSdk.initKoin(...) and before the first " +
                "repository call."
        )

    fun requireDriver(): SqlDriver = driver
        ?: error("MensaDatabase not initialized; SqlDriver unavailable.")

    /** Test/dev helper: drop the cached instance so the next initialize() reopens the DB. */
    internal fun reset() { instance = null; driver = null }
}

/** Suspend bootstrap entry point for hosts. Idempotent; safe to call more than once. */
suspend fun initializeMensaDatabase(factory: DriverFactory) {
    MensaDatabaseHolder.initialize(factory)
}

/**
 * Deletes every row from every cached table. Called when the auth session is
 * irrecoverably dead (refresh failed AND `/api/cs/me` failed after a forced
 * refresh) so the next login starts from a clean slate. Schema is preserved
 * to keep migrations stable.
 */
private val ALL_TABLES = listOf(
    "Addon", "Calendar", "Deal", "Device", "Document",
    "DocumentElaborated", "Event", "EventSchedule", "KeyValue", "Location",
    "Notification", "PaymentMethod", "Receipt", "RegSoci", "Sig", "Stamp",
    "StampUser", "Ticket",
)

suspend fun wipeAllUserData() {
    val driver = MensaDatabaseHolder.requireDriver()
    for (table in ALL_TABLES) {
        driver.execute(identifier = null, sql = "DELETE FROM $table", parameters = 0)
    }
    // iOS Spotlight (members + documents domain + thumbnail cache).
    // No-op when the host hasn't registered a sink (Android, JS).
    runCatching { SpotlightSinkRegistry.sink?.clearAll() }
}
