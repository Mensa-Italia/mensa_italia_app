package it.mensa.app.features.tableport.medals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EmojiFlags
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.features.leaderboard.mathInline
import it.mensa.app.support.tr
import it.mensa.shared.koinAccess
import it.mensa.shared.medals.RegionMedalKind
import it.mensa.shared.medals.RegionMedalStatus
import it.mensa.shared.medals.RegionMedalTier
import it.mensa.shared.medals.RegionMedalsState
import it.mensa.shared.medals.formatFigure
import kotlin.math.roundToInt

/** Which of the cabinet's two pages is showing. */
internal enum class CabinetPage { Mine, Region }

/** A region medal's name, the lines that explain it, its icon and the labels of its secondary figures. */
internal data class RegionMedalInfo(
    val title: String,
    val detail: String,
    val rule: String,
    val icon: ImageVector,
    /** One label per secondary figure, in the order the server sends them. */
    val figures: List<String>,
)

/**
 * Each region medal's texts and icon, by kind. The rules themselves
 * (thresholds, figures) come from the server through the shared module's
 * `RegionMedalStatus`; iOS keeps the same table in `RegionMedals.swift`.
 * Plain adult Italian, no formulas: those are in the info sheet.
 */
internal object RegionMedalCatalog {
    val info: Map<RegionMedalKind, RegionMedalInfo> = mapOf(
        RegionMedalKind.PRESENT to RegionMedalInfo(
            "Tutti presenti",
            "I soci che tornano agli eventi, in rapporto a quanti sono.",
            "Soci della regione che nell'anno si sono visti ad almeno due eventi diversi, in rapporto a quanti sono. Chi compare una volta sola non conta.",
            Icons.Filled.Groups,
            listOf("Soci tornati", "Di passaggio"),
        ),
        RegionMedalKind.HOST to RegionMedalInfo(
            "Casa aperta",
            "Le regioni da cui arrivano ospiti agli eventi di casa.",
            "Da quante regioni diverse arrivano, agli eventi della regione, soci che nell'anno si sono visti almeno due volte.",
            Icons.Filled.Home,
            listOf("Ospiti da fuori"),
        ),
        RegionMedalKind.AWAY to RegionMedalInfo(
            "In trasferta",
            "Le regioni in cui i soci vanno a timbrare fuori casa.",
            "In quante altre regioni i soci della regione, visti almeno due volte nell'anno, sono andati a timbrare.",
            Icons.Filled.Luggage,
            listOf("Soci in trasferta"),
        ),
        RegionMedalKind.MONTHS to RegionMedalInfo(
            "Senza pause",
            "I mesi dell'anno con un evento timbrato in regione.",
            "Mesi dell'anno con almeno un evento nella regione a cui qualcuno ha timbrato.",
            Icons.Filled.EventRepeat,
            listOf("Eventi in regione"),
        ),
        RegionMedalKind.WELCOME to RegionMedalInfo(
            "Benvenuto",
            "Chi si è iscritto quest'anno e ha timbrato presto.",
            "Tra chi si è iscritto all'app quest'anno, quanti hanno preso il primo timbro entro due mesi.",
            Icons.Filled.WavingHand,
            listOf("Nuovi con un timbro", "Nuovi iscritti"),
        ),
        RegionMedalKind.FULLHOUSE to RegionMedalInfo(
            "Sala piena",
            "L'evento più partecipato dell'anno.",
            "L'evento dell'anno con più timbratori, in rapporto ai soci della regione.",
            Icons.Filled.EventSeat,
            listOf("Timbratori"),
        ),
        RegionMedalKind.CORE to RegionMedalInfo(
            "Zoccolo duro",
            "Chi torna davvero agli eventi della regione.",
            "Quanti soci, rispetto a quanti sono, tornano davvero agli eventi della regione: conta chi c'è spesso, poco chi passa una volta.",
            Icons.Filled.Shield,
            listOf("Persone", "Effettive"),
        ),
        RegionMedalKind.NATIONAL to RegionMedalInfo(
            "Padroni di casa",
            "Gli eventi nazionali ospitati in regione.",
            "Eventi nazionali ospitati nella regione.",
            Icons.Filled.EmojiFlags,
            emptyList(),
        ),
        RegionMedalKind.RELAY to RegionMedalInfo(
            "Staffetta",
            "Le persone che organizzano eventi in regione.",
            "Quante persone diverse hanno organizzato eventi nella regione.",
            Icons.Filled.Handshake,
            listOf("Eventi in regione"),
        ),
        RegionMedalKind.PODIUM to RegionMedalInfo(
            "Podio",
            "Il posto della regione in classifica a fine anno.",
            "Posizione della regione nella classifica regioni a fine anno. Si assegna il 31 dicembre.",
            Icons.Filled.EmojiEvents,
            listOf("Punteggio della regione"),
        ),
    )
}

