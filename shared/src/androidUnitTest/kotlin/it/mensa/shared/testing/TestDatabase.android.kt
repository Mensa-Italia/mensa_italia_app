package it.mensa.shared.testing

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import it.mensa.shared.db.MensaDatabase

actual suspend fun newTestDatabase(): MensaDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    MensaDatabase.Schema.create(driver).await()
    return MensaDatabase(driver)
}
