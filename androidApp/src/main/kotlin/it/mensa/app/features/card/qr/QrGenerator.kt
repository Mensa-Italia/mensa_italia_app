package it.mensa.app.features.card.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * QrGenerator — CPU-bound ZXing QR code generation utility.
 *
 * Call on Dispatchers.Default. Returns a [Bitmap] with white background, black modules.
 */
object QrGenerator {

    /**
     * Generate a QR code [Bitmap] from the given [payload].
     *
     * @param payload the string to encode
     * @param sizePx  the output bitmap size in pixels (square)
     * @return [Bitmap] or null on error
     */
    fun generate(payload: String, sizePx: Int = 512): Bitmap? {
        if (payload.isBlank()) return null
        return runCatching {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            )
            val writer = QRCodeWriter()
            val matrix = writer.encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val w = matrix.width
            val h = matrix.height
            val pixels = IntArray(w * h) { idx ->
                val x = idx % w
                val y = idx / w
                if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
                it.setPixels(pixels, 0, w, 0, 0, w, h)
            }
        }.getOrNull()
    }
}
