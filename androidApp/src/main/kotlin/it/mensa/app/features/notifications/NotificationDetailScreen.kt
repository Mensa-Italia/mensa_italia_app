package it.mensa.app.features.notifications

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import it.mensa.app.features.notifications._components.notificationBodyText
import it.mensa.app.features.notifications._components.notificationIcon
import it.mensa.app.features.notifications._components.notificationTitleText
import it.mensa.app.navigation.DeepLinkHandler
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.model.NotificationModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    notificationId: String,
    navController: NavController,
    vm: NotificationDetailViewModel = koinViewModel(parameters = { parametersOf(notificationId) }),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(tr("notifications.detail.title", fallback = "Dettaglio")) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = tr("app.back", fallback = "Indietro"))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        when {
            uiState.loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            uiState.notification != null -> {
                NotificationDetailContent(
                    notification = uiState.notification!!,
                    navController = navController,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(text = tr("notifications.detail.not_found", fallback = "Notifica non trovata"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun NotificationDetailContent(
    notification: NotificationModel,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val icon = remember(notification.id) { notificationIcon(notification) }
    val title = remember(notification.id, notification.tr) { notificationTitleText(notification) }
    val body = remember(notification.id, notification.tr) { notificationBodyText(notification) }
    val target = remember(notification.id) { notificationTarget(notification) }

    val formattedDate = remember(notification.created) {
        val tz = TimeZone.currentSystemDefault()
        val dt = notification.created.toLocalDateTime(tz)
        val days = arrayOf("lunedi", "martedi", "mercoledi", "giovedi", "venerdi", "sabato", "domenica")
        val months = arrayOf("gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno", "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre")
        val day = days.getOrNull(dt.dayOfWeek.ordinal) ?: ""
        val month = months.getOrNull(dt.monthNumber - 1) ?: ""
        "$day ${dt.dayOfMonth} $month ${dt.year}, ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        // Header: icon badge + title + date
        Row(verticalAlignment = Alignment.Top) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = formattedDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))

        if (body.isNotEmpty()) {
            Text(text = body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (target != null) {
            val ctaLabel = ctaLabel(target)
            Button(
                onClick = { DeepLinkHandler.handleNotificationTarget(target = target, navController = navController) },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(ctaLabel)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ctaLabel(target: NotificationTarget): String = when (target) {
    is NotificationTarget.Event -> tr("notifications.go_event", fallback = "Vai all'evento")
    is NotificationTarget.Deal -> tr("notifications.go_deal", fallback = "Vai all'offerta")
    is NotificationTarget.SingleDocument -> tr("notifications.go_document", fallback = "Apri documento")
    is NotificationTarget.MultipleDocuments -> tr("notifications.go_documents", fallback = "Vai ai documenti")
    is NotificationTarget.TicketPurchase -> tr("notifications.go_tickets", fallback = "Vai ai biglietti")
    is NotificationTarget.PaymentUpdateStatus -> tr("notifications.go_receipts", fallback = "Vai alle ricevute")
    is NotificationTarget.Quid -> tr("notifications.go_quid", fallback = "Apri Quid")
    is NotificationTarget.QuidArticle -> tr("notifications.go_quid_article", fallback = "Leggi articolo")
    is NotificationTarget.QuidPdf -> tr("notifications.go_quid_pdf", fallback = "Apri PDF Quid")
    is NotificationTarget.LocalOffice -> tr("notifications.go_local_office", fallback = "Apri gruppo locale")
    is NotificationTarget.AccountConfirmation -> tr("notifications.go_account_confirmation", fallback = "Conferma identità")
}
