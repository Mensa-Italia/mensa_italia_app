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
    const val SIG = "sig"
    const val ADDON = "addon"

    /** Organigramma: gruppi e cariche. Dietro il flag `org_chart_enabled`. */
    const val ORG = "org"

    /** Gruppi locali (linktree). Dietro il flag `local_groups_enabled`. */
    const val LINKTREE_LINK = "linktree_link"
}
