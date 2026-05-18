package it.mensa.app.features.external

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ExternalAddonLoadState {
    object Idle : ExternalAddonLoadState()
    object Loading : ExternalAddonLoadState()
    data class Ready(val url: String) : ExternalAddonLoadState()
    data class Failed(val message: String) : ExternalAddonLoadState()
}

class ExternalAddonViewModel(private val addonId: String) : ViewModel() {

    private val addonsRepo = koinAccess().addons

    private val _state = MutableStateFlow<ExternalAddonLoadState>(ExternalAddonLoadState.Idle)
    val state: StateFlow<ExternalAddonLoadState> = _state.asStateFlow()

    private val _webViewLoading = MutableStateFlow(false)
    val webViewLoading: StateFlow<Boolean> = _webViewLoading.asStateFlow()

    fun load(baseUrl: String) {
        val trimmedBase = baseUrl.trim()
        val trimmedId = addonId.trim()

        if (trimmedBase.isEmpty()) {
            _state.update { ExternalAddonLoadState.Failed("Indirizzo non valido") }
            return
        }
        if (trimmedId.isEmpty()) {
            _state.update { ExternalAddonLoadState.Failed("ID addon non valido") }
            return
        }

        viewModelScope.launch {
            _state.update { ExternalAddonLoadState.Loading }
            try {
                val accessData = addonsRepo.getAccessData(trimmedId)
                val builtUrl = buildUrl(trimmedBase, accessData.params)
                if (builtUrl != null) {
                    _state.update { ExternalAddonLoadState.Ready(builtUrl) }
                } else {
                    _state.update { ExternalAddonLoadState.Failed("Indirizzo non valido") }
                }
            } catch (e: Exception) {
                _state.update { ExternalAddonLoadState.Failed(e.message ?: "Errore sconosciuto") }
            }
        }
    }

    fun onWebViewLoadingStart() = _webViewLoading.update { true }
    fun onWebViewLoadingFinish() = _webViewLoading.update { false }

    companion object {
        /**
         * Mirrors iOS buildURL: appends every (k, v) from accessData.params as
         * query parameters, preserving any pre-existing query params on base.
         */
        fun buildUrl(base: String, params: Map<String, String>): String? {
            return try {
                val uri = android.net.Uri.parse(base)
                val builder = uri.buildUpon()
                for ((k, v) in params) {
                    if (k.isNotEmpty()) builder.appendQueryParameter(k, v)
                }
                builder.build().toString()
            } catch (_: Exception) {
                null
            }
        }
    }
}
