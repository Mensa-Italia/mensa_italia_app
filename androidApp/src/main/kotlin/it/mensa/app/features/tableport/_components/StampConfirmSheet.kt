package it.mensa.app.features.tableport._components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.koinAccess
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.theme.BottomSheetShape
import it.mensa.app.ui.theme.MensaMotion
import it.mensa.shared.model.StampModel
import kotlinx.coroutines.launch

/**
 * StampConfirmSheet — M3 Expressive bottom sheet shown after a successful QR scan.
 *
 * Visual identity: warm parchment-ish bottom sheet (surfaceContainerLow) with
 * the stamp framed by a brand cyan "seal" ring that springs into place using
 * springHeroOvershoot. KickerLabel discipline + emphasized headline + primary
 * action button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StampConfirmSheet(
    stampId: String,
    code: String,
    onDone: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val repo = remember { koinAccess().stamps }
    val colorScheme = MaterialTheme.colorScheme

    var stamp by remember { mutableStateOf<StampModel?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(stampId, code) {
        loading = true
        runCatching { repo.verify(id = stampId, code = code) }
            .onSuccess { stamp = it }
            .onFailure { errorMessage = it.message }
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDone,
        sheetState = sheetState,
        shape = BottomSheetShape,
        containerColor = colorScheme.surfaceContainerLow,
        dragHandle = {
            // Slim, brand-tinted handle
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
            )
        },
    ) {
        AnimatedContent(
            targetState = when {
                loading -> SheetState.Loading
                stamp != null -> SheetState.Ready
                else -> SheetState.Error
            },
            transitionSpec = { MensaMotion.heroTransform() },
            label = "stampSheetContent",
        ) { state ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (state) {
                    SheetState.Loading -> LoadingContent()
                    SheetState.Ready -> ReadyContent(
                        stamp = stamp!!,
                        saving = saving,
                        onConfirm = {
                            scope.launch {
                                saving = true
                                runCatching {
                                    repo.claim(stampId = stampId, code = code)
                                }.onFailure { errorMessage = it.message }
                                saving = false
                                onDone()
                            }
                        },
                        onCancel = onDone,
                    )
                    SheetState.Error -> ErrorContent(
                        message = errorMessage
                            ?: tr("tableport.confirm_not_found", "Timbro non trovato."),
                        onClose = onDone,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ─── States ──────────────────────────────────────────────────────────────────

private enum class SheetState { Loading, Ready, Error }

@Composable
private fun LoadingContent() {
    Spacer(modifier = Modifier.height(48.dp))
    LoadingDots(color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = tr("tableport.confirm_loading", "Verifica del timbro in corso..."),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(48.dp))
}

@Composable
private fun ReadyContent(
    stamp: StampModel,
    saving: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Text(
        text = tr("tableport.confirm_kicker", "NUOVO TIMBRO"),
        style = MaterialTheme.typography.labelSmall,
        color = colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(10.dp))

    // ── Hero seal — stamp framed by a cyan + gold halo ────────────────────
    SealedStampPreview(stamp = stamp)

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = stamp.description.ifEmpty { stamp.id },
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center,
        color = colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = tr(
            "tableport.confirm_subtitle",
            "Questo timbro sarà aggiunto al tuo passaporto.",
        ),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onConfirm,
        enabled = !saving,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        if (saving) {
            LoadingDots(color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(tr("tableport.confirm_cta", "Conferma timbro"))
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        TextButton(
            onClick = onCancel,
            enabled = !saving,
        ) {
            Text(tr("app.cancel", "Annulla"))
        }
    }
}

@Composable
private fun ErrorContent(message: String, onClose: () -> Unit) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = tr("tableport.confirm_error_kicker", "ERRORE"),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = tr("tableport.confirm_error_title", "Timbro non disponibile"),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onClose,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(tr("app.close", "Chiudi"))
    }
}

// ─── Sealed stamp preview ─────────────────────────────────────────────────────

@Composable
private fun SealedStampPreview(stamp: StampModel) {
    // Ring scales in with overshoot — "stamping" feel
    val ringScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = MensaMotion.springHeroOvershoot,
        label = "sealRing",
    )

    val secondary = MaterialTheme.colorScheme.secondary
    Box(
        modifier = Modifier.size(176.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer halo ring — cyan to gold gradient
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(ringScale),
        ) {
            val stroke = 3.dp.toPx()
            drawRoundRect(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        secondary,
                        PassportPalette.goldHi,
                        secondary,
                        PassportPalette.gold,
                        secondary,
                    ),
                ),
                cornerRadius = CornerRadius(40.dp.toPx()),
                style = Stroke(width = stroke),
            )
            // Inner thin gold rule
            drawRoundRect(
                color = PassportPalette.gold.copy(alpha = 0.55f),
                topLeft = androidx.compose.ui.geometry.Offset(8.dp.toPx(), 8.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(
                    size.width - 16.dp.toPx(),
                    size.height - 16.dp.toPx(),
                ),
                cornerRadius = CornerRadius(32.dp.toPx()),
                style = Stroke(width = 0.9f),
            )
        }

        // Stamp image clipped to a rounded square
        if (stamp.image.isNotEmpty()) {
            val url = FilesUrl.build(
                collection = "stamps",
                recordId = stamp.id,
                filename = stamp.image,
                thumb = "800x600",
            )
            CachedAsyncImage(
                model = url,
                contentDescription = stamp.description,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(128.dp)
                    .clip(RoundedCornerShape(24.dp)),
            )
        } else {
            // Fallback — parchment chip with monogram, mirrors PassportDecal
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(PassportPalette.parchment),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "M",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = PassportPalette.coverDeep.copy(alpha = 0.5f),
                )
            }
        }
    }
}

