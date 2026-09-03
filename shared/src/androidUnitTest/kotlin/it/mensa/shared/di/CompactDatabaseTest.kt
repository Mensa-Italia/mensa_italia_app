package it.mensa.shared.di

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import it.mensa.shared.db.MensaDatabase
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Verifica che [compactDatabase] faccia davvero quello per cui esiste: dopo il
 * logout, nel file del database non devono restare i dati del socio che se n'e'
 * andato.
 *
 * Il test gira su un database su FILE, non in memoria: il punto e' proprio cosa
 * resta scritto su disco, e in memoria non si potrebbe misurare. Il driver e'
 * `JdbcSqliteDriver`, cioe' un `SqlDriver` di SQLDelight come quello che gira in
 * produzione: quello che si verifica qui non e' che le stringhe SQL siano
 * giuste, ma che `driver.execute` accetti `VACUUM` e le PRAGMA — la parte su cui
 * un dubbio era legittimo, visto che i driver di SQLDelight passano gli statement
 * per `executeUpdateDelete`.
 */
class CompactDatabaseTest {

    private val secret = "MarioRossiSegretoXYZ"

    @Test
    fun `il vacuum toglie dal file i dati cancellati e lo rimpicciolisce`() = runTest {
        val dbFile = File.createTempFile("mensa-compact", ".db").apply { delete() }
        try {
            val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
            MensaDatabase.Schema.create(driver).await()
            val db = MensaDatabase(driver)

            repeat(ROWS) { i ->
                db.regSociQueries.insertOrReplace(
                    id = "socio$i",
                    uid = i.toLong(),
                    name = "$secret$i",
                    image = "",
                    city = "Roma",
                    birthdate = null,
                    state = "Lazio",
                    fullDataJson = "{}",
                    fullProfileLink = null,
                    nameToSearch = "$secret$i",
                    updatedAt = 0L,
                    dataHash = null,
                    imageHash = null,
                )
            }
            // Il file deve avere davvero i dati dentro prima di poter dire che
            // dopo non ce li ha piu'.
            assertTrue(dbFile.occurrencesOf(secret) > 0, "il seed non e' finito su disco")
            val sizeWithData = dbFile.length()

            driver.execute(identifier = null, sql = "PRAGMA secure_delete = ON", parameters = 0)
            driver.execute(identifier = null, sql = "DELETE FROM RegSoci", parameters = 0)
            compactDatabase(driver)

            assertEquals(0, dbFile.occurrencesOf(secret), "dati residui nel file dopo il vacuum")
            assertTrue(
                dbFile.length() < sizeWithData,
                "il file non si e' rimpicciolito: ${dbFile.length()} vs $sizeWithData",
            )

            // Lo schema sopravvive: dopo il logout si rientra senza migrazioni.
            assertEquals(0, db.regSociQueries.selectAll().awaitAsList().size)
            driver.close()
        } finally {
            dbFile.delete()
            File("${dbFile.absolutePath}-wal").delete()
            File("${dbFile.absolutePath}-shm").delete()
        }
    }

    private fun File.occurrencesOf(needle: String): Int {
        if (!exists()) return 0
        val bytes = readBytes()
        val target = needle.toByteArray()
        var count = 0
        var i = 0
        outer@ while (i <= bytes.size - target.size) {
            for (j in target.indices) {
                if (bytes[i + j] != target[j]) {
                    i++
                    continue@outer
                }
            }
            count++
            i += target.size
        }
        return count
    }

    private companion object {
        const val ROWS = 3000
    }
}
