package it.mensa.app.features.profile.sub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.LocaleManager
import it.mensa.app.support.koinAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class LanguagePickerUiState(
    val availableLocales: List<String> = emptyList(),
    val currentLocale: String? = null,   // null = system default
    val switching: Boolean = false,
)

class LanguagePickerViewModel(
    private val localeManager: LocaleManager,
) : ViewModel() {

    private val i18n = koinAccess().i18n

    private val _uiState = MutableStateFlow(LanguagePickerUiState())
    val uiState: StateFlow<LanguagePickerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Observe available locales from shared I18n
            i18n.availableLocales.collect { locales ->
                _uiState.update { it.copy(availableLocales = locales) }
            }
        }
        viewModelScope.launch {
            localeManager.currentLocale.collect { locale ->
                _uiState.update { it.copy(currentLocale = locale) }
            }
        }
    }

    fun displayName(tag: String): String {
        return try {
            Locale.forLanguageTag(tag).getDisplayName(Locale.getDefault()).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            tag
        }
    }

    fun nativeName(tag: String): String {
        return try {
            val locale = Locale.forLanguageTag(tag)
            locale.getDisplayName(locale).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            tag
        }
    }

    fun pickLocale(tag: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(switching = true) }
            if (tag == null) {
                localeManager.clearLocale()
            } else {
                localeManager.setLocale(tag)
            }
            _uiState.update { it.copy(switching = false) }
        }
    }
}
