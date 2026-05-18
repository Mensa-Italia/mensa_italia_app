package it.mensa.app.features.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.profile._components.ProfileRow
import it.mensa.app.features.profile._components.ProfileSectionGroup
import it.mensa.app.features.profile._components.ProfileSectionTone
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.components.MensaSearchAppBar
import it.mensa.app.ui.components.SearchAppBarNotificationsButton
import org.koin.androidx.compose.koinViewModel

/**
 * DiscoverScreen — M3 Expressive restyled.
 *
 * Sections rendered as drenched tonal `ProfileSectionGroup` containers
 * (Primary / Tertiary / Neutral), each row a `ProfileRow` with circular
 * `IconBadge` + shape morph on press. Mirrors the Profile screen idiom
 * so the two tabs share the same Expressive vocabulary.
 */
@Composable
fun DiscoverScreen(
    onCategoryClick: (DiscoverCategory) -> Unit = {},
    onSearchTap: () -> Unit = {},
    vm: DiscoverViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    var sectionsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { sectionsVisible = true }

    MensaScaffold(
        topBar = {
            MensaSearchAppBar(
                placeholder = tr("discover.search_placeholder", fallback = "Cerca soci, eventi, deal…"),
                onSearchTap = onSearchTap,
                inlineActions = {
                    SearchAppBarNotificationsButton(
                        onClick = { onCategoryClick(DiscoverCategory.Notifications) },
                    )
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp,
            ),
        ) {
            if (!uiState.isLoading) {
                uiState.sections.forEachIndexed { sectionIndex, section ->
                    item(key = "section_${section.titleKey}") {
                        Spacer(Modifier.height(if (sectionIndex == 0) 16.dp else 14.dp))
                        AnimatedSection(visible = sectionsVisible, indexDelay = sectionIndex * 60) {
                            ProfileSectionGroup(
                                kicker = tr(section.kickerKey, fallback = section.kickerFallback),
                                title = tr(section.titleKey, fallback = section.titleFallback),
                                tone = toneFor(sectionIndex),
                            ) {
                                section.categories.forEach { category ->
                                    ProfileRow(
                                        icon = category.icon,
                                        title = tr(category.labelKey, fallback = category.labelFallback),
                                        onClick = { onCategoryClick(category) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedSection(
    visible: Boolean,
    indexDelay: Int,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 320, delayMillis = indexDelay)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 320, delayMillis = indexDelay),
                initialOffsetY = { it / 8 },
            ),
    ) {
        content()
    }
}

private fun toneFor(index: Int): ProfileSectionTone = when (index % 3) {
    0 -> ProfileSectionTone.Primary
    1 -> ProfileSectionTone.Tertiary
    else -> ProfileSectionTone.Neutral
}
