package it.mensa.app.features.events

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import it.mensa.app.features.events.util.EventDateFormatter
import it.mensa.app.services.calendar.CalendarHelper
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.theme.MensaCyan
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.EventScheduleModel
import it.mensa.shared.model.LocationModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * EventDetailScreen — M3 canonico.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit = {},
    onEditClick: (String) -> Unit = {},
    vm: EventDetailViewModel = koinViewModel { parametersOf(eventId) },
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val calendarHelper: CalendarHelper = koinInject()

    val shareTitle = tr("events.share.title", fallback = "Condividi evento")
    val calendarErrorMsg = tr("events.calendar.error", fallback = "Impossibile aprire il calendario")
    val calendarSuccessMsg = tr("events.calendar.success", fallback = "Evento aggiunto al calendario")

    var heroReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { heroReady = true }
    val heroScale by animateFloatAsState(targetValue = if (heroReady) 1f else 0.95f, animationSpec = tween(350), label = "HeroScale")
    val heroAlpha by animateFloatAsState(targetValue = if (heroReady) 1f else 0f, animationSpec = tween(350), label = "HeroAlpha")

    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it); vm.clearError() } }
    LaunchedEffect(state.calendarSuccess) {
        if (state.calendarSuccess) { snackbarHostState.showSnackbar(calendarSuccessMsg); vm.clearCalendarSuccess() }
    }
    LaunchedEffect(state.calendarError) { state.calendarError?.let { snackbarHostState.showSnackbar(it); vm.clearCalendarError() } }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHostState = snackbarHostState,
        topBar = {
            TopAppBar(
                title = { Text(state.event?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = tr("app.back", fallback = "Indietro"))
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    val event = state.event
                    if (event != null) {
                        IconButton(onClick = {
                            val shareText = buildShareText(event)
                            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }
                            context.startActivity(Intent.createChooser(intent, shareTitle))
                        }) { Icon(Icons.Default.Share, contentDescription = tr("events.share.label", fallback = "Condividi")) }
                        if (state.canEdit) {
                            IconButton(onClick = { onEditClick(event.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = tr("app.edit", fallback = "Modifica"))
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.loading && state.event == null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { LoadingDots() }
            }
            state.event == null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(tr("events.not_found", fallback = "Evento non trovato"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                val event = state.event!!
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())) {
                    // Hero image 16:9
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).scale(heroScale).alpha(heroAlpha)) {
                        val imageUrl = buildImageUrl(event)
                        if (imageUrl != null) {
                            CachedAsyncImage(model = imageUrl, contentDescription = event.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MensaCyan))))
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp).align(Alignment.BottomStart)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))))
                        if (event.isNational) {
                            Column(modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(text = tr("events.tag.national", fallback = "EVENTO NAZIONALE"), style = MaterialTheme.typography.labelSmall, color = MensaCyan)
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Spacer(Modifier.height(16.dp))

                        // Info card: data + sede
                        Card {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(40.dp)) {
                                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp)) }
                                    }
                                    Column {
                                        Text(tr("events.detail.start", fallback = "Inizio"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(EventDateFormatter.formatFull(event.whenStart), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(40.dp)) {
                                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.CalendarMonth, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp)) }
                                    }
                                    Column {
                                        Text(tr("events.detail.end", fallback = "Fine"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(EventDateFormatter.formatFull(event.whenEnd), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                event.position?.let { pos ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Place, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp)) }
                                        }
                                        Column {
                                            Text(tr("events.detail.venue", fallback = "Sede"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(if (pos.address.isBlank()) pos.name else "${pos.name} – ${pos.address}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Aggiungi al calendario
                        Button(
                            onClick = {
                                val cal = CalendarHelper.CalendarEvent(
                                    title = event.name,
                                    description = event.description.ifBlank { null },
                                    location = event.position?.let { p -> if (p.address.isBlank()) p.name else "${p.name}, ${p.address}" },
                                    startTimeMillis = event.whenStart.toEpochMilliseconds(),
                                    endTimeMillis = event.whenEnd.toEpochMilliseconds(),
                                )
                                val ok = calendarHelper.openCalendarIntent(cal)
                                if (ok) vm.onCalendarSuccess() else vm.onCalendarError(calendarErrorMsg)
                            },
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
                        ) {
                            Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("events.cta.calendar", fallback = "Aggiungi al calendario"))
                        }

                        if (event.bookingLink.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    val uri = Uri.parse(if (event.bookingLink.startsWith("http")) event.bookingLink else "https://${event.bookingLink}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(tr("events.cta.book", fallback = "Prenota")) }
                        }

                        if (event.position != null) {
                            Spacer(Modifier.height(10.dp))
                            TextButton(
                                onClick = {
                                    event.position?.let { pos ->
                                        val uri = Uri.parse("geo:${pos.lat},${pos.lon}?q=${Uri.encode(event.name)}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(tr("events.cta.maps", fallback = "Apri in Mappe"))
                            }
                        }

                        // Descrizione
                        if (event.description.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(tr("events.section.desc", fallback = "Descrizione"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            }
                            Card { Column(Modifier.padding(16.dp)) { Text(event.description, style = MaterialTheme.typography.bodyMedium) } }
                        }

                        // Contatti
                        val hasMail = event.contact.contains("@")
                        val hasLink = event.infoLink.isNotBlank()
                        if (hasMail || hasLink) {
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(tr("events.section.contacts", fallback = "Contatti"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            }
                            Card {
                                Column(Modifier.padding(16.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (hasMail) {
                                            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${event.contact}"))) }) {
                                                Icon(Icons.Outlined.Email, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(tr("events.contact.email", fallback = "Email"))
                                            }
                                        }
                                        if (hasLink) {
                                            OutlinedButton(onClick = {
                                                val url = if (event.infoLink.startsWith("http")) event.infoLink else "https://${event.infoLink}"
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            }) {
                                                Icon(Icons.Outlined.Language, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(tr("events.contact.site", fallback = "Sito"))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Programma
                        if (state.schedules.isNotEmpty()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(tr("events.section.schedule", fallback = "Programma"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            }
                            ScheduleStack(schedules = state.schedules)
                        }

                        // Embedded map
                        event.position?.let { pos ->
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(tr("events.section.map", fallback = "Dove"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            }
                            EmbeddedMap(pos = pos, name = event.name, onOpenMaps = {
                                val uri = Uri.parse("geo:${pos.lat},${pos.lon}?q=${Uri.encode(event.name)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            })
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleStack(schedules: List<EventScheduleModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        schedules.forEach { s ->
            Card {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(EventDateFormatter.formatDayMonth(s.whenStart), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(EventDateFormatter.formatTime(s.whenStart), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.title.ifBlank { tr("events.schedule.session", fallback = "Sessione") }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        if (s.description.isNotBlank()) {
                            Text(s.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        Text("${EventDateFormatter.formatTime(s.whenStart)} – ${EventDateFormatter.formatTime(s.whenEnd)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmbeddedMap(pos: LocationModel, name: String, onOpenMaps: () -> Unit) {
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(pos.lat, pos.lon), 14f) }
    Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(18.dp))) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = com.google.maps.android.compose.MapUiSettings(zoomControlsEnabled = false, scrollGesturesEnabled = false, zoomGesturesEnabled = false, tiltGesturesEnabled = false),
        ) { Marker(state = MarkerState(LatLng(pos.lat, pos.lon)), title = name) }
        IconButton(onClick = onOpenMaps, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) {
                Icon(Icons.Outlined.OpenInBrowser, contentDescription = tr("events.map.open", fallback = "Apri mappa"), modifier = Modifier.padding(6.dp))
            }
        }
    }
}

private fun buildImageUrl(event: EventModel): String? {
    if (event.image.isBlank()) return null
    if (event.image.startsWith("http")) return event.image
    return FilesUrl.build("events", event.id, event.image, "1200x0")
}

private fun buildShareText(event: EventModel): String {
    val parts = mutableListOf(event.name)
    if (event.infoLink.isNotBlank()) parts.add(event.infoLink)
    return parts.joinToString("\n")
}
