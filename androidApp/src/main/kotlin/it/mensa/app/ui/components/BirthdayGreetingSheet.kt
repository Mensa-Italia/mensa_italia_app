package it.mensa.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.mensa.app.support.tr
import it.mensa.shared.members.BirthdayWish
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Gli auguri al socio, il giorno del suo compleanno. Solo elementi di
 * sistema: icone Material, una molla di Compose che fa saltare la torta, il
 * bottone primario e un feedback aptico all'apertura. Quando mostrarla lo
 * decide `BirthdayGreeting` (:shared). Mirrors iOS `BirthdayGreetingSheet`.
 * Il harness di debug la monta con `--es mensa_screen birthday_preview`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BirthdayGreetingSheet(wish: BirthdayWish, onDone: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val pop = remember { Animatable(0.6f) }
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDone() }
    }

    LaunchedEffect(Unit) {
        // Un attimo dopo la salita della scheda, cosi' l'animazione si vede.
        delay(350)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    ModalBottomSheet(
        onDismissRequest = onDone,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp).size(168.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                )
                Icon(
                    Icons.Filled.Cake,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp).scale(pop.value),
                )
                Decoration(Icons.Filled.Celebration, Color(0xFFFF2D55), 28.dp, -18f, Alignment.TopStart, pop.value)
                Decoration(Icons.Filled.Redeem, Color(0xFFFF3B30), 28.dp, 12f, Alignment.TopEnd, pop.value)
                Decoration(Icons.Filled.CardGiftcard, Color(0xFFAF52DE), 24.dp, -8f, Alignment.BottomStart, pop.value)
                Decoration(Icons.Filled.AutoAwesome, Color(0xFFFFCC00), 26.dp, 0f, Alignment.BottomEnd, pop.value)
            }

            Text(
                text = tr("birthday_greeting.title", "Buon compleanno, {name}!", "name" to wish.firstName),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = wish.age?.let {
                    tr(
                        "birthday_greeting.body_age",
                        "Oggi compi {age} anni: tutta Mensa Italia ti augura una splendida giornata.",
                        "age" to it,
                    )
                } ?: tr("birthday_greeting.body", "Tutta Mensa Italia ti augura una splendida giornata."),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            PrimaryButton(
                text = tr("birthday_greeting.done", "Grazie!"),
                icon = Icons.Filled.Favorite,
                onClick = close,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Decoration(
    icon: ImageVector,
    color: Color,
    size: Dp,
    rotation: Float,
    alignment: Alignment,
    pop: Float,
) {
    Icon(
        icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier
            .align(alignment)
            .offset(y = if (alignment == Alignment.TopEnd) (-6).dp else 0.dp)
            .rotate(rotation)
            .scale(pop)
            .size(size),
    )
}
