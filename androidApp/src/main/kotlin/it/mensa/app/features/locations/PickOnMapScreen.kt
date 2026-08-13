package it.mensa.app.features.locations

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import it.mensa.app.support.tr

/**
 * PickOnMapScreen — pagina 2 del picker: si sceglie la coordinata.
 *
 * Il pin sta fermo al centro del rettangolo della mappa e si muove la mappa
 * sotto: cosi' la coordinata salvata e' esattamente il centro inquadrato, senza
 * la deriva di un marker trascinato a dito. "Avanti" resta spento finche' la
 * mappa non e' stata mossa davvero, altrimenti si salverebbe il centro
 * dell'Italia scambiandolo per una scelta.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PickOnMapScreen(
    vm: NewLocationViewModel,
    onNext: () -> Unit,
    onCancel: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cameraMove by vm.cameraMove.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hasMapsKey = remember { context.hasMapsApiKey() }
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var permissionRequested by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }

    // Uscire dal picker senza aver scritto niente non merita una domanda; dopo
    // che nome o indirizzo sono passati per le mani dell'utente, si'.
    val draftEdited = state.nameEditedByUser || state.addressEditedByUser
    val requestCancel = { if (draftEdited) showCancelConfirm = true else onCancel() }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(vm.initialCamera.target, vm.initialCamera.zoom)
    }

    LaunchedEffect(cameraMove) {
        cameraMove?.let { move ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(move.target, move.zoom),
            )
            vm.onCameraMoveConsumed()
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) return@LaunchedEffect
        vm.onCameraIdle(
            center = cameraPositionState.position.target,
            zoom = cameraPositionState.position.zoom,
            bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds,
            userDriven = cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("app.location.map.title", fallback = "Scegli posizione")) },
                navigationIcon = {
                    IconButton(onClick = requestCancel) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = tr("app.location.cancel", fallback = "Annulla"),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onNext, enabled = state.pinPositioned) {
                        Text(
                            text = tr("app.location.map.next", fallback = "Avanti"),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // La tendina dei risultati non deve mangiarsi la mappa: al massimo
            // il 40% dell'altezza inquadrata.
            val maxResultsHeight = maxHeight * 0.4f

            if (hasMapsKey) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
                )
                CenterPin()
            } else {
                MapUnavailablePanel()
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchBar(
                    state = state,
                    onQueryChange = vm::onSearchQueryChange,
                    onSubmit = vm::onSearchSubmit,
                    onClear = vm::clearSearch,
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                ) {
                    Text(
                        text = tr("app.location.map.hint", fallback = "Muovi la mappa per posizionare il punto"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                if (state.searchResults.isNotEmpty()) {
                    SearchResults(
                        results = state.searchResults,
                        maxHeight = maxResultsHeight,
                        onPicked = vm::onSuggestionPicked,
                    )
                }
                if (state.searchState == SearchState.NoResults) {
                    ResultsPlaceholder(tr("app.location.search.noResults", fallback = "Nessun risultato"))
                }
            }

            MyLocationButton(
                // Dopo un rifiuto il dialog di sistema non ricompare: si spiega
                // a cosa serve il permesso invece di offrire un tap che non fa niente.
                showPermissionHint = permissionRequested && !locationPermission.status.isGranted,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                onClick = {
                    when {
                        locationPermission.status.isGranted -> vm.onMyLocationClick()
                        !permissionRequested -> {
                            permissionRequested = true
                            locationPermission.launchPermissionRequest()
                        }
                    }
                },
            )
        }
    }

    LaunchedEffect(locationPermission.status.isGranted) {
        // Solo dopo un tap esplicito: all'apertura la mappa si centra da sola
        // via LocationProvider, senza spostare il pin.
        if (permissionRequested && locationPermission.status.isGranted) vm.onMyLocationClick()
    }

    BackHandler(enabled = draftEdited) { showCancelConfirm = true }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text(tr("app.location.cancel.confirm.title", fallback = "Annullare la nuova posizione?")) },
            text = { Text(tr("app.location.cancel.confirm.body", fallback = "I dati inseriti andranno persi.")) },
            confirmButton = {
                TextButton(
                    onClick = { showCancelConfirm = false; onCancel() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(tr("app.location.cancel.confirm.confirm", fallback = "Annulla posizione")) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text(tr("app.location.cancel.confirm.dismiss", fallback = "Continua"))
                }
            },
        )
    }
}

/**
 * Pin fisso, punta in basso, non interattivo. L'offset alza l'icona di mezza
 * altezza cosi' la punta cade esattamente sul centro geometrico della mappa.
 */
@Composable
private fun BoxScope.CenterPin() {
    Icon(
        Icons.Default.Place,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = (-PIN_SIZE_DP / 2).dp)
            .size(PIN_SIZE_DP.dp),
    )
}

@Composable
private fun BoxScope.MapUnavailablePanel() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tr(
                "app.location.map.unavailable",
                fallback = "Mappa non disponibile. Puoi comunque cercare un indirizzo.",
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    state: NewLocationState,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val unavailable = state.searchState == SearchState.Unavailable
    Surface(shape = RoundedCornerShape(28.dp), tonalElevation = 3.dp) {
        TextField(
            value = state.searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !unavailable,
            singleLine = true,
            placeholder = {
                Text(
                    if (unavailable) {
                        tr("app.location.search.unavailable", fallback = "Ricerca non disponibile")
                    } else {
                        tr("app.location.search.placeholder", fallback = "Cerca un indirizzo…")
                    },
                )
            },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                when {
                    state.searchState == SearchState.Loading ->
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    state.searchQuery.isNotEmpty() -> IconButton(onClick = onClear) {
                        Icon(
                            Icons.Outlined.Clear,
                            contentDescription = tr("app.location.search.clear", fallback = "Cancella"),
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); onSubmit() }),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun SearchResults(
    results: List<LocationSuggestion>,
    maxHeight: Dp,
    onPicked: (LocationSuggestion) -> Unit,
) {
    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 3.dp) {
        LazyColumn(modifier = Modifier.heightIn(max = maxHeight)) {
            items(results, key = { it.id }) { suggestion ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPicked(suggestion) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(suggestion.title, style = MaterialTheme.typography.bodyMedium)
                    if (suggestion.subtitle.isNotBlank()) {
                        Text(
                            text = suggestion.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ResultsPlaceholder(message: String) {
    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 3.dp) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun MyLocationButton(
    showPermissionHint: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showPermissionHint) {
            val context = LocalContext.current
            Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 3.dp) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = tr(
                            "app.location.map.permission.body",
                            fallback = "Consenti l'accesso alla posizione per centrare la mappa dove ti trovi",
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    // Dopo il primo rifiuto Android non ripropone il dialog:
                    // l'unico posto dove il permesso si concede sono le impostazioni.
                    TextButton(onClick = { context.startActivity(appSettingsIntent(context)) }) {
                        Text(tr("app.location.map.permission.action", fallback = "Consenti"))
                    }
                }
            }
        }
        FloatingActionButton(onClick = onClick) {
            Icon(
                Icons.Outlined.MyLocation,
                contentDescription = tr("app.location.map.myLocation", fallback = "La mia posizione"),
            )
        }
    }
}

private fun appSettingsIntent(context: Context) = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package", context.packageName, null),
)

private const val PIN_SIZE_DP = 40
