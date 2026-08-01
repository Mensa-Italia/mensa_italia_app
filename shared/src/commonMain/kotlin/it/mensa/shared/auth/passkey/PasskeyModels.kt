package it.mensa.shared.auth.passkey

import it.mensa.shared.model.UserModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Una passkey registrata, come la mostra la schermata di gestione.
 * [name] e' l'etichetta scelta al momento della registrazione, di norma il
 * modello del dispositivo.
 */
@Serializable
data class PasskeyModel(
    val id: String = "",
    val name: String = "",
    val ready: Boolean = false,
)

@Serializable
data class PasskeyListResponse(
    val passkeys: List<PasskeyModel> = emptyList(),
)

/**
 * Le WebAuthn options restano JSON opaco lungo tutta la catena.
 *
 * Su Android vanno passate as-is a Credential Manager, su iOS vengono
 * decodificate campo per campo da ASAuthorization, nel browser finiscono dritte
 * in `navigator.credentials`. Tipizzarle qui non servirebbe a nessuno dei tre e
 * ci legherebbe a un formato che non controlliamo.
 */
@Serializable
data class PasskeyLoginChallengeResponse(
    val login_id: String = "",
    val public_key_credential_request_options: JsonElement? = null,
)

@Serializable
data class PasskeyRegistrationChallengeResponse(
    val passkey_id: String = "",
    val public_key_credential_creation_options: JsonElement? = null,
)

/** Challenge di login pronta per il prompt di sistema. */
data class PasskeyChallenge(
    val loginId: String,
    val requestOptionsJson: String,
)

/** Challenge di registrazione pronta per il prompt di sistema. */
data class PasskeyRegistrationChallenge(
    val passkeyId: String,
    val creationOptionsJson: String,
)

/**
 * Esito dell'apertura di una challenge di login.
 *
 * [UNAVAILABLE] copre in un unico caso "utente sconosciuto" e "utente senza
 * passkey": il server li rende deliberatamente indistinguibili per non
 * trasformare l'endpoint in un oracolo su chi e' socio. Per il client la
 * conseguenza e' la stessa — si usa la password — quindi non perde nulla.
 */
enum class PasskeyStartStatus {
    /** Challenge ottenuta: si puo' aprire il prompt biometrico. */
    READY,

    /** Niente passkey utilizzabile per questa email. Si va di password. */
    UNAVAILABLE,

    /** Server irraggiungibile o in errore. Si va di password. */
    NETWORK_ERROR,
}

data class PasskeyStartResult(
    val status: PasskeyStartStatus,
    val challenge: PasskeyChallenge? = null,
)

/**
 * Esito della verifica dell'assertion.
 *
 * I tre esiti negativi vanno gestiti diversamente dalla UI, ed e' l'unica
 * ragione per cui sono distinti: [CHALLENGE_EXPIRED] merita un secondo giro di
 * begin + prompt (l'utente ha solo esitato troppo), [REJECTED] significa che la
 * passkey non e' piu' valida e va anche azzerato il marker locale del prompt di
 * attivazione, [NETWORK_ERROR] e' transitorio.
 */
enum class PasskeyLoginStatus {
    SUCCESS,

    /** Challenge scaduta: rifare begin e riproporre il prompt, una volta sola. */
    CHALLENGE_EXPIRED,

    /** Assertion rifiutata, o passkey revocata lato server. */
    REJECTED,

    /** Utente o passkey non piu' utilizzabili: come UNAVAILABLE sul begin. */
    UNAVAILABLE,

    NETWORK_ERROR,
}

data class PasskeyLoginResult(
    val status: PasskeyLoginStatus,
    val user: UserModel? = null,
)
