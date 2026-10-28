package it.mensa.shared.integrity

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.DeviceCheck.DCAppAttestService
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * App Attest (`DCAppAttestService`): la prova per-richiesta su iOS.
 *
 * Scritto in Kotlin/Native e non in Swift perche' le due chiamate che servono
 * (`attestKey`, `generateAssertion`) sono quello che gira dentro il flusso
 * sospeso di [IntegrityGuard]: farlo passare da Swift vorrebbe dire un'interfaccia
 * di callback in piu' da iniettare per niente. Lo stato (il key id) sta nello
 * stesso `Settings` del Keychain che tiene gia' sessione e credenziali, quindi
 * prende anche l'indurimento `ThisDeviceOnly` di `KeychainHardening`.
 *
 * Funziona cosi':
 *  - **una chiave per utente**: [saveKey] la salva sotto l'id utente, perche' il
 *    server lega la chiave all'account; un altro utente sullo stesso telefono
 *    ne registra una sua;
 *  - **registrazione** ([attest]): `generateKey` + `attestKey` con
 *    `clientDataHash = SHA256(challenge)`. Il key id si salva solo dopo che il
 *    server ha accettato l'attestazione;
 *  - **prova** ([prove]): `generateAssertion` con `clientDataHash =
 *    SHA256(challenge|METHOD|path)`;
 *  - **chiave invalida** (`DCError.invalidKey`: app reinstallata — il Keychain
 *    sopravvive, la chiave nel Secure Enclave no — o ripristino su un altro
 *    telefono): [IntegrityKeyInvalidException], che [IntegrityGuard] risolve
 *    registrando una chiave nuova.
 *
 * Su simulatore, su watchOS e su dispositivi senza Secure Enclave
 * `isSupported` e' false e la prova non si tenta.
 *
 * Il key id e' la stringa che restituisce Apple, cioe' gia' base64 standard
 * dell'hash a 32 byte della chiave pubblica: va al server cosi' com'e', sia nel
 * body dell'attestazione sia in `X-Integrity-Key-Id`.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class, ExperimentalEncodingApi::class)
class AppAttestProvider(private val settings: Settings) : IntegrityProvider {

    private val service: DCAppAttestService get() = DCAppAttestService.sharedService

    override val platform: String = "ios"

    override val requiresKeyEnrollment: Boolean = true

    override suspend fun isSupported(): Boolean = service.isSupported()

    override suspend fun hasKey(userId: String): Boolean = storedKey(userId) != null

    override suspend fun attest(challenge: String): KeyAttestation {
        val keyId = generateKey()
        val hash = IntegrityClientData.clientDataHash(challenge).toNSData()
        val attestation = suspendCancellableCoroutine<NSData> { cont ->
            service.attestKey(keyId, clientDataHash = hash) { data, error ->
                if (data != null) cont.resume(data) else cont.resumeWithException(map(error, forAssertion = false))
            }
        }
        return KeyAttestation(keyId = keyId, attestation = Base64.encode(attestation.toByteArray()))
    }

    override suspend fun saveKey(userId: String, keyId: String) {
        settings[keyName(userId)] = keyId
    }

    override suspend fun forgetKey(userId: String) {
        runCatching { settings.remove(keyName(userId)) }
    }

    override suspend fun prove(userId: String, challenge: String, clientData: String): Map<String, String> {
        val keyId = storedKey(userId) ?: throw IntegrityKeyInvalidException("Nessuna chiave App Attest per questo utente")
        val hash = IntegrityClientData.clientDataHash(clientData).toNSData()
        val assertion = suspendCancellableCoroutine<NSData> { cont ->
            service.generateAssertion(keyId, clientDataHash = hash) { data, error ->
                if (data != null) cont.resume(data) else cont.resumeWithException(map(error, forAssertion = true))
            }
        }
        return mapOf(
            IntegrityHeaders.KEY_ID to keyId,
            IntegrityHeaders.ASSERTION to Base64.encode(assertion.toByteArray()),
        )
    }

    private suspend fun generateKey(): String = suspendCancellableCoroutine { cont ->
        service.generateKeyWithCompletionHandler { keyId, error ->
            if (keyId != null) cont.resume(keyId) else cont.resumeWithException(map(error, forAssertion = false))
        }
    }

    private fun storedKey(userId: String): String? =
        runCatching { settings.getStringOrNull(keyName(userId)) }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun keyName(userId: String) = "integrity.appattest.key.$userId"

    /**
     * Gli errori di `DCError` (dominio `com.apple.devicecheck.error`): 1 =
     * funzione non supportata, 3 = chiave invalida, 4 = server di Apple non
     * raggiungibile, 0 = errore di sistema. Solo il 3 su un'asserzione e' una
     * chiave da rifare.
     */
    private fun map(error: NSError?, forAssertion: Boolean): IntegrityException {
        val code = error?.code?.toInt()
        val detail = error?.localizedDescription ?: "errore sconosciuto"
        return when {
            code == DC_ERROR_INVALID_KEY && forAssertion ->
                IntegrityKeyInvalidException("App Attest: chiave non valida", null)
            code == DC_ERROR_FEATURE_UNSUPPORTED ->
                IntegrityUnsupportedException("App Attest non supportato su questo dispositivo: $detail")
            else -> IntegrityUnavailableException("App Attest: $detail")
        }
    }

    private companion object {
        const val DC_ERROR_FEATURE_UNSUPPORTED = 1
        const val DC_ERROR_INVALID_KEY = 3
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.convert()) }

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun NSData.toByteArray(): ByteArray {
    val out = ByteArray(length.toInt())
    if (out.isNotEmpty()) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}
