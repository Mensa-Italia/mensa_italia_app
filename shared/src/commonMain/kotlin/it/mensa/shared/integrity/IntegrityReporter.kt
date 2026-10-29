package it.mensa.shared.integrity

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.seconds

private const val PATH_REPORT = "/api/integrity/report"

/** Lunghezza massima di `detail` e `underlying`: il server li taglia a 200 comunque. */
private const val MAX_TEXT = 200

internal object ReportOutcome {
    /** La prova non si e' potuta costruire. */
    const val FAILED = "failed"

    /** Una chiave e' stata scartata e sostituita: porta l'errore ORIGINALE di quella vecchia, anche se poi tutto riesce. */
    const val ROTATED = "rotated"
}

/**
 * Rende osservabile il fallimento della prova: manda al server la causa vera
 * (stage, dominio, codice, `NSUnderlyingErrorKey`, sistema, build) con
 * `POST /api/integrity/report`. Prima il `DCError` si buttava via e il server
 * non poteva distinguere "il dispositivo non puo'" da "lo script non manda
 * niente".
 *
 * **Descrive, non concede.** Il report e' testo non fidato scritto in un log:
 * il server non lo usa per nessuna decisione e un client forgiato lo manda
 * identico. Per questo non c'e' niente da proteggere qui: e' a senso unico,
 * fire-and-forget, e se non arriva si perde.
 *
 *  - parte in [scope] (iniettato: `SupervisorJob + Default`), mai sul percorso
 *    della richiesta dell'utente e senza tenere il `sessionLock` del guard;
 *  - `withTimeout(3 s)`, e ogni errore e' inghiottito tranne la cancellazione:
 *    un server giu', un 403 `account_blocked` o un 404 (server vecchio) non
 *    rompono mai niente;
 *  - la richiesta e' autenticata dal client come le altre ed e' esente dalla
 *    prova ([IntegrityScope]): non porta header di integrita' e non genera sfide;
 *  - il chiamante lo usa solo per un fallimento FRESCO (una vera chiamata ad
 *    Apple o al server, non una risposta dalla memoria): il ritmo lo da' il
 *    backoff del guard, piu' un report per rotazione.
 */
internal class IntegrityReporter(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) {

    /** Manda il report in background; il [Job] serve solo ai test. */
    fun report(
        platform: String,
        environment: DeviceEnvironment,
        failure: ProofFailure,
        outcome: String,
        attempt: Int,
        elapsedMs: Long,
    ): Job {
        val body = buildJsonObject {
            put("platform", platform)
            put("stage", failure.stage.wire)
            failure.domain?.let { put("domain", it) }
            failure.code?.let { put("code", it) }
            put("detail", failure.detail.take(MAX_TEXT))
            failure.underlying?.let { put("underlying", it.take(MAX_TEXT)) }
            put("disposition", failure.disposition.wire)
            put("outcome", outcome)
            put("attempt", attempt.coerceAtLeast(1))
            put("key_phase", failure.keyPhase)
            put("key_age_s", failure.keyAgeSeconds)
            put("elapsed_ms", elapsedMs.coerceAtLeast(0))
            if (environment.app.isNotBlank()) put("app", environment.app)
            if (environment.os.isNotBlank()) put("os", environment.os)
            put("ios_app_on_mac", environment.iosAppOnMac)
        }.toString()

        return scope.launch {
            try {
                withTimeoutOrNull(TIMEOUT) {
                    client.post(PATH_REPORT) { setBody(TextContent(body, ContentType.Application.Json)) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort: il report non deve mai poter fare danni.
            }
        }
    }

    private companion object {
        val TIMEOUT = 3.seconds
    }
}
