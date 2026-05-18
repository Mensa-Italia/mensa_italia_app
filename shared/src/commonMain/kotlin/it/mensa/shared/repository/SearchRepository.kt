package it.mensa.shared.repository

import it.mensa.shared.api.endpoints.SearchApi
import it.mensa.shared.model.search.SearchResponse
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

/**
 * Punto unico di ricerca per tutta l'app. Espone un Flow `state` che reagisce
 * automaticamente all'input dell'utente: debounce 300ms, deduplica, gestisce
 * loading/error/result. Per ricerche one-shot programmatic usa `queryOnce()`.
 */
class SearchRepository(private val api: SearchApi) {

    sealed interface State {
        data object Idle : State
        data class Loading(val query: String) : State
        data class Success(val query: String, val response: SearchResponse) : State
        data class Error(val query: String, val cause: Throwable) : State
    }

    data class Params(
        val q: String,
        val types: List<String>? = null,
        val region: String? = null,
        val limitPerType: Int = 10,
        val hydrate: Boolean = true,
    )

    private val _params = MutableStateFlow<Params?>(null)

    /** Aggiorna la query corrente. Le UI debbono usare questo per binding. */
    fun update(params: Params) { _params.value = params }
    fun update(q: String) { _params.value = _params.value?.copy(q = q) ?: Params(q) }
    fun clear() { _params.value = null }

    /**
     * Stream osservabile da UI: re-emette su ogni cambio di parametri,
     * con debounce 300ms e cancellazione delle query precedenti tramite flatMapLatest.
     */
    @OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: Flow<State> = _params
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { p ->
            if (p == null || p.q.isBlank()) flowOf<State>(State.Idle)
            else flow<State> {
                emit(State.Loading(p.q))
                try {
                    val resp = api.search(p.q, p.types, p.region, p.limitPerType, p.hydrate)
                    emit(State.Success(p.q, resp))
                } catch (e: Throwable) {
                    emit(State.Error(p.q, e))
                }
            }
        }

    /** Per ricerche programmatic (es. "trova tutto su Mario Rossi"). */
    suspend fun queryOnce(params: Params): SearchResponse =
        api.search(params.q, params.types, params.region, params.limitPerType, params.hydrate)

    suspend fun queryOnce(q: String): SearchResponse = queryOnce(Params(q))
}
