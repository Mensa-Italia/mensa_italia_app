package it.mensa.app.support

import it.mensa.shared.api.ApiConfig

/**
 * FilesUrl — builds PocketBase file URLs.
 *
 * Android equivalent of iOS FilesURL.swift.
 * Pattern: `{BASE_URL}/api/files/{collection}/{recordId}/{filename}`
 *
 * ## Le misure ammesse per `thumb`
 *
 * PocketBase genera SOLO le misure dichiarate nel campo file: una misura non
 * dichiarata ricade sull'originale con `200 OK`, in silenzio. L'app ne chiedeva
 * sedici diverse e il server ne serviva una, quindi ogni card scaricava
 * l'originale — misurato: 282 KB per un riquadro da 350dp.
 *
 * Da qui in poi si usano solo queste cinque, e vanno dichiarate lato server:
 *
 * | misura     | a cosa serve                                    |
 * |------------|-------------------------------------------------|
 * | `200x200`  | avatar e loghi, ritagliati quadrati             |
 * | `0x500`    | avatar grande del profilo                       |
 * | `0x100`    | miniature per l'indice Spotlight                |
 * | `800x0`    | copertine in lista                              |
 * | `1200x0`   | hero delle schermate di dettaglio               |
 *
 * Le due larghezze fisse finiscono per zero di proposito: `Wx0` lascia
 * decidere l'altezza al server e **conserva il rapporto dell'originale**. Le
 * misure a rapporto fisso che c'erano prima (`800x450`, `1000x600`, `600x400`)
 * ritagliavano la copertina, e le card che ora si dimensionano sul rapporto
 * vero avrebbero letto sempre e solo il rapporto del ritaglio.
 */
object FilesUrl {

    /**
     * Build a fully-qualified URL for a PocketBase file attachment.
     *
     * @param collection PocketBase collection name (e.g. "events")
     * @param recordId   the record's id field
     * @param filename   the filename stored in the record (e.g. "poster.jpg")
     * @param thumb      optional thumbnail size string (e.g. "800x0") — appended as ?thumb=
     */
    fun build(
        collection: String,
        recordId: String,
        filename: String,
        thumb: String? = null,
    ): String {
        val base = "${ApiConfig.BASE_URL}/api/files/$collection/$recordId/$filename"
        return if (thumb != null) "$base?thumb=$thumb" else base
    }
}
