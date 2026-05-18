package it.mensa.app.features.sigs

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.model.SigModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigDetailScreen(
    sigId: String,
    onBack: () -> Unit = {},
    vm: SigDetailViewModel = koinViewModel(parameters = { parametersOf(sigId) }),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(state.sig?.name ?: "", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = tr("app.back", fallback = "Indietro"),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.loading && state.sig == null -> {
                    LoadingDots(modifier = Modifier.align(Alignment.Center))
                }
                state.sig != null -> {
                    SigDetailContent(
                        sig = state.sig!!,
                        onJoin = { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                    )
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                else -> LoadingDots(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun SigDetailContent(
    sig: SigModel,
    onJoin: (String) -> Unit,
) {
    val imageUrl = remember(sig) {
        if (sig.image.isEmpty()) null
        else if (sig.image.startsWith("http")) sig.image
        else FilesUrl.build("sigs", sig.id, sig.image, "1200x0")
    }

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val heroAlpha by animateFloatAsState(
        if (appeared) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), label = "ha",
    )
    val heroOffset by animateFloatAsState(
        if (appeared) 0f else 16f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), label = "ho",
    )
    val titleAlpha by animateFloatAsState(
        if (appeared) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), label = "ta",
    )
    val titleOffset by animateFloatAsState(
        if (appeared) 0f else 12f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), label = "to",
    )
    val bodyAlpha by animateFloatAsState(
        if (appeared) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), label = "ba",
    )
    val bodyOffset by animateFloatAsState(
        if (appeared) 0f else 12f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), label = "bo",
    )
    val actionsAlpha by animateFloatAsState(
        if (appeared) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), label = "aa",
    )
    val actionsOffset by animateFloatAsState(
        if (appeared) 0f else 12f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), label = "ao",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Hero image with badge overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = heroAlpha
                    translationY = heroOffset * density
                },
        ) {
            if (imageUrl != null) {
                CachedAsyncImage(
                    model = imageUrl,
                    contentDescription = sig.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.FillWidth,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer,
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = sig.name.take(2).uppercase(),
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }

            // Gradient scrim at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                        )
                    ),
            )

            // Community badge capsule
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.4f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = tr("sigs.badge", fallback = "Community"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                }
            }
        }

        // Title + group type chip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = titleAlpha
                    translationY = titleOffset * density
                },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = sig.name,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (sig.groupType.isNotEmpty()) {
                val isFacebook = sig.groupType.contains("facebook", ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = sig.groupType.replace("_", " ").replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        // Description card
        if (sig.description.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = bodyAlpha
                        translationY = bodyOffset * density
                    },
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = tr("sigs.about", fallback = "Descrizione").uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = sig.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Join CTA + members note
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = actionsAlpha
                    translationY = actionsOffset * density
                },
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (sig.link.isNotBlank()) {
                val isFacebook = sig.groupType.contains("facebook", ignoreCase = true)
                Button(
                    onClick = { onJoin(sig.link) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        if (isFacebook)
                            tr("sigs.join_facebook", fallback = "Unisciti al gruppo Facebook")
                        else
                            tr("sigs.join", fallback = "Unisciti alla community")
                    )
                }
            }

            Text(
                tr("sigs.members_unavailable", fallback = "L'elenco iscritti non è ancora disponibile da remoto."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
