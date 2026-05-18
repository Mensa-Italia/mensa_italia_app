package it.mensa.shared.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import it.mensa.shared.api.endpoints.DocumentsApi
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.DocumentElaboratedModel
import it.mensa.shared.model.DocumentModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class DocumentsRepository(
    private val api: DocumentsApi,
    private val db: MensaDatabase,
) {
    fun observeAll(): Flow<List<DocumentModel>> =
        db.documentQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel() } }

    fun observeOne(id: String): Flow<DocumentModel?> =
        db.documentQueries.selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toModel() }

    suspend fun refresh() {
        val items = api.list()
        val now = Clock.System.now().toEpochMilliseconds()
        db.transaction {
            db.documentQueries.deleteAll()
            items.forEach { d ->
                db.documentQueries.insertOrReplace(
                    id = d.id,
                    name = d.name,
                    description = d.description,
                    file_ = d.file,
                    uploadedBy = d.uploadedBy,
                    category = d.category,
                    elaborated = d.elaborated,
                    created = d.created.toEpochMilliseconds(),
                    updatedAt = now,
                )
            }
        }
    }

    suspend fun firstSnapshot(): List<DocumentModel> = observeAll().first()

    suspend fun getById(id: String): DocumentModel? =
        db.documentQueries.selectById(id).awaitAsOneOrNull()?.toModel()

    /**
     * Loads the AI-elaborated summary for a document, caching it locally.
     */
    suspend fun getElaborated(elaboratedId: String): DocumentElaboratedModel? {
        if (elaboratedId.isEmpty()) return null
        val cached = db.documentElaboratedQueries.selectById(elaboratedId).awaitAsOneOrNull()
        val remote = runCatching { api.getElaboratedData(elaboratedId) }.getOrNull()
        if (remote != null) {
            db.documentElaboratedQueries.insertOrReplace(
                id = remote.id,
                document = remote.document,
                iaResume = remote.iaResume,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            )
            return remote
        }
        return cached?.toModel()
    }
}
