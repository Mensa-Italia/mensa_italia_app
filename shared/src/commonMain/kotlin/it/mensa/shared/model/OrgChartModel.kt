package it.mensa.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Organigramma — modello associativo: gruppi di cariche con i soci che le ricoprono.
 *
 * Shape disegnata per essere servita da PocketBase (collection `org_chart` o
 * endpoint custom `/api/org-chart`). Per ora i dati arrivano mockati lato API;
 * appena l'endpoint server è pronto basta togliere il mock — il modello non
 * cambia.
 */
@Serializable
data class OrgChartModel(
    val groups: List<OrgChartGroup> = emptyList(),
)

@Serializable
data class OrgChartGroup(
    val id: String = "",
    val title: String = "",
    val members: List<OrgChartMember> = emptyList(),
)

@Serializable
data class OrgChartMember(
    @SerialName("user_id")
    val userId: String = "",
    val name: String = "",
    val role: String = "",
    /** Foto profilo del socio (filename PocketBase su `members_registry`,
     *  oppure URL assoluto). Stringa vuota → fallback iniziali. */
    val image: String = "",
    /** Optional: alcuni soci possono essere dimissionari / inattivi. */
    val inactive: Boolean = false,
    /** Carica "primaria" — eg. Presidente. Riceve il trattamento hero card. */
    @SerialName("is_master")
    val isMaster: Boolean = false,
)
