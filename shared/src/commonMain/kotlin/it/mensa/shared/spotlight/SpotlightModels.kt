package it.mensa.shared.spotlight

/**
 * One member item ready to be pushed to the platform index.
 *
 * Image semantics:
 *  - [imageBytes] non-null  ⇒ fresh download, sink must resize+cache+attach.
 *  - [imageBytes] null + [reuseImage] true  ⇒ data changed but image is
 *    identical to the previously-indexed one; sink should re-emit the cached
 *    resized thumbnail (no network, no re-resize) attached to the new attrs.
 *  - [imageBytes] null + [reuseImage] false ⇒ no image at all (empty/legacy
 *    avatar). Index the item without thumbnail.
 *
 * Members whose hashes are identical to the previous run are NOT emitted —
 * the engine skips them entirely.
 */
data class SpotlightMemberBlock(
    val id: String,
    val name: String,
    /** Search-friendly permutations of [name] (max 6, see engine). */
    val nameKeywords: List<String>,
    val city: String,
    val state: String,
    val emails: List<String>,
    val phones: List<String>,
    val expirationEpochSeconds: Long,
    val imageBytes: ByteArray?,
    val reuseImage: Boolean,
) {
    // ByteArray equality semantics: data classes use referential equality on
    // arrays. Sinks should not depend on equals/hashCode for this type.
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = id.hashCode()
}

/** Diagnostic counters returned by [SpotlightSyncEngine.syncMembers]. */
data class SpotlightMembersSyncReport(
    val total: Int,
    val skippedUnchanged: Int,
    val reIndexedDataOnly: Int,
    val downloadedImages: Int,
    val noImage: Int,
    val deletions: Int,
    val downloadFailures: Int,
)
