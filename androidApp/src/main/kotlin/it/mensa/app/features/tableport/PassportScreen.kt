package it.mensa.app.features.tableport

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import it.mensa.app.features.tableport._components.PassportCoverLining
import it.mensa.app.features.tableport._components.PassportCoverTurn
import it.mensa.app.features.tableport._components.EntryScanButton
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import it.mensa.app.features.tableport._components.PassportCover
import it.mensa.app.features.tableport._components.PassportPage
import it.mensa.app.features.tableport._components.PassportPalette
import it.mensa.app.features.tableport._components.PassportStampScroll
import it.mensa.app.features.tableport._components.StampConfirmSheet
import it.mensa.app.features.tableport._components.StampDetailSheet
import it.mensa.app.features.tableport.medals.RainbowMedalIcon
import it.mensa.app.features.tableport.util.StampImagePrefetcher
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.components.mensaImageLoader
import it.mensa.app.ui.theme.MensaMotion
import it.mensa.shared.model.StampUserModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

private const val STAMPS_PER_PAGE = 6

/**
 * PassportScreen — Tableport hub in M3 Expressive Wave-2 style.
 *
 * Layout: velluto scuro a tutta altezza — lo stesso fondo di
 * `PassportView.swift` — con il libro del passaporto in un HorizontalPager,
 * la striscia di indicatori e l'ExtendedFAB ("Scansiona QR") in basso.
 *
 * Niente contatore "N / 30" in testa: il totale era una costante scritta a
 * mano, non un dato, e trasformava una collezione in una barra di
 * completamento. iOS non l'ha mai avuto.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PassportScreen(
    onNavigateToScanner: () -> Unit,
    onNavigateToMedals: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    backStackEntry: NavBackStackEntry? = null,
    vm: PassportViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val velvet = Brush.verticalGradient(
        listOf(PassportPalette.velvetTop, PassportPalette.velvetBottom),
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

    // Closed, the cover; open (turned like a book's, see `PassportCoverTurn`)
    // the stamps, which sit under the cover all along.
    var isOpen by rememberSaveable { mutableStateOf(false) }
    // Non `rememberSaveable`: StampUserModel non e' Parcelable, e riaprire il
    // dettaglio dopo una rotazione non vale un wrapper.
    var selectedStamp by remember { mutableStateOf<StampUserModel?>(null) }

    val stamps = uiState.stamps
    // The six-stamp page the scroll is at: what the prefetch warms ahead of.
    var scrollPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(stamps) {
        StampImagePrefetcher.warmCollection(stamps, context, mensaImageLoader(context))
    }

    LaunchedEffect(stamps, scrollPage) {
        if (stamps.isNotEmpty()) {
            // `mensaImageLoader` e non `ImageLoader(context)`: quest'ultimo
            // costruiva un loader nuovo a ogni effetto, con la propria memory
            // cache, che nessuno legge — le immagini venivano scaldate in una
            // cache diversa da quella da cui poi vengono disegnate.
            StampImagePrefetcher.warmAround(
                stamps = stamps,
                pageIndex = scrollPage,
                perPage = STAMPS_PER_PAGE,
                context = context,
                imageLoader = mensaImageLoader(context),
                // A fling crosses several pages: warm well ahead of it.
                ahead = 4,
            )
        }
    }

    MensaScaffold(
        topBar = {
            TopAppBar(
                // No title, as on iOS: the cover says what it is, and over the
                // stamps it would sit on the parchment.
                title = {},
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            // Over the stamps scrolling under it, on a disc of
                            // paper so it stays readable (iOS: the glass X).
                            modifier = if (isOpen) {
                                Modifier
                                    .padding(start = 4.dp)
                                    .background(PassportPalette.parchment.copy(alpha = 0.92f), CircleShape)
                            } else {
                                Modifier
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = tr("app.back", "Indietro"),
                                tint = if (isOpen) PassportPalette.coverDeep else Color.White,
                            )
                        }
                    }
                },
                actions = {
                    // The medal collection, its icon cycling through the
                    // rainbow so it gets noticed.
                    IconButton(
                        onClick = onNavigateToMedals,
                        modifier = if (isOpen) {
                            Modifier
                                .padding(end = 4.dp)
                                .background(PassportPalette.parchment.copy(alpha = 0.92f), CircleShape)
                        } else {
                            Modifier
                        },
                    ) {
                        RainbowMedalIcon(contentDescription = tr("medals.title", "Medagliere"))
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
            // The ENTRY stamp, as on iOS, once the passport is open.
            AnimatedVisibility(
                visible = isOpen,
                enter = fadeIn(animationSpec = MensaMotion.tweenEnter),
                exit = fadeOut(animationSpec = MensaMotion.tweenExit),
            ) {
                EntryScanButton(onClick = onNavigateToScanner)
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
        ) {
            // ── Velluto a tutta altezza, come su iOS ────────────────────────
            // Prima qui c'era una fascia di brand alta 360dp fisse. Non aveva
            // relazione con il contenuto: su schermi diversi il taglio fra blu
            // e sfondo cadeva in un punto qualsiasi, e nella maggior parte dei
            // casi in mezzo al passaporto, tranciandolo in due.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(velvet),
            )

            // ── Passport — the one hero of the screen ───────────────────────
            // Closed, the cover; open, every stamp in one floating scroll
            // (`PassportStampScroll`) instead of a pager of six-stamp pages,
            // running the full height of the screen under the bars.
            val pages: @Composable () -> Unit = {
                if (stamps.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding())
                            .padding(bottom = 96.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val passportWidth = 300.dp
                        PassportPage(
                            stamps = stamps,
                            pageIndex = 0,
                            totalPages = 1,
                            totalStamps = 0,
                            stampsRevealed = true,
                            width = passportWidth,
                            height = passportWidth * (125f / 88f),
                            onTapStamp = { selectedStamp = it },
                        )
                    }
                } else {
                    PassportStampScroll(
                        stamps = stamps,
                        stampsRevealed = true,
                        onTapStamp = { selectedStamp = it },
                        onPageChange = { scrollPage = it },
                        // Starts below the top bar, ends above the scan
                        // button; in between the stamps pass under both.
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding() + 72.dp,
                        ),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            // The cover fills the screen, as on iOS, and is swiped (or
            // tapped) open like a book's, the stamps already underneath.
            if (isOpen) {
                pages()
            } else {
                PassportCoverTurn(
                    onOpened = { isOpen = true },
                    pages = pages,
                    cover = {
                        PassportCover(
                            contentPadding = PaddingValues(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding(),
                            ),
                        )
                    },
                    lining = { PassportCoverLining() },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // ── Dettaglio del timbro ────────────────────────────────────────
            selectedStamp?.let { stamp ->
                StampDetailSheet(
                    stamp = stamp,
                    onDismiss = { selectedStamp = null },
                )
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
