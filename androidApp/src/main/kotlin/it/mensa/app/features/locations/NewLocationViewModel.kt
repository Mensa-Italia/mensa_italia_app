package it.mensa.app.features.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import it.mensa.app.services.location.LocationProvider
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.LocationModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sqrt

// ─── Stato ───────────────────────────────────────────────────────────────────

enum class GeocodeState { Idle, Loading, Done, Failed }

enum class SearchState { Idle, Loading, Results, NoResults, Unavailable }

enum class SaveState { Idle, Saving, Failed }

data class NewLocationState(
    val center: LatLng = ITALY_CENTER,
    val pinPositioned: Boolean = false,
    val address: String = "",
    val addressEditedByUser: Boolean = false,
    val lastGeocodedCenter: LatLng? = null,
    val geocodeState: GeocodeState = GeocodeState.Idle,
    val nameSuggestion: String = "",
    val name: String = "",
    val nameEditedByUser: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<LocationSuggestion> = emptyList(),
    val searchState: SearchState = SearchState.Idle,
    val saveState: SaveState = SaveState.Idle,
    val ignoreNextQueryChange: Boolean = false,
    val savedLocation: LocationModel? = null,
)

/** Spostamento di camera chiesto dal ViewModel (ricerca, "la mia posizione"). */
data class CameraMove(val target: LatLng, val zoom: Float)

val ITALY_CENTER = LatLng(41.9028, 12.4964)

// ─── ViewModel ───────────────────────────────────────────────────────────────

/**
 * NewLocationViewModel — il draft della posizione in creazione.
 *
 * Vive sul NavBackStackEntry del graph, non su quello delle singole pagine:
 * fra mappa e dettagli si va avanti e indietro, e il draft deve sopravvivere al
 * push/pop (l'indirizzo corretto a mano non si riscrive due volte).
 */
