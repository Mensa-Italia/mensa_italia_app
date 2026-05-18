package it.mensa.shared.spotlight

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import it.mensa.shared.api.SkipAuthAttribute
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.RegSociModel
import it.mensa.shared.repository.RegSociRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Orchestrates the Spotlight (or any platform search index) sync end-to-end.
 *
 * Pipeline:
 *   1. `regSoci.refreshAndDiff()` — fetch fresh list from API + snapshot the
 *      previous (id → dataHash, imageHash) map from SQLDelight, then upsert.
 *   2. Per-member action: Skip / ReuseImage / DownloadImage / NoImage.
 *   3. Push the "no download needed" set to the sink in batches first
 *      (cheap, gives the UI immediate progress).
 *   4. Concurrent image downloads (cap [maxConcurrentDownloads]); each
 *      completed download is appended to a pending batch and flushed at
 *      [downloadBatchSize] items.
 *   5. Issue a single deletion batch for ids that disappeared server-side.
 *
 * The sink does ZERO logic — see [SpotlightSink] kdoc.
 */
class SpotlightSyncEngine(
    private val regSoci: RegSociRepository,
    private val httpClient: HttpClient,
    private val db: MensaDatabase,
    private val baseUrl: String = "https://svc.mensa.it",
    /** Minimum time between auto-triggered syncs (host calls `syncMembersIfDue`). */
    private val throttleSeconds: Long = 60 * 60,
) {
    private val syncMutex = Mutex()

    /**
     * Read-only throttle gate. The host queries this before kicking off a
     * full refresh; if it returns false, the host should also skip the
     * sibling phases (e.g. docs) so the whole pipeline is gated atomically.
     *
     * The "last run" timestamp lives in SQLDelight (`KeyValue` table) and is
     * therefore automatically cleared by `wipeAllUserData()` on logout /
     * session-dead — the next login is always due regardless of clock.
     */
    suspend fun isDueForSync(): Boolean {
        val last = readLastRunEpochSeconds() ?: return true
        return Clock.System.now().epochSeconds - last >= throttleSeconds
    }

    /**
     * Runs the full member sync. Always proceeds (no throttle check) — the
     * gate is `isDueForSync`. On success, persists the "last run" timestamp
     * so subsequent throttle checks see it.
     */
    suspend fun syncMembers(
        expirationEpochSeconds: Long,
        ignoreCache: Boolean = false,
        onProgress: ((done: Int, total: Int) -> Unit)? = null,
    ): SpotlightMembersSyncReport? = syncMutex.withLock {
        val sink = SpotlightSinkRegistry.sink ?: return@withLock null
        val refresh = regSoci.refreshAndDiff()
        val members = refresh.members
        val previous = refresh.previousHashes
        val total = members.size

        // --- 1. Categorize ---
        val plans = members.map { m -> Plan(m, decide(m, previous[m.id], ignoreCache)) }

        // --- 2. Deletions ---
        val currentIds = members.mapTo(HashSet(members.size)) { it.id }
        val deletedIds = previous.keys.filterNot { it in currentIds }
        if (deletedIds.isNotEmpty()) sink.deleteMembers(deletedIds)

        // --- 3. Progress bookkeeping ---
        var done = 0
        val emit: suspend (Int) -> Unit = { delta ->
            done += delta
            onProgress?.invoke(done, total)
        }
        onProgress?.invoke(0, total)

        val skippedCount = plans.count { it.action == Action.Skip }
        if (skippedCount > 0) emit(skippedCount)

        // --- 4. Push "no download" set first (ReuseImage + NoImage). ---
        val noDownload = plans.filter { it.action == Action.ReuseImage || it.action == Action.NoImage }
        var reuseCount = 0
        var noImageCount = 0
        if (noDownload.isNotEmpty()) {
            noDownload.chunked(downloadBatchSize).forEach { chunk ->
                val blocks = chunk.map { p ->
                    val reuse = p.action == Action.ReuseImage
                    if (reuse) reuseCount++ else noImageCount++
                    buildBlock(p.member, expirationEpochSeconds, imageBytes = null, reuseImage = reuse)
                }
                sink.indexMembers(blocks)
                emit(chunk.size)
            }
        }

        // --- 5. Download + index for the changed-image set. ---
        val downloadPlans = plans.filter { it.action == Action.DownloadImage }
        var failures = 0
        if (downloadPlans.isNotEmpty()) {
            failures = downloadAndIndex(downloadPlans, expirationEpochSeconds, sink) { delta -> emit(delta) }
        }

        writeLastRunEpochSeconds(Clock.System.now().epochSeconds)

        SpotlightMembersSyncReport(
            total = total,
            skippedUnchanged = skippedCount,
            reIndexedDataOnly = reuseCount,
            downloadedImages = downloadPlans.size - failures,
            noImage = noImageCount,
            deletions = deletedIds.size,
            downloadFailures = failures,
        )
    }

    private suspend fun readLastRunEpochSeconds(): Long? =
        db.keyValueQueries.selectById(LAST_RUN_KEY).awaitAsOneOrNull()?.value_?.toLongOrNull()

    private suspend fun writeLastRunEpochSeconds(seconds: Long) {
        db.keyValueQueries.insertOrReplace(key = LAST_RUN_KEY, value_ = seconds.toString())
    }

    private suspend fun downloadAndIndex(
        plans: List<Plan>,
        expiration: Long,
        sink: SpotlightSink,
        progress: suspend (Int) -> Unit,
    ): Int {
        val semaphore = Semaphore(maxConcurrentDownloads)
        val pending = ArrayList<SpotlightMemberBlock>(downloadBatchSize)
        val pendingMutex = Mutex()
        var failures = 0
        val failuresMutex = Mutex()

        suspend fun flushIfFull() {
            val toFlush: List<SpotlightMemberBlock>? = pendingMutex.withLock {
                if (pending.size >= downloadBatchSize) {
                    val snap = pending.toList()
                    pending.clear()
                    snap
                } else null
            }
            if (toFlush != null) {
                sink.indexMembers(toFlush)
                progress(toFlush.size)
            }
        }

        coroutineScope {
            plans.forEach { plan ->
                launch {
                    semaphore.withPermit {
                        val bytes = downloadImage(plan.member)
                        val block = if (bytes == null) {
                            failuresMutex.withLock { failures++ }
                            // Index without image rather than dropping the
                            // member entirely — a transient 5xx shouldn't
                            // erase him from search.
                            buildBlock(plan.member, expiration, imageBytes = null, reuseImage = false)
                        } else {
                            buildBlock(plan.member, expiration, imageBytes = bytes, reuseImage = false)
                        }
                        pendingMutex.withLock { pending.add(block) }
                        flushIfFull()
                    }
                }
            }
        }

        // Tail flush.
        val tail = pendingMutex.withLock {
            val snap = pending.toList()
            pending.clear()
            snap
        }
        if (tail.isNotEmpty()) {
            sink.indexMembers(tail)
            progress(tail.size)
        }

        return failures
    }

    private suspend fun downloadImage(m: RegSociModel): ByteArray? {
        if (m.image.isEmpty() || m.image.contains(LEGACY_AVATAR)) return null
        val url = "$baseUrl/api/files/members_registry/${m.id}/${m.image}?thumb=$THUMB_PARAM"
        return runCatching {
            val resp: HttpResponse = httpClient.get(url) {
                attributes.put(SkipAuthAttribute, true)
                header(HttpHeaders.Authorization, null)
            }
            if (resp.status == HttpStatusCode.OK || resp.status.isSuccess()) resp.body<ByteArray>() else null
        }.getOrNull()
    }

    private fun decide(
        m: RegSociModel,
        prev: RegSociRepository.HashPair?,
        ignoreCache: Boolean,
    ): Action {
        val isValidImage = m.image.isNotEmpty() && !m.image.contains(LEGACY_AVATAR)
        if (ignoreCache) return if (isValidImage) Action.DownloadImage else Action.NoImage
        if (!isValidImage) return Action.NoImage
        val p = prev ?: return Action.DownloadImage  // brand new member
        val imageChanged = p.imageHash != m.imageHash
        val dataChanged = p.dataHash != m.dataHash
        return when {
            imageChanged -> Action.DownloadImage
            dataChanged -> Action.ReuseImage
            else -> Action.Skip
        }
    }

    private fun buildBlock(
        m: RegSociModel,
        expirationEpochSeconds: Long,
        imageBytes: ByteArray?,
        reuseImage: Boolean,
    ): SpotlightMemberBlock {
        val emails = extractEmails(m)
        val phones = extractPhones(m.fullData)
        return SpotlightMemberBlock(
            id = m.id,
            name = m.name,
            nameKeywords = nameKeywords(m.name),
            city = m.city,
            state = m.state,
            emails = emails,
            phones = phones,
            expirationEpochSeconds = expirationEpochSeconds,
            imageBytes = imageBytes,
            reuseImage = reuseImage,
        )
    }

    private data class Plan(val member: RegSociModel, val action: Action)
    private enum class Action { Skip, ReuseImage, DownloadImage, NoImage }

    companion object {
        private const val LEGACY_AVATAR = "cloud32.it/Associazioni/img/Uomo-1.png"
        private const val THUMB_PARAM = "0x100"
        private const val downloadBatchSize = 200
        private const val maxConcurrentDownloads = 8
        private const val LAST_RUN_KEY = "spotlight.lastSyncAt"
    }
}

