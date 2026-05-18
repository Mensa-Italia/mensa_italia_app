package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.OrgChartGroup
import it.mensa.shared.model.OrgChartMember
import it.mensa.shared.model.OrgChartModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Organigramma — backed by PocketBase.
 *
 * Endpoints:
 *  - `GET /api/collections/org_chart_groups/records?sort=order`
 *  - `GET /api/collections/org_chart_members/records?filter=group="GID"&expand=user&sort=order`
 *
 * Schema change history: il campo `org_chart_members.user` puntava in
 * origine alla collection `users` (PB auth). Da quel record si poteva tirare
 * solo l'id e il nome, e per ottenere foto + dati ricchi si faceva una
 * seconda fetch su `members_registry` (id condiviso). Oggi la relation punta
 * direttamente a `members_registry`, quindi un singolo `expand=user`
 * restituisce già nome e foto — la seconda chiamata è stata eliminata.
 */
class OrgChartApi(private val pb: PocketBaseClient) {

    suspend fun get(): OrgChartModel = coroutineScope {
        // 1) Tutti i gruppi.
        val groups = pb.fullList<OrgChartGroupRecord>(
            collection = "org_chart_groups",
            sort = "order"
        )

        // 2) Membri di ogni gruppo in parallelo, con expand=user → ora
        //    members_registry, che già porta nome e foto.
        val groupedMembers = groups.map { g ->
            async {
                g to pb.fullList<OrgChartMemberRecord>(
                    collection = "org_chart_members",
                    filter = "group=\"${g.id}\"",
                    sort = "order",
                    expand = "user"
                )
            }
        }.map { it.await() }

        // 3) Mappa al modello finale leggendo direttamente dall'expand.
        val groupModels = groupedMembers.map { (g, members) ->
            OrgChartGroup(
                id = g.id,
                title = g.title,
                members = members.map { m ->
                    val reg = m.expand?.user
                    OrgChartMember(
                        userId = m.user,
                        name = reg?.name.orEmpty(),
                        role = m.role,
                        // Override sul record di join se presente (alcune righe
                        // sovrascrivono la foto del registry), altrimenti
                        // foto del members_registry espanso.
                        image = m.image.ifEmpty { reg?.image.orEmpty() },
                        inactive = m.inactive,
                        isMaster = m.isMaster,
                    )
                }
            )
        }

        OrgChartModel(groups = groupModels)
    }
}

// ---- PocketBase record DTOs (server shape) ----

@Serializable
private data class OrgChartGroupRecord(
    val id: String = "",
    val title: String = "",
    val order: Int = 0,
)

@Serializable
private data class OrgChartMemberRecord(
    val id: String = "",
    val group: String = "",
    /** Relation id verso `members_registry` (espanso in [expand]). */
    val user: String = "",
    val role: String = "",
    val inactive: Boolean = false,
    @SerialName("is_master")
    val isMaster: Boolean = false,
    val order: Int = 0,
    /** Override foto opzionale: il join record può sovrascrivere la foto
     *  del members_registry. */
    val image: String = "",
    val expand: MemberExpand? = null,
)

/** Subset del record `members_registry` ritornato dall'expand — bastano id,
 *  nome e foto per popolare la UI. */
@Serializable
private data class ExpandedRegistryMember(
    val id: String = "",
    val name: String = "",
    val image: String = "",
)

@Serializable
private data class MemberExpand(
    @SerialName("user")
    val user: ExpandedRegistryMember? = null,
)
