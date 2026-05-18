package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.DocumentElaboratedModel
import it.mensa.shared.model.DocumentModel

class DocumentsApi(private val pb: PocketBaseClient) {

    suspend fun list(): List<DocumentModel> =
        pb.fullList("documents", sort = "-created")

    suspend fun get(id: String): DocumentModel =
        pb.getOne("documents", id)

    /**
     * Fetches elaborated (AI-summarised) document data from collection documents_elaborated.
     * The [id] is the ID of the documents_elaborated record (not the document itself).
     */
    suspend fun getElaboratedData(id: String): DocumentElaboratedModel =
        pb.getOne("documents_elaborated", id)
}
