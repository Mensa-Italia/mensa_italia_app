package it.mensa.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.koinAccess
import it.mensa.app.support.tr
import it.mensa.shared.changelog.ChangelogItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Le schede dell'apertura sopra la Home, una alla volta: prima il messaggio
 * degli sviluppatori (`DeveloperMessage.pending`), poi "Novità" una volta per
 * versione (`Changelog.shouldShow`). Mirrors iOS `launchNotices()`.
 */
@Composable
fun LaunchNotices() {
    val koin = remember { koinAccess() }
    var developerMessage by remember { mutableStateOf<String?>(null) }
    var changelogTurn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Un attimo dopo la Home, cosi' la scheda sale sopra qualcosa di gia'
        // disegnato invece che durante la transizione.
        delay(600)
        developerMessage = runCatching { koin.developerMessage.pending() }.getOrNull()
        if (developerMessage == null) changelogTurn = true
    }

    // Anche al ritorno in primo piano: un messaggio cambiato mentre l'app
    // era aperta si vede subito, senza chiuderla del tutto.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (developerMessage == null) {
            scope.launch {
                runCatching { koin.developerMessage.pending() }.getOrNull()?.let { developerMessage = it }
            }
        }
    }

    // Toccata una notifica `developer_message`: si rimostra il messaggio di
    // adesso, anche se gia' letto. La richiesta resta finche' non la si
    // prende, cosi' funziona anche se la notifica ha aperto l'app da chiusa.
    val showRequested by koin.developerMessage.showRequested.collectAsStateWithLifecycle()
    LaunchedEffect(showRequested) {
        if (!showRequested) return@LaunchedEffect
        koin.developerMessage.consumeShowRequest()
        runCatching { koin.developerMessage.current() }.getOrNull()?.let { developerMessage = it }
    }

    val message = developerMessage
    if (message != null) {
        DeveloperMessageSheet(message = message) {
            scope.launch { runCatching { koin.developerMessage.markSeen(message) } }
            developerMessage = null
            // Il changelog dopo, a scheda chiusa: due insieme non salgono.
            changelogTurn = true
        }
    } else if (changelogTurn) {
        ChangelogOnLaunch()
    }
}

/**
 * "Novità" sopra la Home, una volta per versione. Le regole stanno in
 * `Changelog` (:shared); qui solo la presentazione. Mirrors iOS
 * `ChangelogSheet`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangelogOnLaunch() {
    val changelog = remember { koinAccess().changelog }
    var show by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        show = runCatching { changelog.shouldShow() }.getOrDefault(false)
    }

    if (!show) return
    val close: () -> Unit = {
        changelog.markSeen()
        scope.launch { sheetState.hide() }.invokeOnCompletion { show = false }
    }
    ModalBottomSheet(
        onDismissRequest = {
            changelog.markSeen()
            show = false
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(
                text = tr("changelog.title", "Novità"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            changelog.items.forEach { ChangelogRow(it) }
            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                text = tr("changelog.done", "Continua"),
                onClick = close,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ChangelogRow(item: ChangelogItem) {
    val (icon, color) = iconFor(item.icon)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(34.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = tr(item.titleKey, item.titleFallback),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = tr(item.bodyKey, item.bodyFallback),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun iconFor(name: String): Pair<ImageVector, Color> = when (name) {
    "passport" -> Icons.Filled.MenuBook to Color(0xFF34C759)
    "medal" -> Icons.Filled.MilitaryTech to Color(0xFFFF9500)
    "qr" -> Icons.Filled.QrCodeScanner to Color(0xFF007AFF)
    "bolt" -> Icons.Filled.Bolt to Color(0xFFFFCC00)
    "bell" -> Icons.Filled.NotificationsActive to Color(0xFFFF3B30)
    "fixes" -> Icons.Filled.Build to Color(0xFF8E8E93)
    else -> Icons.Filled.AutoAwesome to Color(0xFF184295)
}
