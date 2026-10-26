package it.mensa.app.features.tableport._components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.support.tr

/** The ENTRY stamp's red. Same as iOS `PassportPalette.stampRed`. */
private val StampRed = Color(0xFFAA2626)

/**
 * The scan button, inked like a rubber stamp on a slip of parchment and set
 * down a little askew. Mirrors iOS `EntryStampButton`.
 */
@Composable
fun EntryScanButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "entryPress",
    )
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = modifier
            .rotate(-2.5f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(8.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .background(PassportPalette.parchment.copy(alpha = 0.95f), shape)
            .border(2.2.dp, StampRed, shape)
            .padding(3.dp)
            .border(1.dp, StampRed.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 23.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = StampRed, modifier = Modifier.size(20.dp))
        Text(
            text = tr("addons.stamp.passport.scan", "ENTRY · SCAN"),
            color = StampRed,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            letterSpacing = 2.2.sp,
        )
    }
}
