package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.SigModel

class SigsApi(internal val pb: PocketBaseClient) {

    /**
     * Flutter default: sort=name, no expand.
     */
    suspend fun list(
        filter: String? = null,
        sort: String = "name"
    ): List<SigModel> =
        pb.fullList("sigs", filter = filter, sort = sort)

    suspend fun get(id: String): SigModel =
        pb.getOne("sigs", id)

    suspend fun create(body: SigModel): SigModel =
        pb.create("sigs", body)

    suspend fun update(id: String, body: SigModel): SigModel =
        pb.update("sigs", id, body)

    suspend fun delete(id: String) =
        pb.delete("sigs", id)
}
