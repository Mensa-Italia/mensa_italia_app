package it.mensa.app.features.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton state holder for the [AccountConfirmationSheet]. Allows decoupled
 * presentation: the [DeepLinkHandler] pushes a request here when it sees an
 * `account_confirmation` notification target, and [MainAppShell] observes the
 * flow to mount the sheet as a modal overlay.
 */
class AccountConfirmationController {

    data class Request(
        val exAppId: String,
        val callbackUrl: String,
        val notificationId: String?,
    )

    private val _current = MutableStateFlow<Request?>(null)
    val current: StateFlow<Request?> = _current.asStateFlow()

    fun present(request: Request) {
        _current.value = request
    }

    fun dismiss() {
        _current.value = null
    }
}
