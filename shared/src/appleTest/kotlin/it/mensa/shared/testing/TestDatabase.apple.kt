package it.mensa.shared.testing

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.native.inMemoryDriver
import it.mensa.shared.db.MensaDatabase

actual suspend fun newTestDatabase(): MensaDatabase =
    MensaDatabase(inMemoryDriver(MensaDatabase.Schema.synchronous()))
