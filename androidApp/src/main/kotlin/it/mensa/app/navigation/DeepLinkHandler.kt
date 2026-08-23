package it.mensa.app.navigation

import androidx.navigation.NavController
import it.mensa.app.features.deals.DealsRoute
import it.mensa.app.features.documents.DocumentsRoutes
import it.mensa.app.features.events.EventRoutes
import it.mensa.app.features.localoffices.LocalOfficesRoutes
import it.mensa.app.features.notifications.AccountConfirmationController
import it.mensa.app.features.notifications.NotificationTarget
import it.mensa.app.features.notifications.NotificationsRoutes
import it.mensa.app.features.quid.QuidRoute
import it.mensa.app.features.receipts.ReceiptsRoutes
import it.mensa.app.features.tickets.TicketsRoutes
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
 * Le rotte arrivano dagli oggetti `*Routes` delle feature e non da stringhe
 * scritte a mano: qui c'erano sia rotte duplicate a mano sia una lista di TODO
 * che aspettavano nav graph ormai cablati da tempo.
 */
object DeepLinkHandler {

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
                navController.navigate(EventRoutes.detail(target.eventId))
            }
            is NotificationTarget.Deal -> {
                navController.navigate(DealsRoute.detail(target.dealId))
            }
            is NotificationTarget.SingleDocument -> {
                navController.navigate(DocumentsRoutes.detail(target.documentId))
            }
            is NotificationTarget.MultipleDocuments -> {
                navController.navigate(DocumentsRoutes.LIST)
            }
            is NotificationTarget.TicketPurchase -> {
                navController.navigate(TicketsRoutes.LIST)
            }
            is NotificationTarget.PaymentUpdateStatus -> {
                navController.navigate(ReceiptsRoutes.LIST)
            }
            is NotificationTarget.Quid -> {
                // Il nome del numero non sta nel payload: lo schermo lo mostra
                // vuoto finche' non carica i suoi dati, che e' meglio di non
                // aprirlo affatto.
                val issueId = target.categoryId.toLongOrNull()
                if (issueId != null) {
                    navController.navigate(QuidRoute.issue(issueId, ""))
                } else {
                    navController.navigate(QuidRoute.ISSUES)
                }
            }
            is NotificationTarget.QuidArticle -> {
                val articleId = target.postId.toLongOrNull()
                if (articleId != null) {
                    navController.navigate(QuidRoute.article(articleId))
                } else {
                    navController.navigate(QuidRoute.ISSUES)
                }
            }
            is NotificationTarget.QuidPdf -> {
                // Volutamente la lista e non il PDF. La rotta PDF vuole un URL,
                // il payload porta un record id, e risolverlo richiede di
                // leggere `quid.observeIssues()` cercando `id == -recordId`
                // (come fa `QuidPDFDeepLinkLoader` su iOS): e' una lettura
                // asincrona, e questa funzione naviga in modo sincrono. La
                // lista e' il posto giusto da cui l'utente arriva al PDF in un
                // tap.
                navController.navigate(QuidRoute.ISSUES)
            }
            is NotificationTarget.LocalOffice -> {
                navController.navigate(LocalOfficesRoutes.detail(target.slug))
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
     * Porta l'utente dove punta una push che ha toccato.
     *
     * Chiamata da [it.mensa.app.ui.shell.MainAppShell] quando drena
     * [it.mensa.app.services.push.PendingPushTarget]: e' li' che esiste un
     * NavController, non dentro il servizio FCM.
     *
     * [openUrl] apre gli indirizzi esterni — il guscio passa l'`UriHandler` di
     * Compose, che su Android finisce in una Chrome Custom Tab.
     */
    fun handlePushTarget(
        target: PushDeepLinkRouter.NotificationTarget,
        navController: NavController,
        openUrl: (String) -> Unit,
    ) {
        when (target) {
            is PushDeepLinkRouter.NotificationTarget.Event -> {
                navController.navigate(EventRoutes.detail(target.eventId))
            }
            is PushDeepLinkRouter.NotificationTarget.Deal -> {
                navController.navigate(DealsRoute.detail(target.dealId))
            }
            is PushDeepLinkRouter.NotificationTarget.Document -> {
                navController.navigate(DocumentsRoutes.detail(target.documentId))
            }
            is PushDeepLinkRouter.NotificationTarget.Ticket -> {
                navController.navigate(TicketsRoutes.detail(target.ticketId))
            }
            is PushDeepLinkRouter.NotificationTarget.Quid -> {
                // `transactionId` qui e' l'id del numero, quando c'e'.
                val issueId = target.transactionId?.toLongOrNull()
                if (issueId != null) {
                    navController.navigate(QuidRoute.issue(issueId, ""))
                } else {
                    navController.navigate(QuidRoute.ISSUES)
                }
            }
            is PushDeepLinkRouter.NotificationTarget.LocalOffice -> {
                navController.navigate(LocalOfficesRoutes.detail(target.officeId))
            }
            is PushDeepLinkRouter.NotificationTarget.ExternalUrl -> {
                openUrl(target.url)
            }
            is PushDeepLinkRouter.NotificationTarget.Unknown -> {
                navController.navigate(NotificationsRoutes.LIST)
            }
        }
    }

}
