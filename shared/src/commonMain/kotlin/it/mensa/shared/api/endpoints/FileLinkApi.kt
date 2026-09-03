package it.mensa.shared.api.endpoints

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Risposta di `/api/cs/file-link`.
 *
 * [signed] distingue un link S3 firmato, che scade, da un normale URL
 * `/api/files/...` restituito quando S3 e' spento (sviluppo locale). Chi
 * consuma il link non deve comportarsi diversamente nei due casi, ma tenerlo
 * da parte per riusarlo dopo si puo' fare solo nel secondo.
 */
@Serializable
data class FileLinkResponse(
    val url: String = "",
    @SerialName("expiresAt") val expiresAt: String? = null,
    val signed: Boolean = false,
)

/**
 * Ottiene un link scaricabile per un allegato, autenticando la richiesta.
 *
 * Serve dove il file non lo scarica l'app ma qualcun altro per suo conto: un
 * PDF aperto nel visualizzatore di sistema, un file passato al foglio di
 * condivisione, una pagina aperta nel browser. In quei casi la richiesta del
 * file esce senza il nostro header, e senza header il server non produce il
 * link firmato verso S3.
 *
 * Il giro e' quello descritto dal requisito: l'app manda l'header su QUESTA
 * chiamata, il server lo verifica, e solo se lo accetta produce il link
 * firmato e lo restituisce. Da li' in poi il link vale da solo, per il tempo
 * configurato lato server (`S3_PRESIGN_TTL_SECONDS`, cinque minuti di
 * default): va usato subito e non messo in cache.
 *
 * Il server controlla anche la view rule della collection con l'identita' di
 * chi chiama, quindi non e' una scorciatoia per leggere allegati di record che
 * l'utente non potrebbe vedere.
 */
class FileLinkApi(
    private val client: HttpClient,
) {
    suspend fun get(
        collection: String,
        recordId: String,
        filename: String,
        thumb: String? = null,
    ): FileLinkResponse =
        client.get("/api/cs/file-link") {
            parameter("collection", collection)
            parameter("record", recordId)
            parameter("file", filename)
            if (thumb != null) parameter("thumb", thumb)
        }.body()
}
