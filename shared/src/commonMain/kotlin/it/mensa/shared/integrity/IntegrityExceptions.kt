package it.mensa.shared.integrity

/**
 * Gli errori del livello di accesso "attenzionato" (`watched`) e dell'account
 * sospeso (`blocked`). Sono eccezioni tipizzate e non `ResponseException`
 * perche' la UI deve distinguerle da un errore HTTP qualunque: le solleva
 * l'interceptor, prima che la validazione del client trasformi il 403 o il
 * 428 in una `ClientRequestException` senza nome.
 *
 * Nessuna di queste e' una `IOException` di proposito: `NetworkErrors` le
 * leggerebbe come "offline", e la UI direbbe "Sei offline" a chi invece e'
 * stato sospeso o non ha un dispositivo che possa dimostrare la propria
 * integrita'.
 */
sealed class IntegrityException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 403 `account_blocked`: l'account e' sospeso. Nessun retry. Chi la intercetta
 * non deve fare logout ne' ripetere la richiesta: lo stato globale
 * [it.mensa.shared.auth.AccountStatus] e' gia' stato aggiornato e le due UI
 * mostrano la schermata di account sospeso.
 */
class AccountBlockedException(message: String = "Account sospeso") : IntegrityException(message)

/**
 * 403 `integrity_failed`: il server ha ricevuto la prova e non l'ha accettata
 * (verdetto negativo, firma errata, hash che non corrisponde...). Nessun retry:
 * ripeterla darebbe lo stesso esito.
 */
class IntegrityFailedException(message: String = "Verifica di integrita' non superata") : IntegrityException(message)

/**
 * Questo dispositivo non puo' produrre la prova: niente Play Integrity (Play
 * Store o Play Services assenti, progetto Cloud non configurato) o niente App
 * Attest (simulatore, watchOS, dispositivo non supportato). Non e' un guasto
 * transitorio, quindi non si riprova.
 *
 * [failure] e' la causa nativa quando la conosciamo (stage, dominio, codice):
 * la UI ne mostra il [ProofFailure.tag].
 */
class IntegrityUnsupportedException(
    message: String,
    cause: Throwable? = null,
    val failure: ProofFailure? = null,
) : IntegrityException(message, cause)

/**
 * La prova non si e' potuta ottenere *adesso*: rete caduta durante la sfida,
 * troppe richieste a Play Integrity, Apple che non risponde, il server che
 * rifiuta di nuovo dopo l'unico retry concesso. Riprovando piu' tardi puo'
 * andare: [IntegrityGuard] la ricorda e non riprova prima del backoff.
 *
 * [failure] come in [IntegrityUnsupportedException].
 */
class IntegrityUnavailableException(
    message: String,
    cause: Throwable? = null,
    val failure: ProofFailure? = null,
) : IntegrityException(message, cause)

/**
 * Solo App Attest: la chiave [keyId] non vale piu' (un'asserzione e' fallita su
 * una chiave abbastanza vecchia da non poter essere il dispositivo il problema).
 * [failure] e' l'errore ORIGINALE di Apple. La gestisce [IntegrityGuard]
 * scartando [keyId] e registrandone una nuova, una volta sola; non arriva mai
 * ai chiamanti.
 */
class IntegrityKeyInvalidException(
    val keyId: String,
    val failure: ProofFailure?,
    cause: Throwable? = null,
) : IntegrityException("App Attest: chiave non valida", cause)
