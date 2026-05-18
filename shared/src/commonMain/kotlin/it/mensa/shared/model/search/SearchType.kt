package it.mensa.shared.model.search

/**
 * Tipi indicizzati lato server. Stringhe esposte come costanti per evitare typo
 * mantenendo la flessibilità (il server può aggiungerne di nuovi).
 */
object SearchType {
    const val EVENT = "event"
    const val DEAL = "deal"
    const val USER = "user"
    const val DOCUMENT = "document"
    const val BOUTIQUE = "boutique"
    const val SIG = "sig"
    const val ADDON = "addon"
}
