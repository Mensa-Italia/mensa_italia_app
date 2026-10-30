package it.mensa.app.features.tableport.medals

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.features.leaderboard.mathInline
import it.mensa.app.support.tr
import it.mensa.app.ui.theme.DarkStage
import it.mensa.app.ui.theme.StageSurface
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The medal collection as a gallery, always dark: how many are earned, then
 * every medal in its glass case ([MedalCase]). Tapping one brings it out in a
 * sheet ([MedalShowcase]). Material 3 Expressive chrome (flexible app bar,
 * wavy progress, loading indicator, bottom sheet) over the stage. Mirrors
 * iOS `MedalCabinetView`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MedalCabinetScreen(onNavigateBack: () -> Unit) = DarkStage {
    val state = rememberMedals()
    val medals = state.medals
    val region = rememberRegionMedals()
    // "Le mie" / "La mia regione": the switch exists only with the flag on and the region known.
    var page by rememberSaveable { mutableStateOf(CabinetPage.Mine) }
    val onRegion = region.available && page == CabinetPage.Region
    var showInfo by remember { mutableStateOf(false) }
    var openYears by remember { mutableStateOf(emptySet<Int>()) }
    var selected by remember { mutableStateOf<Medal?>(null) }
    // The top bar's filter, as on the other lists: by metal earned, or the
    // ones still to earn. The summary keeps counting them all.
    var filter by remember { mutableStateOf(MedalFilter.All) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val shown = remember(medals, filter) { medals.filter(filter::matches) }
    val regionShown = remember(region.current, filter) { region.current.filter(filter::matches) }
    val switch: (@Composable () -> Unit)? = if (region.available) {
        { CabinetSwitch(page = if (onRegion) CabinetPage.Region else CabinetPage.Mine, onChange = { page = it }) }
    } else null
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(tr("medals.title", "Medagliere")) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("app.back", "Indietro"))
                    }
                },
                actions = {
                    if (onRegion) {
                        IconButton(onClick = { showInfo = true }) {
                            Icon(Icons.Outlined.Info, contentDescription = tr("leaderboard.how_it_works", "Come funziona"))
                        }
                    }
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                Icons.Outlined.FilterList,
                                contentDescription = tr("medals.filter.label", "Filtra per metallo"),
                                tint = if (filter != MedalFilter.All) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            )
                        }
                        DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                            MedalFilter.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        filter = option
                                        showFilterMenu = false
                                    },
                                    leadingIcon = option.tier?.let { tier -> ({ MedalArtwork(tier = tier, icon = Icons.Filled.Star, size = 18.dp) }) },
                                    trailingIcon = if (filter == option) ({
                                        Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }) else null,
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = StageSurface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 32.dp,
        )
        when {
            onRegion -> LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "switch", span = { GridItemSpan(maxLineSpan) }) { switch?.invoke() }
                regionPage(
                    region = region,
                    filter = filter,
                    shown = regionShown,
                    openYears = openYears,
                    onToggleYear = { year -> openYears = if (year in openYears) openYears - year else openYears + year },
                    onSelect = { selected = it },
                )
            }
            medals.isNotEmpty() -> LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                switch?.let { item(key = "switch", span = { GridItemSpan(maxLineSpan) }) { it() } }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Summary(
                        medals = medals,
                        headline = tr("medals.count_of", "{earned} di {total}", "earned" to medals.count { it.isEarned }, "total" to medals.size),
                        caption = tr("medals.count_caption_case", "Medaglie in teca"),
                    )
                }
                items(shown, key = { it.id }) { medal ->
                    MedalCase(medal = medal, onClick = { selected = medal })
                }
                if (shown.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { FilterEmpty() }
                }
            }
            state.failed -> Column(Modifier.fillMaxSize().padding(padding)) {
                switch?.let { Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) { it() } }
                Text(
                    text = tr("medals.load_error", "Non riesco a leggere le medaglie. Riprova più tardi."),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                )
            }
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                switch?.let { Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) { it() } }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            }
        }
    }

    if (showInfo) RegionMedalsInfoSheet(onDismiss = { showInfo = false })

    selected?.let { medal -> MedalShowcase(medal = medal, onDismiss = { selected = null }) }
}

/** The line shown when the filter lets no medal through. */
@Composable
private fun FilterEmpty() {
    Text(
        text = tr("medals.filter.empty", "Nessuna medaglia qui, per ora."),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
    )
}

