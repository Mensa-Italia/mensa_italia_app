package it.mensa.app.features.receipts

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.receipts.amountFormatted
import it.mensa.app.features.receipts.fallback
import it.mensa.app.features.receipts.iconVec
import it.mensa.app.features.receipts.kind
import it.mensa.app.features.receipts.labelKey
import it.mensa.app.features.receipts.statusColor
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.model.ReceiptModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    receiptId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: ReceiptDetailViewModel = koinViewModel(parameters = { parametersOf(receiptId) })
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Open Chrome Custom Tab when PDF URL is ready
    LaunchedEffect(uiState.pdfUrl) {
        val urlStr = uiState.pdfUrl ?: return@LaunchedEffect
        val uri = Uri.parse(urlStr)
        val customTab = CustomTabsIntent.Builder().setShowTitle(true).build()
        runCatching { customTab.launchUrl(context, uri) }
            .onFailure { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } }
        vm.onPdfUrlConsumed()
    }

    MensaScaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(tr("receipts.detail.title", fallback = "Ricevuta")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("app.back", fallback = "Indietro"))
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    val receipt = uiState.receipt
                    if (receipt != null) {
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Ricevuta Mensa: ${receipt.amountFormatted} — ${receipt.id}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, null))
                        }) {
                            Icon(Icons.Outlined.Share, contentDescription = tr("receipts.share", fallback = "Condividi"))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState,
            transitionSpec = { fadeIn(animationSpec = spring(stiffness = 300f)) togetherWith fadeOut(animationSpec = spring(stiffness = 300f)) },
            label = "receipt_detail_content",
        ) { state ->
            when {
                state.loading && state.receipt == null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                state.receipt != null -> {
                    ReceiptContent(
                        receipt = state.receipt,
                        downloadingPdf = state.downloadingPdf,
                        onDownloadPdf = vm::downloadPdf,
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                    )
                }
                state.error != null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text(text = state.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text(tr("receipts.not_found", fallback = "Ricevuta non trovata"))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptContent(
    receipt: ReceiptModel,
    downloadingPdf: Boolean,
    onDownloadPdf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val kind = receipt.kind
    val statusColor = receipt.statusColor
    val dateString = formatFullDate(receipt.created.toEpochMilliseconds())

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Header card — amount + status
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(imageVector = kind.iconVec, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Text(text = tr(kind.labelKey, fallback = kind.fallback), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
                Text(
                    text = receipt.amountFormatted,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, letterSpacing = (-0.5).sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = statusColor) {}
                    Text(text = receipt.status.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = statusColor)
                }
            }
        }

        // Info rows card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val desc = receipt.description
                if (!desc.isNullOrBlank()) {
                    ReceiptInfoRow(icon = Icons.Outlined.Description, label = tr("receipts.description", fallback = "Descrizione"), value = desc)
                }
                ReceiptInfoRow(icon = Icons.Outlined.CalendarMonth, label = tr("receipts.date", fallback = "Data"), value = dateString)
                if (receipt.stripeCode.isNotBlank()) {
                    ReceiptInfoRow(icon = Icons.Outlined.CreditCard, label = tr("receipts.stripe", fallback = "Codice Stripe"), value = receipt.stripeCode)
                }
            }
        }

        // CTA: Download PDF
        Button(
            onClick = onDownloadPdf,
            enabled = !downloadingPdf,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
        ) {
            if (downloadingPdf) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Outlined.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("receipts.download_pdf", fallback = "Scarica PDF"))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ReceiptInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier.fillMaxWidth()) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatFullDate(epochMs: Long): String {
    return try {
        val fmt = SimpleDateFormat("EEEE d MMMM yyyy, HH:mm", Locale.ITALIAN)
        fmt.format(Date(epochMs))
    } catch (_: Exception) {
        "—"
    }
}
