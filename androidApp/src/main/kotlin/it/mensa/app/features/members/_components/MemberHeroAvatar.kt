package it.mensa.app.features.members._components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import it.mensa.app.support.FilesUrl
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.components.mensaImageLoader
import it.mensa.app.ui.components.mensaImageRequest
import it.mensa.app.ui.theme.brandGradient
import it.mensa.shared.model.RegSociModel

/**
 * Hero avatar usato nello `MemberDetailScreen`. Caricamento **progressivo**:
 *
 *  1. mostra subito `?thumb=0x100` — la versione già scaricata da Spotlight
 *     e dalle celle lista, tipicamente in memoria/disk cache di Coil → render
 *     istantaneo, niente flash di placeholder.
 *  2. in parallelo richiede `?thumb=0x500` — versione "retina-grade" per
 *     l'hero a 120dp ad alta densità.
 *  3. quando la 0x500 ha terminato il decode, cross-fade in 350 ms.
 *
 * Se PocketBase non ha quel thumb configurato (response 400) il layer high-res
 * non viene mai mostrato → resta visibile la 0x100, niente artefatti.
 *
 * Mirror della classe iOS [MemberHeroAvatar].
 */
@Composable
fun MemberHeroAvatar(
    member: RegSociModel,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val loader = remember(context) { mensaImageLoader(context) }

    val lowUrl = remember(member) { buildAvatarUrl(member, "0x100") }
    val highUrl = remember(member) { buildAvatarUrl(member, "0x500") }

    val initials = remember(member.name) {
        member.name
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (lowUrl == null) {
            // Niente file utile → fallback iniziali, niente download.
            InitialsBubble(initials = initials, size = size)
            return@Box
        }

        // Layer 1: low-res, sempre visibile.
        CachedAsyncImage(
            model = lowUrl,
            contentDescription = member.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Layer 2: high-res, opacity 0 finché Coil non lo ha decodificato.
        if (highUrl != null) {
            var highReady by remember(member) { mutableStateOf(false) }
            val alpha by animateFloatAsState(
                targetValue = if (highReady) 1f else 0f,
                animationSpec = tween(durationMillis = 350),
                label = "hero-cross-fade",
            )
            val highRequest = remember(highUrl, context) {
                mensaImageRequest(context, highUrl)
            }
            AsyncImage(
                model = highRequest,
                imageLoader = loader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha),
                onState = { state ->
                    if (state is AsyncImagePainter.State.Success) {
                        highReady = true
                    }
                },
            )
        }
    }
}

@Composable
private fun InitialsBubble(initials: String, size: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brandGradient),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun buildAvatarUrl(member: RegSociModel, thumb: String): String? {
    val raw = member.image
    return when {
        raw.isEmpty() -> null
        raw.contains("cloud32.it/Associazioni/img/Uomo-1.png") -> null
        raw.startsWith("http://") || raw.startsWith("https://") -> raw
        else -> FilesUrl.build("members_registry", member.id, raw, thumb)
    }
}
