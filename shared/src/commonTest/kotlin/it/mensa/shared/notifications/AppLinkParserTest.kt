package it.mensa.shared.notifications

import it.mensa.shared.tableport.StampQRPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppLinkParserTest {
    @Test
    fun stampLinkFromTheQr() {
        assertEquals(
            AppLink.Stamp(StampQRPayload("l4dzx326382f7fm", "59277e04-de61-45c6-b7a3-bb96ea72b412")),
            AppLinkParser.parse("https://svc.mensa.it/links/stamp/l4dzx326382f7fm:::59277e04-de61-45c6-b7a3-bb96ea72b412"),
        )
    }

    @Test
    fun stampLinkWithQueryOrEncodedSeparator() {
        val expected = AppLink.Stamp(StampQRPayload("abc", "code-1"))
        assertEquals(expected, AppLinkParser.parse("https://svc.mensa.it/links/stamp/abc:::code-1?qrcode"))
        assertEquals(expected, AppLinkParser.parse("https://svc.mensa.it/links/stamp/abc%3A%3A%3Acode-1"))
        assertEquals(expected, AppLinkParser.parse("HTTPS://SVC.MENSA.IT/links/stamp/abc:::code-1#x"))
    }

    @Test
    fun eventLink() {
        assertEquals(AppLink.Event("ev123"), AppLinkParser.parse("https://svc.mensa.it/links/event/ev123"))
        assertEquals(AppLink.Event("ev123"), AppLinkParser.parse("https://svc.mensa.it/links/event/ev123/"))
    }

    @Test
    fun rejectsEverythingElse() {
        assertNull(AppLinkParser.parse("https://svc.mensa.it/links/stamp/abc"))
        assertNull(AppLinkParser.parse("https://svc.mensa.it/links/unknown/abc"))
        assertNull(AppLinkParser.parse("https://svc.mensa.it/api/files/x/y/z"))
        assertNull(AppLinkParser.parse("https://evil.example/links/event/ev123"))
        assertNull(AppLinkParser.parse("mensa://links/event/ev123"))
    }
}
