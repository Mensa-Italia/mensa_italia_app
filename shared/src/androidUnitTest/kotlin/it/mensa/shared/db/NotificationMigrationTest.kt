package it.mensa.shared.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * L'app gia' installata ha il database alla versione 1: la migrazione 1.sqm
 * deve aggiungere `title` e `description` alle notifiche senza perdere quelle
 * salvate.
 */
class NotificationMigrationTest {

    @Test
    fun version1DatabaseKeepsItsNotificationsAndGainsTitleAndDescription() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // La tabella com'era nella versione 1.
        driver.execute(
            null,
            """
            CREATE TABLE Notification (
                id TEXT NOT NULL PRIMARY KEY,
                tr TEXT NOT NULL,
                trNamedParamsJson TEXT NOT NULL,
                dataJson TEXT,
                seen INTEGER,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
            0,
        )
        driver.execute(
            null,
            "INSERT INTO Notification VALUES ('n1', 'push_notification.new_event', '{}', NULL, NULL, 1, 1)",
            0,
        )

        MensaDatabase.Schema.migrate(driver, 1, MensaDatabase.Schema.version).await()

        val db = MensaDatabase(driver)
        val old = db.notificationQueries.selectById("n1").executeAsOneOrNull()
        assertNotNull(old)
        assertEquals("push_notification.new_event", old.tr)
        assertEquals("", old.title)
        assertEquals("", old.description)

        db.notificationQueries.insertOrReplace(
            id = "n2", tr = "", trNamedParamsJson = "{}", dataJson = """{"type":"developer_message"}""",
            seen = null, createdAt = 2, updatedAt = 2, title = "Manutenzione", description = "Stasera il server e' giu'",
        )
        val fresh = db.notificationQueries.selectById("n2").executeAsOne()
        assertEquals("Manutenzione", fresh.title)
        assertEquals("Stasera il server e' giu'", fresh.description)
    }
}
