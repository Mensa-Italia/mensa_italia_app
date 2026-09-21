package it.mensa.app.features.documents

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.shared.api.isMensaHost
import it.mensa.shared.auth.AuthHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/** Le proporzioni di una pagina, in punti PDF: bastano a dare l'altezza alla lista. */
data class PdfPageSize(val width: Int, val height: Int) {
    // Guardia su entrambi i lati: con width a zero il rapporto sarebbe 0f, e
    // `pageWidth / 0f` da' Dp.Infinity, che arrotondata in pixel diventa
    // Int.MAX_VALUE e fa saltare la creazione dei Constraints — la schermata
    // crasherebbe invece di mostrare l'errore.
    val aspectRatio: Float get() =
        if (width <= 0 || height <= 0) 1f else width.toFloat() / height.toFloat()
}

sealed class PdfViewerState {
    object Loading : PdfViewerState()
    data class Ready(val pages: List<PdfPageSize>) : PdfViewerState()
    data class Error(val message: String) : PdfViewerState()
}

/**
 * Apre un PDF e ne disegna le pagine su richiesta.
 *
 * Prima qui si rasterizzavano *tutte* le pagine a 2x in `ARGB_8888` e si
 * tenevano in una `List<Bitmap>` viva quanto lo schermo. Una pagina A4 a 2x
 * sono 1190x1684 px, cioe' circa 8 MB: un verbale di consiglio da trenta
 * pagine ne chiedeva oltre duecento, piu' di quanto l'heap di parecchi
 * telefoni conceda, e finiva in `OutOfMemoryError` prima ancora di mostrare
 * la prima riga.
 *
 * Adesso il renderer resta aperto e disegna la singola pagina quando la lista
 * la chiede, alla larghezza che serve davvero a schermo; una cache LRU a
 * budget di byte tiene le ultime. [PdfRenderer] non e' thread-safe e apre una
 * pagina per volta, quindi ogni disegno passa da [renderMutex].
 */
class PdfViewerViewModel(private val url: String) : ViewModel() {

    private val _state = MutableStateFlow<PdfViewerState>(PdfViewerState.Loading)
    val state: StateFlow<PdfViewerState> = _state.asStateFlow()

    // Scritti su Dispatchers.IO e letti dal thread della UI: servono volatili.
    @Volatile
    private var renderer: PdfRenderer? = null

    @Volatile
    private var descriptor: ParcelFileDescriptor? = null

    @Volatile
    private var loading = false

    private val renderMutex = Mutex()

