package it.mensa.app.features.tickets

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.card._components.QrCodeView
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.model.TicketModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    ticketId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: TicketDetailViewModel = koinViewModel(parameters = { parametersOf(ticketId) })
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    MensaScaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(tr("tickets.detail.title", fallback = "Ticket")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("app.back", fallback = "Indietro"))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                fadeIn(animationSpec = spring(stiffness = 300f)) togetherWith
                    fadeOut(animationSpec = spring(stiffness = 300f))
            },
            label = "ticket_detail_content",
        ) { state ->
            when {
                state.loading && state.ticket == null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.ticket != null -> {
                    TicketContent(
                        ticket = state.ticket,
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                    )
                }

                state.error != null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                else -> {
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text(tr("tickets.not_found", fallback = "Ticket non trovato"))
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketContent(
    ticket: TicketModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val status = ticket.statusComputed
    val statusColor = when (status) {
        TicketStatus.Pending -> Color(0xFFF59E0B)
        TicketStatus.Completed -> Color(0xFF22C55E)
        TicketStatus.Failed -> Color(0xFFEF4444)
        TicketStatus.Unknown -> Color(0xFF9CA3AF)
    }
    val statusLabel = when (status) {
        TicketStatus.Pending -> tr("tickets.status.pending", fallback = "In attesa")
        TicketStatus.Completed -> tr("tickets.status.completed", fallback = "Completato")
        TicketStatus.Failed -> tr("tickets.status.failed", fallback = "Fallito")
        TicketStatus.Unknown -> "—"
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Header — title + status
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = ticket.name ?: tr("tickets.no_name", fallback = "Ticket"),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = statusColor) {}
                Text(text = statusLabel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = statusColor)
            }
        }

        // Description
        val desc = ticket.description
        if (!desc.isNullOrBlank()) {
            Text(text = desc, style = MaterialTheme.typography.bodyLarge)
        }

        // QR code
        val qr = ticket.qr
        if (!qr.isNullOrBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                ) {
                    Text(
                        text = tr("tickets.qr_label", fallback = "Mostra all'ingresso").uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(0.5f, androidx.compose.ui.unit.TextUnitType.Sp),
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    QrCodeView(payload = qr, size = 220.dp, cornerRadius = 12.dp)
                }
            }
        }

        // Info rows card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val deadline = ticket.deadline
                if (deadline != null) {
                    InfoRow(icon = Icons.Outlined.CalendarMonth, label = tr("tickets.deadline", fallback = "Scadenza"), value = formatItalianInstant(deadline))
                }
                val ref = ticket.internalRefId
                if (!ref.isNullOrBlank()) {
                    InfoRow(icon = Icons.Outlined.Numbers, label = tr("tickets.ref", fallback = "Riferimento"), value = ref)
                }
                InfoRow(icon = Icons.Outlined.Schedule, label = tr("tickets.created", fallback = "Creato"), value = formatItalianInstant(ticket.created))
            }
        }

        // Link to event CTA
        val link = ticket.link
        if (!link.isNullOrBlank()) {
            Button(
                onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) } },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
            ) {
                Icon(Icons.Outlined.ArrowOutward, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("tickets.go_event", fallback = "Vai all'evento"))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoRow(
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

private fun formatItalianInstant(instant: Instant): String {
    return try {
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val date = Date(instant.toEpochMilliseconds())
        val fmt = SimpleDateFormat("d MMMM yyyy", Locale.ITALIAN)
        fmt.format(date)
    } catch (_: Exception) {
        "—"
    }
}
