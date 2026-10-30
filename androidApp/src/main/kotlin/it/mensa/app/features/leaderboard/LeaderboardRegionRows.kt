package it.mensa.app.features.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.mensa.app.support.tr
import it.mensa.shared.leaderboard.RegionRow
import it.mensa.shared.leaderboard.RegionScore
import it.mensa.shared.leaderboard.SecretaryRow

/**
 * Rows of the regions' and secretaries' rankings: the place, the name, the
 * points. The numbers behind the points are one tap away, in
 * [RegionScoreSheet]. Same segmented list as the members' rows. Mirrors iOS
 * `RegionRankRow` / `SecretaryRankRow`.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RegionItem(row: RegionRow, index: Int, count: Int, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        leadingContent = { Place(row.position, row.trend) },
        trailingContent = {
            Text(tr("leaderboard.points", "{points} pt", "points" to row.score), style = MaterialTheme.typography.titleMedium)
        },
    ) {
        Text(row.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SecretaryItem(row: SecretaryRow, index: Int, count: Int, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = if (row.isMe) {
            ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Place(row.position, row.trend)
                Spacer(Modifier.width(12.dp))
                RankAvatar(row = row, size = 40.dp)
            }
        },
        supportingContent = { Text(row.region, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            Text(tr("leaderboard.points", "{points} pt", "points" to row.score), style = MaterialTheme.typography.titleMedium)
        },
    ) {
        Text(
            text = if (row.isMe) "${row.displayName} · ${tr("leaderboard.you", "Tu")}" else row.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun Place(position: Int, trend: Int? = null) {
    Column(Modifier.widthIn(min = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val tier = podiumTier(position)
        if (tier != null) {
            Icon(Icons.Filled.MilitaryTech, contentDescription = "$position°", tint = tier.metal.mid)
        } else {
            Text("$position", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Trend(trend)
    }
}

/**
 * Places gained or lost since yesterday's snapshot, under the position:
 * "▲2" in green, "▼1" in red, nothing when unchanged or unknown. The
 * server writes `previous_position` from its daily history.
 */
@Composable
private fun Trend(trend: Int?) {
    if (trend == null || trend == 0) return
    Text(
        text = if (trend > 0) "▲$trend" else "▼${-trend}",
        style = MaterialTheme.typography.labelSmall,
        color = if (trend > 0) Color(0xFF34C759) else Color(0xFFFF453A),
    )
}

// ── The calculation ──────────────────────────────────────────────────────────

/**
 * How a region's (or its secretary's) points were made, as
 * `view_leaderboard_regioni` computes them: participation × community +
 * national events hosted. [subtitle] names the secretary when it is theirs;
 * [onOpenMember] opens their profile. Mirrors iOS `RegionScoreSheet`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RegionScoreSheet(
    score: RegionScore,
    subtitle: String?,
    onOpenMember: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(score.regionName, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 8.dp))
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            // Laid out like the proof in the info sheet: the theorem, three
            // steps with the formula and the numbers in it, the result.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val steps = scoreSteps(score)
                FormulaCard(
                    title = tr("leaderboard.calc.theorem", "Teorema · la regione"),
                    formula = { MathLine("R = c · ΣE_{r} + ΣE_{n}") },
                    caption = tr(
                        "leaderboard.calc.theorem.caption",
                        "\$ΣE_{r}\$ gli eventi in regione, \$c\$ chi timbra, \$ΣE_{n}\$ i nazionali ospitati: i tre passi qui sotto.",
                    ),
                )
                FormulaCard(
                    title = steps[0].first,
                    formula = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MathLine("ΣE_{r} = Σ 10 · d · σ(s)")
                            MathLine("= ${score.participation} pt")
                        }
                    },
                    caption = steps[0].third,
                )
                FormulaCard(
                    title = steps[1].first,
                    formula = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MathLine("c = σ(")
                                Fraction("e", "N_{r}^{0,7} · N̄^{0,3}")
                                MathLine(")")
                            }
                            MathLine("e = ${score.effectiveAttendees}, N_{r} = ${score.membersActive} ⇒ c = ${score.communityPct}%")
                        }
                    },
                    caption = steps[1].third,
                )
                FormulaCard(
                    title = steps[2].first,
                    formula = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MathLine("ΣE_{n} = Σ 10 · d · σ(s)")
                            MathLine("= ${score.nationalPoints} pt")
                        }
                    },
                    caption = steps[2].third,
                )
                FormulaCard(
                    title = tr("leaderboard.calc.result", "Risultato"),
                    formula = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MathLine("R = ${score.communityPct}% · ${score.participation} + ${score.nationalPoints}")
                            MathLine("= ${score.score} pt")
                        }
                    },
                    caption = "",
                )
            }
            if (onOpenMember != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onOpenMember, modifier = Modifier.align(Alignment.End)) {
                    Text(tr("leaderboard.calc.open_profile", "Apri il profilo"))
                }
            }
        }
    }
}

/**
 * The calculation as three steps, each a title, the number it produces and
 * the explanation: the region's events, the community that attends them,
 * the national events hosted. Written for an adult who has never seen the
 * formula. Mirrors iOS `scoreSteps`.
 */
