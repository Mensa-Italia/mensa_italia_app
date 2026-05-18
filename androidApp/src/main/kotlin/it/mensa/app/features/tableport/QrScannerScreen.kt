package it.mensa.app.features.tableport

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import androidx.compose.material3.Button
import androidx.compose.ui.text.font.FontWeight
import it.mensa.app.support.tr
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import org.koin.androidx.compose.koinViewModel

/**
 * QrScannerScreen — CameraX live preview + ML Kit QR scanning.
 *
 * M3 Expressive treatments:
 *   - Permission state: MensaScaffold with parchment surface and IconBadge,
 *     hero kicker, emphasized title, PrimaryButton CTA.
 *   - Scanner state: full-bleed camera with a punched-out scrim, cyan
 *     corner brackets, a pulsing scan line, and a bottom instructional card
 *     in a dark glass material.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onScanned: (stampId: String, code: String) -> Unit,
    onCancel: () -> Unit,
    vm: QrScannerViewModel = koinViewModel(),
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    if (!cameraPermission.status.isGranted) {
        PermissionRationale(
            onGrant = { cameraPermission.launchPermissionRequest() },
            onCancel = onCancel,
        )
        return
    }

    QrCameraView(
        onScanned = { raw ->
            val result = vm.parseQr(raw) ?: return@QrCameraView
            onScanned(result.stampId, result.code)
        },
        onCancel = onCancel,
    )
}

// ─── Permission rationale ─────────────────────────────────────────────────────

@Composable
private fun PermissionRationale(
    onGrant: () -> Unit,
    onCancel: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    MensaScaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr("app.back", "Indietro"),
                        tint = colorScheme.onSurface,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = colorScheme.secondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.NoPhotography,
                        contentDescription = null,
                        tint = colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = tr("qr.permission_kicker", "PERMESSO RICHIESTO"),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tr("qr.camera_permission", "Accesso alla fotocamera"),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = tr(
                    "qr.camera_rationale",
                    "La fotocamera è necessaria per scansionare i QR dei timbri.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("qr.grant_permission", "Concedi accesso"))
            }
        }
    }
}

// ─── Live scanner view ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrCameraView(
    onScanned: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val secondary = MaterialTheme.colorScheme.secondary
    var didMatch by remember { mutableStateOf(false) }
    val isScanning = didMatch

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // ── Camera preview ──────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                    val scanner = BarcodeScanning.getClient(options)

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { ia ->
                            ia.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                if (didMatch) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees,
                                    )
                                    scanner.process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            val raw = barcodes.firstOrNull()?.rawValue
                                            if (!raw.isNullOrEmpty() && !didMatch) {
                                                didMatch = true
                                                onScanned(raw)
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w("QrScanner", "Barcode analysis failed", e)
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    runCatching {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    }.onFailure { Log.e("QrScanner", "Camera bind failed", it) }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── Scrim with punched-out viewfinder ───────────────────────────────
        val frameSize = 280.dp
        val cornerRadius = 28.dp

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen },
        ) {
            // Full dark wash
            drawRect(Color.Black.copy(alpha = 0.55f))

            // Punch a hole where the viewfinder sits
            val px = frameSize.toPx()
            val left = (size.width - px) / 2f
            val top = (size.height - px) / 2f
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(left, top),
                size = Size(px, px),
                cornerRadius = CornerRadius(cornerRadius.toPx()),
                blendMode = BlendMode.Clear,
            )
        }

        // ── Viewfinder: cyan corner brackets + scanning line ────────────────
        Box(
            modifier = Modifier
                .size(frameSize)
                .align(Alignment.Center),
        ) {
            // Pulsing corner brackets — colour breathes between cyan and white
            val infinite = rememberInfiniteTransition(label = "scanPulse")
            val pulse by infinite.animateFloat(
                initialValue = 0.55f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1100),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "pulse",
            )
            val scanY by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2200),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "scanY",
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = with(density) { 3.dp.toPx() }
                val cornerLen = with(density) { 28.dp.toPx() }
                val r = with(density) { cornerRadius.toPx() }
                val w = size.width
                val h = size.height
                val accent = secondary.copy(alpha = pulse)

                // Faint full border to keep silhouette readable
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.18f),
                    cornerRadius = CornerRadius(r),
                    style = Stroke(width = with(density) { 1.dp.toPx() }),
                )

                // Top-Left
                drawLine(accent, Offset(r, 0f), Offset(cornerLen + r, 0f), strokeWidth)
                drawLine(accent, Offset(0f, r), Offset(0f, cornerLen + r), strokeWidth)
                // Top-Right
                drawLine(accent, Offset(w - cornerLen - r, 0f), Offset(w - r, 0f), strokeWidth)
                drawLine(accent, Offset(w, r), Offset(w, cornerLen + r), strokeWidth)
                // Bottom-Left
                drawLine(accent, Offset(r, h), Offset(cornerLen + r, h), strokeWidth)
                drawLine(accent, Offset(0f, h - cornerLen - r), Offset(0f, h - r), strokeWidth)
                // Bottom-Right
                drawLine(accent, Offset(w - cornerLen - r, h), Offset(w - r, h), strokeWidth)
                drawLine(accent, Offset(w, h - cornerLen - r), Offset(w, h - r), strokeWidth)

                // Scanning line — gradient bar that sweeps up and down
                val barInset = with(density) { 12.dp.toPx() }
                val lineY = barInset + (h - 2 * barInset) * scanY
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            secondary.copy(alpha = 0.65f),
                            Color.Transparent,
                        ),
                    ),
                    topLeft = Offset(barInset, lineY - with(density) { 1.dp.toPx() }),
                    size = Size(w - 2 * barInset, with(density) { 2.dp.toPx() }),
                )
            }
        }

        // ── Top bar: transparent, light icons ───────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Circular back button with proper 48dp touch target
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color = Color.Black.copy(alpha = 0.45f),
                shape = CircleShape,
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr("app.back", "Indietro"),
                        tint = Color.White,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = tr("tableport.scan_kicker", "PASSAPORTO").uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = secondary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tr("tableport.scan_title", "Inquadra il QR"),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
            }
        }

        // ── Bottom instructional card ───────────────────────────────────────
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            BottomInstructionCard(isScanning = isScanning)
        }
    }
}

// ─── Bottom card ──────────────────────────────────────────────────────────────

@Composable
private fun BottomInstructionCard(isScanning: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.62f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = tr("tableport.scan_card_kicker", "ISTRUZIONI"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tr(
                    "tableport.scan_hint",
                    "Inquadra il codice per acquisire il timbro",
                ),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            if (isScanning) {
                Spacer(modifier = Modifier.height(14.dp))
                LoadingDots(color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
