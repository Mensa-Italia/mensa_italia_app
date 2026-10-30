package it.mensa.shared.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * La migrazione 3.sqm aggiunge `area` ai soci in cache senza perderli: restano
 * senza regione fino al prossimo refresh del registro.
 */
class RegSociAreaMigrationTest {

    @Test
    fun version3DatabaseKeepsItsMembersAndGainsArea() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // La tabella com'era nella versione 3.
        driver.execute(
            null,
            """
            CREATE TABLE RegSoci (
                id TEXT NOT NULL PRIMARY KEY,
                uid INTEGER NOT NULL,
                name TEXT NOT NULL,
                image TEXT NOT NULL,
                city TEXT NOT NULL,
                birthdate INTEGER,
                state TEXT NOT NULL,
                fullDataJson TEXT NOT NULL,
                fullProfileLink TEXT,
                nameToSearch TEXT NOT NULL,
                updatedAt INTEGER NOT NULL,
                dataHash TEXT,
                imageHash TEXT
            )
            """.trimIndent(),
            0,
        )
        driver.execute(
            null,
            "INSERT INTO RegSoci(id, uid, name, image, city, state, fullDataJson, nameToSearch, updatedAt, imageHash) " +
                "VALUES ('s1', 0, 'Anna Rossi', '', 'Torino', 'Torino', '{}', 'anna rossi', 1, 'h1')",
            0,
        )

        MensaDatabase.Schema.migrate(driver, 3, 4).await()

        val db = MensaDatabase(driver)
        val old = db.regSociQueries.selectById("s1").executeAsOneOrNull()
        assertNotNull(old)
        // Le colonne dopo `state` si leggono ancora al posto giusto.
        assertEquals("Torino", old.state)
        assertEquals("h1", old.imageHash)
        assertEquals("", old.area)
    }
}
