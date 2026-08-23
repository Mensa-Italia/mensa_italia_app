package it.mensa.app.features.tableport._components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.mensa.app.support.AppFormat
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.theme.BottomSheetShape
import it.mensa.shared.model.StampUserModel
import java.util.Date

/**
 * StampDetailSheet — il dettaglio di un singolo timbro.
 *
 * Controparte Android di `PassportStampDetailSheet` in `PassportView.swift`.
 * Su Android non esisteva affatto: `PassportScreen` passava `onTapStamp = {}`,
 * una lambda vuota, quindi il tocco su un francobollo non apriva niente e non
 * dava alcun segnale — sembrava che l'app avesse smesso di rispondere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StampDetailSheet(
    stamp: StampUserModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val record = stamp.stampRecord

    val imageUrl = record
        ?.takeIf { it.image.isNotEmpty() }
        ?.let {
            FilesUrl.build(
                collection = "stamp",
                recordId = it.id,
                filename = it.image,
                thumb = "800x0",
            )
        }

    val title = record?.description_?.takeIf { it.isNotEmpty() } ?: stamp.stampId

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = BottomSheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            PassportDecal(
                imageUrl = imageUrl,
                size = 240.dp,
                // Dritto e con l'annullo, come nel dettaglio iOS: la rotazione
                // serve sulla pagina, dove i timbri devono sembrare apposti a
                // mano, non qui dove il timbro e' il soggetto.
                rotation = 0f,
                showsCancel = true,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            val seconds = stamp.created.epochSeconds
            if (seconds > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    // Locale dell'app. Su iOS le date sono formattate con un
                    // `Locale("it_IT")` scritto a mano, qui e in un'altra
                    // decina di schermate: e' una cosa da sistemare in blocco,
                    // non replicando l'errore su Android.
                    text = AppFormat.format(Date(seconds * 1000L), "d MMMM y"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
