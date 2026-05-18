package it.mensa.shared.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSSearchPathDomainMask
import platform.Foundation.NSSearchPathForDirectoriesInDomains

/**
 * iOS database driver.
 *
 * Schema lifecycle: SQLDelight tracks the schema version in `user_version`.
 * When we bump the schema (new tables/columns) without writing explicit
 * migration scripts, opening an existing older DB throws `no such table: …`
 * on the first query — which crashes the app.
 *
 * Strategy for the rebuild phase: probe the schema with a cheap SELECT on
 * every expected table; if any throws, nuke the DB file and reopen. The
 * cached data is best-effort (just a network mirror) so the repositories
 * will refresh from the API on first use.
 *
 * Note: createDriver is suspend because the wasmJs WebWorkerDriver setup is
 * async; native drivers complete synchronously inside the suspend wrapper.
 * `MensaDatabase.Schema` is async (generateAsync = true) so we adapt it
 * to the synchronous native driver via `.synchronous()`.
 */
@OptIn(ExperimentalForeignApi::class)
actual class DriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        var driver: SqlDriver = NativeSqliteDriver(MensaDatabase.Schema.synchronous(), DB_NAME)
        if (!probe(driver)) {
            runCatching { driver.close() }
            deleteDbFile()
            driver = NativeSqliteDriver(MensaDatabase.Schema.synchronous(), DB_NAME)
        }
        return driver
    }

    private fun probe(driver: SqlDriver): Boolean = runCatching {
        TABLES.forEach { table ->
            driver.executeQuery(
                identifier = null,
                sql = "SELECT 1 FROM $table LIMIT 1",
                mapper = { QueryResult.Value(Unit) },
                parameters = 0,
            )
        }
        // Per-column probes for schema additions that don't add new tables.
        // If the column is missing, SQLite throws "no such column: …".
        COLUMN_PROBES.forEach { (table, column) ->
            driver.executeQuery(
                identifier = null,
                sql = "SELECT $column FROM $table LIMIT 1",
                mapper = { QueryResult.Value(Unit) },
                parameters = 0,
            )
        }
    }.isSuccess

    private fun deleteDbFile() {
        val fm = NSFileManager.defaultManager
        // NSApplicationSupportDirectory = 14, NSUserDomainMask = 1.
        // `.convert()` perche' su watchosArm64 (32-bit) i due parametri sono
        // `UInt` mentre su iosArm64 (64-bit) sono `ULong` — il bridge K/N
        // sceglie il tipo nativo della piattaforma.
        val dirs = NSSearchPathForDirectoriesInDomains(14u.convert(), 1u.convert(), true)
        val support = dirs.firstOrNull() as? String ?: return
        // NativeSqliteDriver stores at <ApplicationSupport>/databases/<name>.
        listOf(
            "$support/databases/$DB_NAME",
            "$support/databases/$DB_NAME-journal",
            "$support/databases/$DB_NAME-wal",
            "$support/databases/$DB_NAME-shm",
        ).forEach { path ->
            runCatching { fm.removeItemAtPath(path, null) }
        }
        // Note: avoid passing Kotlin String as `%@` variadic to NSLog — the
        // bridge doesn't retain it properly, leading to EXC_BAD_ACCESS in
        // CoreFoundation's logging path. Inline the value into the format
        // string instead.
        println("MENSA_DEBUG: nuked DB at ${support}/databases/${DB_NAME}")
    }

    companion object {
        private const val DB_NAME = "mensa.db"
        private val TABLES = listOf(
            "Event", "Deal", "Sig", "Stamp", "RegSoci", "Notification",
            "Boutique", "Document", "DocumentElaborated",
            "Ticket", "Receipt", "StampUser", "Device",
            "EventSchedule", "Calendar", "PaymentMethod", "Addon", "KeyValue",
        )
        private val COLUMN_PROBES = listOf(
            "Document" to "created",
            "Event" to "image",
            "Event" to "infoLink",
            "Event" to "contact",
            "Event" to "isPublic",
            "RegSoci" to "dataHash",
            "RegSoci" to "imageHash",
        )
    }
}
