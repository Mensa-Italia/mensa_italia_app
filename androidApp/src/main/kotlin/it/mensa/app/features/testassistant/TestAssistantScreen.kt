package it.mensa.app.features.testassistant

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.app.ui.components.*
import it.mensa.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

/**
 * TestAssistantScreen — admin dashboard for testmakers power users.
 * M3 Expressive restyling: MensaScaffold + MensaTopAppBar + MensaCard + PrimaryButton.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestAssistantScreen(
    onBack: () -> Unit = {},
    vm: TestAssistantViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MensaTopAppBarSmall(
                title = tr("addons.test_assistant.title", fallback = "Test Assistant"),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("common.back", fallback = "Indietro"),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        if (vm.hasTestmakersPower()) {
            AuthorizedContent(
                platformUrl = vm.platformUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onOpenPlatform = {
                    runCatching {
                        CustomTabsIntent.Builder().build()
                            .launchUrl(context, Uri.parse(vm.platformUrl))
                    }.onFailure {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(vm.platformUrl)),
                        )
                    }
                },
            )
        } else {
            UnauthorizedState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

@Composable
private fun AuthorizedContent(
    platformUrl: String,
    onOpenPlatform: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SectionHeader(
            title = tr("addons.test_assistant.section.title", fallback = "Piattaforma"),
            kicker = tr("addons.test_assistant.kicker", fallback = "TEST MENSA"),
        )

        // Info card
        MensaCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Primary IconBadge — only one badge needed (admin tool)
                    IconBadge(
                        icon = Icons.Outlined.Science,
                        variant = IconBadgeVariant.Primary,
                        size = 48.dp,
                        iconSize = 24.dp,
                        contentDescription = null,
                    )
                    Text(
                        text = tr(
                            "addons.test_assistant.card_title",
                            fallback = "Sistema testelab Mensa",
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Text(
                    text = tr(
                        "addons.test_assistant.card_description",
                        fallback = "Gestisci e somministra i test ufficiali Mensa Italia tramite la piattaforma cloud32 testelab.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Open platform CTA
        PrimaryButton(
            text = tr("addons.test_assistant.open_platform", fallback = "Apri piattaforma"),
            onClick = onOpenPlatform,
            icon = Icons.AutoMirrored.Outlined.OpenInNew,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun UnauthorizedState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconBadge(
            icon = Icons.Outlined.Lock,
            variant = IconBadgeVariant.Primary,
            size = 72.dp,
            iconSize = 36.dp,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = tr(
                "addons.test_assistant.locked_title",
                fallback = "Riservato ai testmakers",
            ),
            style = ExpressiveTypography.headlineMediumEmphasized,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = tr(
                "addons.test_assistant.locked_description",
                fallback = "Questa area è disponibile solo per i soci con permessi testmakers.",
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
