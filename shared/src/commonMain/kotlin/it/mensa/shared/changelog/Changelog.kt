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
        const val ID = "2026-11-stagione"

        private const val KEY_SEEN = "changelog.seen"

        private val ITEMS = listOf(
            ChangelogItem(
                icon = "trophy",
                titleKey = "changelog.2026_11.season.title",
                titleFallback = "Si gioca a stagioni",
                bodyKey = "changelog.2026_11.season.body",
                bodyFallback = "Classifica e medaglie ripartono ogni 1° ottobre e si chiudono il 30 settembre: chi arriva adesso parte alla pari. Le stagioni chiuse restano nell'albo d'oro e nelle annate del medagliere.",
            ),
            ChangelogItem(
                icon = "medal",
                titleKey = "changelog.2026_11.season_medals.title",
                titleFallback = "Medaglie: di stagione e per sempre",
                bodyKey = "changelog.2026_11.season_medals.body",
                bodyFallback = "Le medaglie sui timbri, i viaggi e le occasioni si vincono ogni stagione, con soglie nuove alla portata di una stagione; quelle su tessera, app e calendario restano per sempre. Quelle già vinte le ritrovi nell'annata 2025/26.",
            ),
            ChangelogItem(
                icon = "medal",
                titleKey = "changelog.2026_11.medals.title",
                titleFallback = "Medagliere più chiaro",
                bodyKey = "changelog.2026_11.medals.body",
                bodyFallback = "Filtra le medaglie per metallo e, in ogni medaglia sui timbri, leggi quali timbri contano e perché.",
            ),
            ChangelogItem(
                icon = "medal",
                titleKey = "changelog.2026_11.region_medals.title",
                titleFallback = "Le medaglie della tua regione",
                bodyKey = "changelog.2026_11.region_medals.body",
                bodyFallback = "Nel medagliere, accanto alle tue, le medaglie che la tua regione conquista tutta insieme: dieci traguardi a stagione, tre metalli, e il segno di chi c'era quando sono arrivate.",
            ),
            ChangelogItem(
                icon = "shield",
                titleKey = "changelog.2026_11.privacy.title",
                titleFallback = "Se preferisci non esserci",
                bodyKey = "changelog.2026_11.privacy.body",
                bodyFallback = "Dalle impostazioni della classifica puoi uscirne quando vuoi: la tua riga sparisce per tutti, i tuoi timbri restano.",
            ),
        )
    }
}
