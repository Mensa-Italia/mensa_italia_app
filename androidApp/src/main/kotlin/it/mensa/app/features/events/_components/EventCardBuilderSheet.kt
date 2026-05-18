package it.mensa.app.features.events._components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.mensa.app.ui.components.CachedAsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder

/**
 * EventCardBuilderSheet — Android equivalent of iOS EventCardBuilderSheet.swift.
 * Generates a social event card image via the svc.mensa.it endpoint.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCardBuilderSheet(
    onConfirmed: (ByteArray) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    var isGenerating by remember { mutableStateOf(false) }
    var generatedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var generatedPreviewUrl by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val allFieldsEmpty = listOf(title, date, time, location, address, city).all { it.isBlank() }
    val templateUrl = "https://svc.mensa.it/static/event_card_template.png"
    val endpoint = "https://svc.mensa.it/api/cs/generate-event-card"

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Annulla") }
                Text("Crea copertina", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.padding(horizontal = 32.dp))
            }

            // Preview
            val previewModel = generatedPreviewUrl ?: templateUrl
            CachedAsyncImage(
                model = previewModel,
                contentDescription = "Anteprima copertina",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().aspectRatio(1600f / 900f).clip(RoundedCornerShape(16.dp)),
            )

            if (isGenerating) { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Text("Dettagli", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titolo breve") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Lunedì 1 gennaio") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Ore 21:00") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Ristorante bellissimo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Via Roma 1") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Milano (MI)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Button(
                onClick = {
                    scope.launch {
                        isGenerating = true; error = null
                        try {
                            val params = mapOf(
                                "title" to title.trim(), "line0" to date.trim(), "line1" to time.trim(),
                                "line2" to location.trim(), "line3" to address.trim(), "line4" to city.trim()
                            )
                            val query = params.entries.joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
                            val url = "$endpoint?$query"
                            val bytes = withContext(Dispatchers.IO) { URL(url).readBytes() }
                            generatedBytes = bytes
                            generatedPreviewUrl = url
                        } catch (e: Exception) {
                            error = "Generazione non riuscita. Riprova."
                        } finally { isGenerating = false }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating && !allFieldsEmpty,
            ) { Text("Genera") }

            if (generatedBytes != null) {
                OutlinedButton(
                    onClick = { onConfirmed(generatedBytes!!) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) { Text("Perfetto!") }
            }
        }
    }
}
