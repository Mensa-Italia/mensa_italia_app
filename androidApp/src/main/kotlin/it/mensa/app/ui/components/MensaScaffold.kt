package it.mensa.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * MensaScaffold — the app-wide scaffold with optional hero gradient backdrop.
 *
 * Design system intent:
 * - Default: uses colorScheme.background (Parchment in light, BackdropDark in dark).
 *   Content screens should look like premium stationery, NOT a blue gradient.
 * - Hero mode: pass [heroBrush] to apply a brand gradient ONLY in the top hero area.
 *   This is for screens like Login, Splash, Membership Card — NOT content screens.
 * - Edge-to-edge: la Scaffold NON consuma lo status bar (i top bar dell'app se
 *   lo gestiscono da soli con `statusBarsPadding()`), ma consuma la
 *   navigation bar. Senza quest'ultima il FAB, lo snackbar e il contenuto
 *   finiscono SOTTO la barra di sistema di Android — e' il bug della lista
 *   eventi, dove il FAB "Nuovo evento" era coperto dalla barra a 3 tasti.
 *   Le schermate dentro [MainAppShell] non pagano il padding due volte perche'
 *   la shell consuma gia' l'inset quando la bottom bar e' visibile
 *   (`consumeWindowInsets` su MainAppShell).
 *
 * @param heroBrush optional gradient brush applied as background overlay.
 *   When null, uses the M3 colorScheme.background (correct default for content screens).
 * @param gradientBrush alias for heroBrush, kept for backwards compatibility.
 */
@Composable
fun MensaScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    heroBrush: Brush? = null,
    // Backwards-compat alias — mapped to heroBrush if heroBrush is null
    gradientBrush: Brush? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val effectiveBrush = heroBrush ?: gradientBrush
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (effectiveBrush != null) {
                    Modifier.background(effectiveBrush)
                } else {
                    Modifier.background(backgroundColor)
                },
            ),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // Transparent scaffold background — our Box layer handles coloring
            containerColor = Color.Transparent,
            contentColor = contentColorFor(backgroundColor),
            contentWindowInsets = WindowInsets.navigationBars,
            content = content,
        )
    }
}
