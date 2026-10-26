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
        const val ID = "2026-11-tableport"

        private const val KEY_SEEN = "changelog.seen"

        private val ITEMS = listOf(
            ChangelogItem(
                icon = "passport",
                titleKey = "changelog.2026_11.tableport.title",
                titleFallback = "Il Tableport è in Home",
                bodyKey = "changelog.2026_11.tableport.body",
                bodyFallback = "Il passaporto dei timbri ora sta sopra i tuoi ticket: copertina in pelle, si apre come un libro e i timbri scorrono tutti insieme.",
            ),
            ChangelogItem(
                icon = "medal",
                titleKey = "changelog.2026_11.medals.title",
                titleFallback = "Il medagliere",
                bodyKey = "changelog.2026_11.medals.body",
                bodyFallback = "Trenta medaglie da sbloccare con timbri, eventi e vita associativa. Alcune sono segrete: le trovi nel passaporto.",
            ),
            ChangelogItem(
                icon = "fixes",
                titleKey = "changelog.2026_11.fixes.title",
                titleFallback = "Risolti bug minori",
                bodyKey = "changelog.2026_11.fixes.body",
                bodyFallback = "I QR dei francobolli e i link aprono l'app, l'avvio è più veloce e funziona anche offline, le notifiche portano dritte al contenuto.",
            ),
        )
    }
}
