package it.mensa.app.support

import it.mensa.shared.api.ApiConfig

/**
 * FilesUrl — builds PocketBase file URLs.
 *
 * Android equivalent of iOS FilesURL.swift.
 * Pattern: `{BASE_URL}/api/files/{collection}/{recordId}/{filename}`
 */
object FilesUrl {

    /**
     * Build a fully-qualified URL for a PocketBase file attachment.
     *
     * @param collection PocketBase collection name (e.g. "events")
     * @param recordId   the record's id field
     * @param filename   the filename stored in the record (e.g. "poster.jpg")
     * @param thumb      optional thumbnail size string (e.g. "400x0") — appended as ?thumb=
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
