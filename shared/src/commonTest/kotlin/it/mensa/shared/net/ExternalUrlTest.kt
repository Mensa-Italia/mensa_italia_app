package it.mensa.shared.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExternalUrlTest {

    /**
     * I tre valori che stanno davvero nel database delle convenzioni.
     *
     * Sono questi che facevano chiudere l'app su Android e che su iOS non
     * aprivano niente: il campo `link` di PocketBase e' di tipo `url`, ma
     * quella validazione accetta anche un indirizzo senza schema.
     */
    @Test
    fun `i link delle convenzioni in produzione diventano https`() {
        assertEquals("https://www.bonavitaly.com", ExternalUrl.normalize("www.bonavitaly.com"))
        assertEquals("https://www.dolcelunafirenze.com", ExternalUrl.normalize("www.dolcelunafirenze.com"))
        assertEquals("https://www.masseriecattaneo.it", ExternalUrl.normalize("www.masseriecattaneo.it"))
    }

    @Test
    fun `un url gia' completo non viene toccato`() {
        assertEquals("https://www.mensa.it/privacy", ExternalUrl.normalize("https://www.mensa.it/privacy"))
        assertEquals("http://mensa.it", ExternalUrl.normalize("http://mensa.it"))
    }

    /** I filtri intent di Android confrontano lo schema cosi' com'e': minuscolo. */
    @Test
    fun `lo schema viene messo in minuscolo`() {
        assertEquals("https://Mensa.it", ExternalUrl.normalize("HTTPS://Mensa.it"))
        assertEquals("http://mensa.it", ExternalUrl.normalize("Http://mensa.it"))
    }

    @Test
    fun `spazi e caratteri invisibili attorno al link non contano`() {
        assertEquals("https://mensa.it", ExternalUrl.normalize("  https://mensa.it  "))
        assertEquals("https://mensa.it", ExternalUrl.normalize("\n\thttps://mensa.it\n"))
        assertEquals("https://mensa.it", ExternalUrl.normalize("\u00A0https://mensa.it\u200B"))
    }

    @Test
    fun `vuoto e nullo non danno un link`() {
        assertNull(ExternalUrl.normalize(null))
        assertNull(ExternalUrl.normalize(""))
        assertNull(ExternalUrl.normalize("   "))
    }

    /**
     * Il caso che conta per la UI: se il campo e' stato compilato a mano con
     * del testo, il bottone deve sparire, non aprire una ricerca a caso.
     */
    @Test
    fun `il testo libero non diventa un link`() {
        assertNull(ExternalUrl.normalize("Chiedere in sede"))
        assertNull(ExternalUrl.normalize("da definire"))
        assertNull(ExternalUrl.normalize("https://"))
        assertNull(ExternalUrl.normalize("//"))
    }

    @Test
    fun `un dominio con porta non viene scambiato per uno schema`() {
        assertEquals("https://www.mensa.it:8080/area", ExternalUrl.normalize("www.mensa.it:8080/area"))
        assertEquals("https://localhost:8090", ExternalUrl.normalize("localhost:8090"))
    }

    @Test
    fun `il protocol-relative prende https`() {
        assertEquals("https://cdn.mensa.it/x.png", ExternalUrl.normalize("//cdn.mensa.it/x.png"))
    }

    @Test
    fun `una mail nuda diventa mailto`() {
        assertEquals("mailto:info@mensa.it", ExternalUrl.normalize("info@mensa.it"))
        // Con lo schema gia' addosso resta com'e'.
        assertEquals("mailto:info@mensa.it", ExternalUrl.normalize("mailto:info@mensa.it"))
    }

    /** `tel:0212345` e' uno schema, non un `host:porta`. */
    @Test
    fun `gli schemi senza doppia barra sopravvivono`() {
        assertEquals("tel:0212345", ExternalUrl.normalize("tel:0212345"))
        assertEquals("tel:+390212345", ExternalUrl.normalize("tel:+390212345"))
        assertEquals("geo:45.46,9.18", ExternalUrl.normalize("geo:45.46,9.18"))
        assertEquals("webcal://mensa.it/cal.ics", ExternalUrl.normalize("webcal://mensa.it/cal.ics"))
    }

    /**
     * Questi campi li scrivono gli amministratori delle sedi locali dal
     * pannello: non devono poter far eseguire niente al telefono di un socio.
     */
    @Test
    fun `gli schemi pericolosi vengono rifiutati`() {
        assertNull(ExternalUrl.normalize("javascript:alert(1)"))
        assertNull(ExternalUrl.normalize("JavaScript:alert(1)"))
        assertNull(ExternalUrl.normalize("data:text/html,<script>alert(1)</script>"))
        assertNull(ExternalUrl.normalize("file:///etc/passwd"))
        assertNull(ExternalUrl.normalize("intent://scan/#Intent;scheme=zxing;end"))
        assertNull(ExternalUrl.normalize("content://com.android.contacts/data/1"))
    }

    @Test
    fun `gli spazi nel percorso vengono codificati`() {
        assertEquals(
            "https://mensa.it/verbali/verbale%20consiglio.pdf",
            ExternalUrl.normalize("https://mensa.it/verbali/verbale consiglio.pdf"),
        )
    }

    /** Uno spazio nell'host invece vuol dire che non era un indirizzo. */
    @Test
    fun `uno spazio nell'host annulla il link`() {
        assertNull(ExternalUrl.normalize("www.mensa .it"))
        assertNull(ExternalUrl.normalize("due parole"))
        // Guardando solo la parte dopo la chiocciola si leggerebbe "mensa.it>",
        // che sembra un host buono: va scartata tutta l'autorita'.
        assertNull(ExternalUrl.normalize("Nome Cognome <info@mensa.it>"))
    }

    @Test
    fun `query e fragment restano intatti`() {
        assertEquals(
            "https://mensa.it/x?a=1&b=2#sezione",
            ExternalUrl.normalize("mensa.it/x?a=1&b=2#sezione"),
        )
    }

    @Test
    fun `isWebUrl distingue il browser dal resto`() {
        assertTrue(ExternalUrl.isWebUrl("https://mensa.it"))
        assertTrue(ExternalUrl.isWebUrl("http://mensa.it"))
        assertFalse(ExternalUrl.isWebUrl("mailto:info@mensa.it"))
        assertFalse(ExternalUrl.isWebUrl("tel:0212345"))
        assertFalse(ExternalUrl.isWebUrl("geo:45.46,9.18"))
    }
}
