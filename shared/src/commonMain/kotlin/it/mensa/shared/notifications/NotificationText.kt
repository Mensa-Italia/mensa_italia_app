package it.mensa.shared.notifications

import it.mensa.shared.i18n.I18n
import it.mensa.shared.model.NotificationModel

/**
 * Titolo e testo di una notifica nell'app, uguali su iOS e Android e in ogni
 * schermata (lista, dettaglio, anteprima in Home).
 *
 * Si parte da [NotificationModel.tr], la chiave di traduzione: cosi' il testo
 * segue la lingua dell'app. Quando `tr` e' vuoto (es. `developer_message`) o
 * la chiave non ha traduzione, si usano `title` e `description` salvati dal
 * server, gia' tradotti. Prima con `tr` vuoto si vedeva solo "Notifica" e un
 * testo vuoto.
 */
object NotificationText {
    fun title(n: NotificationModel, i18n: I18n): String = title(n, i18n::t)

    fun body(n: NotificationModel, i18n: I18n): String = body(n, i18n::t)

    /** La regola, con la traduzione passata da fuori per i test. */
    fun title(n: NotificationModel, translate: (String, String?, Map<String, String>) -> String): String {
        if (n.tr.isBlank()) {
            return n.title.ifBlank { translate("notifications.fallback.title", "Notifica", emptyMap()) }
        }
        val key = "${n.tr}.title"
        return translate(key, n.title.ifBlank { key }, n.trNamedParams)
    }

    fun body(n: NotificationModel, translate: (String, String?, Map<String, String>) -> String): String {
        if (n.tr.isBlank()) return n.description
        return translate("${n.tr}.body", n.description, n.trNamedParams)
    }
}
