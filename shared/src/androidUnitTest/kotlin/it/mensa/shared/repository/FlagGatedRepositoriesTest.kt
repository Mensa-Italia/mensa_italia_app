package it.mensa.shared.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.api.endpoints.OrgChartApi
import it.mensa.shared.api.endpoints.QuidApi
import it.mensa.shared.api.endpoints.SettingsApi
import it.mensa.shared.api.mensaDefaults
import it.mensa.shared.config.FeatureFlags
import it.mensa.shared.db.MensaDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Organigramma e audio del Quid si accendono da `configs`. Spenti, nessuno li
 * chiede al server: prima il flag nascondeva solo la voce del profilo, e la
 * ricerca e la lista di un numero del Quid li leggevano comunque.
 */
class FlagGatedRepositoriesTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val requests = mutableListOf<HttpRequestData>()

    private suspend fun flags(orgChart: Boolean, quidAudio: Boolean): Pair<FeatureFlags, HttpClient> {
        val configs = """{"page":1,"perPage":200,"totalItems":2,"totalPages":1,"items":[
            {"id":"c1","key":"org_chart_enabled","value":"$orgChart"},
            {"id":"c2","key":"quid_audio_enabled","value":"$quidAudio"}]}"""
        val client = HttpClient(MockEngine { req ->
            requests += req
            when {
                req.url.encodedPath.endsWith("/configs/records") -> respond(configs, HttpStatusCode.OK, jsonHeaders)
                else -> respond("""{"status":403,"message":"Only superusers can perform this action.","data":{}}""", HttpStatusCode.Forbidden, jsonHeaders)
            }
        }) { mensaDefaults() }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MensaDatabase.Schema.create(driver).await()
        val pb = PocketBaseClient(client)
        return FeatureFlags(SettingsApi(pb, client), MensaDatabase(driver)).apply { bootstrap() } to client
    }

    private fun calls(fragment: String) = requests.count { fragment in it.url.toString() }

    @Test
    fun switchedOffTheyNeverReachTheServer() = runTest {
        val (flags, client) = flags(orgChart = false, quidAudio = false)

        assertTrue(OrgChartRepository(OrgChartApi(PocketBaseClient(client)), flags).fetch().groups.isEmpty())
        assertNull(QuidRepository(QuidApi(PocketBaseClient(client), client), flags).getAudioForArticle(42))

        assertEquals(0, calls("org_chart"))
        assertEquals(0, calls("quid_articles_audio"))
    }

    @Test
    fun switchedOnTheyAsk() = runTest {
        val (flags, client) = flags(orgChart = true, quidAudio = true)

        runCatching { OrgChartRepository(OrgChartApi(PocketBaseClient(client)), flags).fetch() }
        runCatching { QuidRepository(QuidApi(PocketBaseClient(client), client), flags).getAudioForArticle(42) }

        assertTrue(calls("org_chart") > 0)
        assertTrue(calls("quid_articles_audio") > 0)
    }
}
