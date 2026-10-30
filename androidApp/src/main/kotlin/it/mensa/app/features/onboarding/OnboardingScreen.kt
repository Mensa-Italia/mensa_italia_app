package it.mensa.app.features.onboarding

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.app.ui.components.ColumnSplit
import it.mensa.app.ui.components.ColumnSplitBox
import kotlin.math.absoluteValue

/**
 * OnboardingScreen — multi-page intro with HorizontalPager.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
) {
    val vm = androidx.lifecycle.viewmodel.compose.viewModel<OnboardingViewModel>(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                OnboardingViewModel(onComplete) as T
        }
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = 0) { uiState.totalPages }

    // Sync pager → VM
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            vm.onPageSelected(page)
        }
    }

    // Two halves on a wide screen (a foldable open, a tablet): the artwork on
    // the left, the text and controls on the right; on a foldable they meet at
    // the hinge. Mirrors iOS `OnboardingSplit`.
    ColumnSplitBox(Modifier.fillMaxSize()) { split ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.primaryContainer,
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { pageIndex ->
                val page = uiState.pages[pageIndex]
                val pageOffset = ((pagerState.currentPage - pageIndex) +
                        pagerState.currentPageOffsetFraction).absoluteValue

                OnboardingPageContent(
                    page = page,
                    pageIndex = pageIndex,
                    pageOffset = pageOffset,
                    split = split,
                )
            }

            // Bottom area: indicator + CTA. Open, the controls sit under the
            // text, right of the hinge; the left half keeps just the artwork.
            Row(Modifier.fillMaxWidth()) {
                if (split != null) Spacer(Modifier.width(split.leftWidth + split.gap))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Breathing capsule page indicator
                    BreathingDots(
                        count = uiState.totalPages,
                        current = uiState.currentPage,
                    )

                    // CTA
                    Button(
                        onClick = {
                            if (uiState.isLastPage) {
                                vm.onComplete()
                            } else {
                                vm.onNext()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Text(
                            if (uiState.isLastPage)
                                tr("onboarding.cta.start", "Inizia")
                            else
                                tr("onboarding.cta.continue", "Continua")
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
    }
}

// ─── Page Content ─────────────────────────────────────────────────────────────

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    pageIndex: Int,
    pageOffset: Float,
    split: ColumnSplit?,
) {
    val scale by animateFloatAsState(
        targetValue = if (pageOffset < 0.5f) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "PageScale_$pageIndex",
    )
    val alpha by animateFloatAsState(
        targetValue = if (pageOffset < 0.5f) 1f else 0.5f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "PageAlpha_$pageIndex",
    )

    val pageModifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }

    if (split != null) {
        // Open: the artwork on the left, the text on the right. Mirrors iOS
        // `OnboardingPageContainer` with a split.
        Row(modifier = pageModifier, verticalAlignment = Alignment.CenterVertically) {
            BoxWithConstraints(
                modifier = Modifier.width(split.leftWidth).fillMaxHeight().padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                OnboardingHeroIcon(pageIndex = pageIndex, size = minOf(180.dp, maxHeight * 0.7f, maxWidth * 0.8f))
            }
            Spacer(Modifier.width(split.gap))
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                OnboardingCopy(page)
            }
        }
        return
    }

    Column(
        modifier = pageModifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        OnboardingHeroIcon(pageIndex = pageIndex)

        Spacer(Modifier.height(40.dp))

        OnboardingCopy(page)

        Spacer(Modifier.height(60.dp))
    }
}

/** A page's title and subtitle. */
@Composable
private fun OnboardingCopy(page: OnboardingPage) {
    Text(
        text = page.title,
        style = MaterialTheme.typography.headlineLarge,
        color = Color.White,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(12.dp))

    Text(
        text = page.subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.72f),
        textAlign = TextAlign.Center,
    )
}

// ─── Hero Icon Placeholder ────────────────────────────────────────────────────

@Composable
private fun OnboardingHeroIcon(pageIndex: Int, size: Dp = 180.dp) {
    val icon: ImageVector = when (pageIndex) {
        0 -> Icons.Outlined.AutoAwesome
        1 -> Icons.Outlined.CalendarMonth
        2 -> Icons.Outlined.Badge
        else -> Icons.Outlined.Search
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(size * (96f / 180f)),
        )
    }
}

// ─── Breathing Dots Indicator ─────────────────────────────────────────────────

@Composable
private fun BreathingDots(count: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val isActive = i == current
            val width by animateDpAsState(
                targetValue = if (isActive) 26.dp else 7.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "DotWidth_$i",
            )
            val dotAlpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.35f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "DotAlpha_$i",
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = dotAlpha)
                    ),
            )
        }
    }
}
