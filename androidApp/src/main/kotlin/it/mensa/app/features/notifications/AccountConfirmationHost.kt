package it.mensa.app.features.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.services.push.PendingPushTarget
import it.mensa.app.services.push.PushDeepLinkRouter
import org.koin.compose.koinInject

/**
 * Monta il foglio di conferma (identita' o esportazione dati) quando
 * [AccountConfirmationController] ha una richiesta in corso.
 *
 * Sta in un composable a parte perche' deve poter vivere in due posti: dentro
 * [it.mensa.app.ui.shell.MainAppShell] nel caso normale, e dentro la schermata
 * di account sospeso. Un account bloccato resta socio valido per le app
 * esterne — il server tiene queste conferme nell'allowlist — quindi la push
 * deve poter aprire il foglio anche li', dove la shell non c'e'.
 *
 * Con [drainPush] questo host raccoglie da solo le push di conferma parcheggiate
 * in [PendingPushTarget], cosa che altrimenti fa la shell. Lascia dov'e' ogni
 * altro tipo di push: la shell la prendera' quando l'account torna normale.
 */
@Composable
fun AccountConfirmationHost(drainPush: Boolean = false) {
    val controller = koinInject<AccountConfirmationController>()
    val request by controller.current.collectAsStateWithLifecycle()

    if (drainPush) {
        val pending by PendingPushTarget.target.collectAsStateWithLifecycle()
        LaunchedEffect(pending) {
            val inApp = (pending as? PushDeepLinkRouter.NotificationTarget.InApp)?.target ?: return@LaunchedEffect
            val confirmation = AccountConfirmationController.Request.from(inApp) ?: return@LaunchedEffect
            PendingPushTarget.consume()
            controller.present(confirmation)
        }
    }

    request?.let {
        AccountConfirmationSheet(request = it, onDismiss = { controller.dismiss() })
    }
}
