package it.mensa.app.features.localgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.mensa.app.support.AppFormat
import it.mensa.app.support.tr
import it.mensa.shared.localgroup.LocalGroupDates
import java.text.DateFormatSymbols
import java.util.Locale

// Pezzi comuni alle pagine del gruppo locale: solo componenti Material 3
// (Card, ListItem, TopAppBar, LoadingIndicator) messi insieme.

/** A section: its title in primary, then its rows in a [Card]. */
@Composable
internal fun LocalGroupSection(title: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 8.dp),
            )
        } else {
            Spacer(Modifier.height(16.dp))
        }
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) { Column(content = content) }
    }
}

/** The divider between two rows of a card. */
@Composable
internal fun RowDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

/** ListItem colors on a card: transparent, the card gives the surface. */
@Composable
internal fun cardItemColors() = ListItemDefaults.colors(containerColor = Color.Transparent)

/** Trailing chevron of a row that opens a page. */
@Composable
internal fun Chevron() {
    Icon(
        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A text in the middle of the page, with an optional button under it. */
@Composable
internal fun CenteredMessage(text: String, modifier: Modifier = Modifier, action: Pair<String, () -> Unit>? = null) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            action?.let { (label, onClick) -> Button(onClick = onClick) { Text(label) } }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CenteredLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
}

/** The admin pages' top bar: [title], back arrow, optional [actions]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = tr("app.back", "Indietro"))
            }
        },
        actions = { actions() },
    )
}

/** "Mario Rossi" from "MARIO ROSSI" (the registry writes names in capitals). */
internal fun personName(raw: String, locale: Locale = AppFormat.locale()): String =
    raw.trim().split(' ').filter { it.isNotEmpty() }
        .joinToString(" ") { AppFormat.titlecase(it.lowercase(locale), locale) }

/** "03/11/2026" from "2026-11-03" (or the raw text when unreadable). */
internal fun shortDate(iso: String): String {
    val date = LocalGroupDates.parse(iso) ?: return iso
    return "%02d/%02d/%04d".format(date.dayOfMonth, date.monthNumber, date.year)
}

/** The short name of month [number] (1 to 12) in [locale]: "ott", "nov". */
internal fun shortMonth(number: Int, locale: Locale): String =
    DateFormatSymbols.getInstance(locale).shortMonths.getOrNull(number - 1)?.trimEnd('.').orEmpty()
