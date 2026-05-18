package it.mensa.app.features.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.profile._components.ProfileDropdownRow
import it.mensa.app.features.profile._components.ProfileRow
import it.mensa.app.features.profile._components.ProfileSectionGroup
import it.mensa.app.features.profile._components.ProfileSectionTone
import it.mensa.app.features.profile._components.ProfileToggleRow
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.components.MensaSearchAppBar
import it.mensa.shared.model.UserModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigate: (ProfileRoute) -> Unit = {},
    onSearchTap: () -> Unit = {},
    vm: ProfileViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val appVersion = remember(context) {
        runCatching {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${pInfo.versionName}"
        }.getOrDefault("v1.0")
    }

    var sectionsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { sectionsVisible = true }

    MensaScaffold(
        topBar = {
            MensaSearchAppBar(
                placeholder = tr("profile.search_placeholder", fallback = "Cerca soci, eventi, deal…"),
                onSearchTap = onSearchTap,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(top = 0.dp, bottom = padding.calculateBottomPadding() + 32.dp),
        ) {
            item(key = "profile_header") {
                ProfileHeader(user = uiState.user)
            }

            // Account: Membership, Pagamenti, Dispositivi
            item(key = "section_account") {
                Spacer(Modifier.height(16.dp))
                AnimatedSection(visible = sectionsVisible, indexDelay = 0) {
                    ProfileSectionGroup(
                        kicker = tr("app.profile.section_account_kicker", fallback = "ACCOUNT"),
                        title = tr("app.profile.section_account", fallback = "Account"),
                        tone = ProfileSectionTone.Primary,
                    ) {
                        ProfileRow(
                            icon = Icons.Outlined.Refresh,
                            title = tr("app.profile.membership", fallback = "Iscrizione"),
                            onClick = { onNavigate(ProfileRoute.RenewMembership) },
                        )
                        ProfileRow(
                            icon = Icons.Outlined.CreditCard,
                            title = tr("app.profile.payments", fallback = "Pagamenti"),
                            onClick = { onNavigate(ProfileRoute.PaymentMethods) },
                        )
                        ProfileRow(
                            icon = Icons.Outlined.Devices,
                            title = tr("views.devices.title", fallback = "Dispositivi"),
                            onClick = { onNavigate(ProfileRoute.Devices) },
                        )
                    }
                }
            }

            // Donation: Fai una donazione, Calendario
            item(key = "section_donation") {
                Spacer(Modifier.height(14.dp))
                AnimatedSection(visible = sectionsVisible, indexDelay = 60) {
                    ProfileSectionGroup(
                        kicker = tr("app.profile.section_donation_kicker", fallback = "SUPPORTO"),
                        title = tr("app.profile.section_donation", fallback = "Donazione"),
                        tone = ProfileSectionTone.Tertiary,
                    ) {
                        ProfileRow(
                            icon = Icons.Outlined.Favorite,
                            title = tr("views.make_donation.title", fallback = "Fai una donazione"),
                            onClick = { onNavigate(ProfileRoute.MakeDonation) },
                        )
                        ProfileRow(
                            icon = Icons.Outlined.CalendarMonth,
                            title = tr("app.calendar_link.title", fallback = "Calendario"),
                            onClick = { onNavigate(ProfileRoute.CalendarLinker) },
                        )
                    }
                }
            }

            // Association: Organigramma
            item(key = "section_association") {
                Spacer(Modifier.height(14.dp))
                AnimatedSection(visible = sectionsVisible, indexDelay = 120) {
                    ProfileSectionGroup(
                        kicker = tr("app.profile.section_association_kicker", fallback = "ASSOCIAZIONE"),
                        title = tr("app.profile.section_association", fallback = "Associazione"),
                        tone = ProfileSectionTone.Neutral,
                    ) {
                        ProfileRow(
                            icon = Icons.Outlined.AccountTree,
                            title = tr("app.org_chart.title", fallback = "Organigramma"),
                            onClick = { onNavigate(ProfileRoute.OrgChart) },
                        )
                    }
                }
            }

            // App Settings: Lingua, Tema, Notifiche
            item(key = "section_settings") {
                Spacer(Modifier.height(14.dp))
                AnimatedSection(visible = sectionsVisible, indexDelay = 180) {
                    ProfileSectionGroup(
                        kicker = tr("app.profile.section_prefs_kicker", fallback = "IMPOSTAZIONI"),
                        title = tr("app.profile.section_app", fallback = "App"),
                        tone = ProfileSectionTone.Neutral,
                    ) {
                        ProfileRow(
                            icon = Icons.Outlined.Language,
                            title = tr("app.profile.language", fallback = "Lingua"),
                            trailing = uiState.localeName.ifBlank { null },
                            onClick = { onNavigate(ProfileRoute.LanguagePicker) },
                        )
                        ProfileDropdownRow(
                            icon = Icons.Outlined.DarkMode,
                            title = tr("app.profile.theme", fallback = "Tema"),
                            value = uiState.themeMode,
                            options = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK),
                            onSelect = vm::onThemeModeChange,
                            labelFor = { themeLabel(it) },
                        )
                        ProfileToggleRow(
                            icon = Icons.Outlined.Notifications,
                            title = tr("app.profile.notifications", fallback = "Notifiche"),
                            checked = uiState.notificationsEnabled,
                            onCheckedChange = vm::onNotificationsToggle,
                        )
                    }
                }
            }

            // Info: Versione, Privacy Policy, Termini, Crediti
            item(key = "section_info") {
                Spacer(Modifier.height(14.dp))
                AnimatedSection(visible = sectionsVisible, indexDelay = 240) {
                    ProfileSectionGroup(
                        kicker = tr("app.profile.section_info_kicker", fallback = "INFO"),
                        title = tr("app.profile.section_info", fallback = "Informazioni"),
                        tone = ProfileSectionTone.Neutral,
                    ) {
                        ProfileRow(
                            icon = Icons.Outlined.Info,
                            title = tr("app.profile.version", fallback = "Versione"),
                            trailing = appVersion,
                            // display-only: no onClick → no ripple, no chevron
                        )
                        ProfileRow(
                            icon = Icons.Outlined.Policy,
                            title = tr("app.profile.privacy_policy", fallback = "Privacy Policy"),
                            onClick = { onNavigate(ProfileRoute.PrivacyPolicy) },
                        )
                        ProfileRow(
                            icon = Icons.Outlined.Article,
                            title = tr("app.profile.terms", fallback = "Termini di utilizzo"),
                            onClick = { onNavigate(ProfileRoute.Terms) },
                        )
                        ProfileRow(
                            icon = Icons.Outlined.AutoAwesome,
                            title = tr("app.profile.credits", fallback = "Crediti"),
                            onClick = { onNavigate(ProfileRoute.Credits) },
                        )
                    }
                }
            }

            // Logout — Apple/iOS Settings convention: a single centered
            // destructive text button (no icon, no card chrome), with the
            // signed-in email shown as footer underneath.
            item(key = "section_logout") {
                Spacer(Modifier.height(28.dp))
                AnimatedSection(visible = sectionsVisible, indexDelay = 300) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TextButton(
                            onClick = { if (!uiState.loggingOut) vm.onLogoutRequest() },
                            enabled = !uiState.loggingOut,
                        ) {
                            Text(
                                text = if (uiState.loggingOut)
                                    tr("app.profile.logging_out", fallback = "Uscita…")
                                else
                                    tr("views.settings.tile.logout.title", fallback = "Esci"),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        uiState.user?.email?.takeIf { it.isNotBlank() }?.let { email ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = tr(
                                    "app.profile.signed_in_as",
                                    fallback = "Hai effettuato l'accesso come $email",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                        }
                    }
                }
            }

            item(key = "version_footer") {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = tr("app.profile.footer", fallback = "Mensa Italia $appVersion"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = vm::onLogoutDismiss,
            title = {
                Text(tr("app.profile.logout_confirm_title", fallback = "Vuoi uscire dall'account?"))
            },
            text = {
                Text(tr("app.profile.logout_confirm_message", fallback = "Dovrai accedere di nuovo per usare l'app."))
            },
            confirmButton = {
                TextButton(onClick = vm::onLogoutConfirm) {
                    Text(
                        tr("views.settings.tile.logout.title", fallback = "Esci"),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = vm::onLogoutDismiss) {
                    Text(tr("views.make_donation.cancel", fallback = "Annulla"))
                }
            },
        )
    }

    uiState.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = vm::onErrorDismiss,
            title = { Text(tr("app.error.title", fallback = "Errore")) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = vm::onErrorDismiss) { Text("OK") }
            },
        )
    }
}

