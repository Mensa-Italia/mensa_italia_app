package it.mensa.app.features.contacts

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.app.ui.components.*
import it.mensa.app.ui.theme.*
import it.mensa.shared.model.OrgChartMember
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ContactsAddonScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val vm: ContactsAddonViewModel = koinViewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val filteredGroups = remember(uiState) { vm.filteredGroups() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    MensaScaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MensaSearchableTopAppBar(
                title = tr("addons.contacts.title", fallback = "Rubrica Soci"),
                kicker = tr("addons.contacts.kicker", fallback = "CONTATTI"),
                scrollBehavior = scrollBehavior,
                query = uiState.searchQuery,
                onQueryChange = vm::onSearchChange,
                searchPlaceholder = tr("contacts.search_placeholder", fallback = "Cerca un contatto…"),
                onBack = onBack,
                searchContentDescription = tr("common.search", fallback = "Cerca"),
                backContentDescription = tr("common.back", fallback = "Indietro"),
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                targetState = when {
                    uiState.loading -> "loading"
                    filteredGroups.isEmpty() -> "empty"
                    else -> "list"
                },
                transitionSpec = { MensaMotion.heroTransform() },
                label = "ContactsContent",
                modifier = Modifier.fillMaxSize(),
            ) { contentState ->
                when (contentState) {
                    "loading" -> Box(Modifier.fillMaxSize()) {
                        LoadingDots(Modifier.align(Alignment.Center))
                    }
                    "empty" -> Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        IconBadge(
                            icon = Icons.Outlined.Group,
                            variant = IconBadgeVariant.Tertiary,
                            size = 64.dp,
                            iconSize = 32.dp,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (uiState.searchQuery.isEmpty())
                                tr("addons.contacts.empty", fallback = "Nessun contatto disponibile")
                            else
                                tr("search.no_results", fallback = "Nessun risultato"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            filteredGroups.forEach { group ->
                                stickyHeader(key = "group_${group.id}") {
                                    Surface(
                                        color = MaterialTheme.colorScheme.background,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        SectionHeader(
                                            title = vm.localizedGroupTitle(group.title),
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                        )
                                    }
                                }

                                items(group.members, key = { "member_${group.id}_${it.userId}" }) { member ->
                                    ContactRow(
                                        member = member,
                                        onCall = { phone ->
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                                        },
                                        onEmail = { email ->
                                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                                        },
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
private fun ContactRow(
    member: OrgChartMember,
    onCall: (String) -> Unit,
    onEmail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(
                member.name,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        supportingContent = {
            if (member.role.isNotEmpty()) {
                Text(
                    member.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = {
            // Avatar circle — Primary IconBadge for active contact
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (member.image.isNotEmpty()) {
                        CachedAsyncImage(
                            model = member.image,
                            contentDescription = member.name,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = member.name.take(2).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Primary = call
                IconButton(onClick = { onCall(member.userId) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Outlined.Call,
                        contentDescription = tr("contacts.action_call", fallback = "Chiama"),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                // Cyan = email
                IconButton(onClick = { onEmail(member.userId) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Outlined.Email,
                        contentDescription = tr("contacts.action_email", fallback = "Email"),
                        modifier = Modifier.size(20.dp),
                        tint = MensaCyan,
                    )
                }
            }
        },
        modifier = modifier,
    )
}
