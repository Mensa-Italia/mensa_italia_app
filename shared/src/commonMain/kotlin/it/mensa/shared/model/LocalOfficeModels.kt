@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class LocalOfficeModel(
    val id: String = "",
    val name: String = "",
    val region: String = "",
    val slug: String = "",
    val bio: String = "",
    val image: String = "",
)

@Serializable
data class LocalOfficeAdminModel(
    val id: String = "",
    @SerialName("local_office") val localOffice: String = "",
    @SerialName("local_office_name") val localOfficeName: String = "",
    val region: String = "",
    val user: String = "",
    val name: String = "",
    val image: String = "",
    val email: String = "",
    @SerialName("is_the_officer") val isTheOfficer: Boolean = false,
)

@Serializable
data class LocalOfficeAssistantModel(
    val id: String = "",
    @SerialName("local_office") val localOffice: String = "",
    @SerialName("local_office_name") val localOfficeName: String = "",
    val region: String = "",
    val user: String = "",
    val name: String = "",
    val image: String = "",
    val email: String = "",
    /// Macro-area geografica (es. "Nord", "Centro", "Sud"). Esposto dalla
    /// view pubblica per aiutare l'utente esterno a capire dove sostenere il
    /// test piu' comodamente.
    val area: String = "",
    val city: String = "",
    val state: String = "",
)

@Serializable
data class LocalOfficeTestDateModel(
    val id: String = "",
    @SerialName("local_office") val localOffice: String = "",
    @SerialName("local_office_name") val localOfficeName: String = "",
    val region: String = "",
    val date: Instant = Instant.fromEpochMilliseconds(0),
    val location: String = "",
    val notes: String = "",
    @SerialName("max_participants") val maxParticipants: Int = 0,
    val assistants: List<String> = emptyList(),
)

/// A single row of the `view_local_office_linktree` PocketBase view — joined
/// linktree link + parent local_office metadata.
@Serializable
data class LocalOfficeLinktreeRowModel(
    val id: String = "",
    @SerialName("local_office") val localOffice: String = "",
    @SerialName("local_office_name") val localOfficeName: String = "",
    val region: String = "",
    val slug: String = "",
    val bio: String = "",
    val image: String = "",
    val title: String = "",
    val url: String = "",
    val icon: String = "",
    val kind: String = "",           // "section" | "link"
    val parent: String = "",         // "" = root
    @SerialName("sort_order") val sortOrder: Int = 0,
)

/// Raw `local_offices_test_dates` collection record — what we POST/PATCH.
/// The read flow keeps using `LocalOfficeTestDateModel` (the joined view).
@Serializable
data class LocalOfficeTestDateRecord(
    val id: String = "",
    @SerialName("local_office") val localOffice: String = "",
    val date: Instant = Instant.fromEpochMilliseconds(0),
    val location: String = "",
    val notes: String = "",
    @SerialName("max_participants") val maxParticipants: Int = 0,
    val assistants: List<String> = emptyList(),
)

/// Raw `local_offices_links` collection record — what we POST/PATCH.
/// The read flow keeps using `LocalOfficeLinktreeRowModel` (the joined view).
@Serializable
data class LocalOfficeLinkRecord(
    val id: String = "",
    @SerialName("local_office") val localOffice: String = "",
    val kind: String = "",               // "section" | "link"
    val parent: String = "",             // "" = root
    val title: String = "",
    val url: String = "",
    val icon: String = "",
    @SerialName("sort_order") val sortOrder: Int = 0,
    val active: Boolean = true,
)
