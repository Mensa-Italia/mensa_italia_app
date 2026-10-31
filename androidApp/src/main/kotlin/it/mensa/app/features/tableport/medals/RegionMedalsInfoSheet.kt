package it.mensa.app.features.tableport.medals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.mensa.app.features.leaderboard.FormulaCard
import it.mensa.app.features.leaderboard.Fraction
import it.mensa.app.features.leaderboard.InfoPoint
import it.mensa.app.features.leaderboard.MathLine
import it.mensa.app.support.tr
import it.mensa.shared.medals.RegionMedalKind

/**
 * How the region's medals are earned, twice: in words, and as the formulas
 * behind the shares for whoever wants to check them. The formulas mirror
 * the server's calculation; change both together. Same sheet as the
 * leaderboard's (a toggle group, segmented list and cards). Mirrors iOS
 * `RegionMedalsInfoSheet`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RegionMedalsInfoSheet(onDismiss: () -> Unit) {
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
            if (tab == 0) BasicExplanation() else MathExplanation()
        }
    }
}

// ── In words ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BasicExplanation() {
    val general = listOf(
        Triple(
            Icons.Outlined.Groups,
            tr("medals.region.info.team.title", "La squadra"),
            tr(
                "medals.region.rules.team",
                "Le medaglie di regione sono della squadra: le conquista la regione tutta insieme e le vede chi ne fa parte oggi.",
            ),
        ),
        Triple(
            Icons.Outlined.CalendarMonth,
            tr("medals.region.info.yearly.title_v2", "Ogni stagione"),
            tr(
                "medals.region.rules.yearly_v2",
                "Ogni stagione, dal 1° ottobre al 30 settembre, si ricomincia; le annate passate restano in bacheca.",
            ),
        ),
        Triple(
            Icons.Outlined.Person,
            tr("medals.region.info.people.title", "Le persone"),
            tr(
                "medals.region.rules.people_v2",
                "Contano le persone, non i timbri: chi si vede una volta sola nella stagione non fa numero.",
            ),
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        GroupTitle(tr("medals.region.info.general", "In breve"))
        general.forEachIndexed { index, (icon, title, detail) -> InfoPoint(icon, title, detail, index, general.size) }
        GroupTitle(tr("medals.region.info.medals", "Le dieci medaglie"))
        val kinds = RegionMedalKind.entries
        kinds.forEachIndexed { index, kind ->
            val info = RegionMedalCatalog.info.getValue(kind)
            InfoPoint(
                info.icon,
                tr("medals.region.${kind.key}.title", info.title),
                tr("medals.region.${kind.key}.rule${info.keySuffix}", info.rule),
                index,
                kinds.size,
            )
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
    )
}

// ── In formulas ──────────────────────────────────────────────────────────────

@Composable
private fun MathExplanation() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormulaCard(
            title = tr("medals.region.math.definitions", "Definizioni"),
            formula = {
                MathLine("q = ")
                Fraction("v", "N_{r}^{0,7} · N̄^{0,3}")
            },
            caption = tr(
                "medals.region.math.definitions.caption",
                "Sia \$v\$ il numero di persone che la medaglia conta; \$N_{r}\$ i soci attivi del registro nella regione, \$N̄\$ quelli di una regione media. \$q\$ è la quota efficace: l'esponente 0,7 perché l'affluenza cresce meno che linearmente con i soci, quindi una regione più grande chiede più persone, ma meno che in proporzione. Le sedi che coprono due regioni contano come una.",
            ),
        )
        FormulaCard(
            title = tr("medals.region.math.lemma", "Lemma · i soci effettivi"),
            formula = {
                MathLine("e = ")
                Fraction("(Σ n_{i})^{2}", "Σ n_{i}^{2}")
            },
            caption = tr(
                "medals.region.math.lemma.caption_v2",
                "\$n_{i}\$ i timbri dell'\$i\$-esimo socio agli eventi della regione nella stagione. \$e\$ è uguale ai soci presenti se ognuno timbra una volta sola, e tende a 1 se timbra sempre uno solo: conta chi c'è spesso, poco chi passa una volta.",
            ),
        )
        FormulaCard(
            title = tr("medals.region.math.thm1", "Teorema 1 · Sala piena e Zoccolo duro"),
            formula = { MathLine("q ≥ 8,5% · 15% · 25%") },
            caption = tr(
                "medals.region.math.thm1.caption_v2",
                "Bronzo, argento, oro. Per Sala piena \$v\$ sono i timbratori dell'evento migliore della stagione; per Zoccolo duro \$v = e\$, sugli eventi della regione.",
            ),
        )
        FormulaCard(
            title = tr("medals.region.math.thm2", "Teorema 2 · Tutti presenti"),
            formula = { MathLine("q ≥ 5% · 10% · 15%") },
            caption = tr(
                "medals.region.math.thm2.caption_v2",
                "Bronzo, argento, oro. \$v\$ sono i soci visti ad almeno due eventi diversi nella stagione, in tutta Italia: chi compare una volta sola non entra.",
            ),
        )
        FormulaCard(
            title = tr("medals.region.math.thm3", "Teorema 3 · Benvenuto"),
            formula = {
                MathLine("b = ")
                Fraction("n_{t}", "n_{i}")
                MathLine(" ≥ 25% · 40% · 60%")
            },
            caption = tr(
                "medals.region.math.thm3.caption_v2",
                "\$n_{i}\$ i soci della regione iscritti all'app nella stagione, \$n_{t}\$ quelli con il primo timbro entro due mesi. Qui la quota è semplice, senza l'esponente.",
            ),
        )
        FormulaCard(
            title = tr("medals.region.math.thm4", "Teorema 4 · i conteggi"),
            formula = { MathLine("c ≥ c_{bronzo}, c_{argento}, c_{oro}") },
            caption = tr(
                "medals.region.math.thm4.caption_v2",
                "Casa aperta 2 · 4 · 6 regioni. In trasferta 2 · 4 · 6 regioni. Senza pause 6 · 9 · 12 mesi. Padroni di casa 1 · 2 · 3 eventi. Staffetta 2 · 3 · 4 persone. Podio: 3°, 2°, 1° posto al 30 settembre.",
            ),
        )
    }
}