@Composable
private fun scoreSteps(score: RegionScore): List<Triple<String, String, String>> = listOf(
    Triple(
        tr("leaderboard.calc.step1.title", "1 · Gli eventi in regione"),
        tr("leaderboard.points", "{points} pt", "points" to score.participation),
        if (score.eventsTotal == 0) {
            tr("leaderboard.calc.step1.none", "Nessun evento in regione negli ultimi 12 mesi, quindi nessun punto da questa voce.")
        } else {
            tr(
                "leaderboard.calc.step1.detail",
                "Negli ultimi 12 mesi in regione si sono fatti {events} eventi, {attendance} timbri in tutto. Ogni evento vale fino a 10 punti secondo quanti soci della regione hanno timbrato: niente se erano meno del 2%, il massimo dal 15% in su. Ne hanno superato la soglia {scoring}; i punti calano col passare dei mesi e spariscono dopo un anno. Sommati fanno {points} punti.",
                "events" to score.eventsTotal,
                "attendance" to score.attendanceTotal,
                "scoring" to score.eventsScoring,
                "points" to score.participation,
            )
        },
    ),
    Triple(
        tr("leaderboard.calc.step2.title", "2 · Chi timbra"),
        "× ${score.communityPct}%",
        if (score.distinctAttendees == 0) {
            tr("leaderboard.calc.step2.none", "Senza timbri non c'è una comunità da misurare: il fattore è 0%.")
        } else {
            tr(
                "leaderboard.calc.step2.detail",
                "A quegli eventi hanno timbrato {distinct} soci diversi. Contando quanti timbri ha ognuno, equivalgono a {effective} soci fissi: il {core}% dei {members} soci della regione. Questa quota passa nella stessa curva degli eventi, 0% sotto il 2% dei soci e 100% dal 15%, e dà il fattore comunità: {community}%. I punti del passo 1 vengono moltiplicati per questo fattore, perché il punteggio premia l'ampiezza della comunità, non il numero di eventi.",
                "distinct" to score.distinctAttendees,
                "effective" to score.effectiveAttendees,
                "core" to score.corePct,
                "members" to score.membersActive,
                "community" to score.communityPct,
            )
        },
    ),
    Triple(
        tr("leaderboard.calc.step3.title", "3 · I nazionali ospitati"),
        "+ ${tr("leaderboard.points", "{points} pt", "points" to score.nationalPoints)}",
        if (score.nationalEvents == 0) {
            tr("leaderboard.calc.step3.none", "Nessun evento nazionale ospitato negli ultimi 24 mesi: niente da aggiungere.")
        } else {
            tr(
                "leaderboard.calc.step3.detail",
                "La regione ha ospitato {events} eventi nazionali negli ultimi 24 mesi, che valgono {points} punti con la stessa curva sui timbri. Si sommano a parte, senza il fattore del passo 2: un convegno porta gente da tutta Italia, non misura la comunità locale.",
                "events" to score.nationalEvents,
                "points" to score.nationalPoints,
            )
        },
    ),
)
