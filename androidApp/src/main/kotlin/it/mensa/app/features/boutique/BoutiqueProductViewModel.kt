package it.mensa.app.features.boutique

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.BoutiqueModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class BoutiqueProductUiState(
    val product: BoutiqueModel? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class BoutiqueProductViewModel(private val productId: String) : ViewModel() {

    private val repo = koinAccess().boutique

    private val _uiState = MutableStateFlow(BoutiqueProductUiState())
    val uiState: StateFlow<BoutiqueProductUiState> = _uiState.asStateFlow()

    init {
        repo.observeOne(productId)
            .onEach { product ->
                _uiState.update { it.copy(product = product, loading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Extract the first http(s) URL from the product description.
     * Mirrors iOS BoutiqueProductView.orderURL(for:).
     */
    fun extractOrderUrl(product: BoutiqueModel): String? {
        val urlRegex = Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
        return urlRegex.find(product.description)?.value
    }
}
