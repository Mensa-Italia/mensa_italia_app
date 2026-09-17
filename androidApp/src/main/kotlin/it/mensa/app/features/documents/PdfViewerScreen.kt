package it.mensa.app.features.documents

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.ceil

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

/** Oltre questa larghezza una pagina A4 costa decine di MB: non ne vale la pena. */
private const val MAX_RENDER_WIDTH_PX = 2048

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    encodedUrl: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val url = remember(encodedUrl) {
        java.net.URLDecoder.decode(encodedUrl, "UTF-8")
    }
    val vm: PdfViewerViewModel = koinViewModel(parameters = { parametersOf(url) })
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(url) { vm.load(context) }

    MensaScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(tr("addons.documents.pdf_title", fallback = "Documento")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (val s = state) {
                is PdfViewerState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                is PdfViewerState.Error -> {
                    Column(
                        Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            tr("addons.documents.pdf_error", fallback = "Impossibile aprire il PDF"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            s.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is PdfViewerState.Ready -> {
                    PdfPageList(pages = s.pages, vm = vm)
                }
            }
        }
    }
}

/**
 * La lista delle pagine, con zoom e spostamento.
 *
 * Il punto della riscrittura: prima lo zoom era un `graphicsLayer` applicato a
 * ogni immagine *dentro* la `LazyColumn`. `graphicsLayer` e' un effetto di
 * disegno e non tocca il layout, quindi la casella della lista restava alta
 * quanto a 1x mentre l'immagine cresceva: le pagine finivano una sopra
 * l'altra. E `detectTransformGestures` consumava ogni trascinamento, cosi' la
 * lista non scorreva piu' — "non posso muovermi all'interno del foglio".
 *
 * Adesso lo zoom cambia la *larghezza* della pagina, che e' una misura di
 * layout: l'altezza della casella cresce di conseguenza e due pagine non si
 * toccano mai. Lo scorrimento verticale torna a essere quello della lista.
 */
@Composable
private fun PdfPageList(pages: List<PdfPageSize>, vm: PdfViewerViewModel) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // `rememberSaveable`: ruotando il telefono si rimisura tutto, e perdere
    // l'ingrandimento proprio mentre si gira lo schermo per leggere meglio e'
    // il contrario di quel che serve. Su iOS la scala si conserva allo stesso modo.
    var scale by rememberSaveable { mutableFloatStateOf(MIN_SCALE) }
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val baseWidth: Dp = maxWidth
        val viewportWidthPx = with(LocalDensity.current) { baseWidth.toPx() }

        /** Quanto si puo' spostare di lato: zero finche' la pagina ci sta tutta. */
        fun maxPan(forScale: Float) = (viewportWidthPx * forScale - viewportWidthPx).coerceAtLeast(0f)

        /**
         * Applica uno zoom tenendo fermo il punto sotto le dita: senza questo,
         * ingrandire porterebbe sempre verso il bordo sinistro del foglio.
         */
        fun zoomBy(factor: Float, focusX: Float) {
            val newScale = (scale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
            if (newScale == scale) return
            val contentX = (focusX - offsetX) / scale
            offsetX = (focusX - contentX * newScale).coerceIn(-maxPan(newScale), 0f)
            scale = newScale
        }

        fun panBy(dx: Float) {
            offsetX = (offsetX + dx).coerceIn(-maxPan(scale), 0f)
        }

        // Dopo una rotazione la finestra e' larga diversamente, quindi lo
        // spostamento ripescato puo' cadere fuori dai nuovi bordi: si rimette
        // dentro, altrimenti la pagina resta spostata di lato senza motivo.
        LaunchedEffect(viewportWidthPx, scale) {
            offsetX = offsetX.coerceIn(-maxPan(scale), 0f)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                // Il pizzico si prende gli eventi nella passata Initial, cioe'
                // prima della lista: altrimenti la `LazyColumn`, che e' piu'
                // interna, scorrerebbe mentre si sta zoomando.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        var event: androidx.compose.ui.input.pointer.PointerEvent
                        do {
                            event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.count { it.pressed } >= 2) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                if (zoom != 1f || pan != androidx.compose.ui.geometry.Offset.Zero) {
                                    zoomBy(zoom, event.calculateCentroid(useCurrent = true).x)
                                    panBy(pan.x)
                                    // Il verticale lo fa scorrere alla lista, cosi'
                                    // due dita muovono il foglio come un dito solo.
                                    if (pan.y != 0f) scope.launch { listState.scrollBy(-pan.y) }
                                    event.changes.forEach { if (it.pressed) it.consume() }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                // Un dito in orizzontale sposta il foglio; in verticale la lista
                // scorre da sola, perche' `draggable` e' bloccato su un asse.
                .draggable(
                    state = rememberDraggableState { delta -> panBy(delta) },
                    orientation = Orientation.Horizontal,
                    enabled = scale > MIN_SCALE,
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tap ->
                            if (scale > MIN_SCALE) {
                                scale = MIN_SCALE
                                offsetX = 0f
                            } else {
                                zoomBy(DOUBLE_TAP_SCALE, tap.x)
                            }
                        },
                    )
                },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(count = pages.size, key = { it }) { index ->
                PdfPageItem(
                    index = index,
                    size = pages[index],
                    pageWidth = baseWidth * scale,
                    renderWidthPx = renderWidthFor(viewportWidthPx, scale),
                    offsetX = offsetX,
                    vm = vm,
                )
            }
        }
    }
}

/**
 * La larghezza a cui disegnare davvero la pagina.
 *
 * Si sale a scatti interi invece che seguire lo zoom in continuo: cosi' un
 * pizzico non fa ridisegnare il documento a ogni fotogramma, e la cache del
 * view model trova quello che ha gia' fatto.
 */
private fun renderWidthFor(viewportWidthPx: Float, scale: Float): Int {
    val steps = ceil(scale).toInt().coerceIn(1, 3)
    return (viewportWidthPx * steps).toInt().coerceIn(1, MAX_RENDER_WIDTH_PX)
}

@Composable
private fun PdfPageItem(
    index: Int,
    size: PdfPageSize,
    pageWidth: Dp,
    renderWidthPx: Int,
    offsetX: Float,
    vm: PdfViewerViewModel,
) {
    val pageHeight: Dp = pageWidth / size.aspectRatio
    var bitmap by remember(index) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(index, renderWidthPx) {
        bitmap = vm.page(index, renderWidthPx)
    }

    // La casella e' larga quanto la finestra e alta quanto la pagina ingrandita:
    // e' questo che tiene le pagine separate. Quel che esce di lato si taglia.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(pageHeight)
            .clipToBounds()
            .background(Color.White),
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .width(pageWidth)
                    .height(pageHeight)
                    .graphicsLayer { translationX = offsetX },
            )
        } else {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}