/** The region page as the cabinet shows it. */
internal data class RegionMedalsUi(
    /** The flag is on and the member's region is known: the switch exists. */
    val available: Boolean,
    /** Read at least once (or from the saved copy). */
    val loaded: Boolean,
    /** The last read failed with nothing read before. */
    val failed: Boolean,
    val regionName: String,
    val year: Int,
    /** This year's medals, best first. */
    val current: List<Medal>,
    /** Earlier years, latest first, each in the kinds' order. */
    val past: List<Pair<Int, List<Medal>>>,
) {
    val earnedCount: Int get() = current.count { it.isEarned }
}

/**
 * The member's region medals, rereading them when the cabinet appears. The
 * repository does nothing while the `region_medals_enabled` flag is off.
 */
@Composable
internal fun rememberRegionMedals(): RegionMedalsUi {
    val repository = remember { koinAccess().regionMedals }
    val available by repository.observeAvailable().collectAsState(initial = false)
    val state by repository.observe().collectAsState(initial = null)
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(repository) {
        failed = runCatching { repository.refresh() }.isFailure && state == null
    }
    val loaded = state
    return RegionMedalsUi(
        available = available,
        loaded = loaded != null,
        failed = failed,
        regionName = loaded?.regionName.orEmpty(),
        year = loaded?.year ?: 0,
        current = if (loaded == null) emptyList() else loaded.currentYear.map { it.toMedal(loaded, past = false) },
        past = if (loaded == null) emptyList() else loaded.pastYearList.map { year ->
            year to loaded.past(year).map { it.toMedal(loaded, past = true) }
        },
    )
}

@Composable
private fun RegionMedalStatus.toMedal(state: RegionMedalsState, past: Boolean): Medal {
    val info = RegionMedalCatalog.info.getValue(kind)
    val key = kind.key
    val next = if (past) null else nextTier
    val nextLevel = levels.firstOrNull { it.tier == next }
    val figureRows = info.figures.mapIndexedNotNull { index, label ->
        extra.getOrNull(index)?.let { tr("medals.region.$key.fig$index", label) to formatFigure(it) }
    }
    return Medal(
        id = "region-$key-$year",
        title = tr("medals.region.$key.title", info.title),
        detail = tr("medals.region.$key.detail", info.detail),
        icon = info.icon,
        // The metal reached, or bronze: what an unearned one is shown in.
        tier = if (isEarned) tier.toMedalTier() else MedalTier.Bronze,
        isEarned = isEarned,
        isHidden = false,
        value = value.roundToInt(),
        nextTier = next?.toMedalTier(),
        nextThreshold = nextLevel?.threshold?.roundToInt(),
        progress = if (past) 1f else progress.toFloat(),
        levels = levels.map { it.tier.toMedalTier() to it.threshold.roundToInt() },
        countsStamps = false,
        region = RegionMedalDetail(
            year = year,
            rule = tr("medals.region.$key.rule", info.rule),
            valueLabel = valueLabel,
            isPast = past,
            isPodium = kind == RegionMedalKind.PODIUM,
            ladder = levels.map { RegionLadderStep(it.tier.toMedalTier(), thresholdLabel(it), reached = it.tier.ordinal <= tier.ordinal) },
            nextLabel = nextLevel?.let(::thresholdLabel),
            figures = figureRows,
            minimumNote = if (!eligible && minimum > 0 && !past) {
                tr("medals.region.minimum", "Conta dal minimo di {minimum} persone.", "minimum" to formatFigure(minimum))
            } else null,
            witnessed = state.witnessed(this),
        ),
    )
}

private fun RegionMedalTier.toMedalTier(): MedalTier = when (this) {
    RegionMedalTier.GOLD -> MedalTier.Gold
    RegionMedalTier.SILVER -> MedalTier.Silver
    else -> MedalTier.Bronze
}

// ─── The cabinet's switch ────────────────────────────────────────────────────

/** "Le mie" / "La mia regione", as the leaderboard page's switch. */
@Composable
internal fun CabinetSwitch(page: CabinetPage, onChange: (CabinetPage) -> Unit, modifier: Modifier = Modifier) {
    val labels = listOf(
        CabinetPage.Mine to tr("medals.tab.mine", "Le mie"),
        CabinetPage.Region to tr("medals.tab.region", "La mia regione"),
    )
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, (option, label) ->
            SegmentedButton(
                selected = page == option,
                onClick = { onChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
            ) { Text(label) }
        }
    }
}

// ─── The region page ─────────────────────────────────────────────────────────

/**
 * What the region's medals are, said once at the top of their page: of the
 * team, by year, counting people. Mirrors iOS `RegionRules`.
 */
