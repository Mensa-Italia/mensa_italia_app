package it.mensa.app.features.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.features.card._components.MembershipCardHero
import it.mensa.app.features.card._components.PrintableCard
import it.mensa.app.features.card._components.QrCodeView
import it.mensa.app.features.card._components.shareCardImage
import it.mensa.app.features.profile._components.ProfileRow
import it.mensa.app.features.profile._components.ProfileSectionGroup
import it.mensa.app.features.profile._components.ProfileSectionTone
import it.mensa.app.support.tr
import it.mensa.app.ui.components.IconBadge
import it.mensa.app.ui.components.IconBadgeVariant
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.components.PrimaryButton
import it.mensa.app.ui.components.SecondaryButton
import it.mensa.app.ui.theme.ExpressiveTypography
import it.mensa.app.ui.theme.MensaBlue
import org.koin.androidx.compose.koinViewModel

/**
 * CardScreen — "La tua tessera".
 *
 * M3 Expressive language (aligned with TodayScreen):
 *  - Color: drenched MensaBlue hero with cyan halo, animated cyan shimmer on card front
 *  - Shape: 32dp pillowy hero card (morphs to 28dp on press)
 *  - Size: displayMediumEmphasized "Tessera socio" headline; headlineMediumEmphasized name
 *  - Motion: bouncy entrance, breathing halo, flip 180° on Y axis via spring
 *  - Containment: gradient hero + drenched card + white QR back face
 */
