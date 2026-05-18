package it.mensa.shared.iqtest

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.math.max
import kotlin.random.Random

private const val BASE_URL = "https://test.mensa.no"
private const val TEST_URL = "$BASE_URL/Home/Test/it-IT"
private const val USER_AGENT =
    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"

class MensaTestClient {

    private val client: HttpClient = HttpClient {
        install(HttpCookies)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, USER_AGENT)
        }
    }

    suspend fun loadTest(): MensaTestPayload {
        val response = runCatching {
            client.get(TEST_URL) {
                header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                header(HttpHeaders.AcceptLanguage, "it-IT,it;q=0.9,en;q=0.8")
                // Disable gzip/deflate: with the Ktor OkHttp engine on Android,
                // a chunked compressed response on this endpoint can surface as
                // "source exhausted prematurely" because OkHttp transparently
                // decompresses while Ktor's ContentLength check still trips.
                // Asking for identity sidesteps the issue and is harmless on iOS.
                header(HttpHeaders.AcceptEncoding, "identity")
            }
        }.getOrElse { throw MensaTestException.InvalidResponse(it) }

        if (response.status != HttpStatusCode.OK) {
            throw MensaTestException.HttpError(response.status.value)
        }

        val html = runCatching { response.bodyAsText() }
            .getOrElse { throw MensaTestException.ParseFailure("HTML non decodificabile") }

        val token = extractInt64(html, """authorizationToken\s*=\s*(-?\d+)""", "authorizationToken")
        val totalQuestions = runCatching {
            extractInt64(html, """totalTestCount\s*=\s*(\d+)""", "totalTestCount").toInt()
        }.getOrDefault(35)
        val durationSeconds = extractDuration(html) ?: 1500

        val questions = parseQuestions(html, totalQuestions)

        // Fire-and-forget ping
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { ping() }
        }

        return MensaTestPayload(
            token = token,
            totalQuestions = totalQuestions,
            durationSeconds = durationSeconds,
            questions = questions,
        )
    }

    /**
     * Submit a completed test.
     *
     * `answers` keys are question ids as strings — using `Map<String, Int>`
     * instead of `Map<Int, Int>` keeps the Swift call site clean (Swift can't
     * ergonomically construct a `Map<KotlinInt, KotlinInt>` boxed type).
     */
    suspend fun submit(
        payload: MensaTestPayload,
        answers: Map<String, Int>,
        ageGroup: MensaAgeGroup,
        startedAt: Instant,
        finishedAt: Instant,
    ): MensaTestResult {
        val rnd = Random.nextDouble(0.0, 1.0)
        val url = "$BASE_URL/Score/Score/it?rnd=$rnd"

        val startTime = startedAt.toString()
        val endTime = finishedAt.toString()

        val secondsUsed = ((finishedAt - startedAt).inWholeSeconds).toInt()
        val secondsRemaining = max(0, payload.durationSeconds - secondsUsed)
        val lastIndex = payload.totalQuestions - 1

        // Build answers map: key = question id (string), value = object with fields
        val answersJson = buildJsonObject {
            for ((kStr, v) in answers) {
                val kInt = kStr.toIntOrNull() ?: continue
                putJsonObject(kStr) {
                    put("questionId", kInt)
                    put("answerId", v)
                    put("answerCount", 1)
                    put("totalTimeMs", 0)
                    put("visitCount", 1)
                }
            }
        }

        val body: JsonObject = buildJsonObject {
            put("authorizationToken", payload.token)
            put("languageCode", "it")
            putJsonObject("session") {
                put("currentQuestionId", lastIndex)
                put("currentQuestionNumber", lastIndex)
                put("secondsRemaining", secondsRemaining)
                put("secondsUsed", secondsUsed)
                put("currentQuestionTracking", -1)
                put("currentQuestionStartTime", 0)
                put("answers", answersJson)
                put("ageGroup", ageGroup.rawValue)
                put("languageCode", "it")
                put("startTime", startTime)
                put("endTime", endTime)
            }
        }

        val response = runCatching {
            client.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Origin, BASE_URL)
                header(HttpHeaders.Referrer, TEST_URL)
                setBody(body)
            }
        }.getOrElse { throw MensaTestException.InvalidResponse() }

        if (response.status != HttpStatusCode.OK) {
            throw MensaTestException.HttpError(response.status.value)
        }

        return runCatching { response.body<MensaTestResult>() }
            .getOrElse { throw MensaTestException.ParseFailure("Impossibile decodificare MensaTestResult") }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun ping() {
        val id = generateUuid()
        client.get("$BASE_URL/Score/Ping?id=$id")
    }

    private fun extractInt64(html: String, pattern: String, label: String): Long {
        val regex = Regex(pattern)
        val match = regex.find(html)
            ?: throw MensaTestException.ParseFailure("Impossibile estrarre $label")
        return match.groupValues[1].toLongOrNull()
            ?: throw MensaTestException.ParseFailure("Impossibile estrarre $label")
    }

    private fun extractDuration(html: String): Int? {
        val regex = Regex("""testTimeSeconds\s*=\s*(0x[0-9a-fA-F]+|\d+)""")
        val match = regex.find(html) ?: return null
        val raw = match.groupValues[1]
        return if (raw.startsWith("0x")) {
            raw.drop(2).toIntOrNull(16)
        } else {
            raw.toIntOrNull()
        }
    }

    private fun parseQuestions(html: String, count: Int): List<MensaTestQuestion> {
        val startRegex = Regex("""class="question question_(\d+)"""")
        val qImgRegex = Regex("""<img[^>]*src="(https://static\.mensa\.no/images/q/[^"]+\.png)"[^>]*class="[^"]*standardQuestionImage""")
        val aImgRegex = Regex("""<img[^>]*src="(https://static\.mensa\.no/images/q/[^"]+\.png)"[^>]*data-answerid="(\d+)"[^>]*class="[^"]*standardAnswerImage""")

        val starts = startRegex.findAll(html).toList()

        if (starts.isEmpty()) {
            throw MensaTestException.ParseFailure("Nessuna domanda trovata nell'HTML")
        }

        val questions = mutableListOf<MensaTestQuestion>()

        for (i in starts.indices) {
            val match = starts[i]
            val questionId = match.groupValues[1].toLongOrNull() ?: continue

            val sliceStart = match.range.first
            val sliceEnd = if (i + 1 < starts.size) starts[i + 1].range.first else html.length
            val slice = html.substring(sliceStart, sliceEnd)

            val qMatch = qImgRegex.find(slice) ?: continue
            val questionImageUrl = qMatch.groupValues[1]

            val optionsMap = mutableMapOf<Int, String>()
            for (aMatch in aImgRegex.findAll(slice)) {
                val imgUrl = aMatch.groupValues[1]
                val idx = aMatch.groupValues[2].toIntOrNull() ?: continue
                optionsMap[idx] = imgUrl
            }

            if (optionsMap.size == 6) {
                val ordered = (0 until 6).mapNotNull { optionsMap[it] }
                if (ordered.size == 6) {
                    questions.add(
                        MensaTestQuestion(
                            id = questionId,
                            imageUrl = questionImageUrl,
                            options = ordered,
                        )
                    )
                }
            }
        }

        if (questions.isEmpty()) {
            throw MensaTestException.ParseFailure("Parsing delle domande fallito")
        }

        return questions.sortedBy { it.id }
    }
}

// KMP-portable UUID generation (uses kotlin.random; good enough for a ping id)
private fun generateUuid(): String {
    val r = Random.Default
    fun hex(n: Int) = (0 until n).joinToString("") { r.nextInt(16).toString(16) }
    return "${hex(8)}-${hex(4)}-4${hex(3)}-${listOf(8,9,10,11).random().toString(16)}${hex(3)}-${hex(12)}"
}