// --- Extraction helpers (mirror iOS' toLite behavior) ---

private fun extractEmails(m: RegSociModel): List<String> {
    val raw = listOf(
        m.aliasMail,
        m.fullData.stringField("E-mail:"),
        m.fullData.stringField("PEC:"),
    ).mapNotNull { it?.stripMailto() }
    return raw.distinctBy { it.lowercase() }
}

private fun extractPhones(fullData: JsonObject): List<String> = listOf(
    fullData.stringField("Cellulare:"),
    fullData.stringField("Telefono:"),
).mapNotNull { it?.trim()?.takeIf { s -> s.isNotEmpty() } }.distinct()

private fun JsonObject.stringField(key: String): String? {
    val v = get(key) as? JsonPrimitive ?: return null
    val s = v.content.trim()
    return s.ifEmpty { null }
}

private fun String.stripMailto(): String? {
    val t = trim()
    if (t.isEmpty()) return null
    val noPrefix = if (t.startsWith("mailto:", ignoreCase = true)) t.removePrefix("mailto:").removePrefix("MAILTO:") else t
    val final = noPrefix.trim()
    return final.ifEmpty { null }
}

/**
 * Bag-of-words: utente cerca "Marco Montanari", record è "Montanari Marco" →
 * dobbiamo matchare. Esplicitiamo le permutazioni nei keyword. Cap 3 parole
 * = max 6 perms; sopra → solo originale + reverse.
 */
internal fun nameKeywords(name: String): List<String> {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return emptyList()
    val parts = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.size <= 1) return listOf(trimmed)
    if (parts.size > 3) return listOf(trimmed, parts.reversed().joinToString(" "))
    val out = mutableListOf<String>()
    permute(parts, emptyList(), out)
    return out
}

private fun permute(remaining: List<String>, current: List<String>, result: MutableList<String>) {
    if (remaining.isEmpty()) {
        result.add(current.joinToString(" "))
        return
    }
    for (i in remaining.indices) {
        val next = remaining.toMutableList()
        val elem = next.removeAt(i)
        permute(next, current + elem, result)
    }
}
