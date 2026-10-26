package it.mensa.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import it.mensa.app.support.MensaAuthInterceptor
import it.mensa.app.support.koinAccess
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.Path.Companion.toOkioPath

/**
 * CachedAsyncImage — Coil3 wrapper with:
 * - OkHttp network fetcher
 * - Memory cache: 100 MB
 * - Disk cache: 500 MB, named "mensa-images"
 * - Cache-Control rewrite interceptor: injects max-age=86400,public on responses
 *   that lack a Cache-Control header (common with PocketBase file endpoints)
 *
 * @param model image URL or [ImageRequest]
 * @param contentDescription accessibility description
 * @param modifier layout modifier
 * @param contentScale how the image fills the bounds
 */
@Composable
fun CachedAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onState: ((coil3.compose.AsyncImagePainter.State) -> Unit)? = null,
) {
    val context = LocalContext.current
    val imageLoader = remember {
        buildMensaImageLoader(context)
    }

    // Per gli URL PocketBase `/api/files/...` deriviamo una chiave di cache
    // canonica `filename + thumb`, indipendente dal recordId del path. Vedi
    // kdoc di [canonicalCacheKey].
    // Fallita (offline), l'immagine riparte da sola quando il server torna a
    // rispondere: `retry` entra nella richiesta, e una richiesta diversa Coil
    // la ricarica. Vedi `Connectivity.reconnects`.
    var failed by remember(model) { mutableStateOf(false) }
    var retry by remember(model) { mutableIntStateOf(0) }
    if (failed) {
        val connectivity = remember { koinAccess().connectivity }
        LaunchedEffect(connectivity) {
            connectivity.reconnects.drop(1).first()
            failed = false
            retry++
        }
    }

    val request = remember(model, context, retry) {
        when (model) {
            is String -> ImageRequest.Builder(context)
                .data(model)
                .apply { canonicalCacheKey(model)?.let { memoryCacheKey(it); diskCacheKey(it) } }
                .apply { if (retry > 0) memoryCacheKeyExtra("retry", retry.toString()) }
                .build()
            else -> model
        }
    }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        imageLoader = imageLoader,
        contentScale = contentScale,
        modifier = modifier,
        onState = { state ->
            if (state is AsyncImagePainter.State.Error) failed = true
            onState?.invoke(state)
        },
    )
}

/**
 * Chiave canonica per gli URL file PocketBase. Stesso scopo della funzione
 * iOS omonima: `filename + thumb` come identità — il `recordId` nel path è
 * irrilevante perché PB auto-hasha i filename (`<base>_<8char>.<ext>`).
 *
 * Beneficio: due composable che caricano lo stesso file (es. lista soci con
 * `member.id`, OrgChart con `userId`) condividono UNA sola entry su disco/RAM.
 * Restituisce `null` per URL non-PB → Coil ricade sul default URL-as-key.
 */
/**
 * Builder pubblico per chi pilota un `AsyncImage` direttamente (es.
 * `MemberHeroAvatar` con `onState`). Applica la stessa chiave canonica usata
 * da [CachedAsyncImage] così le entry cache sono unificate.
 */
fun mensaImageRequest(context: android.content.Context, url: String): ImageRequest =
    ImageRequest.Builder(context)
        .data(url)
        .apply { canonicalCacheKey(url)?.let { memoryCacheKey(it); diskCacheKey(it) } }
        .build()

internal fun canonicalCacheKey(url: String): String? {
    if (!url.contains("/api/files/")) return null
    val withoutQuery = url.substringBefore('?')
    val filename = withoutQuery.substringAfterLast('/')
    if (filename.isEmpty()) return null
    val thumb = url
        .substringAfter('?', missingDelimiterValue = "")
        .split('&')
        .firstOrNull { it.startsWith("thumb=") }
        ?.removePrefix("thumb=")
        ?: "0"
    return "mensa-img:$filename?thumb=$thumb"
}

// ─── ImageLoader singleton builder ───────────────────────────────────────────

private var cachedImageLoader: ImageLoader? = null

/**
 * Shared accessor for the Mensa-tuned Coil ImageLoader. Other composables that
 * need to drive their own `AsyncImage` (e.g. progressive hero avatars that hook
 * into `onState`) should use this so disk + memory cache + Cache-Control
 * rewriting stay unified across the app.
 */
fun mensaImageLoader(context: android.content.Context): ImageLoader =
    buildMensaImageLoader(context)

private fun buildMensaImageLoader(context: android.content.Context): ImageLoader {
    cachedImageLoader?.let { return it }

    val cacheDir = context.cacheDir.resolve("mensa-images")

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(MensaAuthInterceptor())
        .addNetworkInterceptor(CacheControlInterceptor())
        .build()

    val loader = ImageLoader.Builder(context)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
        }
        .memoryCache {
            MemoryCache.Builder()
                // 100 MB: at 50 the passport pages just turned were
                // already gone when turned back to. Same as iOS.
                .maxSizeBytes(100 * 1024 * 1024)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.toOkioPath())
                .maxSizeBytes(500 * 1024 * 1024) // 500 MB
                .build()
        }
        .networkCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        // Soft 250ms cross-fade on first paint per immagini che arrivano da
        // rete/disk. Coil salta il fade quando l'immagine è già nella memory
        // cache (placeholderMemoryCacheKey path), quindi sulle celle riciclate
        // in scroll non si vede nessuna animazione — comportamento parità iOS.
        .crossfade(250)
        .build()

    cachedImageLoader = loader
    return loader
}

/**
 * OkHttp interceptor that rewrites responses without Cache-Control to use
 * `max-age=86400, public` (24h), making Coil's disk cache effective for
 * PocketBase file URLs which don't emit caching headers by default.
 */
private class CacheControlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val hasCacheControl = response.header("Cache-Control") != null
        return if (hasCacheControl) {
            response
        } else {
            response.newBuilder()
                .header("Cache-Control", "max-age=86400, public")
                .build()
        }
    }
}