/**
 * The region's page: the ring of this year's medals, the rules, the ten
 * medals in their cases, then the earlier years folded under their headers.
 * The filter in the top bar applies to every year. Mirrors iOS
 * `MedalCabinetView.regionPage`.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyGridScope.regionPage(
    region: RegionMedalsUi,
    filter: MedalFilter,
    shown: List<Medal>,
    openYears: Set<Int>,
    onToggleYear: (Int) -> Unit,
    onSelect: (Medal) -> Unit,
) {
    val span: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    if (!region.loaded) {
        item(key = "loading", span = span) {
            Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                if (region.failed) {
                    Text(
                        text = tr("medals.region.load_error", "Non riesco a leggere le medaglie della regione. Riprova più tardi."),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LoadingIndicator()
                }
            }
        }
        return
    }
    if (region.current.isEmpty()) {
        item(key = "empty", span = span) {
            Text(
                text = tr("medals.region.empty", "Le medaglie della regione arriveranno presto."),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            )
        }
    } else {
        item(key = "summary", span = span) {
            Summary(
                medals = region.current,
                headline = tr("medals.region.count_of", "{earned} su {total}", "earned" to region.earnedCount, "total" to region.current.size),
                caption = if (region.regionName.isBlank()) {
                    tr("medals.region.count_caption", "Medaglie di regione {year}", "year" to region.year)
                } else "${region.regionName} · ${region.year}",
                tiers = MedalTier.entries - MedalTier.Platinum,
            )
        }
        item(key = "rules", span = span) { RegionRules() }
        items(shown, key = { it.id }) { medal -> MedalCase(medal = medal, onClick = { onSelect(medal) }) }
        if (shown.isEmpty()) item(key = "filter-empty", span = span) { FilterEmpty() }
    }
    if (region.past.isNotEmpty()) {
        item(key = "years", span = span) {
            Text(
                text = tr("medals.region.years", "Annate").uppercase(),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        region.past.forEach { (year, medals) ->
            val open = year in openYears
            item(key = "year-$year", span = span) {
                YearHeader(year = year, earned = medals.count { it.isEarned }, total = medals.size, expanded = open, onClick = { onToggleYear(year) })
            }
            if (open) {
                val visible = medals.filter(filter::matches)
                items(visible, key = { it.id }) { medal -> MedalCase(medal = medal, onClick = { onSelect(medal) }) }
            }
        }
    }
}

/** What the cabinet's filter can show: everything, one metal (earned), or the ones still to earn. */
private enum class MedalFilter(val tier: MedalTier?) {
    All(null), Bronze(MedalTier.Bronze), Silver(MedalTier.Silver), Gold(MedalTier.Gold), Platinum(MedalTier.Platinum), Locked(null);

    val label: String
        @Composable get() = when (this) {
            All -> tr("medals.filter.all", "Tutte")
            Locked -> tr("medals.filter.locked", "Da ottenere")
            else -> tier!!.label
        }

