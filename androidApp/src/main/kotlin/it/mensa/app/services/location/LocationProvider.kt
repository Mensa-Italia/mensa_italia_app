package it.mensa.app.services.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * LocationProvider — wrapper over FusedLocationProviderClient.
 *
 * Exposes:
 * - [hasPermission] — live state of location permission
 * - [lastKnownLocation] — cached last fix
 * - [requestOnce] — suspend fun for a single fresh fix
 *
 * Permission prompting must be done from the UI via [PushPermissionRequester]
 * pattern — this class only checks and uses permission, never requests it.
 *
 * TODO:
 *  1. Wire lastKnownLocation to a periodic updates flow
 *  2. Add [requestContinuousUpdates] / [stopContinuousUpdates] for map screens
 *  3. Integrate with MapScreen to center on user's position
 */
class LocationProvider(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _lastKnownLocation = MutableStateFlow<Location?>(null)
    val lastKnownLocation: StateFlow<Location?> = _lastKnownLocation.asStateFlow()

    val hasPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * Request a single fresh location fix.
     * Returns null if permission is not granted or the fix times out.
     *
     * @param priority [Priority.PRIORITY_HIGH_ACCURACY] or [Priority.PRIORITY_BALANCED_POWER_ACCURACY]
     */
    @SuppressLint("MissingPermission")
    suspend fun requestOnce(
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY,
    ): Location? {
        if (!hasPermission) return null
        return suspendCancellableCoroutine { cont ->
            val request = CurrentLocationRequest.Builder()
                .setPriority(priority)
                .setDurationMillis(10_000L)
                .build()
            val task = fusedClient.getCurrentLocation(request, null)
            task.addOnSuccessListener { location ->
                _lastKnownLocation.value = location
                cont.resume(location)
            }
            task.addOnFailureListener {
                cont.resume(null)
            }
            cont.invokeOnCancellation {
                task.addOnCanceledListener { /* no-op */ }
            }
        }
    }
}
