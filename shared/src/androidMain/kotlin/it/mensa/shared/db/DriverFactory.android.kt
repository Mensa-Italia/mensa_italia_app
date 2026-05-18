package it.mensa.shared.db

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DriverFactory(private val context: Context) {
    // Suspend because the wasmJs WebWorkerDriver setup is async; AndroidSqliteDriver
    // initialises synchronously inside the suspend wrapper. The async-mode schema
    // is adapted to the synchronous driver via `.synchronous()`.
    actual suspend fun createDriver(): SqlDriver =
        AndroidSqliteDriver(MensaDatabase.Schema.synchronous(), context, "mensa.db")
}
