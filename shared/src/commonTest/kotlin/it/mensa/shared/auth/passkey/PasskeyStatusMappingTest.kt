package it.mensa.shared.auth.passkey

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifica la matrice di degradazione del login con passkey.
 *
 * Non e' un test di comodo: questa mappatura decide se l'utente viene rimandato
 * alla password con una nota neutra, se gli si riapre il prompt, o se vede un
 * errore di rete. Sbagliarne una voce significa o un vicolo cieco nella UI o un
 * errore rosso dove non serve.
 */
class PasskeyStatusMappingTest {

    @Test
    fun beginMapsConflictToUnavailable() {
        // Il server risponde 409 sia per email sconosciuta sia per utente senza
        // passkey, di proposito: distinguerli sarebbe un oracolo su chi e' socio.
        assertEquals(
            PasskeyStartStatus.UNAVAILABLE,
            passkeyStartStatusFor(HttpStatusCode.Conflict),
        )
    }

    @Test
    fun beginMapsEverythingElseToNetworkError() {
        val others = listOf(
            HttpStatusCode.BadRequest,
            HttpStatusCode.Unauthorized,
            HttpStatusCode.NotFound,
            HttpStatusCode.InternalServerError,
            HttpStatusCode.ServiceUnavailable,
            HttpStatusCode.GatewayTimeout,
        )
        for (status in others) {
            assertEquals(
                PasskeyStartStatus.NETWORK_ERROR,
                passkeyStartStatusFor(status),
                "begin: $status dovrebbe essere NETWORK_ERROR",
            )
        }
    }

    @Test
    fun finishMapsGoneToChallengeExpired() {
        // L'unico esito che merita un secondo tentativo automatico.
        assertEquals(
            PasskeyLoginStatus.CHALLENGE_EXPIRED,
            passkeyLoginStatusFor(HttpStatusCode.Gone),
        )
    }

    @Test
    fun finishMapsConflictToUnavailable() {
        assertEquals(
            PasskeyLoginStatus.UNAVAILABLE,
            passkeyLoginStatusFor(HttpStatusCode.Conflict),
        )
    }

    @Test
    fun finishMapsRejections() {
        // 401 assertion non valida, 400 payload non valido: in entrambi i casi la
        // passkey non e' utilizzabile e si passa alla password.
        for (status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.BadRequest)) {
            assertEquals(
                PasskeyLoginStatus.REJECTED,
                passkeyLoginStatusFor(status),
                "finish: $status dovrebbe essere REJECTED",
            )
        }
    }

    @Test
    fun finishMapsServerFailuresToNetworkError() {
        for (status in listOf(
            HttpStatusCode.InternalServerError,
            HttpStatusCode.ServiceUnavailable,
            HttpStatusCode.NotFound,
        )) {
            assertEquals(
                PasskeyLoginStatus.NETWORK_ERROR,
                passkeyLoginStatusFor(status),
                "finish: $status dovrebbe essere NETWORK_ERROR",
            )
        }
    }

    @Test
    fun noStatusMapsToSuccess() {
        // Il successo non passa da qui: arriva solo quando la richiesta non
        // solleva. Se un giorno un 2xx finisse in questa funzione, non deve
        // poter essere confuso con un login riuscito.
        for (status in listOf(HttpStatusCode.OK, HttpStatusCode.Created, HttpStatusCode.Accepted)) {
            val mapped = passkeyLoginStatusFor(status)
            assertEquals(
                PasskeyLoginStatus.NETWORK_ERROR,
                mapped,
                "finish: $status non deve mappare su SUCCESS",
            )
        }
    }
}
