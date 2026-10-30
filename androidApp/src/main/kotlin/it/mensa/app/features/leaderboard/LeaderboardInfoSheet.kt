package it.mensa.app.features.leaderboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Approval
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import it.mensa.app.support.tr
import it.mensa.shared.leaderboard.LeaderboardKind

/**
 * How the points are made, twice: in words for everyone, and as the
 * formulas `view_leaderboard` computes for whoever wants to check them.
 * The formulas mirror the SQL; change both together. Material 3 Expressive:
 * a bottom sheet with a connected toggle-button group, segmented list and
 * cards. Mirrors iOS `LeaderboardInfoSheet`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LeaderboardInfoSheet(kinds: List<LeaderboardKind>, onDismiss: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = tr("leaderboard.how_it_works", "Come funziona"),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(16.dp))
            val options = listOf(
                tr("leaderboard.info.mode.basic", "Base"),
                tr("leaderboard.info.mode.math", "Matematica"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                options.forEachIndexed { index, label ->
                    ToggleButton(
                        checked = tab == index,
                        onCheckedChange = { tab = index },
                        shapes = if (index == 0) {
                            ButtonGroupDefaults.connectedLeadingButtonShapes()
                        } else {
                            ButtonGroupDefaults.connectedTrailingButtonShapes()
                        },
                        modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                    ) { Text(label) }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (tab == 0) BasicExplanation(kinds) else MathExplanation(kinds)
        }
    }
}

// ── In words ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BasicExplanation(kinds: List<LeaderboardKind>) {
    // One group per ranking, in LeaderboardKind order; only the ones switched on.
    val groups = listOf(
        tr("leaderboard.tab.members", "Soci") to listOf(
            Triple(
                Icons.Outlined.Approval,
                tr("leaderboard.info.stamps.title", "Timbri"),
                tr(
                    "leaderboard.info.stamps.detail_v6",
                    "Ogni timbro vale fino a 20 punti. Vale tanto se all'evento hanno timbrato in tanti rispetto ai soci della regione, poco se in pochi. Ed è pesato per quanto è ampia, rispetto ai soci della regione, la comunità che timbra ai suoi eventi.",
                ),
            ),
            Triple(
                Icons.Outlined.HourglassBottom,
                tr("leaderboard.info.time.title_v2", "Scadenza"),
                tr("leaderboard.info.time.detail_v3", "I punti durano un anno e calano piano piano; due anni per gli eventi nazionali."),
            ),
            Triple(
                Icons.Outlined.MilitaryTech,
                tr("leaderboard.info.medals.title", "Medaglie"),
                tr(
                    "leaderboard.info.medals.detail_v5",
                    "Ogni medaglia del Tableport aggiunge punti: più il metallo è prezioso, più punti. Contano solo i timbri che in classifica valgono almeno un punto.",
                ),
            ),
            Triple(
                Icons.Outlined.EmojiEvents,
                tr("leaderboard.info.events.title", "Eventi organizzati"),
                tr(
                    "leaderboard.info.events.detail_v4",
                    "Chi organizza prende fino a 10 punti a evento, con lo stesso criterio dei timbri. Chi fa parte dello staff di una regione non prende i timbri degli eventi di casa.",
                ),
            ),
            Triple(
                Icons.Outlined.FormatListNumbered,
                tr("leaderboard.info.top.title", "I primi 10"),
                tr("leaderboard.info.top.detail_v3", "Si vedono i primi dieci. A pari punti, pari posizione."),
            ),
        ),
        tr("leaderboard.tab.regions", "Regioni") to listOf(
            Triple(
                Icons.Outlined.Map,
                tr("leaderboard.info.regions.score.title", "Punteggio"),
                tr(
                    "leaderboard.info.regions.score.detail_v2",
                    "Si sommano gli eventi fatti in regione nell'ultimo anno, da chiunque organizzati, fino a 10 punti l'uno. Il totale è poi pesato per quanto è ampia, rispetto ai soci della regione, la comunità che timbra ai suoi eventi. I nazionali ospitati si aggiungono a parte.",
                ),
            ),
            Triple(
                Icons.Outlined.Groups,
                tr("leaderboard.info.regions.who.title", "Chi c'è"),
                tr(
                    "leaderboard.info.regions.who.detail_v2",
                    "In classifica le regioni con almeno 3 punti. Chi fa parte dello staff di una regione vede anche la sua, sotto la lista, anche se è sotto i 3. A pari punti, pari posizione. Tocca una riga per vedere il conto, passo per passo.",
                ),
            ),
        ),
        tr("leaderboard.tab.secretaries", "Segretari") to listOf(
            Triple(
                Icons.Outlined.Badge,
                tr("leaderboard.info.secretaries.title", "Come la regione"),
                tr("leaderboard.info.secretaries.detail_v2", "Il segretario vale quanto la sua regione: stessi punti, stessa posizione. Dove i segretari sono due, pari merito."),
            ),
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        groups.filterIndexed { index, _ -> LeaderboardKind.entries[index] in kinds }.forEach { (group, points) ->
            Text(
                text = group,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
            )
            points.forEachIndexed { index, (icon, title, detail) -> InfoPoint(icon, title, detail, index, points.size) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun InfoPoint(icon: ImageVector, title: String, detail: String, index: Int, count: Int) {
    Card(shape = ListItemDefaults.segmentedShapes(index = index, count = count).shape) {
        ListItem(
            leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            headlineContent = { Text(title) },
            supportingContent = { Text(detail) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

// ── In formulas ──────────────────────────────────────────────────────────────

@Composable
private fun MathExplanation(kinds: List<LeaderboardKind>) {
    // Laid out like a proof: definitions, lemmas, theorems, corollaries.
    // Definitions and lemmas always; each theorem only with its ranking on.
    val members = LeaderboardKind.MEMBERS in kinds
    val regions = LeaderboardKind.REGIONS in kinds || LeaderboardKind.SECRETARIES in kinds
    val secretaries = LeaderboardKind.SECRETARIES in kinds
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormulaCard(
            title = tr("leaderboard.math.definitions", "Definizioni"),
            formula = {
                MathLine("s = ")
                Fraction("p", "N_{r}^{0,7} · N̄^{0,3}")
            },
            caption = tr(
                "leaderboard.math.definitions.caption",
                "Sia \$p\$ il numero di soci che hanno preso il timbro di un evento, organizzatore escluso; \$N_{r}\$ i soci attivi della regione dell'evento, \$N̄\$ quelli di una regione media; \$Δt\$ i giorni trascorsi. \$s\$ è la quota di timbratori: l'esponente 0,7 perché l'affluenza cresce meno che linearmente con i soci. Le sedi che coprono due regioni contano come una; per gli eventi nazionali o senza regione \$N_{r} = N̄\$.",
            ),
        )
        FormulaCard(
            title = tr("leaderboard.math.lemma1", "Lemma 1 · la curva"),
            formula = {
                // Two lines: the curve, then its argument.
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MathLine("σ(s) = t^{2}(3 − 2t)")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MathLine("t = clamp(")
                        Fraction("s − 2%", "13%")
                        MathLine(", 0, 1)")
                    }
                }
            },
            caption = tr(
                "leaderboard.math.lemma1.caption",
                "\$σ\$ è nulla sotto il 2%, vale ½ all'8,5%, 1 dal 15%; continua e con derivata nulla alle soglie, quindi senza salti. Le soglie sono quote, non numeri di timbri: lo stesso evento pesa diversamente in regioni di dimensione diversa.",
            ),
            extra = { CurvePlot() },
        )
        FormulaCard(
            title = tr("leaderboard.math.lemma2", "Lemma 2 · la comunità"),
            formula = {
                MathLine("e = ")
                Fraction("(Σ n_{i})^{2}", "Σ n_{i}^{2}")
                MathLine(",  c = σ(")
                Fraction("e", "N_{r}^{0,7} · N̄^{0,3}")
                MathLine(")")
            },
            caption = tr(
                "leaderboard.math.lemma2.caption",
                "\$n_{i}\$ i timbri dell'\$i\$-esimo socio agli eventi regionali degli ultimi 12 mesi. \$e\$ è il numero di soci equivalenti: uguale ai timbratori se ognuno timbra una volta sola, tende a 1 se timbra sempre uno solo. La sua quota sulla regione passa nella stessa \$σ\$, quindi \$c = 1\$ dal 15% in su. Per i nazionali \$c = 1\$.",
            ),
        )
        FormulaCard(
            title = tr("leaderboard.math.lemma3", "Lemma 3 · la scadenza"),
            formula = {
                MathLine("d = max(0, 1 − ")
                Fraction("Δt", "365")
                MathLine(")")
            },
            caption = tr("leaderboard.math.lemma3.caption", "Lineare; 730 al posto di 365 per gli eventi nazionali."),
        )
        if (members) {
            FormulaCard(
                title = tr("leaderboard.math.thm1", "Teorema 1 · il timbro"),
                formula = { MathLine("T = 20 · d · σ(s) · c") },
                caption = tr(
                    "leaderboard.math.thm1.caption",
                    "Con \$s\$, \$d\$ e \$c\$ dell'evento e della sua regione. \$T = 0\$ per chi fa parte dello staff della regione dell'evento.",
                ),
            )
        }
        FormulaCard(
            title = tr("leaderboard.math.thm2", "Teorema 2 · l'evento organizzato"),
            formula = { MathLine("E = 10 · d · σ(s) · c") },
            caption = tr("leaderboard.math.thm2.caption", "Per chi lo organizza; \$d\$ dalla data dell'evento."),
        )
        if (members) {
            FormulaCard(
                title = tr("leaderboard.math.thm3", "Teorema 3 · le medaglie"),
                formula = { MathLine("M = Σ m,  m ∈ {3, 5, 8, 10, 20}") },
                caption = tr(
                    "leaderboard.math.thm3.caption_v2",
                    "Bronzo 3, argento 5, oro 10, platino 20; 8 per le medaglie a livello unico. Contano solo i timbri con \$20 · σ(s) ≥ 1\$, cioè \$s\$ oltre il 3,8%, a prescindere da \$d\$ e \$c\$.",
                ),
            )
            FormulaCard(
                title = tr("leaderboard.math.cor1", "Corollario 1 · il socio"),
                formula = { MathLine("S = ΣT + ΣE + M") },
                caption = tr("leaderboard.math.cor1.caption", "Sui timbri e sugli eventi degli ultimi 12 mesi (24 i nazionali); ordinamento per \$S\$ arrotondato."),
            )
        }
        if (regions) {
            FormulaCard(
                title = tr("leaderboard.math.thm4", "Teorema 4 · la regione"),
                formula = { MathLine("R = c · ΣE_{r} + ΣE_{n}") },
                caption = tr(
                    "leaderboard.math.thm4.caption",
                    "\$E_{r}\$ come nel Teorema 2 ma con \$c = 1\$, sugli eventi non nazionali degli ultimi 12 mesi nella regione, da chiunque organizzati; il fattore si applica una volta sola alla somma, ed è lo stesso: \$Σ c·E = c·ΣE\$. \$E_{n}\$ sui nazionali ospitati, \$d\$ a 24 mesi, senza \$c\$.",
                ),
            )
        }
        if (secretaries) {
            FormulaCard(
                title = tr("leaderboard.math.cor2", "Corollario 2 · il segretario"),
                formula = { MathLine("S_{seg} = R") },
                caption = tr("leaderboard.math.cor2.caption_v2", "Della regione di cui è segretario: stessi punti, stessa posizione."),
            )
        }
    }
}

@Composable
internal fun FormulaCard(
    title: String,
    formula: @Composable () -> Unit,
    caption: String,
    extra: (@Composable () -> Unit)? = null,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) { formula() }
            extra?.let {
                Spacer(Modifier.height(12.dp))
                it()
            }
            if (caption.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = mathInline(
                        caption,
                        MaterialTheme.typography.bodyMedium.fontSize,
                        MaterialTheme.colorScheme.onSurface,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

internal val MathSize = 21.sp
internal val MathStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = MathSize)

@Composable
internal fun MathLine(source: String) {
    Text(text = math(source), style = MathStyle, color = MaterialTheme.colorScheme.onSurface, softWrap = false)
}

/** Numerator over denominator, with the bar as wide as the wider of the two. */
@Composable
internal fun Fraction(numerator: String, denominator: String) {
    Column(
        Modifier.width(IntrinsicSize.Max).padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(math(numerator), style = MathStyle.copy(fontSize = 18.sp), color = MaterialTheme.colorScheme.onSurface, softWrap = false)
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .height(1.5.dp)
                .background(MaterialTheme.colorScheme.onSurface),
        )
        Text(math(denominator), style = MathStyle.copy(fontSize = 18.sp), color = MaterialTheme.colorScheme.onSurface, softWrap = false)
    }
}

