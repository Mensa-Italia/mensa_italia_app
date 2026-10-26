package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.LocalOfficeAdminModel
import it.mensa.shared.model.LocalOfficeAssistantModel
import it.mensa.shared.model.LocalOfficeLinkRecord
import it.mensa.shared.model.LocalOfficeLinktreeRowModel
import it.mensa.shared.model.LocalOfficeModel
import it.mensa.shared.model.LocalOfficeTestDateModel
import it.mensa.shared.model.LocalOfficeTestDateRecord
import it.mensa.shared.model.SigModel
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonObject

class LocalOfficesApi(private val pb: PocketBaseClient) {

    /// Full list of all local offices, sorted by name. Requires auth.
    @Throws(Exception::class)
    suspend fun listAll(): List<LocalOfficeModel> =
        pb.fullList("local_offices", sort = "name")

    /// Public-area variant: legge la view dedicata `view_local_office` (path
    /// e shape diversi dalla collection interna — solo i campi che vanno
    /// esposti a un visitatore non autenticato).
    @Throws(Exception::class)
    suspend fun listAllPublic(): List<LocalOfficeModel> =
        pb.fullListUnauthenticated("view_local_office", sort = "name")

    /// Public variant by slug — stessa view `view_local_office`.
    @Throws(Exception::class)
    suspend fun bySlugPublic(slug: String): LocalOfficeModel? {
        val escaped = slug.replace("'", "\\'")
        return pb.fullListUnauthenticated<LocalOfficeModel>(
            "view_local_office",
            filter = "slug='$escaped'",
        ).firstOrNull()
    }

    /// Public variant by id — stessa view.
    @Throws(Exception::class)
    suspend fun byIdPublic(id: String): LocalOfficeModel? {
        val escaped = id.replace("'", "\\'")
        return pb.fullListUnauthenticated<LocalOfficeModel>(
            "view_local_office",
            filter = "id='$escaped'",
        ).firstOrNull()
    }

    /// Resolve a single office by slug (used by deep-link entry).
    /// Returns the first match or null.
    @Throws(Exception::class)
    suspend fun bySlug(slug: String): LocalOfficeModel? {
        val escaped = slug.replace("'", "\\'")
        return pb.fullList<LocalOfficeModel>(
            "local_offices",
            filter = "slug='$escaped'",
        ).firstOrNull()
    }

    /// Full linktree (sections + links) for a single office, sorted by sort_order.
    /// Uses the public view — no auth required.
    @Throws(Exception::class)
    suspend fun linktreeByOffice(officeId: String): List<LocalOfficeLinktreeRowModel> =
        pb.fullListUnauthenticated(
            "view_local_office_linktree",
            filter = "local_office='$officeId'",
            sort = "sort_order",
        )

    /// Same data resolved by slug (used when navigating from a deep link where
    /// only the slug is known).
    @Throws(Exception::class)
    suspend fun linktreeBySlug(slug: String): List<LocalOfficeLinktreeRowModel> {
        val escaped = slug.replace("'", "\\'")
        return pb.fullListUnauthenticated(
            "view_local_office_linktree",
            filter = "slug='$escaped'",
            sort = "sort_order",
        )
    }

    /// Segretario + cosegretari (admins) of a local office. Public view.
    @Throws(Exception::class)
    suspend fun adminsByOffice(officeId: String): List<LocalOfficeAdminModel> =
        pb.fullListUnauthenticated(
            "view_local_office_admins",
            filter = "local_office='$officeId'",
        )

    /// Test assistants of a local office. Public view.
    @Throws(Exception::class)
    suspend fun assistantsByOffice(officeId: String): List<LocalOfficeAssistantModel> =
        pb.fullListUnauthenticated(
            "view_local_office_assistants",
            filter = "local_office='$officeId'",
        )

    /// Upcoming test dates (date >= now) for a single office, sorted ascending.
    /// Public view.
    @Throws(Exception::class)
    suspend fun upcomingTestDatesByOffice(officeId: String): List<LocalOfficeTestDateModel> {
        val isoNow = Clock.System.now().toString()
        return pb.fullListUnauthenticated(
            "view_local_office_test_dates",
            filter = "local_office='$officeId' && date>='$isoNow'",
            sort = "date",
        )
    }

    /// Events relation-filtered to a local office. Uses the existing EventModel —
    /// the extra `local_office` field in the PocketBase JSON is ignored by the
    /// deserializer (ignoreUnknownKeys = true).
    @Throws(Exception::class)
    suspend fun eventsByOffice(officeId: String): List<EventModel> =
        pb.fullList(
            "events",
            filter = "local_office='$officeId'",
            sort = "-when_start",
        )

    /// Sigs relation-filtered to a local office (territoriali).
    @Throws(Exception::class)
    suspend fun sigsByOffice(officeId: String): List<SigModel> =
        pb.fullList(
            "sigs",
            filter = "local_office='$officeId'",
        )

    // --- Test dates CRUD ---

    /// Create a new test-date record. PocketBase assigns the id; the `id` field
    /// on the returned record holds the newly created id.
    @Throws(Exception::class)
    suspend fun createTestDate(record: LocalOfficeTestDateRecord): LocalOfficeTestDateRecord =
        pb.create("local_offices_test_dates", record)

    /// Partial-patch a test-date record. Pass only the fields you want to change
    /// as a `JsonObject` (kotlinx-serialization's `buildJsonObject { ... }`).
    @Throws(Exception::class)
    suspend fun updateTestDate(id: String, patch: JsonObject): LocalOfficeTestDateRecord =
        pb.update("local_offices_test_dates", id, patch)

    /// Delete a test-date record by id.
    @Throws(Exception::class)
    suspend fun deleteTestDate(id: String) =
        pb.delete("local_offices_test_dates", id)

    // --- Linktree entries CRUD ---

    /// Create a new linktree entry (section or link).
    @Throws(Exception::class)
    suspend fun createLink(record: LocalOfficeLinkRecord): LocalOfficeLinkRecord =
        pb.create("local_offices_links", record)

    /// Partial-patch a linktree entry.
    @Throws(Exception::class)
    suspend fun updateLink(id: String, patch: JsonObject): LocalOfficeLinkRecord =
        pb.update("local_offices_links", id, patch)

    /// Delete a linktree entry by id.
    @Throws(Exception::class)
    suspend fun deleteLink(id: String) =
        pb.delete("local_offices_links", id)
}
