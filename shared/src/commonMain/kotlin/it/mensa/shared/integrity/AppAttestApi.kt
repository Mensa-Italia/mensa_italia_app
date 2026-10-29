package it.mensa.shared.integrity

/**
 * La porta verso Apple (`DCAppAttestService`), senza alcuna policy: tre chiamate
 * e la domanda "si puo'?". Sta in commonMain perche' il ciclo di vita della chiave
 * ([AppAttestLifecycle]) si prova con un doppione in JVM; l'implementazione
 * vera, in Kotlin/Native, e' `DcAppAttestApi` in appleMain e fa solo da
 * traduttore.
 *
 * Ogni errore nativo esce come [AppAttestNativeError], mai come altro.
 */
internal interface AppAttestApi {
    suspend fun isSupported(): Boolean

    /** Una chiave nuova nel Secure Enclave; ne restituisce il key id (base64 standard). */
    suspend fun generateKey(): String

    /** L'oggetto di attestazione CBOR di [keyId]. Vale una volta per chiave. */
    suspend fun attestKey(keyId: String, clientDataHash: ByteArray): ByteArray

    /** L'asserzione CBOR di [keyId] su [clientDataHash]. */
    suspend fun generateAssertion(keyId: String, clientDataHash: ByteArray): ByteArray
}

/**
 * Un errore di Apple com'e': dominio e codice dell'`NSError`, la sua descrizione
 * e quella dell'`NSUnderlyingErrorKey` (dominio/codice/descrizione), che e' dove
 * compare, per esempio, "Invalid appUUID". [domain] e [code] sono null solo se
 * Apple ha chiamato la callback senza dato e senza errore.
 *
 * Non e' una [IntegrityException]: decide [AppAttestLifecycle] cosa farne.
 */
internal class AppAttestNativeError(
    val domain: String?,
    val code: Int?,
    val detail: String,
    val underlying: String?,
) : Exception("$domain/$code: $detail")
