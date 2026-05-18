package it.mensa.app.features.publicarea

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.model.LocalOfficeAssistantModel
import it.mensa.shared.model.LocalOfficeModel
import it.mensa.shared.model.LocalOfficeTestDateModel

/**
 * Pre-login detail of a single test session. Mirrors
 * `PublicTestSessionDetailView.swift`: date + location + capacity + this
 * session's assistants (filtered by user-id from the parent's full list).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicTestSessionDetailScreen(
    testDate: LocalOfficeTestDateModel,
    office: LocalOfficeModel,
    allAssistants: List<LocalOfficeAssistantModel>,
    onMemberClick: (PublicMemberContact) -> Unit,
    onBack: () -> Unit,
) {
    val sessionAssistants = remember(testDate.assistants, allAssistants) {
        val ids = testDate.assistants.toSet()
        allAssistants.filter { it.user in ids }
    }

    MensaScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        tr("public.test_session.title", fallback = "Sessione di test"),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = tr("common.back", fallback = "Indietro"),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Kicker + date prominent
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = tr("public.test_session.kicker", fallback = "SESSIONE DI TEST"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.8.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = formatItalianDateTime(testDate.date.toEpochMilliseconds()),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = office.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Info card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = tr("public.test_session.section.details", fallback = "Dettagli"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (testDate.location.isNotBlank()) {
                            InfoRow(
                                icon = Icons.Outlined.LocationOn,
                                title = tr("public.test_session.info.where", fallback = "Dove"),
                                value = testDate.location,
                            )
                        }
                        if (testDate.maxParticipants > 0) {
                            InfoRow(
                                icon = Icons.Outlined.People,
                                title = tr("public.test_session.info.seats", fallback = "Posti"),
                                value = tr(
                                    "public.test_session.info.seats_value",
                                    fallback = "max ${testDate.maxParticipants}",
                                ),
                            )
                        }
                        if (testDate.notes.isNotBlank()) {
                            InfoRow(
                                icon = Icons.Outlined.Notes,
                                title = tr("public.test_session.info.notes", fallback = "Note"),
                                value = testDate.notes,
                            )
                        }
                    }
                }
            }

            // Assistants of this session
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (sessionAssistants.isNotEmpty()) {
                    Text(
                        text = tr(
                            "public.test_session.section.assistants",
                            fallback = "Assistenti di questa sessione",
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    sessionAssistants.forEach { a ->
                        AssistantRow(a) {
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
                                    officeName = office.name,
                                    region = office.region,
                                    area = a.area,
                                    state = a.state,
                                    city = a.city,
                                ),
                            )
                        }
                    }
                    Text(
                        text = tr(
                            "public.test_session.assistants_footer",
                            fallback = "Tocca un nome per scrivere all'assistente e prenotare il posto.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = tr(
                            "public.test_session.section.assistants_empty",
                            fallback = "Assistenti",
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = tr(
                                "public.test_session.assistants_empty_body",
                                fallback = "Nessun assistente assegnato a questa sessione. Scrivi a un referente del gruppo per informazioni.",
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, value: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

