package it.mensa.app.features.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import it.mensa.app.navigation.drillNavGraphs
import it.mensa.app.support.tr
import it.mensa.app.ui.components.IndexDetailLayout
import it.mensa.app.ui.components.IndexDetailPane
import it.mensa.app.ui.components.openInPane
import it.mensa.app.ui.components.paneRoot

/**
 * SearchTab — global search, on a phone or beside the result it opened.
 *
 * Narrow, it is [SearchScreen] alone and a result is pushed on the shell's
 * `NavController` ([onItemClick], as before). With room for two columns the
 * results stay on the left and the chosen one opens on the right, in a
 * detail pane with a back stack of its own, so picking another result drops
 * what was pushed inside the previous one. Nothing is open at first.
 *
 * Mirrors iOS `SearchView` (`NavigationSplitView`, one selection of every
 * kind of result, "Nessun risultato aperto" until one is picked).
 */
@Composable
fun SearchTab(
    onItemClick: (HydratedHit) -> Unit,
    onBack: () -> Unit,
) {
    val pane = rememberNavController()
    val paneRoot = pane.paneRoot()
    var selectedHitId by rememberSaveable { mutableStateOf<String?>(null) }
    // The pane emptied (back out of the result): nothing is selected any more.
    LaunchedEffect(paneRoot) { if (paneRoot == null) selectedHitId = null }

    IndexDetailLayout(
        collapsed = { SearchScreen(onItemClick = onItemClick, onBack = onBack) },
        index = {
            SearchScreen(
                onItemClick = { hit ->
                    hit.toDetailRoute()?.let { route ->
                        selectedHitId = hit.id
                        pane.openInPane(route)
                    }
                },
                onBack = onBack,
                selectedHitId = selectedHitId,
            )
        },
        detail = {
            IndexDetailPane(
                navController = pane,
                empty = { SearchDetailEmpty() },
            ) { drillNavGraphs(pane) }
        },
    )
}

/** The detail column while no result is open. */
@Composable
private fun SearchDetailEmpty() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = tr("app.search.detail_empty.title", "Nessun risultato aperto"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = tr("app.search.detail_empty.message", "Scegli un risultato per vederlo qui."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
