package it.mensa.app.features.deals

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.components.LoadingDots
import androidx.compose.material3.Card
import androidx.compose.material3.TopAppBar
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.model.DealsContactModel
import it.mensa.shared.model.DealModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * DealDetailScreen — full-page deal detail.
 *
 * iOS parity: DealDetailView.swift
 * - Hero image (220dp) with brand-gradient placeholder
 * - Title, sector chip, active badge, discount pill
 * - Location row
 * - Sections: description, who, validity, howToGet
 * - Contacts card (GlassCard)
 * - Actions: Apri link (Chrome Custom Tab), Copia codice, Mostra QR, Condividi
 * - Edit/Delete for admins (gated by canEdit)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealDetailScreen(
    dealId: String,
    onBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DealDetailViewModel = koinViewModel(parameters = { parametersOf(dealId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var qrSheetVisible by remember { mutableStateOf(false) }
    val qrSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Error dialog
    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text(tr("app.error.title", fallback = "Errore")) },
            text = { Text(state.error ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            },
        )
    }

    // Delete confirm dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(tr("addons.deals.delete.confirm.title", fallback = "Eliminare il deal?")) },
            text = { Text(tr("addons.deals.delete.confirm.body", fallback = "Questa azione non può essere annullata.")) },
            confirmButton = {
                TextButton(
                    onClick = { /* Delete handled in nav graph via ViewModel */ },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(tr("app.delete", fallback = "Elimina")) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(tr("app.cancel", fallback = "Annulla"))
                }
            },
        )
    }

    MensaScaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(state.deal?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = tr("app.back", fallback = "Indietro"),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    state.deal?.link?.takeIf { it.isNotEmpty() }?.let { link ->
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, link) }
                            context.startActivity(Intent.createChooser(intent, null))
                        }) {
                            Icon(Icons.Outlined.Share, contentDescription = tr("app.share", fallback = "Condividi"))
                        }
                    }
                    if (state.canEdit) {
                        IconButton(onClick = { state.deal?.id?.let { onNavigateToEdit(it) } }) {
                            Icon(Icons.Outlined.Edit, contentDescription = tr("app.edit", fallback = "Modifica"))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.loading && state.deal == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) { LoadingDots() }
            }

            state.deal == null && state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.error ?: "", style = MaterialTheme.typography.bodyLarge)
                }
            }

            state.deal != null -> {
                DealDetailContent(
                    deal = state.deal!!,
                    contacts = state.contacts,
                    loadingContacts = state.loadingContacts,
                    onOpenQr = { qrSheetVisible = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) { LoadingDots() }
            }
        }
    }

    // QR Code bottom sheet
    if (qrSheetVisible) {
        val deal = state.deal
        val code = deal?.let { discountCode(it) }
        if (deal != null && code != null) {
            ModalBottomSheet(
                onDismissRequest = { qrSheetVisible = false },
                sheetState = qrSheetState,
            ) {
                QrSheetContent(code = code, dealName = deal.name)
            }
        }
    }
}

// ─── Main content ─────────────────────────────────────────────────────────────

