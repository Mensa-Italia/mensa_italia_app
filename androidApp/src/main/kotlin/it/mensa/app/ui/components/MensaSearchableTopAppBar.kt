package it.mensa.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import it.mensa.app.ui.theme.EasingEmphasizedAccelerate
import it.mensa.app.ui.theme.EasingEmphasizedDecelerate

/**
 * MensaSearchableTopAppBar — Large emphasized topbar with an inline search mode.
 *
 * - Inactive: standard [MensaTopAppBar] (kicker + headline, scroll-collapsing)
 *   with a `Search` action icon in the trailing slot.
 * - Active: tap on Search morphs the bar into a single-line [TopAppBar] with a
 *   transparent `TextField` in the title slot, back arrow to exit, and a clear
 *   icon when the query is non-empty.
 *
 * Transition uses M3 Expressive motion (`EasingEmphasizedDecelerate` enter,
 * `EasingEmphasizedAccelerate` exit, asymmetric duration).
 *
 * Use this for any list/detail screen where the user needs to filter inline
 * content. Replaces the (incorrect) pattern of stacking an `OutlinedTextField`
 * or `SearchBar` below a `LargeTopAppBar` — those are not the M3 canonical
 * pattern for in-view filtering.
 *
 * @param title main title shown when search is inactive
 * @param kicker optional kicker above the title (uppercase label)
 * @param scrollBehavior used by the large bar when search is inactive
 * @param query current search query (hoisted)
 * @param onQueryChange called when the user edits the query
 * @param searchPlaceholder placeholder text inside the input field
 * @param onBack invoked when the user taps the back arrow in inactive mode
 * @param searchContentDescription a11y label for the Search action icon
 * @param backContentDescription a11y label for the back arrow
 * @param clearContentDescription a11y label for the clear icon
 * @param extraActions additional trailing actions shown in inactive mode,
 *        rendered to the left of the Search icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MensaSearchableTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    query: String,
    onQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    onBack: (() -> Unit)? = null,
    searchContentDescription: String = "Cerca",
    backContentDescription: String = "Indietro",
    clearContentDescription: String = "Pulisci",
    extraActions: @Composable RowScope.() -> Unit = {},
) {
    var searchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(searchActive) {
        if (searchActive) focusRequester.requestFocus()
    }

    AnimatedContent(
        targetState = searchActive,
        transitionSpec = {
            val enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 280,
                    easing = EasingEmphasizedDecelerate,
                ),
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = 280,
                    easing = EasingEmphasizedDecelerate,
                ),
                initialOffsetY = { -it / 8 },
            )
            val exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 180,
                    easing = EasingEmphasizedAccelerate,
                ),
            )
            enter togetherWith exit
        },
        label = "MensaSearchableTopAppBarMorph",
        modifier = modifier,
    ) { active ->
        if (active) {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text(searchPlaceholder) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        onQueryChange("")
                        searchActive = false
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backContentDescription,
                        )
                    }
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = clearContentDescription,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    // surface per AppBarTokens.ContainerColor — small bar
                    // without scrollBehavior stays on body surface
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        } else {
            MensaTopAppBar(
                title = title,
                kicker = kicker,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = backContentDescription,
                            )
                        }
                    }
                },
                actions = {
                    extraActions()
                    IconButton(onClick = { searchActive = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = searchContentDescription,
                        )
                    }
                },
            )
        }
    }
}
