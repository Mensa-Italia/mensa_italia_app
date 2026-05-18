package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.BoutiqueModel

class BoutiqueApi(private val pb: PocketBaseClient) {

    suspend fun list(): List<BoutiqueModel> =
        pb.fullList("boutique", sort = "name")

    suspend fun get(id: String): BoutiqueModel =
        pb.getOne("boutique", id)
}