    fun matches(medal: Medal): Boolean = when (this) {
        All -> true
        Locked -> !medal.isEarned
        else -> medal.isEarned && medal.tier == tier
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Summary(
    medals: List<Medal>,
    headline: String,
    caption: String,
    tiers: List<MedalTier> = MedalTier.entries,
) {
    val earned = medals.count { it.isEarned }
    val gold = MedalTier.Gold.metal
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularWavyProgressIndicator(
                progress = { earned.toFloat() / medals.size.coerceAtLeast(1) },
                color = gold.mid,
                trackColor = Color.White.copy(alpha = 0.10f),
                modifier = Modifier.size(84.dp),
            )
            Text(
                text = "$earned",
                style = MaterialTheme.typography.headlineMedium.copy(
                    brush = Brush.verticalGradient(listOf(gold.hi, gold.mid)),
                ),
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.width(18.dp))
        Column {
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = caption.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                tiers.forEach { tier ->
                    val label = tier.label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = label },
                    ) {
                        MedalArtwork(tier = tier, icon = Icons.Filled.Star, size = 18.dp)
                        Text(
                            text = "${medals.count { it.tier == tier && it.isEarned }}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }
}

// ─── Showcase ────────────────────────────────────────────────────────────────

/**
 * One medal brought out of its case, in a bottom sheet: large, in a halo of
 * its metal with slowly turning rays once earned, turned by dragging to show
 * the engraved reverse; then its name, what it's for, the ladder of its
 * metals and how far along the next one is.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MedalShowcase(medal: Medal, onDismiss: () -> Unit) {
    val angle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val metal = medal.tier.metal
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StageSurface,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                // Settles on whichever face is nearer.
                                val target = (angle.value / 180f).roundToInt() * 180f
                                scope.launch { angle.animateTo(target, spring(dampingRatio = 0.72f, stiffness = 120f)) }
                            },
                        ) { _, dragAmount ->
                            scope.launch { angle.snapTo(angle.value + dragAmount / density * 0.8f) }
                        }
                    }
                    .semantics { contentDescription = medal.title },
                contentAlignment = Alignment.Center,
            ) {
                Halo(
                    color = if (medal.isEarned) metal.mid else Color(0xFF5A5F6B),
                    rays = medal.isEarned,
                    modifier = Modifier.matchParentSize(),
                )
                SpinningMedal(
                    angle = angle.value,
                    medal = medal,
                    size = 196.dp,
                    reverseCaption = medal.tier.label.uppercase(),
                )
            }

            TierBadge(medal)
            Spacer(Modifier.height(14.dp))
            Text(
                // A region medal is "Name 2026": each year is its own.
                text = medal.region?.let { "${medal.title} ${it.year}" } ?: medal.title,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = medal.detail,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            val region = medal.region
            if (region != null) {
                if (region.witnessed) {
                    Spacer(Modifier.height(14.dp))
                    WitnessBadge()
                }
                Spacer(Modifier.height(28.dp))
                RegionTierLadder(medal, region, Modifier.padding(horizontal = 28.dp))
                Spacer(Modifier.height(24.dp))
                RegionStatus(medal, region, Modifier.padding(horizontal = 32.dp))
                Spacer(Modifier.height(24.dp))
                RegionRuleCard(region.rule, Modifier.padding(horizontal = 24.dp))
            } else {
                if (medal.levels.size > 1 && !medal.isHidden) {
                    Spacer(Modifier.height(28.dp))
                    TierLadder(medal, Modifier.padding(horizontal = 28.dp))
                }
                Spacer(Modifier.height(24.dp))
                Status(medal, Modifier.padding(horizontal = 32.dp))
                if (medal.countsStamps && !medal.isHidden) {
                    Spacer(Modifier.height(24.dp))
                    StampRules(Modifier.padding(horizontal = 24.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = tr("medals.spin_hint", "Trascina per girarla"),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.35f),
            )
        }
    }
}

/**
 * Which stamps a stamp-counting medal sees: the ranking's one-point rule
 * with figures by region size, then the organizer and staff exclusions.
 * The same for every such medal, so the question is answered where it
 * comes up. Mirrors iOS `StampRules`.
 */
@Composable
private fun StampRules(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = tr("medals.stamp_rules.title", "Quali timbri contano"),
            style = MaterialTheme.typography.titleSmall,
        )
        StampRule(
            Icons.Filled.Verified,
            tr(
                "medals.stamp_rules.threshold_v3",
                "Conta il timbro che in classifica vale almeno un punto: chi ha timbrato all'evento, in rapporto ai soci attivi della regione in cui si è svolto, deve raggiungere la soglia da cui la classifica comincia ad assegnare punti. È una quota, non un numero fisso: una regione con più soci chiede più timbratori, ma meno che in proporzione.",
            ),
        )
        StampRule(
            Icons.Filled.Campaign,
            tr("medals.stamp_rules.organizer_v2", "I timbri dei tuoi eventi vanno nella medaglia Organizzatore, non qui."),
        )
        StampRule(
            Icons.Filled.Groups,
            tr("medals.stamp_rules.staff_v2", "Lo staff di una regione, finché in carica, non conta i timbri degli eventi non nazionali della propria regione."),
        )
        Text(
            text = mathInline(
                tr("medals.stamp_rules.more_v3", "Il criterio esatto, formula compresa, è nella scheda ⓘ della classifica, sezione Matematica."),
                MaterialTheme.typography.labelMedium.fontSize,
                Color.White.copy(alpha = 0.45f),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.45f),
        )
    }
}

@Composable
internal fun StampRule(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        Text(
            text = mathInline(text, MaterialTheme.typography.bodyMedium.fontSize, MaterialTheme.colorScheme.onSurface),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A glow of the medal's colour, and rays turning slowly behind an earned one. */
@Composable
private fun Halo(color: Color, rays: Boolean, modifier: Modifier = Modifier) {
    val turn = rememberInfiniteTransition(label = "halo").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart),
        label = "haloTurn",
    )
    // The turn is read only while drawing: each frame redraws the rays
    // without recomposing.
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        // Faded out by the box's nearer edges, so nothing is cut off.
        val r = size.minDimension * 0.5f
        drawCircle(
            Brush.radialGradient(
                listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0.10f), Color.Transparent),
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )
        if (rays) {
            rotate(turn.value, pivot = c) {
                val count = 18
                val spread = (PI / count * 0.55).toFloat()
                val reach = r
                for (i in 0 until count) {
                    val a = (2 * PI * i / count).toFloat()
                    val ray = Path().apply {
                        moveTo(c.x, c.y)
                        lineTo(c.x + cos(a - spread) * reach, c.y + sin(a - spread) * reach)
                        lineTo(c.x + cos(a + spread) * reach, c.y + sin(a + spread) * reach)
                        close()
                    }
                    drawPath(
                        ray,
                        Brush.radialGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent), center = c, radius = reach),
                    )
                }
            }
        }
    }
}

