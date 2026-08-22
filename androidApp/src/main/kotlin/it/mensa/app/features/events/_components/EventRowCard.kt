package it.mensa.app.features.events._components

import it.mensa.app.support.rememberAppLocale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.mensa.app.features.events.util.EventDateFormatter
import it.mensa.app.features.events.util.icon
import it.mensa.app.features.events.util.label
import it.mensa.app.features.events.util.tint
import it.mensa.app.support.FilesUrl
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.components.rememberCoverRatio
import it.mensa.app.ui.theme.MensaCyan
import it.mensa.shared.geo.EventScopes
import it.mensa.shared.model.EventModel

/**
 * EventRowCard — M3 canonico.
 * Card M3 wrapper, date badge con Surface/CircleShape, shape morph on press.
 */
@Composable
fun EventRowCard(
    event: EventModel,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val locale = rememberAppLocale()
    val isPast = EventDateFormatter.isPast(event)
    val imageUrl = buildImageUrl(event)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "EventCardScale",
    )

    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .graphicsLayer { alpha = if (isPast) 0.72f else 1f },
        interactionSource = interactionSource,
    ) {
        Column {
            // Hero image
            //
            // Il rapporto era fisso a 16:9, e le locandine che 16:9 non sono
            // venivano tagliate sopra e sotto. Ora lo detta l'immagine vera,
            // dentro i limiti di [CoverRatio].
            val cover = rememberCoverRatio(imageUrl)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(cover.ratio)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                if (imageUrl != null) {
                    CachedAsyncImage(
                        model = imageUrl,
                        contentDescription = event.name,
                        contentScale = ContentScale.Crop,
                        // matchParentSize e non fillMaxWidth: con la sola
                        // larghezza l'immagine si dimensionava in altezza sul
                        // proprio rapporto e a ritagliarla finiva il clip del
                        // Box, quindi fuori centro invece che dal Crop.
                        modifier = Modifier.matchParentSize(),
                        onState = cover.onImageState,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                        MensaCyan.copy(alpha = 0.55f),
                                    )
                                )
                            )
                    )
                }
                // Top gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.TopStart)
                        .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.40f), Color.Transparent)))
                )
                // Tag chips top-left
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isPast) {
                        EventTagChip(text = "Concluso", tint = Color.Gray)
                        Spacer(Modifier.width(6.dp))
                    }
                    // Nazionale, Internazionale, Locale o Online: la decide
                    // EventScopes, non `is_national` da solo.
                    val scope = EventScopes.of(event)
                    EventTagChip(text = scope.label(), tint = scope.tint(), icon = scope.icon)
                    if (event.isSpot) {
                        Spacer(Modifier.width(6.dp))
                        EventTagChip(text = "Spot", tint = Color(0xFFFFA500))
                    }
                }
            }

            // Meta block
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // Date badge
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp).padding(top = 2.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Data al posto di KickerLabel
                    Text(
                        text = EventDateFormatter.formatMedium(event.whenStart, locale),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val pos = event.position
                    if (pos != null) {
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Place,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = if (pos.address.isBlank()) pos.name else pos.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pillola sopra la locandina.
 *
 * `tint` prima arrivava e veniva buttato via: il fondo era sempre bianco al 18%
 * e il testo sempre bianco, quindi Nazionale e Locale uscivano identici — ed e'
 * per questo che aggiungere "Internazionale" senza toccare questa funzione non
 * si sarebbe visto. Adesso il fondo e' nero pieno (contrasto AA su qualunque
 * immagine, come su iOS) e il colore lo portano il bordo e l'icona.
 */
@Composable
private fun EventTagChip(text: String, tint: Color, icon: ImageVector? = null) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, tint),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

private fun buildImageUrl(event: EventModel): String? {
    if (event.image.isBlank()) return null
    if (event.image.startsWith("http")) return event.image
    return FilesUrl.build("events", event.id, event.image, "800x0")
}
