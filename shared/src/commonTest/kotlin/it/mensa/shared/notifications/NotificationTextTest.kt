package it.mensa.shared.notifications

import it.mensa.shared.model.NotificationModel
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationTextTest {
    // Un catalogo con una sola chiave tradotta; le altre tornano il fallback.
    private val catalog = mapOf(
        "push_notification.new_event.title" to "Nuovo evento: {name}",
        "push_notification.new_event.body" to "Apri per i dettagli",
    )
    private val translate: (String, String?, Map<String, String>) -> String = { key, fallback, args ->
        catalog[key]?.let { t -> args.entries.fold(t) { acc, (k, v) -> acc.replace("{$k}", v) } } ?: fallback ?: key
    }

    @Test
    fun translatedKeyWins() {
        val n = NotificationModel(tr = "push_notification.new_event", trNamedParams = mapOf("name" to "Cena"), title = "ignorato")
        assertEquals("Nuovo evento: Cena", NotificationText.title(n, translate))
        assertEquals("Apri per i dettagli", NotificationText.body(n, translate))
    }

    @Test
    fun emptyTrUsesTheSavedTitleAndDescription() {
        val n = NotificationModel(tr = "", title = "Manutenzione", description = "Stasera il server e' giu'")
        assertEquals("Manutenzione", NotificationText.title(n, translate))
        assertEquals("Stasera il server e' giu'", NotificationText.body(n, translate))
    }

    @Test
    fun keyWithoutTranslationFallsBackToTheSavedText() {
        val n = NotificationModel(tr = "push_notification.unknown", title = "Titolo dal server", description = "Testo dal server")
        assertEquals("Titolo dal server", NotificationText.title(n, translate))
        assertEquals("Testo dal server", NotificationText.body(n, translate))
    }

    @Test
    fun nothingAtAllSaysNotifica() {
        assertEquals("Notifica", NotificationText.title(NotificationModel(), translate))
        assertEquals("", NotificationText.body(NotificationModel(), translate))
    }
}