    /**
     * Un ottavo dell'heap concesso al processo, tra 16 e 96 MB.
     *
     * E' la stessa logica delle cache immagini: abbastanza per tenere le
     * pagine attorno a quella che si sta leggendo, poco abbastanza da non
     * competere con il resto dell'app.
     */
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).coerceIn(16L * 1024 * 1024, 96L * 1024 * 1024).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun load(context: Context) {
        if (renderer != null || loading) return
        loading = true
        viewModelScope.launch {
            _state.update { PdfViewerState.Loading }
            try {
                val sizes = withContext(Dispatchers.IO) {
                    val file = downloadAndCache(context, url)
                    openRenderer(file)
                }
                _state.update { PdfViewerState.Ready(sizes) }
            } catch (e: Exception) {
                _state.update { PdfViewerState.Error(e.message ?: "Errore durante il caricamento del PDF") }
            } finally {
                loading = false
            }
        }
    }

    /**
     * La pagina [index] disegnata larga [widthPx], o `null` se il documento e'
     * stato chiuso nel frattempo o la memoria non basta.
     *
     * Un `OutOfMemoryError` qui non deve chiudere l'app: si svuota la cache e
     * si rinuncia a quella pagina, che resta un riquadro vuoto invece di un
     * crash in mezzo alla lettura.
     */
    suspend fun page(index: Int, widthPx: Int): Bitmap? {
        if (widthPx <= 0) return null
        val key = "$index@$widthPx"
        cache.get(key)?.let { return it }

        return withContext(Dispatchers.IO) {
            renderMutex.withLock {
                cache.get(key)?.let { return@withLock it }
                val active = renderer ?: return@withLock null
                if (index !in 0 until active.pageCount) return@withLock null
                try {
                    val page = active.openPage(index)
                    try {
                        val heightPx = (widthPx.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                        // Il PDF non porta un fondo: senza questo il testo nero
                        // finirebbe su trasparente, cioe' nero su nero in tema scuro.
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        cache.put(key, bitmap)
                        bitmap
                    } finally {
                        page.close()
                    }
                } catch (_: OutOfMemoryError) {
                    cache.evictAll()
                    null
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Chiude renderer e descrittore.
     *
     * Le bitmap in cache non si riciclano: la lista puo' averne ancora una sul
     * canvas mentre si esce, e disegnare una bitmap riciclata e' un crash.
     * Basta lasciarle al garbage collector.
     */
    override fun onCleared() {
        viewModelScope.launch(Dispatchers.IO) {
            renderMutex.withLock {
                runCatching { renderer?.close() }
                runCatching { descriptor?.close() }
                renderer = null
                descriptor = null
            }
        }
        cache.evictAll()
        super.onCleared()
    }

    private fun openRenderer(file: File): List<PdfPageSize> {
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        // `PdfRenderer` lancia su PDF corrotto o protetto da password. Se il
        // descrittore non lo chiudiamo noi qui, resta aperto: l'utente riprova,
        // ogni tentativo ne perde uno, e dopo un po' il processo non apre piu'
        // niente. Il file troncato che porta qui e' un caso reale, vedi il
        // download atomico piu' sotto.
        val opened = try {
            PdfRenderer(fd)
        } catch (e: Throwable) {
            runCatching { fd.close() }
            throw e
        }
        descriptor = fd
        renderer = opened
        return (0 until opened.pageCount).map { i ->
            val page = opened.openPage(i)
            try {
                PdfPageSize(page.width, page.height)
            } finally {
                page.close()
            }
        }
    }

    /**
     * Scarica il PDF, autenticando la richiesta.
     *
     * L'header e' cio' che serve: il backend serve gli allegati solo a
     * richieste autenticate e a chiunque altro risponde 404. Senza,
     * l'utente vedeva "HTTP 404" al posto di ogni documento.
     *
     * `isMensaHost` e' la stessa regola del client Ktor e dell'ImageLoader: il
     * Bearer va al nostro backend e a nessun altro, che questo viewer puo'
     * ricevere anche URL di terzi.
     *
     * Il token si legge qui e non si tiene: e' quello della sessione corrente.
     */
    private fun downloadAndCache(context: Context, url: String): File {
        val cacheDir = File(context.cacheDir, "pdfs").also { it.mkdirs() }
        val fileName = url.hashCode().toString() + ".pdf"
        val file = File(cacheDir, fileName)
        if (file.exists() && file.length() > 0) return file

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .apply {
                val token = AuthHolder.token
                if (token != null && isMensaHost(url.toHttpUrlOrNull()?.host.orEmpty())) {
                    header("Authorization", "Bearer $token")
                }
            }
            .build()
        // Si scarica su un file di appoggio e lo si rinomina solo a scaricamento
        // finito. Prima si scriveva dritti sul file definitivo: se la rete
        // cadeva a meta', restava in cache un PDF troncato lungo piu' di zero,
        // e la guardia qui sopra lo considerava buono. Da quel momento quel
        // documento rispondeva "Impossibile aprire il PDF" per sempre, perche'
        // il nome del file e' deterministico e nessuno lo invalidava.
        val partial = File(cacheDir, "$fileName.part")
        try {
            // OkHttp segue da solo il 307 verso S3 e in quel passaggio toglie
            // l'Authorization, perche' il redirect cambia host: il token della
            // sessione non finisce in un dominio che non e' nostro.
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val body = response.body ?: error("Empty response body")
                FileOutputStream(partial).use { out -> body.byteStream().copyTo(out) }
            }
            if (partial.length() <= 0) error("Download vuoto")
            if (!partial.renameTo(file)) {
                partial.copyTo(file, overwrite = true)
            }
        } finally {
            if (partial.exists()) partial.delete()
        }
        return file
    }
}
