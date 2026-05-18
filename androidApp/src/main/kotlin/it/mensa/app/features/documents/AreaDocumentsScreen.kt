package it.mensa.app.features.documents

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.components.MensaSearchableTopAppBar
import it.mensa.app.ui.theme.MensaMotion
import it.mensa.shared.model.DocumentModel
import org.koin.androidx.compose.koinViewModel
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaDocumentsScreen(
    onNavigateToDetail: (String) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val vm: AreaDocumentsViewModel = koinViewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val filtered = remember(uiState) { vm.filtered() }
    val categories = remember(uiState.documents) { vm.categories() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var filterExpanded by remember { mutableStateOf(false) }

    val dateFmt = remember {
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("it", "IT"))
    }

    MensaScaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MensaSearchableTopAppBar(
                title = tr("addons.documents.title", fallback = "Area Documenti"),
                scrollBehavior = scrollBehavior,
                query = uiState.searchQuery,
                onQueryChange = vm::onSearchChange,
                searchPlaceholder = tr("addons.documents.search_placeholder", fallback = "Cerca un documento…"),
                onBack = onBack,
                searchContentDescription = tr("common.search", fallback = "Cerca"),
                backContentDescription = tr("common.back", fallback = "Indietro"),
                extraActions = {
                    if (categories.size > 1) {
                        IconButton(onClick = { filterExpanded = true }) {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = tr("addons.documents.filter", fallback = "Filtra"),
                                tint = if (uiState.selectedCategory != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded = filterExpanded,
                            onDismissRequest = { filterExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(tr("addons.documents.all", fallback = "Tutti")) },
                                leadingIcon = {
                                    RadioButton(selected = uiState.selectedCategory == null, onClick = null)
                                },
                                onClick = { vm.onCategoryChange(null); filterExpanded = false },
                            )
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(vm.localizedCategory(cat)) },
                                    leadingIcon = {
                                        RadioButton(selected = uiState.selectedCategory == cat, onClick = null)
                                    },
                                    onClick = { vm.onCategoryChange(cat); filterExpanded = false },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                targetState = when {
                    uiState.loading && uiState.documents.isEmpty() -> "loading"
                    uiState.documents.isEmpty() -> "empty"
                    filtered.isEmpty() -> "no_match"
                    else -> "list"
                },
                transitionSpec = { MensaMotion.heroTransform() },
                label = "DocumentsContent",
                modifier = Modifier.fillMaxSize(),
            ) { contentState ->
                when (contentState) {
                    "loading" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingDots()
                    }
                    "empty" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                            Text(
                                tr("addons.documents.empty", fallback = "Nessun documento"),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    "no_match" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            tr("search.no_results", fallback = "Nessun risultato"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        PullToRefreshBox(
                            isRefreshing = uiState.loading,
                            onRefresh = vm::refresh,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(filtered, key = { it.id }) { doc ->
                                    val dateStr = remember(doc.created) {
                                        dateFmt.format(Date(doc.created.toEpochMilliseconds()))
                                    }
                                    DocumentRowCard(
                                        doc = doc,
                                        dateString = dateStr,
                                        localizedCat = vm.localizedCategory(doc.category),
                                        onClick = { onNavigateToDetail(doc.id) },
                                        modifier = Modifier.fillMaxWidth(),
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
private fun DocumentRowCard(
    doc: DocumentModel,
    dateString: String,
    localizedCat: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Cyan file-type badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = fileIcon(doc.file),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    doc.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
                val docDesc = doc.description
                if (!docDesc.isNullOrEmpty()) {
                    Text(
                        docDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (doc.category.isNotEmpty()) {
                        Text(
                            localizedCat,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            " · ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Open action badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = tr("addons.documents.open", fallback = "Apri"),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private fun fileIcon(filename: String): ImageVector {
    return when (filename.substringAfterLast('.', "").lowercase()) {
        "pdf" -> Icons.Outlined.PictureAsPdf
        "doc", "docx" -> Icons.Outlined.Description
        "xls", "xlsx", "csv" -> Icons.Outlined.TableChart
        "ppt", "pptx", "key" -> Icons.Outlined.Slideshow
        "jpg", "jpeg", "png", "heic", "webp", "gif" -> Icons.Outlined.Image
        "zip", "rar", "7z" -> Icons.Outlined.Archive
        "mp4", "mov", "m4v" -> Icons.Outlined.Movie
        else -> Icons.Outlined.Description
    }
}
