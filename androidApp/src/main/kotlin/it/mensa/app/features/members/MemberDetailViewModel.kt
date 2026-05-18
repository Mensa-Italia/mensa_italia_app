package it.mensa.app.features.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.RegSociModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

data class MemberDetailUiState(
    val member: RegSociModel? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class MemberDetailViewModel(private val memberId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberDetailUiState())
    val uiState: StateFlow<MemberDetailUiState> = _uiState.asStateFlow()

    private val repo get() = koinAccess().regSoci

    init {
        repo.observeOne(memberId)
            .onEach { member ->
                if (member != null) _uiState.update { it.copy(member = member, loading = false) }
            }
            .catch { }
            .launchIn(viewModelScope)

        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            if (_uiState.value.member == null) _uiState.update { it.copy(loading = true) }
            try {
                val fetched = repo.getById(memberId)
                if (fetched != null) _uiState.update { it.copy(member = fetched, loading = false) }
                else _uiState.update { it.copy(loading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, loading = false) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    // ── fullData extraction ───────────────────────────────────────────────────

    fun extractFullData(member: RegSociModel): List<Pair<String, String>> {
        return member.fullData.entries
            .mapNotNull { (key, value) ->
                val s = when (value) {
                    is JsonPrimitive -> value.content
                    else -> value.toString().trim('"')
                }
                if (s.isEmpty() || s == "null") null
                else key to s
            }
            .sortedBy { it.first.lowercase() }
    }

    fun profileRows(member: RegSociModel): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()
        if (member.name.isNotEmpty()) rows.add("Nome" to member.name.split(" ").joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercaseChar() } })
        if (member.city.isNotEmpty()) rows.add("Città" to member.city.lowercase().replaceFirstChar { it.uppercaseChar() })
        if (member.state.isNotEmpty()) rows.add("Regione" to member.state.lowercase().replaceFirstChar { it.uppercaseChar() })
        member.birthdate?.let { bd ->
            rows.add("Data di nascita" to formatInstant(bd.toEpochMilliseconds()))
        }
        rows.addAll(extractFullData(member).filter { (k, _) -> isProfileKey(k) })
        return rows
    }

    fun mensaRows(member: RegSociModel): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()
        if (member.id.isNotEmpty()) rows.add("ID Socio" to member.id)
        rows.addAll(extractFullData(member).filter { (k, _) -> isMensaKey(k) })
        if (rows.isEmpty()) rows.add("ID Socio" to member.id)
        return rows
    }

    fun contactRows(member: RegSociModel): List<Pair<String, String>> =
        extractFullData(member).filter { (k, _) -> isContactKey(k) }
            .map { (k, v) -> k to prettifyContact(v) }

    fun sigRows(member: RegSociModel): List<Pair<String, String>> =
        extractFullData(member).filter { (k, _) -> isSigKey(k) }

    private fun isContactKey(k: String): Boolean {
        val lk = k.lowercase()
        return lk.contains("email") || lk.contains("mail") || lk.contains("phone") ||
            lk.contains("tel") || lk.contains("cell") || lk.contains("facebook") ||
            lk.contains("instagram") || lk.contains("website") || lk.contains("sito")
    }

    private fun isSigKey(k: String): Boolean {
        val lk = k.lowercase()
        return lk.contains("sig") || lk.contains("gruppo")
    }

    private fun isMensaKey(k: String): Boolean {
        val lk = k.lowercase()
        return lk.contains("iscriz") || lk.contains("scaden") || lk.contains("tessera") ||
            lk.contains("membership") || lk.contains("expire") || lk.contains("local")
    }

    private fun isProfileKey(k: String) = !isContactKey(k) && !isSigKey(k) && !isMensaKey(k)

    private fun prettifyContact(v: String): String = when {
        v.startsWith("mailto:") -> v.removePrefix("mailto:")
        v.startsWith("tel:") -> v.removePrefix("tel:")
        else -> v
    }

    private fun formatInstant(ms: Long): String {
        val date = java.util.Date(ms)
        val fmt = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.ITALIAN)
        return fmt.format(date)
    }
}