/** The metal, in a capsule of its colour; "not yet" for a locked medal. */
@Composable
private fun TierBadge(medal: Medal) {
    val metal = medal.tier.metal
    val earned = medal.isEarned
    Row(
        modifier = Modifier
            .border(1.dp, if (earned) metal.mid else Color.White.copy(alpha = 0.2f), CircleShape)
            .background(if (earned) metal.mid.copy(alpha = 0.14f) else Color.Transparent, CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (earned) Icon(Icons.Filled.Verified, contentDescription = null, tint = metal.hi, modifier = Modifier.size(16.dp))
        Text(
            text = if (earned) medal.tier.label.uppercase() else tr("medals.locked", "Non ancora ottenuta").uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = if (earned) metal.hi else Color.White.copy(alpha = 0.55f),
        )
    }
}

/**
 * Each metal of the medal from the first to the last, with the value that
 * earns it: the ones reached lit, the next one ringed by how far along it is.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TierLadder(medal: Medal, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        medal.levels.forEachIndexed { index, (tier, threshold) ->
            val reached = medal.value >= threshold
            val isNext = medal.nextTier == tier
            if (index > 0) {
                Box(
                    Modifier
                        .weight(1f)
                        .padding(top = 27.dp)
                        .height(2.dp)
                        .background(if (reached) tier.metal.mid else Color.White.copy(alpha = 0.14f), CircleShape),
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
                            color = tier.metal.mid,
                            trackColor = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.size(56.dp),
                        )
                    }
                    MedalArtwork(tier = tier, icon = medal.icon, size = 38.dp, locked = !reached)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = tier.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (reached) tier.metal.hi else Color.White.copy(alpha = 0.45f),
                )
                Text(
                    text = "$threshold",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (reached || isNext) Color.White else Color.White.copy(alpha = 0.45f),
                )
            }
        }
    }
}

/** Where the member stands with it: how far the next metal is, or that it's done. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Status(medal: Medal, modifier: Modifier = Modifier) {
    val nextTier = medal.nextTier
    val nextThreshold = medal.nextThreshold
    Column(modifier.widthIn(max = 320.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (nextTier != null && nextThreshold != null && !medal.isHidden) {
            Text(
                text = tr("medals.next", "Prossimo traguardo: %1\$@ a %2\$d")
                    .replace("%1\$@", nextTier.label)
                    .replace("%2\$d", "$nextThreshold"),
                style = MaterialTheme.typography.titleSmall,
            )
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
                    .replace("%1\$d", "${medal.value}")
                    .replace("%2\$d", "$nextThreshold"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (medal.isEarned && medal.levels.size > 1) {
            Text(
                text = tr("medals.max_tier", "Livello massimo raggiunto"),
                style = MaterialTheme.typography.titleSmall,
                color = medal.tier.metal.hi,
            )
        }
    }
}

/**
 * The medal turned about its vertical axis: the reverse shows once it's
 * past edge-on, and the coin's rim shows its thickness on the way round.
 */
@Composable
private fun SpinningMedal(
    angle: Float,
    medal: Medal,
    size: Dp,
    reverseCaption: String,
    modifier: Modifier = Modifier,
) {
    val radians = angle * PI.toFloat() / 180f
    val showsBack = cos(radians) < 0f
    val metal = medal.tier.metal
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(size * 0.07f * abs(sin(radians)))
                .height(size * 0.96f)
                .background(Brush.verticalGradient(listOf(metal.lo, metal.mid, metal.lo)), CircleShape),
        )
        MedalArtwork(
            tier = medal.tier,
            icon = medal.icon,
            size = size,
            locked = !medal.isEarned,
            sheen = sin(radians),
            reverse = if (showsBack) MedalReverse(medal.title, reverseCaption) else null,
            modifier = Modifier
                .graphicsLayer {
                    rotationY = angle
                    cameraDistance = 14f * density
                    // The reverse is drawn mirrored, so it reads right once turned.
                    scaleX = if (showsBack) -1f else 1f
                }
                .shadow(18.dp, CircleShape, clip = false),
        )
    }
}
