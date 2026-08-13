package it.mensa.app.features.locations

import android.content.Context
import android.content.pm.PackageManager

/**
 * MapsKey — lettura a runtime della chiave Google Maps dal manifest.
 *
 * Il placeholder `${MAPS_API_KEY}` puo' risolversi a stringa vuota (build senza
 * secret): con la chiave vuota il SDK non scarica le tile e `GoogleMap` resta un
 * rettangolo grigio senza dire perche'. Chiedere la chiave prima di comporre la
 * mappa permette di mostrare un messaggio onesto e di tenere il flusso
 * completabile — coordinate, ricerca e geocoding non dipendono dalla chiave.
 */
private const val MAPS_API_KEY_META = "com.google.android.geo.API_KEY"

fun Context.mapsApiKey(): String? {
    val metaData = try {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
    return metaData?.getString(MAPS_API_KEY_META)?.takeIf { it.isNotBlank() }
}

fun Context.hasMapsApiKey(): Boolean = mapsApiKey() != null
