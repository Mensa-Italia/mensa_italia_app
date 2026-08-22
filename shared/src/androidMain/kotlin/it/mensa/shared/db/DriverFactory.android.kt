package it.mensa.shared.db

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DriverFactory(private val context: Context) {
    // Suspend solo perche' lo schema e' generato in modalita' async
    // (`generateAsync = true`): AndroidSqliteDriver si inizializza in modo
    // sincrono dentro il wrapper, e lo schema async gli viene adattato con
    // `.synchronous()`.
    actual suspend fun createDriver(): SqlDriver =
        AndroidSqliteDriver(MensaDatabase.Schema.synchronous(), context, "mensa.db")
}
