package it.mensa.app.features.documents

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Apre il PDF di un documento partendo dal solo id.
 *
 * Prima al suo posto c'era una schermata di dettaglio con il riassunto AI, e
 * il PDF si apriva solo da un bottone in fondo. Ora toccare un documento lo
 * apre e basta: questa schermata esiste solo per i punti d'ingresso che hanno
 * l'id ma non il file (notifiche, deep link, Spotlight, ricerca). Appena
 * l'indirizzo e' noto si passa al visore e questa esce dallo stack, cosi'
 * "indietro" torna alla lista e non qui.
 */
@Composable
fun DocumentOpenerScreen(
    docId: String,
    onOpenPdf: (String) -> Unit,
    onBack: () -> Unit,
    vm: DocumentDetailViewModel = koinViewModel(parameters = { parametersOf(docId) }),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val document = uiState.document

    LaunchedEffect(document?.id, document?.file) {
        val doc = document ?: return@LaunchedEffect
        if (doc.file.isNotEmpty()) {
            onOpenPdf(FilesUrl.build(collection = "documents", recordId = doc.id, filename = doc.file))
        }
    }

    MensaScaffold {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                uiState.loading -> LoadingDots()
                document == null || document.file.isEmpty() -> Text(
                    tr("addons.documents.file_unavailable", fallback = "Documento non disponibile"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LoadingDots()
            }
        }
    }
}
