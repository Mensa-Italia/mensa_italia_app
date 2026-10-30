package it.mensa.shared.changelog

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.db.MensaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Una novita' del changelog. [icon] e' un nome semantico: ogni piattaforma lo
 * traduce nella sua icona (SF Symbol, Material).
 */
data class ChangelogItem(
    val icon: String,
    val titleKey: String,
    val titleFallback: String,
    val bodyKey: String,
    val bodyFallback: String,
)

/**
 * "Novita'" all'apertura dell'app, una volta sola per versione.
 *
 * C'era nella vecchia app Flutter (`showChangelog`, confrontava la versione
 * con l'ultima vista) e la riscrittura nativa non l'aveva portato. Qui la
 * versione e' [ID], scritto a mano e non il numero dell'app: il numero lo
 * decide il tag in CI, e non ogni rilascio ha qualcosa da raccontare. Per
 * attivarlo in un rilascio si cambiano [ID] e [items]; con [items] vuota non
 * compare.
 *
 * Si mostra a chi apre l'app con la sessione gia' attiva, cioe' al socio che
 * ha appena aggiornato. Subito dopo un login no: li' ci sono gia'
 * l'onboarding (socio nuovo) o la proposta della passkey, e il changelog
 * comparira' all'apertura seguente.
 */
class Changelog internal constructor(
    private val db: MensaDatabase,
    private val auth: AuthRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val items: List<ChangelogItem> = ITEMS

    @Throws(Exception::class)
    suspend fun shouldShow(): Boolean {
        if (items.isEmpty()) return false
        if (auth.lastLoginMethod.value != null) return false
        val seen = runCatching { db.keyValueQueries.selectById(KEY_SEEN).awaitAsOneOrNull()?.value_ }.getOrNull()
        return seen != ID
    }

    /** Visto: non ricompare fino al prossimo [ID]. Anche dall'onboarding, per i soci nuovi. */
    fun markSeen() {
        scope.launch { runCatching { db.keyValueQueries.insertOrReplace(key = KEY_SEEN, value_ = ID) } }
    }

    companion object {
        /** Cambialo, con [ITEMS], per far ricomparire il changelog in un rilascio. */
        const val ID = "2026-11-medaglie-regione"

        private const val KEY_SEEN = "changelog.seen"

        private val ITEMS = listOf(
            ChangelogItem(
                icon = "medal",
                titleKey = "changelog.2026_11b.region_medals.title",
                titleFallback = "Le medaglie della tua regione",
                bodyKey = "changelog.2026_11b.region_medals.body",
                bodyFallback = "Nel medagliere, accanto alle tue, le medaglie che la tua regione conquista tutta insieme: dieci traguardi all'anno, tre metalli, e il segno di chi c'era quando sono arrivate.",
            ),
            ChangelogItem(
                icon = "trophy",
                titleKey = "changelog.2026_11b.trend.title",
                titleFallback = "Su o giù da ieri",
                bodyKey = "changelog.2026_11b.trend.body",
                bodyFallback = "In classifica ogni riga dice se rispetto a ieri è salita o scesa.",
            ),
            ChangelogItem(
                icon = "fixes",
                titleKey = "changelog.2026_11b.screens.title",
                titleFallback = "Schermi grandi e pieghevoli",
                bodyKey = "changelog.2026_11b.screens.body",
                bodyFallback = "Layout a due colonne sui pieghevoli aperti e sui tablet, animazioni più leggere, batteria più contenta.",
            ),
        )
    }
}
