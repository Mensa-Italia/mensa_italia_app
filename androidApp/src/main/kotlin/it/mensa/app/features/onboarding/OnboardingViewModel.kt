package it.mensa.app.features.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OnboardingPage(
    val icon: String,        // Material icon name reference for placeholder
    val title: String,
    val subtitle: String,
)

data class OnboardingUiState(
    val currentPage: Int = 0,
    val pages: List<OnboardingPage>,
) {
    val isLastPage: Boolean get() = currentPage == pages.size - 1
    val totalPages: Int get() = pages.size
}

class OnboardingViewModel(
    private val onComplete: () -> Unit,
) : ViewModel() {

    // Pages are populated at VM construction time so the i18n tr() calls
    // happen when the catalog is already loaded (bootstrapped before this phase).
    private val _pages: List<OnboardingPage> = buildPages()

    private val _uiState = MutableStateFlow(OnboardingUiState(pages = _pages))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNext() {
        _uiState.update { state ->
            if (state.currentPage < state.pages.size - 1) {
                state.copy(currentPage = state.currentPage + 1)
            } else state
        }
    }

    fun onPageSelected(index: Int) {
        _uiState.update { it.copy(currentPage = index.coerceIn(0, it.pages.size - 1)) }
    }

    fun onComplete() {
        onComplete.invoke()
    }

    private fun buildPages(): List<OnboardingPage> = listOf(
        OnboardingPage(
            icon = "sparkles",
            title = "Benvenuto in Mensa",
            subtitle = "Sei entrato a far parte del 2% più brillante. Ti aiutiamo a sfruttare al meglio la community.",
        ),
        OnboardingPage(
            icon = "calendar_add_on",
            title = "Eventi in tutta Italia",
            subtitle = "Trova cene, conferenze e incontri vicino a te — o quelli nazionali. Iscriviti con un tap e aggiungili al calendario.",
        ),
        OnboardingPage(
            icon = "badge",
            title = "La tua tessera, sempre con te",
            subtitle = "Mostrala al coordinatore quando partecipi a un evento. Niente più tessere di plastica da cercare nel portafoglio.",
        ),
        OnboardingPage(
            icon = "search",
            title = "Una ricerca per tutto",
            subtitle = "Persone, eventi, convenzioni, documenti — un solo posto per trovare quello che ti serve.",
        ),
    )
}
