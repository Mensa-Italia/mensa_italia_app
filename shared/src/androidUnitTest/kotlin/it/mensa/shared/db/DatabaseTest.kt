package it.mensa.shared.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DatabaseTest {

    private fun createTestDatabase(): MensaDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MensaDatabase.Schema.create(driver)
        return MensaDatabase(driver)
    }

    @Test
    fun testEventInsertAndSelect() {
        val db = createTestDatabase()
        val queries = db.eventQueries

        queries.insertOrReplace(
            id = "evt1",
            name = "Test Event",
            description = "A test event",
            whenStart = 1700000000000L,
            whenEnd = 1700003600000L,
            owner = "owner1",
            isNational = 1L,
            isSpot = 0L,
            bookingLink = "https://example.com/book",
            positionJson = """{"id":"loc1","name":"Rome","lat":41.9,"lon":12.5,"address":"Via Roma 1","state":"Lazio"}""",
            updatedAt = 1700000000000L,
        )

        val event = queries.selectById("evt1").executeAsOneOrNull()
        assertNotNull(event)
        assertEquals("evt1", event.id)
        assertEquals("Test Event", event.name)
        assertEquals("A test event", event.description)
        assertEquals(1700000000000L, event.whenStart)
        assertEquals(1L, event.isNational)
        assertEquals(0L, event.isSpot)

        val all = queries.selectAll().executeAsList()
        assertEquals(1, all.size)

        queries.deleteById("evt1")
        val deleted = queries.selectById("evt1").executeAsOneOrNull()
        assertNull(deleted)
    }

    @Test
    fun testDealInsertAndSelect() {
        val db = createTestDatabase()
        val queries = db.dealQueries

        queries.insertOrReplace(
            id = "deal1",
            name = "Best Deal",
            commercialSector = "Food",
            positionJson = null,
            isLocal = 1L,
            details = "20% off",
            who = "active_members",
            starting = null,
            ending = null,
            howToGet = "Show membership card",
            link = "https://example.com",
            owner = "owner1",
            attachment = null,
            isActive = 1L,
            vatNumber = "IT12345678",
            updatedAt = 1700000000000L,
        )

        val deal = queries.selectById("deal1").executeAsOneOrNull()
        assertNotNull(deal)
        assertEquals("deal1", deal.id)
        assertEquals("Best Deal", deal.name)
        assertEquals("Food", deal.commercialSector)
        assertEquals(1L, deal.isActive)
        assertNull(deal.positionJson)

        queries.deleteAll()
        val remaining = queries.selectAll().executeAsList()
        assertEquals(0, remaining.size)
    }

    @Test
    fun testKeyValueInsertAndSelect() {
        val db = createTestDatabase()
        val queries = db.keyValueQueries

        queries.insertOrReplace(key = "last_sync", value_ = "1700000000000")
        queries.insertOrReplace(key = "user_token", value_ = "abc123")
        queries.insertOrReplace(key = "empty_key", value_ = null)

        val entry = queries.selectById("last_sync").executeAsOneOrNull()
        assertNotNull(entry)
        assertEquals("last_sync", entry.key)
        assertEquals("1700000000000", entry.value_)

        val emptyEntry = queries.selectById("empty_key").executeAsOneOrNull()
        assertNotNull(emptyEntry)
        assertNull(emptyEntry.value_)

        val all = queries.selectAll().executeAsList()
        assertEquals(3, all.size)

        queries.deleteById("user_token")
        val afterDelete = queries.selectAll().executeAsList()
        assertEquals(2, afterDelete.size)
    }

    @Test
    fun testRegSociInsertAndSearch() {
        val db = createTestDatabase()
        val queries = db.regSociQueries

        queries.insertOrReplace(
            id = "1001",
            uid = 1001L,
            name = "Mario Rossi",
            image = "https://example.com/img.jpg",
            city = "Roma",
            birthdate = null,
            state = "Lazio",
            fullDataJson = "{}",
            fullProfileLink = null,
            nameToSearch = "Mario Rossi . Rossi Mario",
            updatedAt = 1700000000000L,
        )

        val result = queries.selectById("1001").executeAsOneOrNull()
        assertNotNull(result)
        assertEquals("Mario Rossi", result.name)
        assertEquals("Roma", result.city)

        val searchResults = queries.searchByName("%Mario%").executeAsList()
        assertEquals(1, searchResults.size)
        assertEquals("Mario Rossi", searchResults[0].name)
    }
}
