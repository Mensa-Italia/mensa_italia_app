package it.mensa.app.features.podcasts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.model.Podcast
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastsListScreen(
    onNavigateToEpisodes: (podcastId: String, podcastTitle: String) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val vm: PodcastsListViewModel = koinViewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Track which indices have appeared for staggered animation
    val visibleIndices = remember { mutableStateListOf<Int>() }
    LaunchedEffect(uiState.podcasts) {
        uiState.podcasts.forEachIndexed { idx, _ ->
            if (idx !in visibleIndices) {
                kotlinx.coroutines.delay(minOf(idx, 12) * 60L)
                visibleIndices.add(idx)
            }
        }
    }

    MensaScaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(tr("addons.podcasts.title", fallback = "Podcast")) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("common.back", fallback = "Indietro"),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.loading && uiState.podcasts.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) { LoadingDots() }
            }

            uiState.podcasts.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Headphones,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Text(
                            tr("addons.podcasts.empty", fallback = "Nessun podcast"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            tr("addons.podcasts.empty_description", fallback = "Non sono ancora disponibili podcast."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.loading,
                    onRefresh = vm::refresh,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(
                            items = uiState.podcasts,
                            key = { _, podcast -> podcast.id },
                        ) { idx, podcast ->
                            AnimatedVisibility(
                                visible = idx in visibleIndices,
                                enter = slideInVertically(
                                    animationSpec = spring(dampingRatio = 0.86f, stiffness = 300f),
                                    initialOffsetY = { 56 },
                                ) + fadeIn(),
                            ) {
                                PodcastCard(
                                    podcast = podcast,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onNavigateToEpisodes(podcast.id, podcast.title)
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastCard(
    podcast: Podcast,
    modifier: Modifier = Modifier,
) {
    val episodeCountText = when (podcast.episodesCount) {
        1 -> tr("addons.podcasts.episode_count_one", fallback = "1 episodio")
        else -> "${podcast.episodesCount} episodi"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        ) {
            if (!podcast.imageUrl.isNullOrEmpty()) {
                CachedAsyncImage(
                    model = podcast.imageUrl,
                    contentDescription = podcast.title,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Headphones,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
            }

            // Gradient overlay with title
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                            startY = 0.4f * 200f,
                        ),
                    ),
            )

            // Title + badges at bottom-start
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Play badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.PlayCircle,
                                contentDescription = tr("addons.podcasts.play", fallback = "Riproduci"),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    // Mic badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
                                contentDescription = episodeCountText,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    // Episode count chip
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("${podcast.episodesCount} ${tr("addons.podcasts.episodes", fallback = "EP")}") },
                    )
                }
            }
        }
    }
}
