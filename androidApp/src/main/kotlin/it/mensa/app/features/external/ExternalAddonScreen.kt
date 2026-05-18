package it.mensa.app.features.external

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.app.ui.components.*
import it.mensa.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalAddonScreen(
    addonId: String,
    baseUrl: String,
    title: String = "",
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: ExternalAddonViewModel = koinViewModel(parameters = { parametersOf(addonId) })
    val state by vm.state.collectAsStateWithLifecycle()
    val webViewLoading by vm.webViewLoading.collectAsStateWithLifecycle()

    LaunchedEffect(addonId, baseUrl) {
        vm.load(baseUrl)
    }

    val displayTitle = title.ifEmpty { tr("addons.external.title", fallback = "Addon") }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    MensaScaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MensaTopAppBarSmall(
                title = displayTitle,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = tr("app.back", fallback = "Indietro"))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (val s = state) {
                is ExternalAddonLoadState.Idle,
                is ExternalAddonLoadState.Loading -> {
                    LoadingDots(Modifier.align(Alignment.Center))
                }

                is ExternalAddonLoadState.Ready -> {
                    Column(Modifier.fillMaxSize()) {
                        if (webViewLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        AddonWebView(
                            url = s.url,
                            onGoBack = onBack,
                            onLoadingStart = vm::onWebViewLoadingStart,
                            onLoadingFinish = vm::onWebViewLoadingFinish,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                is ExternalAddonLoadState.Failed -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            modifier = Modifier.padding(bottom = 8.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            tr("addons.external.error", fallback = "Impossibile aprire l'addon"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            s.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * WebView wrapper — logic unchanged, only scaffold updated.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AddonWebView(
    url: String,
    onGoBack: () -> Unit,
    onLoadingStart: () -> Unit,
    onLoadingFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val webView = remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        val targetUrl = request.url?.toString() ?: return false
                        if (targetUrl.contains("svc.mensa.it/goback")) {
                            onGoBack()
                            return true
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        onLoadingStart()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onLoadingFinish()
                    }
                }
                loadUrl(url)
                webView.value = this
            }
        },
        update = { wv ->
            if (wv.url != url) wv.loadUrl(url)
        },
        modifier = modifier,
    )
}
