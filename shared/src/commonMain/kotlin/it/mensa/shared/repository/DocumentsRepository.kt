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

    /**
     * Il documento, dal database se c'e', altrimenti dal server.
     *
     * Leggeva solo il database, che si riempie quando si apre la lista. La
     * notifica di un documento nuovo arriva prima che la lista lo abbia mai
     * scaricato, quindi toccarla dava "Documento non disponibile"; aprirlo
     * prima dalla lista lo metteva in database e da li' in poi la notifica
     * funzionava. Lo hanno notato i soci. Null solo se manca anche sul server
     * o la rete non risponde.
     */
    suspend fun getById(id: String): DocumentModel? {
        db.documentQueries.selectById(id).awaitAsOneOrNull()?.let { return it.toModel() }
        // Solo se e' davvero quel record: un JSON diverso (un errore, una lista)
        // si decodifica comunque in un modello vuoto, con tutti i default.
        val remote = runCatching { api.get(id) }.getOrNull()?.takeIf { it.id == id } ?: return null
        db.documentQueries.insertOrReplace(
            id = remote.id,
            name = remote.name,
            description = remote.description,
            file_ = remote.file,
            uploadedBy = remote.uploadedBy,
            category = remote.category,
            elaborated = remote.elaborated,
            created = remote.created.toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
        return remote
    }

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
