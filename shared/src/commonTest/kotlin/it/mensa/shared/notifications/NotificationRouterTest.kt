package it.mensa.shared.notifications

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
