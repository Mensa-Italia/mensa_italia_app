package it.mensa.app.navigation

import it.mensa.shared.notifications.AppLink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Un link `svc.mensa.it/links/...` arrivato prima che ci sia un NavController
 * per aprirlo. Stessa logica di [it.mensa.app.services.push.PendingPushTarget]:
 * lo scrive MainActivity, lo drena MainAppShell. Se l'app parte sul login o
 * sul muro del rinnovo il link resta qui finche' il guscio non c'e'.
 */
object PendingAppLink {

    private val _link = MutableStateFlow<AppLink?>(null)

    val link: StateFlow<AppLink?> = _link.asStateFlow()

    fun set(link: AppLink) {
        _link.value = link
    }

    fun consume(): AppLink? {
        val current = _link.value
        _link.value = null
        return current
    }
}