class NewLocationViewModel(
    private val search: LocationSearchService,
    private val geocoder: ReverseGeocoderService,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(
        NewLocationState(
            searchState = if (search.isAvailable) SearchState.Idle else SearchState.Unavailable,
        ),
    )
    val state: StateFlow<NewLocationState> = _state.asStateFlow()

    private val _cameraMove = MutableStateFlow<CameraMove?>(null)
    val cameraMove: StateFlow<CameraMove?> = _cameraMove.asStateFlow()

    private val locations get() = koinAccess().locations
    private val auth get() = koinAccess().auth

    /** Porzione di mappa inquadrata: polarizza la ricerca sui dintorni. */
    private var viewport: LatLngBounds? = null

    private var searchJob: Job? = null
    private var geocodeJob: Job? = null

    /**
     * Camera con cui comporre la mappa. Segue l'ultima inquadratura ferma, cosi'
     * tornando indietro dai dettagli si ritrova il punto dove si era rimasti e
     * non l'Italia intera.
     */
    var initialCamera: CameraMove = CameraMove(ITALY_CENTER, ITALY_ZOOM)
        private set

    init {
        // Senza permesso si apre sull'Italia: il fix non arriva e chiedere il
        // permesso da qui aprirebbe un dialog che l'utente non ha sollecitato.
        if (locationProvider.hasPermission) {
            locationProvider.lastKnownLocation.value?.let {
                initialCamera = CameraMove(LatLng(it.latitude, it.longitude), NEARBY_ZOOM)
            }
            viewModelScope.launch { centerOnLastKnownLocation(markPositioned = false) }
        }
    }

    /**
     * Apre il flusso sulla posizione gia' selezionata dal chiamante: il pin
     * parte posizionato, cosi' "Avanti" e' subito attivo e la mappa inquadra il
     * punto invece dell'Italia intera.
     */
    fun startFrom(locationId: String) {
        if (_state.value.pinPositioned) return
        viewModelScope.launch {
            val location = locations.observeAll().first().firstOrNull { it.id == locationId } ?: return@launch
            if (_state.value.pinPositioned) return@launch
            val center = LatLng(location.lat, location.lon)
            initialCamera = CameraMove(center, EXISTING_ZOOM)
            _state.update {
                it.copy(
                    center = center,
                    pinPositioned = true,
                    address = location.address,
                    lastGeocodedCenter = center,
                    geocodeState = if (location.address.isBlank()) GeocodeState.Idle else GeocodeState.Done,
                    nameSuggestion = location.name,
                )
            }
            // La risoluzione e' asincrona: se la mappa e' gia' composta,
            // initialCamera e' stato letto e serve uno spostamento esplicito.
            _cameraMove.value = initialCamera
        }
    }

    // ─── Mappa ────────────────────────────────────────────────────────────────

    /**
     * Camera ferma: la coordinata salvata e' il centro geometrico della mappa,
     * quindi si aggiorna a ogni fermata. [userDriven] distingue la panoramica
     * dell'utente dall'animazione che abbiamo chiesto noi.
     */
    fun onCameraIdle(center: LatLng, zoom: Float, bounds: LatLngBounds?, userDriven: Boolean) {
        viewport = bounds
        initialCamera = CameraMove(center, zoom)
        val positioned = _state.value.pinPositioned || userDriven
        _state.update { it.copy(center = center, pinPositioned = positioned) }
        if (positioned) scheduleReverseGeocode(center)
    }

    fun onCameraMoveConsumed() {
        _cameraMove.value = null
    }

    /** Tap su "la mia posizione": il permesso lo chiede la schermata. */
    fun onMyLocationClick() {
        viewModelScope.launch { centerOnLastKnownLocation(markPositioned = true) }
    }

    private suspend fun centerOnLastKnownLocation(markPositioned: Boolean) {
        val fix = locationProvider.requestOnce() ?: return
        val target = LatLng(fix.latitude, fix.longitude)
        // Se l'utente ha gia' inquadrato un punto suo, un fix che arriva tardi
        // non deve strappargli via la mappa da sotto le dita.
        if (!markPositioned && _state.value.pinPositioned) return
        if (markPositioned) _state.update { it.copy(pinPositioned = true) }
        _cameraMove.value = CameraMove(target, NEARBY_ZOOM)
    }

    // ─── Ricerca ──────────────────────────────────────────────────────────────

    fun onSearchQueryChange(query: String) {
        if (_state.value.ignoreNextQueryChange && query == _state.value.searchQuery) {
            // Rimbalzo del campo dopo che la selezione di un risultato ne ha
            // riscritto il testo: non e' una battuta, la tendina resta chiusa.
            _state.update { it.copy(ignoreNextQueryChange = false) }
            return
        }
        _state.update { it.copy(searchQuery = query, ignoreNextQueryChange = false) }
        if (_state.value.searchState == SearchState.Unavailable) return
        searchJob?.cancel()
        if (query.trim().length < MIN_QUERY_LENGTH) {
            _state.update { it.copy(searchResults = emptyList(), searchState = SearchState.Idle) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch(query.trim())
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                searchState = SearchState.Idle,
                ignoreNextQueryChange = false,
            )
        }
    }

    /** Tasto "Cerca" della tastiera: applica direttamente il primo risultato. */
    fun onSearchSubmit() {
        val query = _state.value.searchQuery.trim()
        if (query.length < MIN_QUERY_LENGTH || _state.value.searchState == SearchState.Unavailable) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            runSearch(query)
            _state.value.searchResults.firstOrNull()?.let { onSuggestionPicked(it) }
        }
    }

    private suspend fun runSearch(query: String) {
        _state.update { it.copy(searchState = SearchState.Loading) }
        // Provider muto: si degrada a "nessun risultato" invece di lasciare lo
        // spinner acceso. La ricerca resta facoltativa, il flusso si chiude lo stesso.
        val results = withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
            search.autocomplete(query, viewport)
        }.orEmpty()
        _state.update {
            it.copy(
                searchResults = results,
                searchState = if (results.isEmpty()) SearchState.NoResults else SearchState.Results,
            )
        }
    }

    fun onSuggestionPicked(suggestion: LocationSuggestion) {
        viewModelScope.launch {
            val resolved = withTimeoutOrNull(SEARCH_TIMEOUT_MS) { search.resolve(suggestion) }
            if (resolved == null) {
                _state.update { it.copy(searchState = SearchState.NoResults) }
                return@launch
            }
            val center = LatLng(resolved.lat, resolved.lon)
            geocodeJob?.cancel()
            _state.update {
                it.copy(
                    center = center,
                    pinPositioned = true,
                    address = resolved.address,
                    // Il risultato porta con se' l'indirizzo giusto: quello
                    // corretto a mano prima non vale piu'.
                    addressEditedByUser = false,
                    lastGeocodedCenter = center,
                    geocodeState = GeocodeState.Done,
                    nameSuggestion = resolved.name,
                    name = if (it.nameEditedByUser) it.name else resolved.name,
                    searchQuery = suggestion.title,
                    searchResults = emptyList(),
                    searchState = SearchState.Idle,
                    ignoreNextQueryChange = true,
                )
            }
            _cameraMove.value = CameraMove(center, EXISTING_ZOOM)
        }
    }

    // ─── Geocoding inverso ────────────────────────────────────────────────────

    private fun scheduleReverseGeocode(center: LatLng) {
        val last = _state.value.lastGeocodedCenter
        if (last != null && distanceMeters(last, center) <= GEOCODE_MIN_MOVE_M) return
        // Spostarsi davvero dal punto geocodificato invalida la correzione a
        // mano: l'indirizzo scritto per il civico di prima non descrive piu' qui.
        if (_state.value.addressEditedByUser) _state.update { it.copy(addressEditedByUser = false) }

        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            delay(GEOCODE_DEBOUNCE_MS)
            _state.update { it.copy(geocodeState = GeocodeState.Loading) }
            val place = geocoder.reverse(center.latitude, center.longitude)
            if (place == null) {
                _state.update { it.copy(geocodeState = GeocodeState.Failed) }
                return@launch
            }
            _state.update {
                it.copy(
                    address = if (it.addressEditedByUser) it.address else place.address,
                    lastGeocodedCenter = center,
                    geocodeState = GeocodeState.Done,
                    nameSuggestion = place.nameSuggestion,
                    name = if (it.nameEditedByUser) it.name else place.nameSuggestion,
                )
            }
        }
    }

    // ─── Dettagli e salvataggio ───────────────────────────────────────────────

    fun onAddressChange(value: String) =
        _state.update { it.copy(address = value, addressEditedByUser = true) }

    fun onNameChange(value: String) =
        _state.update { it.copy(name = value, nameEditedByUser = true) }

    fun save() {
        val current = _state.value
        if (current.name.isBlank() || current.saveState == SaveState.Saving) return
        // Stato segnato subito e non dentro la coroutine: due tap in rapida
        // successione passerebbero entrambi la guardia e creerebbero due record.
        _state.update { it.copy(saveState = SaveState.Saving) }
        viewModelScope.launch {
            try {
                val created = locations.createAndAddLocal(
                    name = current.name.trim(),
                    address = current.address.trim(),
                    // Le coordinate a 13 decimali sono la firma dei record
                    // legacy senza indirizzo: 7 bastano per il centimetro.
                    lat = round7(current.center.latitude),
                    lon = round7(current.center.longitude),
                    createdBy = auth.currentUser.value?.id ?: "",
                )
                _state.update { it.copy(saveState = SaveState.Idle, savedLocation = created) }
            } catch (_: Exception) {
                _state.update { it.copy(saveState = SaveState.Failed) }
            }
        }
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 3
        const val SEARCH_DEBOUNCE_MS = 300L
        const val SEARCH_TIMEOUT_MS = 5_000L
        const val GEOCODE_DEBOUNCE_MS = 500L
        const val GEOCODE_MIN_MOVE_M = 25.0
        const val ITALY_ZOOM = 5.5f
        const val NEARBY_ZOOM = 15f
        const val EXISTING_ZOOM = 16f
    }
}

private fun round7(value: Double): Double = (value * 1e7).roundToLong() / 1e7

/**
 * Distanza approssimata in metri. Equirettangolare invece di haversine: serve
 * solo a decidere se si e' superata la soglia dei 25 m, e su quelle distanze
 * l'errore e' sotto il centimetro.
 */
private fun distanceMeters(a: LatLng, b: LatLng): Double {
    val meanLatRad = Math.toRadians((a.latitude + b.latitude) / 2)
    val dLat = abs(a.latitude - b.latitude) * METERS_PER_DEGREE
    val dLon = abs(a.longitude - b.longitude) * METERS_PER_DEGREE * cos(meanLatRad)
    return sqrt(dLat * dLat + dLon * dLon)
}

private const val METERS_PER_DEGREE = 111_320.0
