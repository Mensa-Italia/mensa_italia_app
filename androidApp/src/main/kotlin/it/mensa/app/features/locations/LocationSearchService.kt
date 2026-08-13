package it.mensa.app.features.locations

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/** Una riga della tendina dei risultati. */
data class LocationSuggestion(
    val id: String,
    val title: String,
    val subtitle: String,
)

/** Il risultato risolto: quello che finisce nel draft quando l'utente sceglie. */
data class ResolvedSuggestion(
    val lat: Double,
    val lon: Double,
    val address: String,
    val name: String,
)

/**
 * LocationSearchService — ricerca di indirizzi e POI per il picker posizioni.
 *
 * Due provider, in quest'ordine:
 *  1. Places SDK. L'autocomplete con session token e' gratuito e illimitato,
 *     e si paga solo il fetch del posto scelto: per questo il token va rinnovato
 *     dopo ogni selezione, altrimenti le battute successive finiscono fuori
 *     sessione e diventano a consumo.
 *  2. [Geocoder] di sistema, quando la chiave Maps manca o Places fallisce.
 *     Da' meno risultati ma non costa nulla e non richiede chiavi.
 *
 * Se non c'e' nessuno dei due la ricerca si disabilita: la mappa resta usabile,
 * il punto si posiziona a mano.
 */
class LocationSearchService(private val context: Context) {

    private val placesClient: PlacesClient? by lazy {
        // Inizializzare Places senza chiave lascia un client che fallisce a ogni
        // richiesta: meglio non averlo affatto e andare diretti sul Geocoder.
        val key = context.mapsApiKey() ?: return@lazy null
        if (!Places.isInitialized()) Places.initializeWithNewPlacesApiEnabled(context, key)
        runCatching { Places.createClient(context) }.getOrNull()
    }

    private val geocoder: Geocoder? by lazy {
        if (Geocoder.isPresent()) Geocoder(context) else null
    }

    private var sessionToken: AutocompleteSessionToken? = null

    // I risultati del Geocoder non hanno un id stabile da rifetchare: si tiene
    // l'ultima infornata in memoria per poterla risolvere alla selezione.
    private var fallbackResults: Map<String, ResolvedSuggestion> = emptyMap()

    val isAvailable: Boolean get() = placesClient != null || geocoder != null

    /**
     * Suggerimenti per [query], polarizzati su [viewport] (la porzione di mappa
     * che l'utente sta guardando).
     */
    suspend fun autocomplete(query: String, viewport: LatLngBounds?): List<LocationSuggestion> {
        placesClient?.let { client ->
            val token = sessionToken ?: AutocompleteSessionToken.newInstance().also { sessionToken = it }
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(token)
                .apply { viewport?.let { setLocationBias(RectangularBounds.newInstance(it)) } }
                .build()
            val response = client.findAutocompletePredictions(request).awaitOrNull()
            if (response != null) {
                fallbackResults = emptyMap()
                return response.autocompletePredictions.take(MAX_RESULTS).map { prediction ->
                    LocationSuggestion(
                        id = prediction.placeId,
                        title = prediction.getPrimaryText(null).toString(),
                        subtitle = prediction.getSecondaryText(null).toString(),
                    )
                }
            }
        }
        return geocoderSuggestions(query, viewport)
    }

    /**
     * Coordinate e indirizzo del suggerimento scelto, o null se il provider non
     * risponde. Chiude la sessione di autocomplete: dalla prossima battuta
     * ricomincia una sessione nuova.
     */
    suspend fun resolve(suggestion: LocationSuggestion): ResolvedSuggestion? {
        fallbackResults[suggestion.id]?.let { return it }
        val client = placesClient ?: return null
        val request = FetchPlaceRequest.builder(suggestion.id, PLACE_FIELDS)
            .setSessionToken(sessionToken)
            .build()
        val place = client.fetchPlace(request).awaitOrNull()?.place
        sessionToken = null
        val location = place?.location ?: return null
        return ResolvedSuggestion(
            lat = location.latitude,
            lon = location.longitude,
            address = place.formattedAddress ?: suggestion.subtitle,
            name = place.displayName ?: suggestion.title,
        )
    }

    private suspend fun geocoderSuggestions(
        query: String,
        viewport: LatLngBounds?,
    ): List<LocationSuggestion> {
        val geocoder = geocoder ?: return emptyList()
        val addresses = withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                if (viewport != null) {
                    geocoder.getFromLocationName(
                        query,
                        MAX_RESULTS,
                        viewport.southwest.latitude,
                        viewport.southwest.longitude,
                        viewport.northeast.latitude,
                        viewport.northeast.longitude,
                    )
                } else {
                    geocoder.getFromLocationName(query, MAX_RESULTS)
                }
            }.getOrNull()
        }.orEmpty()

        val resolved = addresses.associate { address ->
            val id = "${GEOCODER_ID_PREFIX}${address.latitude},${address.longitude}"
            id to ResolvedSuggestion(
                lat = address.latitude,
                lon = address.longitude,
                address = address.formattedAddress(),
                name = address.placeTitle(),
            )
        }
        fallbackResults = resolved
        return addresses.map { address ->
            LocationSuggestion(
                id = "${GEOCODER_ID_PREFIX}${address.latitude},${address.longitude}",
                title = address.placeTitle(),
                subtitle = address.placeSubtitle(),
            )
        }
    }

    private companion object {
        const val MAX_RESULTS = 6
        const val GEOCODER_ID_PREFIX = "geocoder:"
        val PLACE_FIELDS = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
        )
    }
}

/**
 * Un [Task] di Play Services come suspend. Il fallimento non e' eccezionale qui:
 * chiave scaduta, quota finita o rete assente sono tutti casi in cui il picker
 * ripiega sul Geocoder, quindi si ritorna null invece di lanciare.
 */
private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { cont ->
    val resumed = AtomicBoolean(false)
    addOnSuccessListener { if (resumed.compareAndSet(false, true)) cont.resume(it) }
    addOnFailureListener { if (resumed.compareAndSet(false, true)) cont.resume(null) }
    addOnCanceledListener { if (resumed.compareAndSet(false, true)) cont.resume(null) }
}
