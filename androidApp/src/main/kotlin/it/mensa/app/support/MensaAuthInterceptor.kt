package it.mensa.app.support

import it.mensa.shared.api.isMensaHost
import it.mensa.shared.auth.AuthHolder
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Mette l'`Authorization` sulle richieste dirette al nostro backend fatte con
 * un OkHttp nostro: le immagini (Coil) e l'audio dei podcast (ExoPlayer).
 *
 * Immagini e audio non passano da Ktor, quindi il Bearer che
 * [it.mensa.shared.api.AuthPlugin] aggiunge alle chiamate API non le tocca. Senza questo, ogni `/api/files/...` arriva al server come richiesta
 * anonima, e da anonima il server non produce piu' il link firmato verso S3
 * (vedi l'hook `OnFileDownloadRequest` di MensaCore): le immagini
 * continuerebbero ad arrivare, ma servite da PocketBase invece che da S3.
 *
 * Tre dettagli che contano:
 *
 *  - il token si legge DENTRO `intercept`, a ogni richiesta, non alla
 *    costruzione del client: l'ImageLoader e il player sono singleton che
 *    vivono quanto il processo, e catturare il token una volta sola vorrebbe dire tenersi
 *    quello di chi era loggato all'avvio;
 *  - `isMensaHost` e' la stessa regola del client Ktor, per la stessa ragione:
 *    lo stesso ImageLoader carica anche copertine da CDN esterni, e a un
 *    origine estranea il Bearer non va mandato mai — perde la sessione e in
 *    almeno un caso noto si becca un 403;
 *  - e' un `addInterceptor` (applicativo) e non un `addNetworkInterceptor`:
 *    il server risponde 307 verso S3, e OkHttp toglie da solo
 *    l'`Authorization` quando un redirect cambia host. Un interceptor di rete
 *    girerebbe di nuovo sulla richiesta verso S3 e ce lo rimetterebbe,
 *    spedendo il token della sessione a un dominio che non e' nostro.
 */
class MensaAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!isMensaHost(request.url.host)) return chain.proceed(request)
        val token = AuthHolder.token ?: return chain.proceed(request)
        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
    }
}
