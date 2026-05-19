package it.mensa.app.features.documents

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private fun downloadAndCache(context: Context, url: String): File {
        val cacheDir = File(context.cacheDir, "pdfs").also { it.mkdirs() }
        val fileName = url.hashCode().toString() + ".pdf"
        val file = File(cacheDir, fileName)
        if (file.exists() && file.length() > 0) return file

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
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
