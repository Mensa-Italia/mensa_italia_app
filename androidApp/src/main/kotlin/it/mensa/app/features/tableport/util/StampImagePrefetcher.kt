package it.mensa.app.features.tableport.util

import android.content.Context
import coil3.ImageLoader
import coil3.request.CachePolicy
import it.mensa.app.features.tableport._components.StampThumb
import it.mensa.app.support.FilesUrl
import it.mensa.app.ui.components.canonicalCacheKey
import it.mensa.app.ui.components.mensaImageRequest
import it.mensa.shared.model.StampUserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

/**
 * StampImagePrefetcher — loads the stamps of the pages around the open one
 * into the same Coil caches, under the same keys, the stamps on screen use.
 * Mirrors iOS StampImagePrefetcher.swift.
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
        // Pages warmed past the open one: a continuous scroll covers several
        // in one fling.
        ahead: Int = 1,
    ) {
        if (stamps.isEmpty() || perPage <= 0) return
        val from = ((pageIndex - 1) * perPage).coerceAtLeast(0)
        val to = ((pageIndex + 1 + ahead) * perPage).coerceAtMost(stamps.size)
        if (from >= to) return
        warm(stamps.subList(from, to), context, imageLoader)
    }

    private val diskScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Tre download alla volta: le immagini a schermo non restano in coda dietro. */
    private val diskSlots = Semaphore(3)
    private val queued: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * Tutta la collezione su disco, in sottofondo e tre alla volta: sfogliando,
     * il timbro nuovo arriva dal disco invece che dalla rete. Parte dalla Home,
     * prima che il passaporto si apra, e di nuovo all'apertura per i timbri
     * appena arrivati. Quello che e' gia' su disco non si riscarica, e niente
     * finisce nella memory cache: una collezione intera non deve spingerne
     * fuori le immagini a schermo. Mirrors iOS `warmCollection`.
     */
    fun warmCollection(stamps: List<StampUserModel>, context: Context, imageLoader: ImageLoader) {
        val appContext = context.applicationContext
        stamps.mapNotNull(::gridUrl).forEach { url ->
            val key = canonicalCacheKey(url) ?: url
            if (!queued.add(key)) return@forEach
            diskScope.launch {
                try {
                    diskSlots.withPermit {
                        val onDisk = imageLoader.diskCache?.openSnapshot(key)?.use { true } ?: false
                        if (!onDisk) {
                            imageLoader.execute(
                                mensaImageRequest(appContext, url).newBuilder()
                                    .memoryCachePolicy(CachePolicy.DISABLED)
                                    .build(),
                            )
                        }
                    }
                } finally {
                    queued.remove(key)
                }
            }
        }
    }

    private fun gridUrl(su: StampUserModel): String? {
        val record = su.stampRecord ?: return null
        if (record.image.isEmpty()) return null
        return FilesUrl.build(
            collection = "stamp",
            recordId = record.id,
            filename = record.image,
            thumb = StampThumb.GRID,
        )
    }

    private fun warm(stamps: List<StampUserModel>, context: Context, imageLoader: ImageLoader) {
        // The stamps on screen key their cache entries canonically
        // (`filename + thumb`, see `canonicalCacheKey`). Keyed by the URL
        // instead, everything warmed here sat in entries no stamp read:
        // each page still loaded its stamps as it came into view.
        stamps.mapNotNull(::gridUrl).forEach { url ->
            imageLoader.enqueue(mensaImageRequest(context, url))
        }
    }
}
