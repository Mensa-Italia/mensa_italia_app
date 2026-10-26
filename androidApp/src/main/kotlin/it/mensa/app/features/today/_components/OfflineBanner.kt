package it.mensa.app.features.today._components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.AppFormat
import it.mensa.app.support.koinAccess
import it.mensa.app.support.rememberAppLocale
import it.mensa.app.support.tr

/**
 * "Sei offline": i dati a schermo sono quelli dell'ultimo contatto col
 * server. Compare solo quando la rete ha davvero fallito, sparisce da sola
 * appena il server risponde. Mirrors iOS `OfflineBanner`.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    val connectivity = remember { koinAccess().connectivity }
    val reachable by connectivity.reachable.collectAsStateWithLifecycle()
    val lastContact by connectivity.lastContact.collectAsStateWithLifecycle()
    val locale = rememberAppLocale()

    AnimatedVisibility(
        visible = reachable == false,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    Text(
                        text = tr("app.offline.title", "Sei offline"),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    val last = lastContact
                    Text(
                        text = if (last != null) {
                            tr(
                                "app.offline.updated_at",
                                "Dati aggiornati al {date}",
                                "date" to AppFormat.format(last, AppFormat.Skeleton.MEDIUM_WITH_TIME, locale),
                            )
                        } else {
                            tr("app.offline.not_updated", "I dati potrebbero non essere aggiornati")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
