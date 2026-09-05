package it.mensa.app.features.documents

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
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
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

sealed class PdfViewerState {
    object Loading : PdfViewerState()
    data class Ready(val pages: List<Bitmap>) : PdfViewerState()
    data class Error(val message: String) : PdfViewerState()
}

class PdfViewerViewModel(private val url: String) : ViewModel() {

    private val _state = MutableStateFlow<PdfViewerState>(PdfViewerState.Loading)
    val state: StateFlow<PdfViewerState> = _state.asStateFlow()

    private val _currentScale = MutableStateFlow(1f)
    val currentScale: StateFlow<Float> = _currentScale.asStateFlow()

    fun load(context: Context) {
        viewModelScope.launch {
            _state.update { PdfViewerState.Loading }
            try {
                val file = withContext(Dispatchers.IO) { downloadAndCache(context, url) }
                val bitmaps = withContext(Dispatchers.IO) { renderPdf(file) }
                _state.update { PdfViewerState.Ready(bitmaps) }
            } catch (e: Exception) {
                _state.update { PdfViewerState.Error(e.message ?: "Errore durante il caricamento del PDF") }
            }
        }
    }

    fun onScaleChange(scale: Float) {
        _currentScale.update { (scale * it).coerceIn(0.5f, 5f) }
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
        // OkHttp segue da solo il 307 verso S3 e in quel passaggio toglie
        // l'Authorization, perche' il redirect cambia host: il token della
        // sessione non finisce in un dominio che non e' nostro.
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body ?: error("Empty response body")
            FileOutputStream(file).use { out -> body.byteStream().copyTo(out) }
        }
        return file
    }

    private fun renderPdf(file: File): List<Bitmap> {
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        val bitmaps = mutableListOf<Bitmap>()
        try {
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val scale = 2f // Render at 2x for crisp display
                val bitmap = Bitmap.createBitmap(
                    (page.width * scale).toInt(),
                    (page.height * scale).toInt(),
                    Bitmap.Config.ARGB_8888,
                )
                // White background
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bitmap)
            }
        } finally {
            renderer.close()
            fd.close()
        }
        return bitmaps
    }
}
