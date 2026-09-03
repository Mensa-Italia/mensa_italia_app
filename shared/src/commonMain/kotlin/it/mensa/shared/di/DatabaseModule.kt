package it.mensa.shared.di

import app.cash.sqldelight.db.SqlDriver
import it.mensa.shared.db.DriverFactory
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.db.createDatabase
import it.mensa.shared.spotlight.SpotlightSinkRegistry
import kotlinx.coroutines.withContext
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

/**
 * Su [ioDispatcher] e non sul thread chiamante: la catena del logout parte dal
 * main thread su entrambe le piattaforme (`viewModelScope` su Android, un
 * `Task` di SwiftUI su iOS) e qui dentro si riscrive l'intero file del
 * database. Prima erano 18 DELETE sincrone — gia' abbastanza per un
 * micro-freeze; con il VACUUM sarebbe diventato visibile.
 */
suspend fun wipeAllUserData(): Unit = withContext(ioDispatcher) {
    val driver = MensaDatabaseHolder.requireDriver()

    // Prima delle DELETE, non dopo: `secure_delete` dice a SQLite di
    // sovrascrivere con zeri il contenuto delle righe cancellate invece di
    // limitarsi a marcare le pagine come libere. Vale solo per le DELETE che
    // vengono dopo, quindi l'ordine conta. E' una PRAGMA di sessione: muore
    // con la connessione, non tocca il file.
    runCatching {
        driver.execute(identifier = null, sql = "PRAGMA secure_delete = ON", parameters = 0)
    }

    for (table in ALL_TABLES) {
        driver.execute(identifier = null, sql = "DELETE FROM $table", parameters = 0)
    }

    compactDatabase(driver)

    // iOS Spotlight (members + documents domain + thumbnail cache).
    // No-op when the host hasn't registered a sink (Android, JS).
    runCatching { SpotlightSinkRegistry.sink?.clearAll() }
}

/**
 * Riscrive il file del database da capo dopo lo svuotamento.
 *
 * Una `DELETE FROM` non restituisce spazio: le pagine restano allocate al file
 * e, senza `secure_delete`, il loro contenuto resta leggibile con un editor
 * esadecimale. Dopo un logout in `mensa.db` ci sarebbero ancora l'anagrafica
 * dei soci, i documenti e le ricevute di chi e' appena uscito — su un
 * dispositivo che magari non e' suo.
 *
 * Due passaggi, in quest'ordine:
 *
 *  1. `VACUUM` ricostruisce il database in un file nuovo e ci copia dentro
 *    soltanto le pagine ancora in uso — che a questo punto sono gli schemi
 *    vuoti. Non puo' girare dentro una transazione, e infatti qui non ce
 *    n'e' una aperta.
 *  2. `PRAGMA wal_checkpoint(TRUNCATE)` perche' entrambi i driver aprono il
 *    database in modalita' WAL: le scritture del VACUUM finiscono nel file
 *    `-wal`, e finche' non si fa un checkpoint il `.db` principale continua a
 *    contenere le pagine di prima. Il checkpoint le travasa e azzera il
 *    `-wal`, che altrimenti resterebbe li' con le immagini delle pagine
 *    vecchie.
 *
 * L'ordine e' quello e non un altro: un checkpoint fatto *prima* del VACUUM
 * lascia nel `-wal` le pagine che il VACUUM ci scrive subito dopo.
 *
 * Misurato su SQLite 3.45 con 3000 righe e `secure_delete` inizialmente OFF,
 * contando le occorrenze di una stringa segreta nei byte di `.db` + `-wal`:
 *
 * | sequenza                                    | residui | file    |
 * |---------------------------------------------|---------|---------|
 * | `DELETE` soltanto (com'era)                 |    3378 | 136 KB  |
 * | `DELETE` + `VACUUM`, senza checkpoint       |    3378 | 144 KB  |
 * | `secure_delete` + `DELETE` + checkpoint     |       0 | 115 KB  |
 * | questa: + `VACUUM` prima del checkpoint     |       0 |   8 KB  |
 *
 * Da leggere cosi': il VACUUM da solo non toglie niente, perche' in modalita'
 * WAL i dati vecchi stanno nel `-wal` e il VACUUM ci scrive dentro invece di
 * svuotarlo. E' il checkpoint a ripulire; il VACUUM serve a non lasciare un
 * file pieno di pagine libere.
 *
 * Ogni passo e' isolato in un `runCatching`: e' una misura di igiene, non una
 * condizione di correttezza. Se il VACUUM non riesce — un'altra connessione ha
 * una transazione aperta, il disco e' pieno — i dati sono comunque gia' stati
 * cancellati, e far fallire il logout per questo sarebbe sbagliato.
 *
 * Nota sul percorso: `SqlDriver.execute` arriva a `execSQL` su Android e a
 * `sqlite3_step` su SQLiter, cioe' esattamente il modo documentato di eseguire
 * `VACUUM` e le PRAGMA su entrambe le piattaforme.
 */
internal fun compactDatabase(driver: SqlDriver) {
    runCatching {
        driver.execute(identifier = null, sql = "VACUUM", parameters = 0)
    }
    runCatching {
        driver.execute(identifier = null, sql = "PRAGMA wal_checkpoint(TRUNCATE)", parameters = 0)
    }
}
