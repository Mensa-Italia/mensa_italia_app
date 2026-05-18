package it.mensa.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * GlassCard — thin alias for [MensaCard]. Kept for backwards compatibility.
 *
 * @deprecated Use [MensaCard] directly. The `tint` parameter is ignored in the
 *   new implementation — card surface colors are derived from the M3 color scheme.
 *   Wave 2 will migrate all callsites.
 */
@Deprecated(
    message = "Use MensaCard instead. GlassCard is a thin alias kept for " +
        "backwards compatibility. The 'tint' parameter is no longer respected.",
    replaceWith = ReplaceWith(
        expression = "MensaCard(modifier, shape = RoundedCornerShape(cornerRadius), " +
            "padding = padding, content = content)",
        imports = [
            "it.mensa.app.ui.components.MensaCard",
            "androidx.compose.foundation.shape.RoundedCornerShape",
        ],
    ),
)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") tint: Color? = null,
    padding: Dp = 20.dp,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit,
) {
    val shape: Shape = RoundedCornerShape(cornerRadius)
    MensaCard(
        modifier = modifier,
        shape = shape,
        padding = padding,
        content = content,
    )
}