@Composable
private fun AnimatedSection(
    visible: Boolean,
    indexDelay: Int,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 320, delayMillis = indexDelay)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 320, delayMillis = indexDelay),
                initialOffsetY = { it / 8 },
            ),
    ) {
        content()
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> tr("app.theme.system", fallback = "Sistema")
    ThemeMode.LIGHT -> tr("app.theme.light", fallback = "Chiaro")
    ThemeMode.DARK -> tr("app.theme.dark", fallback = "Scuro")
}

@Composable
private fun ProfileHeader(user: UserModel?) {
    val displayName = remember(user?.name, user?.username, user?.email) {
        deriveDisplayName(user)
    }
    val email = user?.email?.takeIf { it.isNotBlank() }
    val avatarUrl = user?.let { u ->
        if (u.avatar.isNotBlank()) {
            FilesUrl.build(
                collection = "users",
                recordId = u.id,
                filename = u.avatar,
                thumb = "300x300",
            )
        } else null
    }
    val initials = remember(user?.name, user?.username, user?.email) {
        deriveInitials(user)
    }
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            when {
                avatarUrl != null -> CachedAsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                initials.isNotBlank() -> Text(
                    text = initials,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = colorScheme.onSecondaryContainer,
                    ),
                )
                else -> Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = displayName,
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (email != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun deriveDisplayName(user: UserModel?): String {
    if (user == null) return "Socio Mensa"
    val direct = user.name.trim().ifBlank { user.username.trim() }
    if (direct.isNotBlank()) return direct
    val local = user.email.substringBefore('@', "").trim()
    if (local.isBlank()) return "Socio Mensa"
    return local.replace('.', ' ').replace('_', ' ').replace('-', ' ')
        .split(' ').filter { it.isNotBlank() }
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }
        .ifBlank { "Socio Mensa" }
}

private fun deriveInitials(user: UserModel?): String {
    if (user == null) return ""
    val baseName = user.name.trim().ifBlank { user.username.trim() }.ifBlank {
        user.email.substringBefore('@', "").replace('.', ' ').replace('_', ' ').trim()
    }
    if (baseName.isBlank()) return ""
    val parts = baseName.split(' ', '.', '_', '-').filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}".uppercase()
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> ""
    }
}
