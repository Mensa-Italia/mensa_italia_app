package it.mensa.shared.tableport

/**
 * Legge il contenuto del QR di un francobollo.
 *
 * Ci sono due forme in circolazione, e vanno accettate entrambe:
 *
 *  - `<stampId>:::<codice>` — la forma "nuda", quella che questo parser
 *    supponeva fosse l'unica;
 *  - `https://svc.mensa.it/links/stamp/<stampId>:::<codice>` — quella che il
 *    backend genera davvero (`main/hooks/events_notify_users.go`) e che sta
 *    stampata su ogni QR gia' mandato per mail.
 *
 * Prima si prendeva tutto cio' che stava prima di `:::` come id, quindi sulla
 * seconda forma l'id diventava l'URL intero e il timbro non lo trovava piu'
 * nessuno. I QR gia' distribuiti non si possono richiamare indietro: la forma
 * con l'URL va letta, non corretta a monte.
 */
data class StampQRPayload(
    val stampId: String,
    val verificationCode: String,
)

object StampQRParser {
    private const val SEPARATOR = ":::"

    /**
     * Torna il payload, oppure `null` se la stringa non contiene due parti
     * separate da `:::` e non vuote.
     */
    fun parse(raw: String): StampQRPayload? {
        val parts = raw.trim().split(SEPARATOR)
        if (parts.size < 2) return null

        // `substringAfterLast('/')` regge entrambe le forme: su un id nudo non
        // c'e' nessuna barra e la stringa torna com'e'. Gli id di PocketBase
        // sono 15 caratteri alfanumerici, una barra non ce la possono avere.
        val stampId = parts[0].trim().substringAfterLast('/')

        // Il codice e' l'ultima cosa nell'URL, ma una query o un fragment ci
        // si possono attaccare — `?qrcode` lo aggiunge il backend stesso su
        // /links/stamp. Senza questo taglio il codice non combacia piu'.
        val verificationCode = parts[1].trim()
            .substringBefore('?')
            .substringBefore('#')
            .trim()

        if (stampId.isEmpty() || verificationCode.isEmpty()) return null
        return StampQRPayload(stampId = stampId, verificationCode = verificationCode)
    }
}
