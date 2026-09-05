package it.mensa.shared.tableport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StampQRParserTest {

    /**
     * La forma che il backend genera davvero, e che sta stampata su ogni QR
     * gia' mandato per mail: `main/hooks/events_notify_users.go` scrive
     * l'URL intero, non l'id nudo. Prima l'id diventava
     * "https://svc.mensa.it/links/stamp/wuerjlgim1iyqbj" e nessuna scansione
     * andava a buon fine.
     */
    @Test
    fun `legge la forma con URL intero`() {
        val payload = StampQRParser.parse(
            "https://svc.mensa.it/links/stamp/wuerjlgim1iyqbj:::4b326159-c182-4326-bbf8-f25e3c350eb6"
        )
        assertEquals("wuerjlgim1iyqbj", payload?.stampId)
        assertEquals("4b326159-c182-4326-bbf8-f25e3c350eb6", payload?.verificationCode)
    }

    /** La forma nuda continua a funzionare: i QR vecchi non si buttano. */
    @Test
    fun `legge la forma senza URL`() {
        val payload = StampQRParser.parse("wuerjlgim1iyqbj:::4b326159-c182-4326-bbf8-f25e3c350eb6")
        assertEquals("wuerjlgim1iyqbj", payload?.stampId)
        assertEquals("4b326159-c182-4326-bbf8-f25e3c350eb6", payload?.verificationCode)
    }

    /** `?qrcode` se lo attacca il backend stesso su /links/stamp. */
    @Test
    fun `scarta query e fragment attaccati al codice`() {
        val conQuery = StampQRParser.parse(
            "https://svc.mensa.it/links/stamp/abc123def456ghi:::codice-1?qrcode"
        )
        assertEquals("abc123def456ghi", conQuery?.stampId)
        assertEquals("codice-1", conQuery?.verificationCode)

        val conFragment = StampQRParser.parse("abc123def456ghi:::codice-1#top")
        assertEquals("codice-1", conFragment?.verificationCode)
    }

    @Test
    fun `tollera spazi intorno al contenuto`() {
        val payload = StampQRParser.parse("  wuerjlgim1iyqbj:::codice-1  ")
        assertEquals("wuerjlgim1iyqbj", payload?.stampId)
        assertEquals("codice-1", payload?.verificationCode)
    }

    @Test
    fun `rifiuta cio che non e un QR di francobollo`() {
        assertNull(StampQRParser.parse(""))
        assertNull(StampQRParser.parse("wuerjlgim1iyqbj"))
        assertNull(StampQRParser.parse("https://example.com"))
        // Separatore presente ma una delle due meta' e' vuota.
        assertNull(StampQRParser.parse("wuerjlgim1iyqbj:::"))
        assertNull(StampQRParser.parse(":::codice-1"))
        // Solo l'URL, senza codice: e' il link "nudo" che si apre dal browser.
        assertNull(StampQRParser.parse("https://svc.mensa.it/links/stamp/wuerjlgim1iyqbj"))
    }
}
