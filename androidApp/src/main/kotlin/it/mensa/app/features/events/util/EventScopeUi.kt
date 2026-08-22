package it.mensa.app.features.events.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import it.mensa.app.support.tr
import it.mensa.app.ui.theme.MensaCyan
import it.mensa.app.ui.theme.MensaTeal
import it.mensa.shared.geo.EventScope

/**
 * Come si vede un [EventScope]: etichetta, icona, colore.
 *
 * Sta tutto qui perche' i punti che lo disegnano sono tre — la chip della
 * lista, il badge del dettaglio, il pin della mappa — e prima ognuno scriveva
 * "Nazionale" per conto suo. Era anche il motivo per cui un evento
 * internazionale non si distingueva: nessuno dei tre aveva il caso.
 */
@Composable
fun EventScope.label(): String = when (this) {
    EventScope.ONLINE -> tr("events.type.online", fallback = "Online")
    EventScope.LOCAL -> tr("events.type.local", fallback = "Locale")
    EventScope.NATIONAL -> tr("events.type.national", fallback = "Nazionale")
    EventScope.INTERNATIONAL -> tr("events.type.international", fallback = "Internazionale")
}

val EventScope.icon: ImageVector
    get() = when (this) {
        EventScope.ONLINE -> Icons.Outlined.Wifi
        EventScope.LOCAL -> Icons.Outlined.Place
        EventScope.NATIONAL -> Icons.Filled.Flag
        EventScope.INTERNATIONAL -> Icons.Outlined.Public
    }

/**
 * Tinta della chip. Nazionale e Locale restano i due blu di brand; per
 * Internazionale serviva un terzo colore che non fosse un altro blu (non si
 * sarebbe distinto) ne' l'arancio, gia' preso da "Spot".
 */
@Composable
fun EventScope.tint(): Color = when (this) {
    EventScope.ONLINE -> MaterialTheme.colorScheme.onSurfaceVariant
    EventScope.LOCAL -> MensaCyan
    EventScope.NATIONAL -> MaterialTheme.colorScheme.primary
    EventScope.INTERNATIONAL -> MensaTeal
}
