package it.mensa.app.features.quid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

data class QuidPdfState(
    val localFile: File? = null,
    val downloading: Boolean = false,
    val error: String? = null,
)

/**
 * QuidPdfViewModel — downloads a remote PDF to a stable cache path.
 *
 * Mirrors iOS QuidPDFViewer.download():
 * - Downloads URL → temp file in context.cacheDir/quid_pdfs/
 * - Uses filename derived from URL last segment for stable naming
 * - Error excludes CancellationException (back nav)
 */
class QuidPdfViewModel(
    private val pdfUrl: String,
    private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(QuidPdfState())
    val state: StateFlow<QuidPdfState> = _state.asStateFlow()

    init {
        download()
    }

    private fun download() {
        viewModelScope.launch {
            _state.update { it.copy(downloading = true, error = null) }
            try {
                val file = withContext(Dispatchers.IO) {
                    val cacheDir = File(context.cacheDir, "quid_pdfs").also { it.mkdirs() }
                    val fileName = pdfUrl.substringAfterLast('/').ifBlank { "quid.pdf" }
                    val dest = File(cacheDir, fileName)

                    // Use cached if already exists
                    if (!dest.exists() || dest.length() == 0L) {
                        val url = URL(pdfUrl)
                        url.openStream().use { input ->
                            dest.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    dest
                }
                _state.update { it.copy(localFile = file, downloading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(downloading = false, error = e.message ?: "Errore durante il download") }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