@Composable
private fun DealDetailContent(
    deal: DealModel,
    contacts: List<DealsContactModel>,
    loadingContacts: Boolean,
    onOpenQr: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var appeared by remember { mutableStateOf(false) }
    var copiedCode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { appeared = true }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(0.dp))

        // Hero image
        AnimatedVisibility(
            visible = appeared,
            enter = fadeIn(spring()) + scaleIn(spring(), initialScale = 1.04f),
        ) {
            HeroImage(deal = deal, modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Title block
        AnimatedVisibility(
            visible = appeared,
            enter = fadeIn(spring()) + slideInVertically(spring(), initialOffsetY = { 12 }),
        ) {
            TitleBlock(deal = deal, modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Description
        deal.details?.takeIf { it.isNotEmpty() }?.let { details ->
            SectionBlock(
                title = tr("addons.deals.details.subblock.description.title", fallback = "Descrizione"),
                icon = Icons.Outlined.TextFields,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(details, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Who
        deal.who?.takeIf { it.isNotEmpty() }?.let { who ->
            SectionBlock(
                title = tr("addons.deals.details.subblock.who.title", fallback = "A chi è rivolto"),
                icon = Icons.Outlined.HowToReg,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(who, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Validity
        if (deal.starting != null || deal.ending != null) {
            SectionBlock(
                title = tr("app.deals.validity", fallback = "Validità"),
                icon = Icons.Outlined.CalendarMonth,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    deal.starting?.let {
                        Text(
                            text = "${tr("app.deals.from", fallback = "Dal")}: ${formatInstant(it)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    deal.ending?.let {
                        Text(
                            text = "${tr("app.deals.until", fallback = "Fino al")}: ${formatInstant(it)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        // How to get
        deal.howToGet?.takeIf { it.isNotEmpty() }?.let { howToGet ->
            SectionBlock(
                title = tr("addons.deals.details.subblock.howtoget.title", fallback = "Come ottenere il deal"),
                icon = Icons.Outlined.Tag,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(howToGet, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Contacts
        if (contacts.isNotEmpty()) {
            SectionBlock(
                title = tr("addons.contacts.title", fallback = "Contatti"),
                icon = Icons.Outlined.Person,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    contacts.forEach { contact ->
                        ContactRow(contact = contact)
                    }
                }
            }
        } else if (loadingContacts) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tr("app.deals.loading_contacts", fallback = "Carico contatti…"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Actions block
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Open link
            deal.link?.takeIf { it.isNotEmpty() }?.let { link ->
                Button(
                    onClick = {
                        try {
                            val uri = Uri.parse(link)
                            CustomTabsIntent.Builder().build().launchUrl(context, uri)
                        } catch (_: Exception) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
                ) {
                    Icon(Icons.Outlined.OpenInBrowser, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("app.open_link", fallback = "Apri link"))
                }
            }

            // Copy discount code
            discountCode(deal)?.let { code ->
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Codice sconto", code))
                        copiedCode = true
                        scope.launch { delay(1500); copiedCode = false }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(if (copiedCode) Icons.Outlined.CheckCircle else Icons.Outlined.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (copiedCode) tr("app.copied", fallback = "Copiato") else tr("app.deals.copy_code", fallback = "Copia codice {code}", "code" to code))
                }

                TextButton(onClick = onOpenQr, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.QrCode, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("app.deals.show_qr", fallback = "Mostra QR sconto"))
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun HeroImage(deal: DealModel, modifier: Modifier = Modifier) {
    val imageUrl = deal.attachment?.takeIf { it.isNotEmpty() }?.let {
        FilesUrl.build(collection = "deals", recordId = deal.id, filename = it, thumb = "1000x600")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(22.dp)),
    ) {
        if (imageUrl != null) {
            CachedAsyncImage(
                model = imageUrl,
                contentDescription = deal.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(androidx.compose.ui.graphics.Color(0xFF1A5276), androidx.compose.ui.graphics.Color(0xFF061F2E))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tag,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun TitleBlock(deal: DealModel, modifier: Modifier = Modifier) {
    val brandColor = MaterialTheme.colorScheme.primary
    val discountPill = discountPill(deal)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = deal.name,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (deal.commercialSector.isNotBlank()) {
                Text(
                    text = deal.commercialSector,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = brandColor,
                    modifier = Modifier
                        .background(brandColor.copy(alpha = 0.10f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            if (deal.isActive) {
                Text(
                    text = tr("app.deals.active", fallback = "Attivo"),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                    modifier = Modifier
                        .background(
                            androidx.compose.ui.graphics.Color(0xFF2E7D32).copy(alpha = 0.18f),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            if (discountPill != null) {
                Text(
                    text = discountPill,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        deal.position?.let { pos ->
            val label = if (pos.address.isBlank()) pos.name else "${pos.name} – ${pos.address}"
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionBlock(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        content()
    }
}

@Composable
private fun ContactRow(contact: DealsContactModel) {
    val context = LocalContext.current

    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (contact.name.isNotEmpty()) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            contact.note?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (contact.email.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}")))
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text(contact.email, style = MaterialTheme.typography.labelSmall)
                    }
                }
                contact.phoneNumber?.takeIf { it.isNotEmpty() }?.let { phone ->
                    TextButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.replace(" ", "")}")))
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text(phone, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// ─── QR Sheet ─────────────────────────────────────────────────────────────────

@Composable
private fun QrSheetContent(code: String, dealName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(dealName, style = MaterialTheme.typography.titleMedium)

        val bitmap = remember(code) { generateQrBitmap(code) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code $code",
                modifier = Modifier
                    .size(240.dp)
                    .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(20.dp))
                    .padding(16.dp),
            )
        }

        Text(
            text = code,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
            ),
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(50)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Text(
            text = tr("app.deals.qr_hint", fallback = "Mostra questo codice in cassa per ricevere lo sconto."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun discountPill(deal: DealModel): String? {
    val regex = Regex("""(\d{1,3})\s?%""")
    val candidates = listOfNotNull(deal.details, deal.who)
    for (text in candidates) {
        regex.find(text)?.let { return it.value.replace(" ", "") }
    }
    return null
}

private fun discountCode(deal: DealModel): String? {
    val sources = listOfNotNull(deal.howToGet, deal.details)
    val codeRegex = Regex("""(?:codice|code)[:\s]+([A-Z0-9_\-]{4,20})""", RegexOption.IGNORE_CASE)
    for (text in sources) {
        codeRegex.find(text)?.groupValues?.getOrNull(1)?.let { return it }
    }
    return null
}

private fun formatInstant(instant: Instant): String {
    return try {
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.dayOfMonth}/${local.monthNumber}/${local.year}"
    } catch (_: Exception) { "—" }
}

private fun generateQrBitmap(content: String): Bitmap? {
    return try {
        val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bmp
    } catch (_: Exception) { null }
}
