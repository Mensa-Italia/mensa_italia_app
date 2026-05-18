package it.mensa.app.features.tableport

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import coil3.ImageLoader
import it.mensa.app.features.tableport._components.PassportCover
import it.mensa.app.features.tableport._components.PassportPage
import it.mensa.app.features.tableport._components.PassportPalette
import it.mensa.app.features.tableport._components.StampConfirmSheet
import it.mensa.app.features.tableport.util.StampImagePrefetcher
import androidx.compose.ui.text.font.FontWeight
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.theme.MensaMotion
import org.koin.androidx.compose.koinViewModel

private const val STAMPS_PER_PAGE = 6
private const val TOTAL_STAMP_GOAL = 30

/**
 * PassportScreen — Tableport hub in M3 Expressive Wave-2 style.
 *
 * Layout: brand-blue hero zone with kicker + counter + progress meter, then
 * a Parchment content surface hosting the passport book in a HorizontalPager,
 * with a page-indicator strip and an ExtendedFAB ("Scansiona QR") at the
 * bottom-end per M3 Expressive guidance.
 *
 * Hero discipline: this is the screen's one hero — the passport book itself,
 * elevated on a Parchment surface with a brand backdrop above.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PassportScreen(
    onNavigateToScanner: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    backStackEntry: NavBackStackEntry? = null,
    vm: PassportViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val brandHero = Brush.verticalGradient(
        listOf(colorScheme.primary, colorScheme.primaryContainer),
    )

    // Receive QR scan result handed back via SavedStateHandle
    LaunchedEffect(backStackEntry) {
        backStackEntry?.savedStateHandle?.let { handle ->
            val stampId = handle.get<String>("qr_stamp_id")
            val code = handle.get<String>("qr_code")
            if (!stampId.isNullOrEmpty() && !code.isNullOrEmpty()) {
                vm.onQrScanned(stampId, code)
                handle.remove<String>("qr_stamp_id")
                handle.remove<String>("qr_code")
            }
        }
    }

    var isOpen by rememberSaveable { mutableStateOf(false) }
    var stampsRevealed by rememberSaveable { mutableStateOf(false) }

    val stamps = uiState.stamps
    val pageCount = maxOf(1, (stamps.size + STAMPS_PER_PAGE - 1) / STAMPS_PER_PAGE)
    val pagerState = rememberPagerState(pageCount = { pageCount + 1 }) // +1 for cover

    LaunchedEffect(stamps) {
        if (stamps.isNotEmpty()) {
            StampImagePrefetcher.warmAll(stamps, context, ImageLoader(context))
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage > 0) {
            isOpen = true
            stampsRevealed = true
        }
    }

    MensaScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tr("tableport.title", "Passaporto"),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = tr("app.back", "Indietro"),
                                tint = Color.White,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = pagerState.currentPage > 0,
                enter = scaleIn(animationSpec = MensaMotion.springHeroOvershoot) +
                    fadeIn(animationSpec = MensaMotion.tweenEnter),
                exit = scaleOut(animationSpec = MensaMotion.springStandard) +
                    fadeOut(animationSpec = MensaMotion.tweenExit),
            ) {
                ExtendedFloatingActionButton(
                    text = {
                        Text(
                            text = tr("tableport.scan_cta", "Scansiona QR"),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = null,
                        )
                    },
                    onClick = onNavigateToScanner,
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
        ) {
            // ── Background composition: brand hero on top, parchment below ──
            // We paint hero in the upper portion of the screen so the
            // passport book sits on a warm Parchment plate below.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .background(brandHero),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── Hero zone — kicker + counter + progress meter ───────────
                HeroHeader(
                    collected = stamps.size,
                    total = TOTAL_STAMP_GOAL,
                )

                // ── Passport book — the one hero of the screen ──────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val passportWidth = 300.dp
                    val passportHeight = passportWidth * (125f / 88f)

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.width(passportWidth),
                        pageSpacing = 14.dp,
                    ) { page ->
                        if (page == 0) {
                            PassportCover(
                                width = passportWidth,
                                height = passportHeight,
                                isOpen = isOpen,
                                onTap = {
                                    isOpen = true
                                    stampsRevealed = true
                                },
                            )
                        } else {
                            PassportPage(
                                stamps = stamps,
                                pageIndex = page - 1,
                                totalPages = pageCount,
                                totalStamps = stamps.size,
                                stampsRevealed = stampsRevealed,
                                width = passportWidth,
                                height = passportHeight,
                                onTapStamp = {},
                            )
                        }
                    }
                }

                // ── Page indicator (springs to current) ─────────────────────
                PageIndicator(
                    pageCount = pageCount + 1,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier.padding(vertical = 16.dp),
                )

                Spacer(modifier = Modifier.height(96.dp)) // breathing room for FAB
            }

            // ── Stamp confirmation sheet ────────────────────────────────────
            uiState.pendingVerification?.let { pending ->
                StampConfirmSheet(
                    stampId = pending.stampId,
                    code = pending.code,
                    onDone = { vm.clearPendingVerification() },
                )
            }
        }
    }
}

// ─── Hero header ─────────────────────────────────────────────────────────────

@Composable
private fun HeroHeader(collected: Int, total: Int) {
    val progress = (collected.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = MensaMotion.springHero,
        label = "passportProgress",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = tr("tableport.collection_kicker", "RACCOLTA TIMBRI"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Emphasized counter — one hero typography moment per screen
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "$collected",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Text(
                text = " / $total",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White.copy(alpha = 0.70f),
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = tr("tableport.stamps_suffix", "timbri collezionati"),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.80f),
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Progress meter — cyan fill on white-translucent track
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.18f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(MaterialTheme.colorScheme.secondary, Color.White),
                        ),
                    ),
            )
        }
    }
}

// ─── Page indicator dots ─────────────────────────────────────────────────────

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { idx ->
            val isCurrent = currentPage == idx
            val width by animateDpAsState(
                targetValue = if (isCurrent) 22.dp else 8.dp,
                animationSpec = MensaMotion.springShape,
                label = "dotWidth_$idx",
            )
            val alpha by animateFloatAsState(
                targetValue = if (isCurrent) 1f else 0.40f,
                animationSpec = MensaMotion.springStandard,
                label = "dotAlpha_$idx",
            )
            // Cover dot is gold, page dots are secondary — colour echoes the document.
            val color = if (idx == 0) PassportPalette.gold else MaterialTheme.colorScheme.secondary
            Box(
                modifier = Modifier
                    .width(width)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = alpha)),
            )
        }
    }
}
