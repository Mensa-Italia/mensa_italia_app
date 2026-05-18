package it.mensa.app.features.quid

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.quid._components.QuidHtmlRenderer
import it.mensa.app.features.quid._components.QuidNarrationBanner
import it.mensa.app.features.quid._components.buildAudioTrack
import it.mensa.app.features.quid._components.quidTrackId
import it.mensa.app.features.quid.util.QuidDateParser
import it.mensa.app.services.audio.AudioPlayerController
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.components.LoadingDots
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuidArticleScreen(
    articleId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuidArticleViewModel = koinViewModel(parameters = { parametersOf(articleId) }),
    audioController: AudioPlayerController = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val currentTrack by audioController.currentTrack.collectAsState()
    val isPlaying by audioController.isPlaying.collectAsState()

    val isThisArticlePlaying = currentTrack?.id == quidTrackId(articleId) && isPlaying

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.loading && state.article == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingDots()
                }
            }

            state.error != null && state.article == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
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
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            state.article != null -> {
                val article = state.article!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Hero cover image — full bleed
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                    ) {
                        if (article.coverImageUrl != null) {
                            CachedAsyncImage(
                                model = article.coverImageUrl,
                                contentDescription = article.titlePlain,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                            )
                        }

                        // Bottom gradient fade for legibility
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0f), Color.Black.copy(alpha = 0.5f)),
                                    ),
                                ),
                        )

                        // Article icon badge bottom-start
                        Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoStories,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                            .padding(top = 20.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Category label
                        if (article.categoryNames.isNotEmpty()) {
                            Text(
                                text = article.categoryNames.joinToString(" · ").uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        // Title
                        Text(
                            text = article.titlePlain,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        // Byline with hairline rules
                        BylineRow(dateStr = article.date)

                        // Narration banner
                        state.audio?.let { audio ->
                            QuidNarrationBanner(
                                audio = audio,
                                articleId = articleId,
                                articleTitle = article.titlePlain,
                                artworkUrl = article.coverImageUrl,
                            )
                        }

                        // Body HTML
                        if (article.contentHtml.isNotEmpty()) {
                            QuidHtmlRenderer(
                                html = article.contentHtml,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(600.dp),
                            )
                        }

                        // CTA — open on site
                        if (article.link.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val uri = Uri.parse(article.link)
                                        CustomTabsIntent.Builder().build().launchUrl(context, uri)
                                    } catch (_: Exception) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.link)))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    Icons.Outlined.OpenInBrowser,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(tr("addons.quid.open_on_site", fallback = "Apri sul sito"))
                            }
                        }
                    }
                }

                // Transparent top app bar overlaid on hero
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = tr("app.back", fallback = "Indietro"),
                                tint = Color.White,
                            )
                        }
                    },
                    actions = {
                        state.audio?.let { audio ->
                            IconButton(onClick = {
                                val trackId = quidTrackId(articleId)
                                if (currentTrack?.id == trackId) {
                                    audioController.togglePlayPause()
                                } else {
                                    audioController.play(
                                        buildAudioTrack(audio, articleId, article.titlePlain, article.coverImageUrl),
                                    )
                                }
                            }) {
                                Icon(
                                    imageVector = if (isThisArticlePlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isThisArticlePlaying)
                                        tr("addons.quid.audio.pause", fallback = "Pausa")
                                    else
                                        tr("addons.quid.audio.play", fallback = "Riproduci"),
                                    tint = Color.White,
                                )
                            }
                        }

                        if (article.link.isNotEmpty()) {
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, article.link)
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }) {
                                Icon(
                                    Icons.Outlined.Share,
                                    contentDescription = tr("app.share", fallback = "Condividi"),
                                    tint = Color.White,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}

// ─── Byline row ───────────────────────────────────────────────────────────────

@Composable
private fun BylineRow(dateStr: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
        )
        Text(
            text = QuidDateParser.longDateText(dateStr),
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 2.dp),
        )
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
        )
    }
}
