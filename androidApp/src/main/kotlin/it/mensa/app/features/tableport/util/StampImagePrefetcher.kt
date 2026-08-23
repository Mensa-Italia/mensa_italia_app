package it.mensa.app.features.tableport.util

import android.content.Context
import coil3.ImageLoader
import coil3.request.ImageRequest
import it.mensa.app.features.tableport._components.StampThumb
import it.mensa.app.support.FilesUrl
import it.mensa.shared.model.StampUserModel

/**
 * StampImagePrefetcher — pre-fetches stamp images into the Coil disk cache.
 * Mirrors iOS StampImagePrefetcher.swift.
 *
 * Call [warmAll] after stamps are loaded to ensure images are cached before
 * the user scrolls through passport pages.
 */
object StampImagePrefetcher {

    /**
     * Scalda solo la pagina attorno a [pageIndex], non tutta la collezione.
     *
     * `warmAll` accodava ogni timbro in una volta sola. Coil serve le
     * richieste da un pool limitato, quindi le immagini della pagina che
     * l'utente sta *guardando* finivano in coda dietro decine di prefetch di
     * pagine che non aveva ancora aperto: il risultato e' proprio il
     * caricamento in ritardo di quello che si vede.
     *
     * Una pagina avanti e una indietro bastano: il pager si sfoglia una
     * pagina per volta.
     */
    fun warmAround(
        stamps: List<StampUserModel>,
        pageIndex: Int,
        perPage: Int,
        context: Context,
        imageLoader: ImageLoader,
    ) {
        if (stamps.isEmpty() || perPage <= 0) return
        val from = ((pageIndex - 1) * perPage).coerceAtLeast(0)
        val to = ((pageIndex + 2) * perPage).coerceAtMost(stamps.size)
        if (from >= to) return
        warm(stamps.subList(from, to), context, imageLoader)
    }

    private fun warm(stamps: List<StampUserModel>, context: Context, imageLoader: ImageLoader) {
        stamps.forEach { su ->
            val record = su.stampRecord ?: return@forEach
            if (record.image.isEmpty()) return@forEach
            val url = FilesUrl.build(
                collection = "stamp",
                recordId = record.id,
                filename = record.image,
                thumb = StampThumb.GRID,
            )
            val request = ImageRequest.Builder(context)
                .data(url)
                .memoryCacheKey(url)
                .diskCacheKey(url)
                .build()
            imageLoader.enqueue(request)
        }
    }
}