@Composable
internal fun RegionRules(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = tr("medals.region.rules.title", "Come funzionano"),
            style = MaterialTheme.typography.titleSmall,
        )
        StampRule(
            Icons.Filled.Groups,
            tr(
                "medals.region.rules.team",
                "Le medaglie di regione sono della squadra: le conquista la regione tutta insieme e le vede chi ne fa parte oggi.",
            ),
        )
        StampRule(
            Icons.Filled.CalendarMonth,
            tr("medals.region.rules.yearly", "Ogni anno si ricomincia; le annate passate restano in bacheca."),
        )
        StampRule(
            Icons.Filled.Person,
            tr(
                "medals.region.rules.people",
                "Contano le persone, non i timbri: chi si vede una volta sola nell'anno non fa numero.",
            ),
        )
        Text(
            text = mathInline(
                tr("medals.region.rules.more", "Il criterio esatto, formula compresa, è nella scheda ⓘ, sezione Matematica."),
                MaterialTheme.typography.labelMedium.fontSize,
                Color.White.copy(alpha = 0.45f),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.45f),
        )
    }
}

/** An earlier year's header: tap to open or close its medals. */
@Composable
internal fun YearHeader(year: Int, earned: Int, total: Int, expanded: Boolean, onClick: () -> Unit) {
    val summary = tr("medals.region.count_of", "{earned} su {total}", "earned" to earned, "total" to total)
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .clickable(onClickLabel = null, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$year, $summary" }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$year",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(summary, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.6f))
        Spacer(Modifier.size(8.dp))
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
        )
    }
}

// ─── The region medal's detail ───────────────────────────────────────────────

/** "C'eri anche tu": the member was in the region when this metal was reached. */
@Composable
internal fun WitnessBadge(modifier: Modifier = Modifier) {
    Row(
        modifier
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .background(Color.White.copy(alpha = 0.06f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
        Text(
            text = tr("medals.region.witness", "C'eri anche tu"),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.8f),
        )
    }
}

/** The ladder of a region medal: each metal with the figure that earns it, the ones the year reached lit. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RegionTierLadder(medal: Medal, region: RegionMedalDetail, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        region.ladder.forEachIndexed { index, step ->
            val isNext = medal.nextTier == step.tier
            if (index > 0) {
                Box(
                    Modifier
                        .weight(1f)
                        .padding(top = 27.dp)
                        .height(2.dp)
                        .background(if (step.reached) step.tier.metal.mid else Color.White.copy(alpha = 0.14f), CircleShape),
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics(mergeDescendants = true) {},
            ) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    if (isNext) {
                        CircularWavyProgressIndicator(
                            progress = { medal.progress },
                            color = step.tier.metal.mid,
                            trackColor = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.size(56.dp),
                        )
                    }
                    MedalArtwork(tier = step.tier, icon = medal.icon, size = 38.dp, locked = !step.reached)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = step.tier.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (step.reached) step.tier.metal.hi else Color.White.copy(alpha = 0.45f),
                )
                Text(
                    text = step.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (step.reached || isNext) Color.White else Color.White.copy(alpha = 0.45f),
                )
            }
        }
    }
}

/**
 * Where the region stands with a medal: the figure, the secondary figures in
 * words, and how far the next metal is. The podium says when it's awarded.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RegionStatus(medal: Medal, region: RegionMedalDetail, modifier: Modifier = Modifier) {
    val metal = medal.tier.metal
    val nextTier = medal.nextTier
    Column(modifier.widthIn(max = 320.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = when {
                region.isPast -> tr("medals.region.value_year", "Nell'anno")
                region.isPodium -> tr("medals.region.value_podium", "Posizione attuale")
                else -> tr("medals.region.value_now", "Adesso")
            }.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 2.sp,
            color = Color.White.copy(alpha = 0.5f),
        )
        Text(
            text = region.valueLabel,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (medal.isEarned) metal.hi else Color.White,
        )
        if (region.figures.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                region.figures.forEach { (label, figure) ->
                    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(figure, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        region.minimumNote?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.55f))
        }
        if (region.isPodium && !medal.isEarned && !region.isPast) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = tr("medals.region.podium_pending", "Si assegna il 31 dicembre"),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.75f),
            )
        }
        if (nextTier != null && region.nextLabel != null) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = tr("medals.next", "Prossimo traguardo: %1\$@ a %2\$d")
                    .replace("%1\$@", nextTier.label)
                    .replace("%2\$d", region.nextLabel),
                style = MaterialTheme.typography.titleSmall,
            )
            if (!region.isPodium) {
                Spacer(Modifier.height(10.dp))
                LinearWavyProgressIndicator(
                    progress = { medal.progress },
                    color = nextTier.metal.mid,
                    trackColor = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = tr("medals.progress", "%1\$d su %2\$d")
                        .replace("%1\$d", region.valueLabel)
                        .replace("%2\$d", region.nextLabel),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (medal.isEarned && !region.isPast) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = tr("medals.max_tier", "Livello massimo raggiunto"),
                style = MaterialTheme.typography.titleSmall,
                color = metal.hi,
            )
        }
    }
}

/** How the medal is earned, in the card of the stamp rules. */
@Composable
internal fun RegionRuleCard(rule: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = tr("medals.region.rule_title", "Come si ottiene"),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = rule,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
