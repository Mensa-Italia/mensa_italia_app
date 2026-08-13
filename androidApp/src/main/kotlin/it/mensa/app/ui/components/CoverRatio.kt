package it.mensa.app.ui.components

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImagePainter

/**
 * Il rapporto larghezza/altezza da dare alla copertina di una card.
 *
 * Le liste avevano due difetti opposti: i SIG su iOS lasciavano bande vuote
 * (l'immagine entrava per intero in una finestra piu' alta di lei), gli eventi
 * su Android venivano tagliati (16:9 fisso su locandine che 16:9 non sono).
 * La cura e' la stessa per entrambi: il contenitore prende il rapporto
 * dell'immagine vera, e l'immagine resta in Crop.
 *
 * Il rapporto pero' si conosce solo quando l'immagine e' arrivata, e questo
 * fa riassestare la lista sotto le dita. Due accorgimenti lo riducono a una
 * volta sola per immagine:
 *
 *  - la mappa vive qui, non dentro la card. LazyColumn ricicla le celle, e un
 *    `remember` dentro la riga verrebbe buttato via proprio scorrendo, cioe'
 *    nel caso che conta;
 *  - il valore e' persistito, quindi dalla seconda apertura dell'app la lista
 *    e' gia' della misura giusta al primo frame.
 *
 * La chiave e' l'URL completo, che contiene collezione, record e nome file:
 * PocketBase rinomina il file a ogni upload, quindi quando un referente
 * cambia la copertina la chiave cambia da sola e il valore vecchio non viene
 * mai riusato.
 */
object CoverRatio {

    /**
     * Sotto il quadrato la card smetterebbe di essere una riga e diventerebbe
     * una pagina: una locandina 3:4 occuperebbe da sola mezza schermata di
     * sola immagine. Sotto questo limite si torna a tagliare, di proposito.
     */
    const val MIN = 1f

    /**
     * Sopra questo rapporto la striscia diventa troppo bassa perche' lo scrim
     * da 60dp e i chip in alto restino leggibili. E' tarato appena sopra il
     * 2,53 che l'app stessa impone quando si carica la copertina di un SIG,
     * cosi' le banner passano intatte invece di essere tagliate per un pelo.
     */
    const val MAX = 2.6f

    /** Usato finche' non si sa niente, e per le card senza immagine. */
    const val FALLBACK = 16f / 9f

    private const val PREFS = "mensa_cover_ratios"

    private val cache = mutableStateMapOf<String, Float>()
    private var hydrated = false

    /** Il rapporto noto per [key], gia' dentro i limiti, oppure null. */
    fun known(context: Context, key: String?): Float? {
        if (key == null) return null
        hydrate(context)
        return cache[key]
    }

    /** Registra il rapporto misurato sull'immagine appena decodificata. */
    fun remember(context: Context, key: String?, ratio: Float) {
        if (key == null || !ratio.isFinite() || ratio <= 0f) return
        if (cache[key] == ratio) return
        cache[key] = ratio
        prefs(context).edit().putFloat(key, ratio).apply()
    }

    fun clamp(ratio: Float): Float = ratio.coerceIn(MIN, MAX)

    /**
     * I rapporti salvati si leggono tutti insieme al primo accesso: sono
     * qualche decina di byte a voce e leggerli uno per uno da dentro la
     * composizione vorrebbe dire toccare il disco a ogni riga della lista.
     */
    private fun hydrate(context: Context) {
        if (hydrated) return
        hydrated = true
        prefs(context).all.forEach { (key, value) ->
            if (value is Float) cache[key] = value
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/**
 * Il rapporto da usare adesso per la copertina identificata da [key], e la
 * callback da passare a [CachedAsyncImage] per misurarla quando arriva.
 *
 * Il cambio e' animato: senza, la riga scatterebbe da 16:9 alla misura vera
 * nel frame in cui l'immagine finisce di caricare.
 */
@Composable
fun rememberCoverRatio(key: String?): CoverRatioState {
    val context = LocalContext.current
    val target = CoverRatio.known(context, key)?.let(CoverRatio::clamp) ?: CoverRatio.FALLBACK
    val animated by animateFloatAsState(targetValue = target, label = "CoverRatio")
    return remember(key, animated) {
        CoverRatioState(
            ratio = animated,
            onImageState = { state ->
                val size = (state as? AsyncImagePainter.State.Success)?.painter?.intrinsicSize
                if (size != null && size.height > 0f) {
                    CoverRatio.remember(context, key, size.width / size.height)
                }
            },
        )
    }
}

/** Vedi [rememberCoverRatio]. */
class CoverRatioState(
    val ratio: Float,
    val onImageState: (AsyncImagePainter.State) -> Unit,
)