/**
 * Typesets a formula the way a book would: single letters (variables) in
 * italic, words (max, clamp) and numbers upright, `^{…}` raised and `_{…}`
 * lowered.
 */
internal fun math(source: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < source.length) {
        val c = source[i]
        when {
            (c == '^' || c == '_') && source.getOrNull(i + 1) == '{' -> {
                val close = source.indexOf('}', i)
                val inner = source.substring(i + 2, close)
                withStyle(
                    SpanStyle(
                        fontSize = 0.62.em,
                        baselineShift = if (c == '^') BaselineShift(0.55f) else BaselineShift(-0.2f),
                    ),
                ) { append(math(inner)) }
                i = close + 1
            }
            // Operators, upright and on their own: ΣT is Σ then T.
            c == 'Σ' || c == 'Δ' -> {
                append(c)
                i++
            }
            c.isLetter() -> {
                var j = i
                // A letter and its combining marks (N̄) are one symbol.
                while (j < source.length && (source[j].isLetter() || source[j] == '̄')) j++
                val word = source.substring(i, j)
                val symbol = word.count { it.isLetter() } == 1
                if (symbol) withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(word) } else append(word)
                i = j
            }
            else -> {
                append(c)
                i++
            }
        }
    }
}

/** Captions name the symbols between `$…$`, set like the formulas above. */
internal fun mathInline(caption: String, size: TextUnit, color: Color): AnnotatedString = buildAnnotatedString {
    caption.split('$').forEachIndexed { index, part ->
        if (index % 2 == 0) {
            append(part)
        } else {
            withStyle(SpanStyle(fontFamily = FontFamily.Serif, fontSize = size * 1.15f, color = color)) {
                append(math(part))
            }
        }
    }
}

