package it.mensa.app.features.card._components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.io.File
import java.io.FileOutputStream

/**
 * Render [content] off-screen to a PNG bitmap and launch a system share sheet.
 *
 * Mounts a [ComposeView] inside a transient invisible [FrameLayout] attached to
 * the current Activity decor (so the composition has access to ViewTreeLifecycle
 * + SavedStateRegistry + ViewModelStore), waits for a measure pass, then draws
 * the view to a `Bitmap`. The container is removed before the share intent is
 * launched.
 *
 * Mirrors iOS `CardView.shareButton` which builds a `PrintableCardView` via
 * `ImageRenderer`.
 */
fun shareCardImage(
    activity: Activity,
    widthPx: Int = 1080,
    heightPx: Int = 680,
    content: @Composable () -> Unit,
) {
    val decor = activity.window?.decorView as? ViewGroup ?: return

    val container = FrameLayout(activity).apply {
        layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
        visibility = View.INVISIBLE
    }

    val composeView = ComposeView(activity).apply {
        layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    // Inherit the activity's ViewTree owners so the off-screen composition can
    // resolve lifecycle / view-model / saved-state.
    composeView.setViewTreeLifecycleOwner(decor.findViewTreeLifecycleOwner())
    composeView.setViewTreeViewModelStoreOwner(decor.findViewTreeViewModelStoreOwner())
    composeView.setViewTreeSavedStateRegistryOwner(decor.findViewTreeSavedStateRegistryOwner())
    composeView.setContent(content)

    container.addView(composeView)
    decor.addView(container)

    composeView.post {
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY),
        )
        composeView.layout(0, 0, widthPx, heightPx)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        composeView.draw(Canvas(bitmap))

        decor.removeView(container)

        launchShareIntent(activity, bitmap)
    }
}

private fun launchShareIntent(context: Context, bitmap: Bitmap) {
    val dir = File(context.cacheDir, "shared_card").apply { mkdirs() }
    val file = File(dir, "tessera_mensa.png")
    runCatching {
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    }.onFailure { return }

    val authority = "${context.packageName}.fileprovider"
    val uri = runCatching { FileProvider.getUriForFile(context, authority, file) }
        .getOrElse { return }

    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, null).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(chooser) }
}
