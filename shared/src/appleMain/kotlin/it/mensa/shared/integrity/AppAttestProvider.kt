package it.mensa.shared.integrity

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.DeviceCheck.DCAppAttestService
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUnderlyingErrorKey
import platform.Foundation.create
import platform.Foundation.iOSAppOnMac
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.OsFamily
import kotlin.native.Platform

/**
 * App Attest (`DCAppAttestService`): la prova per-richiesta su iOS.
 *
 * Qui non c'e' piu' nessuna decisione. Cosa sia una chiave, dove si salvi, quando
 * vada rifatta e come si classifichino gli errori di Apple sta in commonMain
 * ([AppAttestLifecycle], [classify]) dove si prova in JVM; questo file e' la
 * traduzione tra Kotlin e Apple:
 *
 *  - [DcAppAttestApi] fa le tre chiamate a `DCAppAttestService` e trasforma ogni
 *    `NSError` in un [AppAttestNativeError] (dominio, codice, descrizione e
 *    `NSUnderlyingErrorKey`, che e' dove comparirebbe "Invalid appUUID");
 *  - [AppAttestProvider] implementa [IntegrityProvider] delegando al ciclo di vita.
 *
 * Scritto in Kotlin/Native e non in Swift perche' le chiamate che servono
 * (`attestKey`, `generateAssertion`) sono quello che gira dentro il flusso
 * sospeso di [IntegrityGuard]: farlo passare da Swift vorrebbe dire un'interfaccia
 * di callback in piu' da iniettare per niente.
 *
 * Su simulatore, su watchOS e su dispositivi senza Secure Enclave
 * `isSupported` e' false e la prova non si tenta.
 *
 * Il key id e' la stringa che restituisce Apple, cioe' gia' base64 standard
 * dell'hash a 32 byte della chiave pubblica: va al server cosi' com'e', sia nel
 * body dell'attestazione sia in `X-Integrity-Key-Id`.
 */
internal class AppAttestProvider(
    private val lifecycle: AppAttestLifecycle,
    private val environment: DeviceEnvironment,
) : IntegrityProvider {

    override val platform: String = PLATFORM_IOS

    override suspend fun isSupported(): Boolean = lifecycle.isSupported()

    override suspend fun ensureEnrolled(userId: String, server: EnrollmentServer) =
        lifecycle.ensureEnrolled(userId, server)

    override suspend fun discardKey(userId: String, keyId: String?) = lifecycle.discardKey(userId, keyId)

    override suspend fun prove(userId: String, challenge: String, clientData: String): Map<String, String> =
        lifecycle.prove(userId, clientData)

    override fun environment(): DeviceEnvironment = environment
}

/** Le chiamate a `DCAppAttestService`, una per una, senza policy. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class DcAppAttestApi : AppAttestApi {

    private val service: DCAppAttestService get() = DCAppAttestService.sharedService

    override suspend fun isSupported(): Boolean = service.isSupported()

    override suspend fun generateKey(): String = suspendCancellableCoroutine { cont ->
        service.generateKeyWithCompletionHandler { keyId, error ->
            if (keyId != null) cont.resume(keyId) else cont.resumeWithException(error.toNativeError())
        }
    }

    override suspend fun attestKey(keyId: String, clientDataHash: ByteArray): ByteArray {
        val hash = clientDataHash.toNSData()
        val attestation = suspendCancellableCoroutine<NSData> { cont ->
            service.attestKey(keyId, clientDataHash = hash) { data, error ->
                if (data != null) cont.resume(data) else cont.resumeWithException(error.toNativeError())
            }
        }
        return attestation.toByteArray()
    }

    override suspend fun generateAssertion(keyId: String, clientDataHash: ByteArray): ByteArray {
        val hash = clientDataHash.toNSData()
        val assertion = suspendCancellableCoroutine<NSData> { cont ->
            service.generateAssertion(keyId, clientDataHash = hash) { data, error ->
                if (data != null) cont.resume(data) else cont.resumeWithException(error.toNativeError())
            }
        }
        return assertion.toByteArray()
    }
}

/**
 * L'`NSError` di Apple com'e': dominio, codice, `localizedDescription` e
 * l'errore sotto (`NSUnderlyingErrorKey`) come `dominio/codice: descrizione`.
 * Un errore nullo (callback senza dato e senza errore) e' "errore sconosciuto".
 */
internal fun NSError?.toNativeError(): AppAttestNativeError {
    if (this == null) return AppAttestNativeError(domain = null, code = null, detail = "errore sconosciuto", underlying = null)
    val inner = userInfo[NSUnderlyingErrorKey] as? NSError
    return AppAttestNativeError(
        domain = domain,
        code = code.toInt(),
        detail = localizedDescription,
        underlying = inner?.let { "${it.domain}/${it.code}: ${it.localizedDescription}" },
    )
}

/**
 * Il dispositivo per il report: build dell'app, sistema (`iOS 27.0.1 (24A446)`)
 * e se e' un'app per iPhone che gira su un Mac con Apple silicon.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
internal fun appleDeviceEnvironment(): DeviceEnvironment {
    val info = NSProcessInfo.processInfo
    val app = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: ""
    val name = when (Platform.osFamily) {
        OsFamily.IOS -> "iOS"
        OsFamily.WATCHOS -> "watchOS"
        else -> "Apple"
    }
    val version = info.operatingSystemVersion.useContents { "$majorVersion.$minorVersion.$patchVersion" }
    // "Version 27.0.1 (Build 24A446)": della build interessa solo il codice.
    val build = Regex("""\((?:Build )?([^)]+)\)""").find(info.operatingSystemVersionString)?.groupValues?.get(1)
    return DeviceEnvironment(
        app = app,
        os = if (build != null) "$name $version ($build)" else "$name $version",
        iosAppOnMac = info.iOSAppOnMac,
    )
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