/**
 * The stamp curve over s, the share of the region's members who stamped, from
 * 0 to 20%: nothing under 2%, everything from 15%, a smoothstep in between,
 * with the half-way point at 8.5% marked.
 */
@Composable
private fun CurvePlot() {
    val measurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val labelStyle = TextStyle(color = colors.onSurfaceVariant, fontSize = 10.sp)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(120.dp),
    ) {
        val left = 26.dp.toPx()
        val bottom = size.height - 18.dp.toPx()
        val top = 8.dp.toPx()
        val right = size.width - 6.dp.toPx()
        val maxN = 20f
        fun x(n: Float) = left + (right - left) * n / maxN
        fun y(v: Float) = bottom - (bottom - top) * v
        fun f(s: Float): Float {
            val t = ((s - 2f) / 13f).coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }

        // Axes.
        drawLine(colors.outlineVariant, Offset(left, bottom), Offset(right, bottom), strokeWidth = 1.dp.toPx())
        drawLine(colors.outlineVariant, Offset(left, top), Offset(left, bottom), strokeWidth = 1.dp.toPx())

        val curve = Path()
        val area = Path().apply { moveTo(x(0f), bottom) }
        val steps = 120
        for (k in 0..steps) {
            val n = maxN * k / steps
            val p = Offset(x(n), y(f(n)))
            if (k == 0) curve.moveTo(p.x, p.y) else curve.lineTo(p.x, p.y)
            area.lineTo(p.x, p.y)
        }
        area.lineTo(x(maxN), bottom)
        area.close()
        drawPath(area, Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.25f), Color.Transparent), startY = top, endY = bottom))
        drawPath(curve, colors.primary, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

        // Half-way at 8.5%.
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        drawLine(colors.tertiary, Offset(x(8.5f), bottom), Offset(x(8.5f), y(0.5f)), strokeWidth = 1.dp.toPx(), pathEffect = dash)
        drawLine(colors.tertiary, Offset(left, y(0.5f)), Offset(x(8.5f), y(0.5f)), strokeWidth = 1.dp.toPx(), pathEffect = dash)
        drawCircle(colors.tertiary, radius = 4.dp.toPx(), center = Offset(x(8.5f), y(0.5f)))

        listOf(0, 5, 10, 15, 20).forEach { n ->
            val label = measurer.measure("$n%", labelStyle)
            drawText(label, topLeft = Offset(x(n.toFloat()) - label.size.width / 2f, bottom + 4.dp.toPx()))
        }
        listOf(0 to "0", 1 to "20", 2 to "10").forEach { (v, text) ->
            val value = if (v == 2) 0.5f else v.toFloat()
            val label = measurer.measure(text, labelStyle)
            drawText(label, topLeft = Offset(left - label.size.width - 5.dp.toPx(), y(value) - label.size.height / 2f))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(tr("leaderboard.math.plot.y", "punti del timbro"), color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Text(tr("leaderboard.math.plot.x_v4", "s, quota di soci della regione che hanno timbrato"), color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}
