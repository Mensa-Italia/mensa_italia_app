package it.mensa.shared.db

import app.cash.sqldelight.db.SqlDriver

fun createDatabase(driver: SqlDriver): MensaDatabase = MensaDatabase(driver)
