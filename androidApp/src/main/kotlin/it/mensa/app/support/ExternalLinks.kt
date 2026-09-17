package it.mensa.app.support

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import it.mensa.shared.net.ExternalUrl

/**
 * L'unico modo in cui l'app apre un indirizzo esterno.
 *
 * Prima ogni schermata se la cavava da sola, e il dettaglio convenzione lo
 * faceva cosi':
 *
 * ```
 * try { CustomTabsIntent...launchUrl(context, Uri.parse(link)) }
 * catch (_: Exception) { context.startActivity(Intent(ACTION_VIEW, Uri.parse(link))) }
 * ```
 *
 * Con un link senza schema — e nel database ce ne sono, `www.bonavitaly.com` —
 * `Uri.parse` non protesta ma produce un indirizzo relativo che nessuna app sa
 * gestire: il primo tentativo lanciava `ActivityNotFoundException`, il `catch`
 * rifaceva *la stessa identica chiamata*, la seconda eccezione non la
 * prendeva nessuno e l'app si chiudeva in faccia al socio.
 *
 * Qui l'indirizzo passa prima da [ExternalUrl.normalize], che gli mette lo
 * schema, e ogni `startActivity` e' protetto davvero. Quando non si puo'
 * aprire niente la funzione restituisce `false` invece di far saltare tutto.
 */
object ExternalLinks {

    /**
     * Apre [raw] e dice se ci e' riuscita.
     *
     * Gli indirizzi web passano dalle Custom Tabs, che restano dentro l'app;
     * `mailto:`, `tel:` e simili vanno al sistema, che sa a chi darli.
     */
    fun open(context: Context, raw: String?): Boolean {
        val url = ExternalUrl.normalize(raw)
        if (url == null) {
            toastFailure(context)
            return false
        }
        val uri = runCatching { Uri.parse(url) }.getOrNull()
        if (uri == null) {
            toastFailure(context)
            return false
        }

        if (ExternalUrl.isWebUrl(url)) {
            val viaCustomTab = runCatching {
                CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, uri)
            }.isSuccess
            if (viaCustomTab) return true
            // Nessun browser che supporti le Custom Tabs: si prova con l'intent
            // generico, che magari un'altra app raccoglie.
        }

        if (startView(context, uri)) return true
        toastFailure(context)
        return false
    }

    /** `true` se da [raw] si ricava qualcosa di apribile: serve a nascondere i bottoni morti. */
    fun canOpen(raw: String?): Boolean = ExternalUrl.normalize(raw) != null

    /**
     * `true` solo se [raw] e' un indirizzo *web*.
     *
     * Serve dove si deve scegliere fra browser, posta e telefono guardando il
     * valore: [canOpen] da' ragione anche a una mail nuda, che normalizza in
     * `mailto:`, e non basta a distinguere i casi.
     */
    fun isWebLink(raw: String?): Boolean =
        ExternalUrl.normalize(raw)?.let { ExternalUrl.isWebUrl(it) } == true

    /**
     * Scrive a [email], con oggetto facoltativo.
     *
     * `ACTION_SENDTO` su `mailto:` non trova nessuno se sul dispositivo non c'e'
     * un client di posta — su un tablet capita — e senza protezione sarebbe un
     * altro `ActivityNotFoundException` in faccia all'utente.
     */
    fun sendEmail(context: Context, email: String?, subject: String? = null): Boolean {
        val address = email?.trim().orEmpty()
        if (address.isEmpty()) return false
        val target = buildString {
            append("mailto:")
            // La chiocciola va lasciata stare: `Uri.encode` senza caratteri
            // ammessi la trasformerebbe in `%40`, e non tutti i client di posta
            // se la rileggono come destinatario.
            append(Uri.encode(address, "@"))
            if (!subject.isNullOrBlank()) {
                append("?subject=")
                append(Uri.encode(subject))
            }
        }
        val uri = runCatching { Uri.parse(target) }.getOrNull() ?: return false
        if (start(context, Intent(Intent.ACTION_SENDTO, uri))) return true
        toastFailure(context)
        return false
    }

    /** Apre il tastierino con [phone] gia' composto. */
    fun dial(context: Context, phone: String?): Boolean {
        val number = phone?.filterNot { it.isWhitespace() }.orEmpty()
        if (number.isEmpty()) return false
        val uri = runCatching { Uri.parse("tel:${Uri.encode(number)}") }.getOrNull() ?: return false
        if (start(context, Intent(Intent.ACTION_DIAL, uri))) return true
        toastFailure(context)
        return false
    }

    private fun startView(context: Context, uri: Uri): Boolean =
        start(context, Intent(Intent.ACTION_VIEW, uri))

    private fun start(context: Context, intent: Intent): Boolean = runCatching {
        // Fuori da un'Activity l'intent ha bisogno di un task suo, o Android
        // rifiuta di farlo partire.
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }.isSuccess

    private fun toastFailure(context: Context) {
        val message = runCatching {
            koinAccess().i18n.t(
                "app.link_not_openable",
                "Impossibile aprire il link",
                emptyMap(),
            )
        }.getOrDefault("Impossibile aprire il link")
        runCatching { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }
}
