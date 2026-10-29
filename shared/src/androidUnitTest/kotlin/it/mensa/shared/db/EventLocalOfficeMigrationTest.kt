package it.mensa.shared.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * La migrazione 2.sqm aggiunge `localOffice` agli eventi in cache senza
 * perderli: restano con il gruppo vuoto fino al prossimo refresh.
 */
class EventLocalOfficeMigrationTest {

    @Test
    fun version2DatabaseKeepsItsEventsAndGainsLocalOffice() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // La tabella com'era nella versione 2.
        driver.execute(
            null,
            """
            CREATE TABLE Event (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                image TEXT NOT NULL DEFAULT '',
                infoLink TEXT NOT NULL DEFAULT '',
                contact TEXT NOT NULL DEFAULT '',
                whenStart INTEGER NOT NULL,
                whenEnd INTEGER NOT NULL,
                owner TEXT NOT NULL,
                isNational INTEGER NOT NULL DEFAULT 0,
                isSpot INTEGER NOT NULL DEFAULT 0,
                isPublic INTEGER NOT NULL DEFAULT 0,
                bookingLink TEXT,
                positionJson TEXT,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
            0,
        )
        driver.execute(
            null,
            "INSERT INTO Event(id, name, description, whenStart, whenEnd, owner, updatedAt) VALUES ('e1', 'Cena', 'Desc', 1, 2, 'u1', 1)",
            0,
        )

        MensaDatabase.Schema.migrate(driver, 2, 3).await()

        val db = MensaDatabase(driver)
        val old = db.eventQueries.selectById("e1").executeAsOneOrNull()
        assertNotNull(old)
        assertEquals("u1", old.owner)
        assertEquals("", old.localOffice)
    }
}
