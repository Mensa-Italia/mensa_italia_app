package it.mensa.app.features.profile.sub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val colorScheme = MaterialTheme.colorScheme

    val appVersion = remember(context) {
        runCatching {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.longVersionCode})"
        }.getOrDefault("1.0")
    }

    val year = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(tr("app.credits.title", fallback = "Crediti")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ── Hero ─────────────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = colorScheme.primary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "M",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onPrimary,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Mensa Italia",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onBackground,
                    )
                    Text(
                        tr("app.credits.tagline", fallback = "L'app ufficiale dei soci"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Author Section ────────────────────────────────────────────────
            item {
                // Section header — titleSmall colore primary (KickerLabel eliminato)
                Text(
                    text = tr("app.credits.section.author_title", fallback = "Sviluppato da"),
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Matteo Sipione") },
                    supportingContent = { Text(tr("app.credits.developed_by", fallback = "Sviluppatore iOS & Android")) },
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Code,
                                    null,
                                    tint = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(start = 72.dp)) }
            item {
                ListItem(
                    headlineContent = { Text("matteo@sipio.it") },
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Email,
                                    null,
                                    tint = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    },
                    trailingContent = {
                        Icon(
                            Icons.Outlined.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    },
                    modifier = Modifier.clickable { uriHandler.openUri("mailto:matteo@sipio.it") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            item {
                Text(
                    tr(
                        "app.credits.author_footer",
                        fallback = "App nativa Android in Jetpack Compose sopra un core Kotlin Multiplatform condiviso con iOS.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            // ── Tech Section ──────────────────────────────────────────────────
            item {
                Text(
                    text = tr("app.credits.section.tech", fallback = "Stack tecnologico"),
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(tr("app.credits.tech.kmp", fallback = "Kotlin Multiplatform · Ktor · SQLDelight")) },
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Code,
                                    null,
                                    tint = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(start = 72.dp)) }
            item {
                ListItem(
                    headlineContent = { Text("Jetpack Compose · Material 3 Expressive") },
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.tertiaryContainer,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Code,
                                    null,
                                    tint = colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(start = 72.dp)) }
            item {
                ListItem(
                    headlineContent = { Text(tr("app.credits.tech.pocketbase", fallback = "PocketBase backend")) },
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Storage,
                                    null,
                                    tint = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            // ── Copyright footer ──────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Mensa Italia",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "© $year · v$appVersion",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Text(
                        tr("app.credits.made_with_love", fallback = "Fatta con cura in Italia"),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
