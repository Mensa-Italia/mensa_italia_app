package it.mensa.shared.db

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

/**
 * `js(IR)` database driver backed by SQLDelight's `WebWorkerDriver` + sql.js.
 *
 * Implementation note: this used to call `createDefaultWebWorkerDriver()`, which
 * internally does `new Worker(new URL("@cashapp/sqldelight-sqljs-worker/...",
 * import.meta.url))`. That pattern is webpack-aware but breaks under Vite when
 * the bundle is consumed as a pre-built library — Vite cannot trace the URL
 * back to a worker entry it should emit.
 *
 * The host (Astro) is required to:
 *  1. Copy `node_modules/@cashapp/sqldelight-sqljs-worker/sqljs.worker.js` to
 *     `public/sqljs.worker.js`.
 *  2. Copy `node_modules/sql.js/dist/sql-wasm.wasm` to `public/sql-wasm.wasm`.
 *     The worker references it as `/sql-wasm.wasm` (root path).
 *
 * Schema lifecycle: `MensaDatabase.Schema` is async (generateAsync = true);
 * `Schema.awaitCreate(driver)` is the suspend equivalent of native/Android's
 * blocking `Schema.create(driver)`.
 */
actual class DriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        val driver = WebWorkerDriver(Worker("/sqljs.worker.js"))
        MensaDatabase.Schema.awaitCreate(driver)
        return driver
    }
}
