package it.mensa.shared.db

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver

/**
 * WasmJs database driver backed by SQLDelight's `WebWorkerDriver` + sql.js.
 *
 * Worker bootstrap is delegated to SQLDelight's [createDefaultWebWorkerDriver],
 * which internally does:
 *   `new Worker(new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url))`
 * The Cash App worker (npm peer `@cashapp/sqldelight-sqljs-worker:2.1.0`,
 * `sql.js:1.10.x`, devNpm `copy-webpack-plugin:9.1.0`) flushes a snapshot to
 * IndexedDB / OPFS on commit, so the cache survives reloads on supported browsers.
 *
 * Schema lifecycle: `MensaDatabase.Schema` is async (generateAsync = true);
 * `Schema.awaitCreate(driver)` is the suspend equivalent of the native/Android
 * `Schema.create(driver)` and is required because the worker round-trip is async.
 */
actual class DriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        val driver = createDefaultWebWorkerDriver()
        MensaDatabase.Schema.awaitCreate(driver)
        return driver
    }
}
