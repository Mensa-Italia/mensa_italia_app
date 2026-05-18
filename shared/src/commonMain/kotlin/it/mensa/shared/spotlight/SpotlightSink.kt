package it.mensa.shared.spotlight

/**
 * Platform sink for system-level search indexing (CoreSpotlight on iOS,
 * AppSearch / similar on a future Android port).
 *
 * KMP is the single source of truth for **what** to index: it talks to the
 * server, snapshots the previous hashes from SQLDelight, computes the
 * per-member delta (new / data-only / image-changed / unchanged / removed),
 * downloads the image bytes for the ones that need a fresh thumbnail, and
 * hands ready-to-index [SpotlightMemberBlock]s to this sink in batches.
 *
 * The sink only has to:
 *  - translate the block into the platform's searchable-item type;
 *  - if [SpotlightMemberBlock.imageBytes] is non-null, resize/encode the
 *    thumbnail (Spotlight wants ~540×540 JPEG @0.7) and attach it. Cache the
 *    resized output keyed by `id` so a later "reuse image" block can re-attach
 *    it without a re-download;
 *  - if [SpotlightMemberBlock.imageBytes] is null and [reuseImage] is true,
 *    look up the cached resized thumbnail and attach it;
 *  - submit the batch to the platform index.
 *
 * No diff, no networking, no hash bookkeeping happens here.
 */
interface SpotlightSink {
    suspend fun indexMembers(batch: List<SpotlightMemberBlock>)
    suspend fun deleteMembers(ids: List<String>)
    suspend fun clearAll()
}

/**
 * Late-binding holder. iOS registers an implementation in `iosAppApp.swift`
 * after `MensaSdk.doInitKoinIos()`. Android (today) leaves it null → KMP
 * skips the sync entirely.
 */
object SpotlightSinkRegistry {
    @kotlin.concurrent.Volatile
    var sink: SpotlightSink? = null
}