@Composable
fun CardScreen(
    onTicketsClick: () -> Unit = {},
    onReceiptsClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onSearchTap: () -> Unit = {},
) {
    val vm: CardViewModel = koinViewModel()
    val state by vm.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    fun launchShare() {
        val activity = context as? android.app.Activity ?: return
        val user = state.user ?: return
        val name = user.name.trim().ifBlank { user.username.trim().ifBlank { user.email.substringBefore('@') } }
        val memberId = state.membershipCode.orEmpty()
        val expiry = state.expirationDate.orEmpty()
        shareCardImage(activity) {
            PrintableCard(
                fullName = name,
                memberId = memberId,
                expiry = expiry,
                modifier = Modifier.fillMaxSize(),
            )
        }
        onShareClick()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
        vm.onSnackbarDismissed()
    }

    MensaScaffold(
        snackbarHostState = snackbarHostState,
        topBar = {
            it.mensa.app.ui.components.MensaSearchAppBar(
                placeholder = tr("card.search_placeholder", fallback = "Cerca soci, eventi, deal…"),
                onSearchTap = onSearchTap,
            )
        },
    ) { innerPadding ->
        when {
            state.loading -> CardLoading(modifier = Modifier.fillMaxSize().padding(innerPadding))
            state.user == null -> EmptyCardState(modifier = Modifier.fillMaxSize().padding(innerPadding))
            else -> CardContent(
                state = state,
                onAddToWalletClick = vm::onAddToWalletClick,
                onRenewClick = vm::onRenewClick,
                onTicketsClick = onTicketsClick,
                onReceiptsClick = onReceiptsClick,
                onShareClick = ::launchShare,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

// ─── Main content ─────────────────────────────────────────────────────────────

@Composable
private fun CardContent(
    state: CardUiState,
    onAddToWalletClick: () -> Unit,
    onRenewClick: () -> Unit,
    onTicketsClick: () -> Unit,
    onReceiptsClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val user = state.user ?: return
    val fullName = remember(user.name, user.username, user.email) {
        user.name.trim().ifBlank {
            user.username.trim().ifBlank {
                user.email
                    .substringBefore('@', "")
                    .replace('.', ' ')
                    .replace('_', ' ')
                    .split(' ')
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { part ->
                        part.replaceFirstChar { ch -> ch.uppercaseChar() }
                    }
                    .ifBlank { "Socio Mensa" }
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item(key = "title_row") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = tr("card.kicker", fallback = "LA TUA TESSERA"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tr("card.title", fallback = "Tessera socio"),
                    style = ExpressiveTypography.headlineLargeEmphasized.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 32.sp,
                        lineHeight = 36.sp,
                    ),
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        item(key = "flip_card") {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(12.dp))
                MembershipCardHero(
                    fullName = fullName,
                    memberId = state.membershipCode ?: "—",
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = tr("card.flip_hint", fallback = "Tocca la tessera per girarla"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        // ── QR Code section (always visible) ──
        item(key = "qr_section") {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = tr("card.qr_kicker", fallback = "MOSTRA AL COORDINATORE"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.5.sp,
                    ),
                )
                Spacer(Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        QrCodeView(
                            payload = state.qrPayload.orEmpty(),
                            size = 180.dp,
                            cornerRadius = 12.dp,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "ID ${state.membershipCode ?: "—"}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 2.sp,
                            ),
                        )
                    }
                }
            }
        }

        // ── Membership info section ──
        item(key = "membership_section") {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = tr("card.membership_kicker", fallback = "MEMBERSHIP"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.5.sp,
                    ),
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("card.expiry_label", fallback = "Scadenza"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = state.expirationDate ?: "—",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        when (state.cardStatus) {
                            CardStatus.Expired -> {
                                FilledTonalButton(onClick = onRenewClick) {
                                    Text(
                                        text = tr("card.renew_cta", fallback = "Rinnova"),
                                    )
                                }
                            }
                            CardStatus.Active -> {
                                SuggestionChip(
                                    onClick = {},
                                    enabled = false,
                                    label = {
                                        Text(tr("card.active", fallback = "Attiva"))
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        disabledLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    ),
                                    border = null,
                                )
                            }
                            CardStatus.ExpiringSoon -> {
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = {
                                        Text(tr("card.expiring_soon", fallback = "In scadenza"))
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    ),
                                    border = null,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Navigation tiles: Tickets + Receipts ──
        item(key = "navigation_tiles") {
            Spacer(Modifier.height(16.dp))
            ProfileSectionGroup(
                kicker = tr("card.nav_section_kicker", fallback = "DOCUMENTI"),
                title = tr("card.nav_section_title", fallback = "I tuoi documenti"),
                tone = ProfileSectionTone.Primary,
            ) {
                ProfileRow(
                    icon = Icons.Outlined.ConfirmationNumber,
                    title = tr("card.nav_tickets", fallback = "I miei ticket"),
                    onClick = onTicketsClick,
                )
                ProfileRow(
                    icon = Icons.Outlined.Receipt,
                    title = tr("card.nav_receipts", fallback = "Le mie ricevute"),
                    onClick = onReceiptsClick,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Action buttons: Wallet + Share ──
        item(key = "actions") {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryButton(
                    text = tr("card.action_wallet", fallback = "Aggiungi al wallet"),
                    onClick = onAddToWalletClick,
                    icon = Icons.Outlined.AccountBalanceWallet,
                    modifier = Modifier.fillMaxWidth(),
                    loading = state.walletLoading,
                )
                SecondaryButton(
                    text = tr("card.action_share", fallback = "Condividi tessera"),
                    onClick = onShareClick,
                    icon = Icons.Outlined.Share,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item(key = "footer_spacer") { Spacer(Modifier.height(24.dp)) }
    }
}

// ─── Loading + empty states ───────────────────────────────────────────────────

@Composable
private fun CardLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LoadingDots()
            Spacer(Modifier.height(16.dp))
            Text(
                text = tr("card.loading", fallback = "Caricamento tessera..."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyCardState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            IconBadge(
                icon = Icons.Outlined.PersonOff,
                variant = IconBadgeVariant.Tertiary,
                size = 72.dp,
                iconSize = 36.dp,
            )
            Text(
                text = tr("card.empty.title", fallback = "Non sei autenticato"),
                style = ExpressiveTypography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = tr("card.empty.body", fallback = "Accedi per visualizzare la tua tessera Mensa"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton(
                text = tr("card.empty.cta", fallback = "Accedi"),
                onClick = { /* navigate to login */ },
            )
        }
    }
}
