package it.mensa.app.features.card._components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.mensa.app.features.card.qr.QrGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * QrCodeView — renders a ZXing-generated QR code for [payload].
 *
 * Generation runs on [Dispatchers.Default] (CPU-bound). Shows a placeholder icon
 * while generating or on empty payload.
 *
 * @param payload string to encode in the QR code
 * @param size    composable size (square)
 */
@Composable
fun QrCodeView(
    payload: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    cornerRadius: Dp = 12.dp,
) {
    var bitmap by remember(payload) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(payload) {
        if (payload.isNotBlank()) {
            bitmap = withContext(Dispatchers.Default) {
                QrGenerator.generate(payload, sizePx = 512)
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White),
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "QR code tessera",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.QrCode,
                contentDescription = "QR code",
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}
