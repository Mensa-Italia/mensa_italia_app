package it.mensa.app.features.quid._components

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * QuidHtmlRenderer — WebView wrapper in AndroidView.
 *
 * Strategy: wraps the WordPress HTML in a full HTML document with CSS that
 * inherits Material 3 theme colors injected as hex strings. JS disabled.
 * Link clicks are intercepted and opened in a Chrome Custom Tab.
 *
 * Background is transparent so the app surface/scaffold shows through.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun QuidHtmlRenderer(
    html: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    // Resolve theme colors to hex once per composition
    val textColor = remember(colorScheme.onSurface) {
        colorScheme.onSurface.toArgb().toHexColor()
    }
    val secondaryColor = remember(colorScheme.onSurfaceVariant) {
        colorScheme.onSurfaceVariant.toArgb().toHexColor()
    }
    val primaryColor = remember(colorScheme.primary) {
        colorScheme.primary.toArgb().toHexColor()
    }
    val surfaceColor = remember(colorScheme.surface) {
        colorScheme.surface.toArgb().toHexColor()
    }

    val wrappedHtml = remember(html, textColor, secondaryColor, primaryColor) {
        buildHtml(html, textColor, secondaryColor, primaryColor)
    }

    AndroidView(
        modifier = modifier,
        factory = {
            WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                // Transparent background — theme surface shows through
                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        val uri = request.url ?: return false
                        try {
                            CustomTabsIntent.Builder()
                                .build()
                                .launchUrl(context, uri)
                        } catch (_: Exception) {
                            context.startActivity(
                                android.content.Intent(android.content.Intent.ACTION_VIEW, uri),
                            )
                        }
                        return true
                    }
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                null,
                wrappedHtml,
                "text/html",
                "utf-8",
                null,
            )
        },
    )
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun Int.toHexColor(): String {
    return "#%06X".format(this and 0xFFFFFF)
}

private fun buildHtml(
    body: String,
    textColor: String,
    secondaryColor: String,
    primaryColor: String,
): String = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; }
  html, body {
    margin: 0;
    padding: 0;
    background: transparent;
    font-family: Georgia, 'Times New Roman', serif;
    font-size: 17px;
    line-height: 1.65;
    color: $textColor;
    word-wrap: break-word;
    overflow-wrap: break-word;
  }
  body { padding: 0 2px; }
  h1, h2, h3, h4 {
    font-family: Georgia, serif;
    font-weight: bold;
    color: $textColor;
    margin-top: 1.2em;
    margin-bottom: 0.4em;
    line-height: 1.3;
  }
  h1 { font-size: 1.6em; }
  h2 { font-size: 1.35em; }
  h3 { font-size: 1.15em; }
  p {
    margin-top: 0;
    margin-bottom: 1em;
  }
  a { color: $primaryColor; text-decoration: underline; }
  em, i { font-style: italic; }
  strong, b { font-weight: bold; }
  blockquote {
    border-left: 3px solid $primaryColor;
    margin: 1em 0;
    padding-left: 1em;
    font-style: italic;
    color: $secondaryColor;
  }
  img {
    max-width: 100%;
    height: auto;
    border-radius: 4px;
    display: block;
    margin: 0.5em 0;
  }
  figure { margin: 1em 0; }
  figcaption {
    font-size: 0.8em;
    color: $secondaryColor;
    font-style: italic;
    margin-top: 4px;
  }
  pre, code {
    white-space: pre-wrap;
    word-break: break-word;
    overflow-wrap: anywhere;
    font-family: inherit;
    font-size: 0.9em;
    background: rgba(0,0,0,0.06);
    border-radius: 3px;
    padding: 2px 4px;
  }
  pre { padding: 0.8em; border-radius: 6px; }
  ul, ol { padding-left: 1.5em; margin-bottom: 1em; }
  li { margin-bottom: 0.3em; }
  hr {
    border: none;
    border-top: 1px solid rgba(128,128,128,0.3);
    margin: 1.5em 0;
  }
  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.9em;
    margin-bottom: 1em;
  }
  th, td {
    border: 1px solid rgba(128,128,128,0.3);
    padding: 6px 8px;
    text-align: left;
  }
  th { font-weight: bold; background: rgba(0,0,0,0.04); }
</style>
</head>
<body>
$body
</body>
</html>
""".trimIndent()
