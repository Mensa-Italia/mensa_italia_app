package it.mensa.shared.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.mensa.shared.api.endpoints.SearchApi
import it.mensa.shared.model.search.SearchHit
import it.mensa.shared.model.search.SearchRequest
import it.mensa.shared.model.search.SearchResponse
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Creates a minimal HttpClient with MockEngine for use in open subclasses of SearchApi. */
private fun makeMockClient(): HttpClient {
    val engine = MockEngine { _ ->
        respond(
            content = "{}",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        )
    }
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}

/** Fake SearchApi that tracks calls and returns a canned response. */
private class FakeSearchApi(
    private val canned: SearchResponse = SearchResponse(
        query = "test",
        total = 3,
        results = mapOf(
            "event" to listOf(SearchHit("e1", 0.9, "Event 1", deepLink = "mensa://event/e1")),
            "deal" to listOf(SearchHit("d1", 0.8, "Deal 1", deepLink = "mensa://deal/d1")),
            "user" to listOf(SearchHit("u1", 0.7, "User 1", deepLink = "mensa://user/u1")),
        )
    )
) : SearchApi(makeMockClient()) {

    var callCount = 0
    var lastQuery: String? = null

    override suspend fun search(request: SearchRequest, maxRetries: Int): SearchResponse {
        callCount++
        lastQuery = request.q
        return canned.copy(query = request.q)
    }

    override suspend fun search(
        q: String,
        types: List<String>?,
        region: String?,
        limitPerType: Int,
        hydrate: Boolean,
    ): SearchResponse {
        callCount++
        lastQuery = q
        return canned.copy(query = q)
    }
}

class SearchRepositoryTest {

    @Test
    fun updateThenDebounceEmitsLoadingAndSuccess() = runTest {
        val api = FakeSearchApi()
        val repo = SearchRepository(api)

        val states = mutableListOf<SearchRepository.State>()
        val job = launch {
            repo.state.toList(states)
        }

        repo.update("foo")
        advanceTimeBy(350) // past the 300ms debounce

        job.cancel()

        assertTrue(states.any { it is SearchRepository.State.Loading }, "Expected at least one Loading state")
        val success = states.filterIsInstance<SearchRepository.State.Success>()
        assertTrue(success.isNotEmpty(), "Expected at least one Success state")
        assertEquals("foo", success.last().query)
        assertEquals(3, success.last().response.total)
    }

    @Test
    fun clearEmitsIdle() = runTest {
        val api = FakeSearchApi()
        val repo = SearchRepository(api)

        val states = mutableListOf<SearchRepository.State>()
        val job = launch {
            repo.state.toList(states)
        }

        repo.update("something")
        advanceTimeBy(350)
        repo.clear()
        advanceTimeBy(350)

        job.cancel()

        val lastState = states.lastOrNull()
        assertIs<SearchRepository.State.Idle>(lastState, "Expected last state to be Idle but was $lastState")
    }

    @Test
    fun blankQueryEmitsIdle() = runTest {
        val api = FakeSearchApi()
        val repo = SearchRepository(api)

        val states = mutableListOf<SearchRepository.State>()
        val job = launch {
            repo.state.toList(states)
        }

        repo.update("")
        advanceTimeBy(350)

        job.cancel()

        assertTrue(
            states.isEmpty() || states.last() is SearchRepository.State.Idle,
            "Expected Idle for blank query but got: ${states.lastOrNull()}"
        )
        assertEquals(0, api.callCount, "API should not be called for blank query")
    }

    @Test
    fun rapidUpdatesOnlySendsLastQueryToServer() = runTest {
        val api = FakeSearchApi()
        val repo = SearchRepository(api)

        val states = mutableListOf<SearchRepository.State>()
        val job = launch {
            repo.state.toList(states)
        }

        // Rapid updates — only the last one should reach the API due to debounce
        repo.update("c")
        advanceTimeBy(100)
        repo.update("ca")
        advanceTimeBy(100)
        repo.update("car")
        advanceTimeBy(100)
        repo.update("carb")
        advanceTimeBy(350) // Now debounce fires for "carb"

        job.cancel()

        assertEquals(1, api.callCount, "Expected exactly 1 API call due to debounce, got ${api.callCount}")
        assertEquals("carb", api.lastQuery, "Expected last query to be 'carb' but got '${api.lastQuery}'")

        val successes = states.filterIsInstance<SearchRepository.State.Success>()
        assertTrue(successes.isNotEmpty())
        assertEquals("carb", successes.last().query)
    }
}
