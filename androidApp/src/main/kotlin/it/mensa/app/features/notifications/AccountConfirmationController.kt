package it.mensa.app.features.notifications

import it.mensa.shared.notifications.DataExportScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton state holder for the [AccountConfirmationSheet]. Allows decoupled
 * presentation: the [DeepLinkHandler] pushes a request here when it sees an
 * `account_confirmation` / `data_export_confirmation` notification target, and
 * [AccountConfirmationHost] observes the flow to mount the sheet as a modal
 * overlay — from [MainAppShell] normally, from the account-suspended screen
 * when the server has blocked the account (these confirmations stay allowed).
 */
class AccountConfirmationController {

    /**
     * Cosa si chiede al socio di confermare. Un caso per tipo di richiesta: per
     * aggiungerne uno basta un caso qui, il suo ramo in [from] e il suo corpo
     * in [AccountConfirmationSheet].
     */
    sealed class Request {
        abstract val exAppId: String
        abstract val callbackUrl: String
        abstract val notificationId: String?

        /** Verifica d'identita' (`account_confirmation`): concede [permissions] all'app. */
        data class Identity(
            override val exAppId: String,
            override val callbackUrl: String,
            override val notificationId: String?,
            val permissions: List<String> = listOf("CHECK_USER_EXISTENCE"),
        ) : Request()

        /** Esportazione una tantum di [scope] (`data_export_confirmation`). */
        data class DataExport(
            val requestId: String,
            override val exAppId: String,
            val scope: DataExportScope,
            override val callbackUrl: String,
            override val notificationId: String?,
        ) : Request()

        companion object {
            /** La richiesta per un [NotificationTarget], o null se il target non ne ha una. */
            fun from(target: NotificationTarget): Request? = when (target) {
                is NotificationTarget.AccountConfirmation -> Identity(
                    exAppId = target.exAppId,
                    callbackUrl = target.callbackUrl,
                    notificationId = target.notificationId,
                )
                is NotificationTarget.DataExportConfirmation -> DataExport(
                    requestId = target.requestId,
                    exAppId = target.exAppId,
                    scope = target.scope,
                    callbackUrl = target.callbackUrl,
                    notificationId = target.notificationId,
                )
                else -> null
            }
        }
    }

    private val _current = MutableStateFlow<Request?>(null)
    val current: StateFlow<Request?> = _current.asStateFlow()

    fun present(request: Request) {
        _current.value = request
    }

    fun dismiss() {
        _current.value = null
    }
}
