package it.mensa.app.features.locations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.shared.model.LocationModel
import java.util.Locale

/**
 * NameLocationScreen — pagina 3 del picker: correggi l'indirizzo, dai il nome, salvi.
 *
 * Sta su una schermata sua e non su un pannello sopra la mappa: qui serve la
 * tastiera, e la tastiera sopra la mappa lascia visibile una striscia di mappa
 * inutile e un form compresso.
 *
 * "Indietro" non e' un annullamento: riporta alla mappa col draft intatto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameLocationScreen(
    vm: NewLocationViewModel,
    onSaved: (LocationModel) -> Unit,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var nameFocused by remember { mutableStateOf(false) }
    var nameTouched by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedLocation) {
        state.savedLocation?.let(onSaved)
    }

    val nameMissing = nameTouched && state.name.isBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("app.location.details.title", fallback = "Dettagli posizione")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = tr("app.location.back", fallback = "Indietro"),
                        )
                    }
                },
                actions = {
                    if (state.saveState == SaveState.Saving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp))
                    } else {
                        TextButton(onClick = vm::save, enabled = state.name.isNotBlank()) {
                            Text(
                                text = tr("app.location.save", fallback = "Salva posizione"),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.address,
                onValueChange = vm::onAddressChange,
                label = { Text(tr("app.location.address", fallback = "Indirizzo")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 3,
                placeholder = if (state.geocodeState == GeocodeState.Failed) {
                    { Text(tr("app.location.address.unavailable", fallback = "Indirizzo non trovato, scrivilo tu")) }
                } else null,
                supportingText = if (state.geocodeState == GeocodeState.Loading) {
                    { Text(tr("app.location.address.loading", fallback = "Ricerca indirizzo in corso…")) }
                } else null,
                trailingIcon = if (state.geocodeState == GeocodeState.Loading) {
                    { CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = vm::onNameChange,
                label = { Text(tr("app.location.name", fallback = "Nome del posto")) },
                placeholder = { Text(tr("app.location.name.hint", fallback = "Come vuoi chiamare questo posto")) },
                modifier = Modifier
                    .fillMaxWidth()
                    // L'errore compare solo quando l'utente esce dal campo
                    // lasciandolo vuoto: all'apertura sarebbe un rimprovero gratuito.
                    .onFocusChanged { focus ->
                        if (!focus.isFocused && nameFocused) nameTouched = true
                        nameFocused = focus.isFocused
                    },
                singleLine = true,
                isError = nameMissing,
                supportingText = if (nameMissing) {
                    { Text(tr("app.location.name.required", fallback = "Dai un nome alla posizione")) }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )

            Text(
                text = tr(
                    "app.location.coordinates",
                    fallback = "Lat {lat} · Lon {lon}",
                    "lat" to formatCoordinate(state.center.latitude),
                    "lon" to formatCoordinate(state.center.longitude),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.saveState == SaveState.Failed) {
                Text(
                    text = tr(
                        "app.location.error.save",
                        fallback = "Non è stato possibile salvare la posizione. Riprova.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// Locale fisso: le coordinate si scrivono col punto decimale in qualsiasi
// lingua, e con la virgola sembrerebbero due numeri separati.
private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.5f", value)
