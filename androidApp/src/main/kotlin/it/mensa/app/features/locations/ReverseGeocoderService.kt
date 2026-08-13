package it.mensa.app.features.locations

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * GeocodedPlace — indirizzo formattato + nome proposto per una coordinata.
 */
data class GeocodedPlace(
    val address: String,
    val nameSuggestion: String,
)

/**
 * ReverseGeocoderService — coordinata → indirizzo, con il geocoder di sistema.
 *
 * On-device: non passa dalla chiave Google Maps, quindi l'indirizzo si compila
 * anche in una build senza chiave (dove la mappa resta grigia). Su device senza
 * servizi di geocoding [Geocoder.isPresent] e' false e il servizio ritorna
 * sempre null: la pagina "Dettagli posizione" chiede l'indirizzo a mano.
 */
class ReverseGeocoderService(private val context: Context) {

    private val geocoder: Geocoder? by lazy {
        if (Geocoder.isPresent()) Geocoder(context) else null
    }

    val isAvailable: Boolean get() = geocoder != null

    /**
     * Ritorna l'indirizzo della coordinata, o null se il geocoder manca,
     * fallisce o non conosce il punto (mare aperto, montagna).
     */
    suspend fun reverse(lat: Double, lon: Double): GeocodedPlace? {
        val geocoder = geocoder ?: return null
        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.awaitFromLocation(lat, lon)
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(lat, lon, 1) }.getOrNull()
            }
        }
        val address = addresses?.firstOrNull() ?: return null
        return GeocodedPlace(
            address = address.formattedAddress(),
            nameSuggestion = address.placeTitle(),
        )
    }
}

/**
 * Variante con listener (API 33+). Il callback puo' arrivare due volte in caso
 * di errore tardivo del provider, quindi la ripresa e' protetta: riprendere una
 * continuation gia' risolta e' una IllegalStateException.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private suspend fun Geocoder.awaitFromLocation(lat: Double, lon: Double): List<Address>? =
    suspendCancellableCoroutine { cont ->
        val resumed = AtomicBoolean(false)
        getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                if (resumed.compareAndSet(false, true)) cont.resume(addresses)
            }

            override fun onError(errorMessage: String?) {
                if (resumed.compareAndSet(false, true)) cont.resume(null)
            }
        })
    }

/**
 * Indirizzo nel formato dei record gia' in produzione — quello di Google,
 * `Via Donato Creti, 32, 40128 Bologna BO, Italia`. Quando il provider e'
 * Google `getAddressLine(0)` e' gia' esattamente quella stringa; la
 * ricomposizione manuale copre i provider alternativi.
 */
internal fun Address.formattedAddress(): String {
    getAddressLine(0)?.takeIf { it.isNotBlank() }?.let { return it }
    val street = listOfNotNull(thoroughfare, subThoroughfare).joinToString(", ")
    val city = listOfNotNull(postalCode, locality, adminArea).joinToString(" ")
    return listOf(street, city, countryName ?: "")
        .filter { it.isNotBlank() }
        .joinToString(", ")
}

/**
 * Nome proposto: il POI se il geocoder ne conosce uno, altrimenti la via col
 * civico. `featureName` vale il solo numero civico quando non c'e' un POI, e
 * "32" da solo non e' un nome utile.
 */
internal fun Address.placeTitle(): String {
    val street = listOfNotNull(thoroughfare, subThoroughfare).joinToString(", ")
    val feature = featureName?.takeIf { it.isNotBlank() && it != subThoroughfare && it != thoroughfare }
    return feature ?: street.ifBlank { locality ?: adminArea ?: "" }
}

/**
 * Riga di dettaglio sotto il titolo nei risultati di ricerca: citta' e nazione,
 * senza ripetere la via che sta gia' nel titolo.
 */
internal fun Address.placeSubtitle(): String =
    listOfNotNull(postalCode, locality, adminArea, countryName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
