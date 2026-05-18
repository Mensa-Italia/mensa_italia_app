package it.mensa.app.features.publicarea

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.theme.BackdropDark
import it.mensa.app.ui.theme.MensaBlue
import it.mensa.shared.model.LocalOfficeAdminModel
import it.mensa.shared.model.LocalOfficeAssistantModel
import it.mensa.shared.model.LocalOfficeModel
import it.mensa.shared.model.LocalOfficeTestDateModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pre-login local-office detail. Mirrors `PublicLocalOfficeDetailView.swift`:
 * stretchy hero cover, kicker + name + bio, then test dates / admins /
 * assistants sections. Tapping an admin/assistant opens the system mail
 * composer (no auth required → no in-app messaging).
 */
@Composable
fun PublicLocalOfficeDetailScreen(
    officeId: String,
    onBack: () -> Unit,
    onTestSessionClick: (LocalOfficeTestDateModel) -> Unit,
    onMemberClick: (PublicMemberContact) -> Unit,
    vm: PublicLocalOfficeDetailViewModel = koinViewModel(parameters = { parametersOf(officeId) }),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingDots()
            }
            state.error != null -> ErrorMessage(state.error!!)
            state.office != null -> OfficeContent(
                office = state.office!!,
                admins = state.admins,
                assistants = state.assistants,
                testDates = state.testDates,
                onAdminClick = { admin ->
                    onMemberClick(
                        PublicMemberContact(
                            name = admin.name,
                            roleLabel = if (admin.isTheOfficer) "Segretario" else "Cosegretario",
                            email = admin.email,
                            imageUrl = if (admin.image.isEmpty()) null
                            else it.mensa.app.support.FilesUrl.build(
                                "view_local_office_admins",
                                admin.id,
                                admin.image,
                                "400x400",
                            ),
                            officeName = state.office!!.name,
                            region = state.office!!.region,
                        ),
                    )
                },
                onAssistantClick = { a ->
                    onMemberClick(
                        PublicMemberContact(
                            name = a.name,
                            roleLabel = "Assistente al test",
                            email = a.email,
                            imageUrl = if (a.image.isEmpty()) null
                            else it.mensa.app.support.FilesUrl.build(
                                "view_local_office_assistants",
                                a.id,
                                a.image,
                                "200x200",
                            ),
                            officeName = state.office!!.name,
                            region = state.office!!.region,
                            area = a.area,
                            state = a.state,
                            city = a.city,
                        ),
                    )
                },
                onTestSessionClick = onTestSessionClick,
            )
        }

        // Floating back button over the hero.
        Surface(
            modifier = Modifier
                .statusBarsPadding()
                .padding(12.dp)
                .size(40.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = tr("common.back", fallback = "Indietro"),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun OfficeContent(
    office: LocalOfficeModel,
    admins: List<LocalOfficeAdminModel>,
    assistants: List<LocalOfficeAssistantModel>,
    testDates: List<LocalOfficeTestDateModel>,
    onAdminClick: (LocalOfficeAdminModel) -> Unit,
    onAssistantClick: (LocalOfficeAssistantModel) -> Unit,
    onTestSessionClick: (LocalOfficeTestDateModel) -> Unit,
) {
    val coverUrl = remember(office.id, office.image) {
        if (office.image.isEmpty()) null
        else FilesUrl.build("view_local_office", office.id, office.image, "1200x800")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
        ) {
            if (coverUrl != null) {
                CachedAsyncImage(
                    model = coverUrl,
                    contentDescription = office.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(MensaBlue, BackdropDark))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Business,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                        ),
                    ),
            )
        }

        // Kicker + name + region + bio
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = tr("public.local_office.kicker", fallback = "GRUPPO LOCALE"),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.8.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = office.name,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (office.region.isNotBlank() &&
                !office.region.equals(office.name, ignoreCase = true)
            ) {
                Text(
                    text = office.region,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (office.bio.isNotBlank()) {
                Text(
                    text = office.bio,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (testDates.isNotEmpty()) {
            SectionBlock(
                title = tr("public.local_office.section.test_dates", fallback = "Prossime sessioni di test"),
                footer = tr(
                    "public.local_office.test_dates_footer",
                    fallback = "Tocca una sessione per vedere chi contattare e prenotare il posto.",
                ),
            ) {
                testDates.forEach { td ->
                    TestDateCard(td) { onTestSessionClick(td) }
                }
            }
        }

        val sortedAdmins = remember(admins) {
            admins.sortedWith(compareByDescending<LocalOfficeAdminModel> { it.isTheOfficer }.thenBy { it.name })
        }
        if (sortedAdmins.isNotEmpty()) {
            SectionBlock(
                title = tr("public.local_office.section.admins", fallback = "Referenti"),
                footer = tr("public.local_office.admins_footer", fallback = "Tocca un nome per scrivere al referente."),
            ) {
                sortedAdmins.forEach { admin ->
                    AdminRow(admin) { onAdminClick(admin) }
                }
            }
        }

        if (assistants.isNotEmpty()) {
            SectionBlock(
                title = tr("public.local_office.section.assistants", fallback = "Assistenti al test"),
                footer = tr(
                    "public.local_office.assistants_footer",
                    fallback = "Gli assistenti gestiscono le sessioni di test. Toccane uno per contattarlo.",
                ),
            ) {
                assistants.forEach { a ->
                    AssistantRow(a) { onAssistantClick(a) }
                }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

// ─── Section + Row helpers ────────────────────────────────────────────────────

@Composable
private fun SectionBlock(
    title: String,
    footer: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun TestDateCard(
    td: LocalOfficeTestDateModel,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(36.dp)
                    .padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatItalianDateTime(td.date.toEpochMilliseconds()),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (td.location.isNotBlank()) {
                    InlineMeta(Icons.Outlined.LocationOn, td.location)
                }
                if (td.notes.isNotBlank()) {
                    InlineMeta(Icons.Outlined.Notes, td.notes, maxLines = 3)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (td.maxParticipants > 0) {
                    Pill(
                        text = tr(
                            "public.local_office.test_date.max_short",
                            fallback = "max ${td.maxParticipants}",
                        ),
                    )
                }
                if (td.assistants.isNotEmpty()) {
                    Text(
                        text = if (td.assistants.size == 1)
                            tr("public.local_office.test_date.assistants_one", fallback = "1 assistente")
                        else
                            tr(
                                "public.local_office.test_date.assistants_other",
                                fallback = "${td.assistants.size} assistenti",
                            ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminRow(admin: LocalOfficeAdminModel, onClick: () -> Unit) {
    val imageUrl = remember(admin.id, admin.image) {
        if (admin.image.isEmpty()) null
        else FilesUrl.build("view_local_office_admins", admin.id, admin.image, "400x400")
    }
    PersonRow(
        imageUrl = imageUrl,
        name = admin.name,
        subtitle = null,
        trailing = {
            Pill(
                text = if (admin.isTheOfficer)
                    tr("public.local_office.role.officer", fallback = "Segretario")
                else
                    tr("public.local_office.role.co_officer", fallback = "Cosegretario"),
                tone = if (admin.isTheOfficer) PillTone.Primary else PillTone.Neutral,
            )
        },
        onClick = onClick,
    )
}

@Composable
internal fun AssistantRow(a: LocalOfficeAssistantModel, onClick: () -> Unit) {
    val imageUrl = remember(a.id, a.image) {
        if (a.image.isEmpty()) null
        else FilesUrl.build("view_local_office_assistants", a.id, a.image, "200x200")
    }
    PersonRow(
        imageUrl = imageUrl,
        name = a.name,
        subtitle = assistantSubtitle(a),
        trailing = {
            Icon(
                Icons.Outlined.Mail,
                contentDescription = tr("common.email", fallback = "Email"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun PersonRow(
    imageUrl: String?,
    name: String,
    subtitle: String?,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(imageUrl = imageUrl, name = name)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleCase(name),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailing()
        }
    }
}

@Composable
private fun Avatar(imageUrl: String?, name: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            CachedAsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = initials(name),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InlineMeta(icon: ImageVector, text: String, maxLines: Int = 1) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal enum class PillTone { Primary, Neutral }

@Composable
internal fun Pill(text: String, tone: PillTone = PillTone.Primary) {
    val (bg, fg) = when (tone) {
        PillTone.Primary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.primary
        PillTone.Neutral -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = CircleShape, color = bg) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun ErrorMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(24.dp),
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

internal fun formatItalianDateTime(epochMillis: Long): String {
    val df = SimpleDateFormat("EEEE d MMMM yyyy, HH:mm", Locale.ITALIAN)
    return df.format(Date(epochMillis))
        .replaceFirstChar { it.uppercase(Locale.ITALIAN) }
}

internal fun assistantSubtitle(a: LocalOfficeAssistantModel): String? {
    val seen = mutableSetOf<String>()
    val parts = mutableListOf<String>()
    for (raw in listOf(a.area, a.state, a.city)) {
        val v = raw.trim()
        if (v.isEmpty()) continue
        val k = v.lowercase()
        if (k in seen) continue
        seen.add(k)
        parts.add(v)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

internal fun titleCase(s: String): String =
    s.split(' ').joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ITALIAN) else it.toString() }
    }

internal fun initials(name: String): String {
    val parts = name.trim().split(' ').filter { it.isNotEmpty() }.take(2)
    val chars = parts.mapNotNull { it.firstOrNull() }
    return chars.joinToString("").uppercase().ifEmpty { "?" }
}

