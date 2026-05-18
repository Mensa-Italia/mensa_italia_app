package it.mensa.app.navigation

import androidx.navigation.NavController
import it.mensa.app.features.notifications.AccountConfirmationController
import it.mensa.app.features.notifications.NotificationTarget
import it.mensa.app.features.notifications.NotificationsRoutes
import it.mensa.app.services.push.PushDeepLinkRouter
import org.koin.mp.KoinPlatform

/**
 * DeepLinkHandler — utility for routing typed notification targets to nav destinations.
 *
 * Two entry points:
 *  1. [handleNotificationTarget] — called from NotificationsListScreen / NotificationDetailScreen
 *     when the user taps a notification with a parsed [NotificationTarget].
 *  2. [handlePushTarget] — called from MensaMessagingService when an FCM push arrives
 *     and the app is foregrounded; maps [PushDeepLinkRouter.NotificationTarget] to routes.
 *
 * Mirrors iOS NotificationsListView.destinationView(for:) routing logic.
 *
 * TODO (deep links):
 *   - documents/detail/{id} route when DocumentsNavGraph is wired
 *   - quid/issue/{categoryId} and quid/article/{postId} when QuidNavGraph is wired
 *   - receipts/list when ReceiptsNavGraph is wired
 *   - tickets/list when TicketsNavGraph is wired
 */
object DeepLinkHandler {

    // ─── Routes (pending nav graphs) ─────────────────────────────────

    /** Builds an event detail route. */
    private fun eventRoute(eventId: String) = "events/detail/$eventId"

    /** Builds a deal detail route. */
    private fun dealRoute(dealId: String) = "deals/detail/$dealId"

    /** Builds a ticket detail route. */
    private fun ticketRoute(ticketId: String) = "tickets/detail/$ticketId"

    /** Builds a local office detail route. */
    private fun localOfficeRoute(slug: String) = "local_offices/detail/$slug"

    // ─── Notification target routing ──────────────────────────────────────────

    /**
     * Route a [NotificationTarget] (parsed from in-app notification model data).
     * Called from NotificationsListScreen and NotificationDetailScreen.
     */
    fun handleNotificationTarget(
        target: NotificationTarget,
        navController: NavController,
    ) {
        when (target) {
            is NotificationTarget.Event -> {
                navController.navigate(eventRoute(target.eventId))
            }
            is NotificationTarget.Deal -> {
                navController.navigate(dealRoute(target.dealId))
            }
            is NotificationTarget.SingleDocument -> {
                navController.navigate("documents/detail/${target.documentId}")
            }
            is NotificationTarget.MultipleDocuments -> {
                navController.navigate("documents/list")
            }
            is NotificationTarget.TicketPurchase -> {
                // TODO (deep links): navigate to tickets list
                // navController.navigate("tickets/list")
            }
            is NotificationTarget.PaymentUpdateStatus -> {
                // TODO (deep links): navigate to receipts list
                // navController.navigate("receipts/list")
            }
            is NotificationTarget.Quid -> {
                // TODO (deep links): navigate to quid/issue/{categoryId}
                // navController.navigate("quid/issue/${target.categoryId}")
            }
            is NotificationTarget.QuidArticle -> {
                // TODO (deep links): navigate to quid/article/{postId}
                // navController.navigate("quid/article/${target.postId}")
            }
            is NotificationTarget.QuidPdf -> {
                // TODO (deep links): navigate to quid/pdf/{recordId}
                // navController.navigate("quid/pdf/${target.recordId}")
            }
            is NotificationTarget.LocalOffice -> {
                navController.navigate(localOfficeRoute(target.slug))
            }
            is NotificationTarget.AccountConfirmation -> {
                // No navigation — surface the modal approval sheet instead.
                runCatching {
                    KoinPlatform.getKoin().get<AccountConfirmationController>().present(
                        AccountConfirmationController.Request(
                            exAppId = target.exAppId,
                            callbackUrl = target.callbackUrl,
                            notificationId = target.notificationId,
                        ),
                    )
                }
            }
        }
    }

    // ─── FCM push target routing ──────────────────────────────────────────────

    /**
     * Route a [PushDeepLinkRouter.NotificationTarget] (parsed from FCM data payload).
     * Called from MensaMessagingService when the app processes a foreground push.
     */
    fun handlePushTarget(
        target: PushDeepLinkRouter.NotificationTarget,
        navController: NavController,
    ) {
        when (target) {
            is PushDeepLinkRouter.NotificationTarget.Event -> {
                navController.navigate(eventRoute(target.eventId))
            }
            is PushDeepLinkRouter.NotificationTarget.Deal -> {
                navController.navigate(dealRoute(target.dealId))
            }
            is PushDeepLinkRouter.NotificationTarget.Document -> {
                navController.navigate("documents/detail/${target.documentId}")
            }
            is PushDeepLinkRouter.NotificationTarget.Ticket -> {
                navController.navigate(ticketRoute(target.ticketId))
            }
            is PushDeepLinkRouter.NotificationTarget.Quid -> {
                // TODO (deep links): navigate to quid/article or quid screen
                // target.transactionId?.let { navController.navigate("quid/article/$it") }
            }
            is PushDeepLinkRouter.NotificationTarget.LocalOffice -> {
                navController.navigate(localOfficeRoute(target.officeId))
            }
            is PushDeepLinkRouter.NotificationTarget.ExternalUrl -> {
                // Open in Chrome Custom Tab — handled by caller (MensaMessagingService)
            }
            is PushDeepLinkRouter.NotificationTarget.Unknown -> {
                // Navigate to notifications list as fallback
                navController.navigate(NotificationsRoutes.LIST)
            }
        }
    }

    // ─── Stripe redirect handling ─────────────────────────────────────────────

    /**
     * Handle Stripe redirect intents from the mensa:// deep link scheme.
     *
     * Called from MainActivity.onNewIntent when a Stripe payment redirect arrives.
     * Stripe posts back to mensa://stripe/return?payment_intent=pi_xxx&... or
     * mensa://stripe/cancel depending on payment outcome.
     *
     * @param path URI path, e.g. "/stripe/return" or "/stripe/cancel"
     * @param navController active NavController
     */
    fun handleStripeRedirect(
        path: String?,
        navController: NavController,
    ) {
        when {
            path?.startsWith("/stripe/return") == true -> {
                // TODO (payments): navigate to payment success screen
                // navController.navigate("payments/success")
            }
            path?.startsWith("/stripe/cancel") == true -> {
                // TODO (payments): navigate to payment cancelled screen
                // navController.navigate("payments/cancelled")
            }
            else -> {
                // Unrecognized mensa:// deep link — navigate to Today as fallback
                // navController.navigate(Route.Today.path)
            }
        }
    }
}
