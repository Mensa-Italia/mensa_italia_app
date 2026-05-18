package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.RegSociModel
import it.mensa.shared.model.withDefaultAvatarStripped

/**
 * Members registry collection.
 * Wiki API-Integration.md: collection `members_registry`, sort by name.
 */
class RegSociApi(private val pb: PocketBaseClient) {

    suspend fun list(
        filter: String? = null,
        sort: String = "name",
    ): List<RegSociModel> =
        pb.fullList<RegSociModel>("members_registry", filter = filter, sort = sort)
            .map { it.withDefaultAvatarStripped() }

    suspend fun get(id: String): RegSociModel =
        pb.getOne<RegSociModel>("members_registry", id).withDefaultAvatarStripped()

    /**
     * Server-side filtered search by name. Uses PocketBase `~` (like) operator.
     * The caller is responsible for offline-first fallback to local DB.
     */
    suspend fun searchByName(query: String, perPage: Int = 50): List<RegSociModel> {
        val safe = query.replace("'", "")
        return pb.list<RegSociModel>(
            collection = "members_registry",
            perPage = perPage,
            filter = "name ~ '$safe'",
            sort = "name",
        ).items.map { it.withDefaultAvatarStripped() }
    }
}
