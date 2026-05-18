package it.mensa.app.features.tableport.util

import android.content.Context
import coil3.ImageLoader
import coil3.request.ImageRequest
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

    fun warmAll(stamps: List<StampUserModel>, context: Context, imageLoader: ImageLoader) {
        stamps.forEach { su ->
            val record = su.stampRecord ?: return@forEach
            if (record.image.isEmpty()) return@forEach
            val url = FilesUrl.build(
                collection = "stamp",
                recordId = record.id,
                filename = record.image,
                thumb = "600x400",
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
