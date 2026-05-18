package it.mensa.shared.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import it.mensa.shared.api.FilePart
import it.mensa.shared.api.endpoints.SigsApi
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.SigModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

/**
 * Draft used to create or update a SIG. Mirrors the Flutter
 * `addSig` / `updateSig` shape so iOS callers can round-trip identical data
 * through the Kotlin shared module.
 *
 * `groupType` must be one of:
 *   sig_facebook, sig, local, chat_whatsapp, chat_telegram, chat
 *
 * `description` is pass-through: Flutter omits it today, but the field exists
 * on `SigModel` so we keep it available for the backoffice.
 */
data class SigDraft(
    val name: String,
    val link: String,
    val groupType: String,
    val description: String = "",
    val imageBytes: ByteArray? = null,
    val imageFilename: String? = null,
    val imageContentType: String? = "image/jpeg",
)

class SigsRepository(
    private val api: SigsApi,
    private val db: MensaDatabase,
    @Suppress("UNUSED_PARAMETER") private val json: Json
) {
    fun observeAll(): Flow<List<SigModel>> =
        db.sigQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel() } }

    suspend fun refresh(filter: String? = null, sort: String = "name") {
        val items = api.list(filter = filter, sort = sort)
        db.transaction {
            db.sigQueries.deleteAll()
            items.forEach { s -> upsertRow(s) }
        }
    }

    suspend fun getById(id: String): SigModel? {
        val row = db.sigQueries.selectById(id).awaitAsOneOrNull() ?: return null
        return row.toModel()
    }

    /** Create a SIG with an optional cover image. */
    suspend fun create(draft: SigDraft): SigModel {
        val fields = baseFields(draft)
        val files = imageFiles(draft)
        val created: SigModel = api.pb.createMultipart("sigs", fields, files)
        upsertRow(created)
        return created
    }

    /** Update a SIG. Image is sent only if [SigDraft.imageBytes] is non-null. */
    suspend fun update(id: String, draft: SigDraft): SigModel {
        val fields = baseFields(draft)
        val files = imageFiles(draft)
        val updated: SigModel = api.pb.updateMultipart("sigs", id, fields, files)
        upsertRow(updated)
        return updated
    }

    suspend fun delete(id: String) {
        api.delete(id)
        db.sigQueries.deleteById(id)
    }

    // --- helpers -------------------------------------------------------------

    private fun baseFields(draft: SigDraft): Map<String, Any?> {
        val map = linkedMapOf<String, Any?>(
            "name" to draft.name,
            "link" to draft.link,
            "group_type" to draft.groupType,
        )
        if (draft.description.isNotBlank()) map["description"] = draft.description
        return map
    }

    private fun imageFiles(draft: SigDraft): List<FilePart> {
        val bytes = draft.imageBytes ?: return emptyList()
        val filename = draft.imageFilename ?: "image.jpg"
        val contentType = draft.imageContentType ?: "image/jpeg"
        return listOf(FilePart("image", filename, contentType, bytes))
    }

    private suspend fun upsertRow(s: SigModel) {
        db.sigQueries.insertOrReplace(
            id = s.id,
            name = s.name,
            description = s.description,
            image = s.image,
            link = s.link,
            groupType = s.groupType,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
    }
}
