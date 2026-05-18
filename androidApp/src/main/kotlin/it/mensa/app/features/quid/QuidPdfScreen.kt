package it.mensa.app.features.quid

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.app.ui.components.LoadingDots
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * QuidPdfScreen — full-screen PDF viewer for legacy Quid issues.
 *
 * Mirrors iOS QuidPDFViewer.swift strategy:
 * - Downloads PDF to cacheDir/quid_pdfs/ via QuidPdfViewModel
 * - Renders via Android system Intent (ACTION_VIEW pdf) — delegates to installed PDF viewer
 * - Shows loading + error states
 *
 * System PDF viewer intent approach (instead of PdfRenderer pager):
 * The iOS equivalent uses QuickLook which is also a system component.
 * Android equivalent = ACTION_VIEW with application/pdf to system PDF app.
 * For a fully in-app rendering, use QuidPdfRendererScreen (see TODO).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuidPdfScreen(
    pdfUrl: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuidPdfViewModel = koinViewModel(parameters = { parametersOf(pdfUrl) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // When file is ready, launch the system PDF viewer immediately
    state.localFile?.let { file ->
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // Only open once (avoid re-triggering on recomposition)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback: open in browser
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl)))
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = tr("app.back", fallback = "Indietro"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.downloading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        LoadingDots()
                        Text(
                            text = tr("addons.quid.loading", fallback = "Caricamento PDF…"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = tr("addons.quid.error", fallback = "Errore"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = state.error ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        // Fallback: open in browser
                        Button(onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl)))
                            } catch (_: Exception) { }
                        }) {
                            Text(tr("addons.quid.open_on_site", fallback = "Apri nel browser"))
                        }
                    }
                }

                state.localFile != null -> {
                    // File ready — system PDF viewer launched; show a "success" hint
                    Text(
                        text = tr("addons.quid.pdf_opened", fallback = "PDF aperto nell'app PDF di sistema"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            }
        }
    }
}
