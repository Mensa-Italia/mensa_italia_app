package it.mensa.app.features.members

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.members._components.MemberCellCompact
import it.mensa.app.support.tr
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.components.MensaSearchableTopAppBar
import it.mensa.app.ui.theme.EasingEmphasizedDecelerate
import it.mensa.app.ui.theme.MensaMotion
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MembersDirectoryScreen(
    onMemberClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    vm: MembersDirectoryViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val filtered = remember(state) { vm.filtered(state) }
    val sectioned = remember(filtered) { vm.sectioned(filtered) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MensaSearchableTopAppBar(
                title = tr("members.registry.title", fallback = "Registro Soci"),
                scrollBehavior = scrollBehavior,
                query = state.query,
                onQueryChange = vm::onQueryChange,
                searchPlaceholder = tr("members.search_placeholder", fallback = "Cerca un socio…"),
                onBack = onBack,
                searchContentDescription = tr("common.search", fallback = "Cerca"),
                backContentDescription = tr("common.back", fallback = "Indietro"),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AnimatedContent(
                targetState = when {
                    state.loading && state.members.isEmpty() -> "loading"
                    filtered.isEmpty() -> "empty"
                    else -> "list"
                },
                transitionSpec = { MensaMotion.heroTransform() },
                label = "MembersContent",
                modifier = Modifier.fillMaxSize(),
            ) { contentState ->
                when (contentState) {
                    "loading" -> Box(Modifier.fillMaxSize()) {
                        LoadingDots(modifier = Modifier.align(Alignment.Center))
                    }
                    "empty" -> Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (state.query.isEmpty())
                                tr("members.empty", fallback = "Directory vuota")
                            else
                                tr("members.no_results", fallback = "Nessun socio trovato"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (state.query.isEmpty())
                                tr("members.empty_description", fallback = "Trascina giù per aggiornare la directory.")
                            else
                                tr("members.no_results_description", fallback = "Prova con un altro nome o città."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        var appeared by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { appeared = true }

                        PullToRefreshBox(
                            isRefreshing = state.loading,
                            onRefresh = { vm.refresh() },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp),
                            ) {
                                sectioned.forEach { (letter, members) ->
                                    // Sticky alphabetical header
                                    stickyHeader(key = "letter_$letter") {
                                        Surface(
                                            color = MaterialTheme.colorScheme.background,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                text = letter,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        start = 20.dp,
                                                        end = 20.dp,
                                                        top = 16.dp,
                                                        bottom = 4.dp,
                                                    ),
                                            )
                                        }
                                    }

                                    items(members, key = { it.id }) { member ->
                                        val entranceScale by animateFloatAsState(
                                            targetValue = if (appeared) 1f else 0.95f,
                                            animationSpec = tween(
                                                durationMillis = 300,
                                                easing = EasingEmphasizedDecelerate,
                                            ),
                                            label = "MemberEntrance${member.id}",
                                        )
                                        Column(
                                            modifier = Modifier
                                                .scale(entranceScale)
                                                .clickable { onMemberClick(member.id) },
                                        ) {
                                            MemberCellCompact(
                                                member = member,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                            )
                                            if (member != members.last()) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(start = 72.dp, end = 22.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
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
        }
    }

    state.error?.let { err ->
        AlertDialog(
            onDismissRequest = vm::clearError,
            title = { Text(tr("app.error.title", fallback = "Errore")) },
            text = { Text(err) },
            confirmButton = { TextButton(onClick = vm::clearError) { Text("OK") } },
        )
    }
}
