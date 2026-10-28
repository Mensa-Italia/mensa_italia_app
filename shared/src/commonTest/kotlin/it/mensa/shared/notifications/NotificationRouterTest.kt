package it.mensa.shared.notifications

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** I `data` che il backend manda davvero (MensaCore: hooks, crons, dbtools). */
class NotificationRouterTest {

    @Test
    fun newDocument() {
        assertEquals(
            NotificationTargetSingleDocument("wm7jzf2viskouom"),
            NotificationRouter.targetFromData(mapOf("type" to "single_document", "document_id" to "wm7jzf2viskouom")),
        )
    }

    @Test
    fun newEventAndDeal() {
        assertEquals(NotificationTargetEvent("e1"), NotificationRouter.targetFromData(mapOf("type" to "event", "event_id" to "e1")))
        assertEquals(NotificationTargetDeal("d1"), NotificationRouter.targetFromData(mapOf("type" to "deal", "deal_id" to "d1")))
    }

    /** `crons/quidnotify` manda `quid_id`, non `category_id`. */
    @Test
    fun newQuidIssue() {
        assertEquals(NotificationTargetQuid("412"), NotificationRouter.targetFromData(mapOf("type" to "quid", "quid_id" to "412")))
        assertEquals(NotificationTargetQuid("7"), NotificationRouter.targetFromData(mapOf("type" to "quid", "category_id" to "7")))
    }

    // ── data_export_confirmation (§8 del contratto) ──────────────────────────

    private fun exportPayload(vararg overrides: Pair<String, String?>): Map<String, String> {
        val base = mutableMapOf(
            "type" to "data_export_confirmation",
            "requestId" to "req123",
            "keyAppId" to "app456",
            "scope" to "stamps",
            "url" to "https://bot.example.org/callback",
        )
        overrides.forEach { (k, v) -> if (v == null) base.remove(k) else base[k] = v }
        return base
    }

    @Test
    fun dataExportConfirmationStamps() {
        assertEquals(
            NotificationTargetDataExportConfirmation(
                requestId = "req123",
                exAppId = "app456",
                scope = DataExportScope.STAMPS,
                callbackUrl = "https://bot.example.org/callback",
                notificationId = null,
            ),
            NotificationRouter.targetFromData(exportPayload()),
        )
    }

    @Test
    fun dataExportConfirmationMedalsCarriesTheNotificationId() {
        val target = NotificationRouter.targetFromData(exportPayload("scope" to "medals", "internal_id" to "n1"))
        assertEquals(DataExportScope.MEDALS, (target as NotificationTargetDataExportConfirmation).scope)
        assertEquals("n1", target.notificationId)
    }

    @Test
    fun dataExportConfirmationNeedsEveryField() {
        listOf("requestId", "keyAppId", "scope", "url").forEach { field ->
            assertNull(NotificationRouter.targetFromData(exportPayload(field to null)), "senza $field")
            assertNull(NotificationRouter.targetFromData(exportPayload(field to "")), "$field vuoto")
        }
    }

    /** Un tipo di dato che l'app non conosce non deve diventare una conferma generica. */
    @Test
    fun dataExportConfirmationRejectsUnknownScope() {
        assertNull(NotificationRouter.targetFromData(exportPayload("scope" to "everything")))
        assertNull(NotificationRouter.targetFromData(exportPayload("scope" to "STAMPS")))
    }

    @Test
    fun dataExportConfirmationIsNotAnAccountConfirmation() {
        // `account_confirmation` continua a funzionare, con i suoi campi.
        assertEquals(
            NotificationTargetAccountConfirmation("app1", "https://x.example/cb", null),
            NotificationRouter.targetFromData(
                mapOf("type" to "account_confirmation", "keyAppId" to "app1", "url" to "https://x.example/cb"),
            ),
        )
    }

    @Test
    fun dataExportConfirmationHasItsOwnIcon() {
        assertEquals("square.and.arrow.up", NotificationRouter.systemIconName("data_export_confirmation"))
    }
}
