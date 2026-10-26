package it.mensa.app.support

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import it.mensa.shared.net.Connectivity
import kotlinx.coroutines.flow.drop

/**
 * La rete del telefono, dal sistema, girata a [Connectivity]: tornata, si
 * chiede subito al server invece di aspettare il controllo periodico; persa,
 * il server si segna irraggiungibile senza aspettare che una richiesta
 * fallisca. Mirrors iOS `NWPathMonitor` in `ConnectivityStore`.
 */
object NetworkWatcher {
    fun start(context: Context, connectivity: Connectivity) {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = connectivity.onNetworkChange(true)

                // Passando dal Wi-Fi ai dati la rete di default si perde e
                // un'altra arriva subito: offline solo se non ne resta nessuna.
                override fun onLost(network: Network) {
                    if (cm.activeNetwork == null) connectivity.onNetworkChange(false)
                }
            })
        }
    }
}

/**
 * Rilancia [onReconnect] quando il server torna a rispondere dopo un periodo
 * offline (`Connectivity.reconnects`): la pagina aperta offline si aggiorna
 * da sola, senza uscire e rientrare. Si mette accanto al pull-to-refresh, con
 * la stessa azione. Mirrors iOS `refreshableOnReconnect`.
 */
@Composable
fun RefreshOnReconnect(onReconnect: () -> Unit) {
    val connectivity = remember { koinAccess().connectivity }
    val action by rememberUpdatedState(onReconnect)
    LaunchedEffect(connectivity) {
        // Il valore attuale e' quello di quando la pagina si apre: conta solo
        // un ritorno successivo.
        connectivity.reconnects.drop(1).collect { action() }
    }
}
