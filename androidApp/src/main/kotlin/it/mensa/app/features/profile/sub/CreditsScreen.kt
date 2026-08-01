package it.mensa.app.features.profile.sub

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold

/**
 * "Crediti" — una sola attribuzione, centrata sullo schermo.
 *
 * Niente hero, niente stack tecnologico, niente colophon: la pagina dice chi
 * ha scritto l'app e nient'altro. Il grigio su grigio e la coppia di pesi
 * diversi fanno tutto il lavoro tipografico, senza bordi ne' card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    onBack: () -> Unit,
) {
    MensaScaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("app.credits.title", fallback = "Crediti")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Stessa chiave di iOS. Il fallback qui diceva "Sviluppatore
                // iOS & Android", che non e' il valore in catalogo: un fallback
                // sbagliato finisce a Tolgee come sorgente italiana.
                Text(
                    text = tr("app.credits.developed_by", fallback = "Sviluppato da"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
                // Nome proprio: non passa da `tr()` in nessuna lingua.
                Text(
                    text = "Matteo Sipione",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
