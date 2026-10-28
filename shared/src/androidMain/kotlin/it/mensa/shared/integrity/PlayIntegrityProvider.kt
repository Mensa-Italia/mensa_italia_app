package it.mensa.shared.integrity

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Play Integrity, **Standard API**: la prova per-richiesta su Android.
 *
 * Il [StandardIntegrityTokenProvider] si prepara una volta sola (e' la parte
 * lenta: Google scalda il verdetto sul dispositivo) e si riusa per ogni token.
 * La preparazione e' pigra, alla prima prova, e si rifa se Google dice che il
 * provider non vale piu' (`INTEGRITY_TOKEN_PROVIDER_INVALID`, succede dopo ore
 * di uso o a un aggiornamento di Play Services): in quel caso la richiesta del
 * token viene ripetuta una volta con un provider nuovo.
 *
 * Il token e' legato alla richiesta tramite `requestHash` =
 * base64url-senza-padding(SHA-256(`challenge|METHOD|path`)); il server lo
 * ricalcola e lo confronta (vedi [IntegrityClientData]).
 *
 * [cloudProjectNumber] e' il numero del progetto Google Cloud collegato all'app
 * in Play Console; lo imposta `PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER` in
 * `androidApp/build.gradle.kts`. Se e' 0 (non configurato) il dispositivo risulta
 * "non supportato" e la prova non si tenta nemmeno: Google risponderebbe
 * `CLOUD_PROJECT_NUMBER_IS_INVALID` a ogni richiesta.
 */
class PlayIntegrityProvider(
    context: Context,
    private val cloudProjectNumber: Long,
) : IntegrityProvider {

    private val manager: StandardIntegrityManager =
        IntegrityManagerFactory.createStandard(context.applicationContext)

    private val prepareLock = Mutex()
    private var tokenProvider: StandardIntegrityTokenProvider? = null

    override val platform: String = "android"

    override suspend fun isSupported(): Boolean = cloudProjectNumber > 0

    override suspend fun prove(userId: String, challenge: String, clientData: String): Map<String, String> {
        val requestHash = IntegrityClientData.requestHash(clientData)
        val token = try {
            requestToken(requestHash)
        } catch (e: StandardIntegrityException) {
            if (e.errorCode != StandardIntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID) throw map(e)
            // Provider scaduto: se ne prepara uno nuovo e si ritenta una volta.
            invalidate()
            try {
                requestToken(requestHash)
            } catch (again: StandardIntegrityException) {
                throw map(again)
            }
        }
        return mapOf(IntegrityHeaders.TOKEN to token)
    }

    private suspend fun requestToken(requestHash: String): String {
        val provider = provider()
        val request = StandardIntegrityTokenRequest.builder().setRequestHash(requestHash).build()
        val result: StandardIntegrityToken = try {
            provider.request(request).await()
        } catch (e: StandardIntegrityException) {
            throw e
        } catch (t: Throwable) {
            throw IntegrityUnavailableException("Play Integrity: ${t.message}", t)
        }
        return result.token()
    }

    /** Il provider in uso; lo prepara alla prima chiamata. Un solo prepare alla volta. */
    private suspend fun provider(): StandardIntegrityTokenProvider = prepareLock.withLock {
        tokenProvider?.let { return@withLock it }
        val prepared = try {
            manager.prepareIntegrityToken(
                PrepareIntegrityTokenRequest.builder().setCloudProjectNumber(cloudProjectNumber).build()
            ).await()
        } catch (e: StandardIntegrityException) {
            throw map(e)
        } catch (t: Throwable) {
            throw IntegrityUnavailableException("Play Integrity: ${t.message}", t)
        }
        tokenProvider = prepared
        prepared
    }

    private suspend fun invalidate() = prepareLock.withLock { tokenProvider = null }

    /**
     * Gli errori di Play Integrity in due famiglie: quelli che dipendono dal
     * dispositivo (nessun Play Store o Play Services, app non installata da Play,
     * progetto Cloud sbagliato), per cui riprovare e' inutile; e quelli
     * passeggeri (rete, troppe richieste, errore di Google).
     */
    private fun map(e: StandardIntegrityException): IntegrityException = when (e.errorCode) {
        StandardIntegrityErrorCode.API_NOT_AVAILABLE,
        StandardIntegrityErrorCode.PLAY_STORE_NOT_FOUND,
        StandardIntegrityErrorCode.APP_NOT_INSTALLED,
        StandardIntegrityErrorCode.PLAY_SERVICES_NOT_FOUND,
        StandardIntegrityErrorCode.APP_UID_MISMATCH,
        StandardIntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID ->
            IntegrityUnsupportedException("Play Integrity non disponibile (codice ${e.errorCode})", e)
        else -> IntegrityUnavailableException("Play Integrity non ha risposto (codice ${e.errorCode})", e)
    }
}

/** I `Task` di Play Services come funzioni sospese, senza aggiungere la libreria di interop. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
