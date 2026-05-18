package it.mensa.app.features.members

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.ContactMail
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.members._components.MemberAvatar
import it.mensa.app.features.members._components.MemberHeroAvatar
import it.mensa.app.support.tr
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.model.RegSociModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    memberId: String,
    onBack: () -> Unit = {},
    vm: MemberDetailViewModel = koinViewModel(parameters = { parametersOf(memberId) }),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val member = state.member

    val displayName = member?.name?.split(" ")?.joinToString(" ") { w ->
        w.lowercase().replaceFirstChar { it.uppercaseChar() }
    } ?: ""
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(displayName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = tr("app.back", fallback = "Indietro"),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.loading && member == null -> {
                    LoadingDots(modifier = Modifier.align(Alignment.Center))
                }
                member != null -> {
                    MemberDetailContent(
                        member = member,
                        vm = vm,
                        onOpenUrl = { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        onDial = { phone ->
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                        },
                        onEmail = { email ->
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                        },
                    )
                }
                state.error != null -> {
                    Text(
                        state.error ?: "",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                else -> LoadingDots(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun MemberDetailContent(
    member: RegSociModel,
    vm: MemberDetailViewModel,
    onOpenUrl: (String) -> Unit,
    onDial: (String) -> Unit,
    onEmail: (String) -> Unit,
) {
    val profileRows = remember(member) { vm.profileRows(member) }
    val mensaRows = remember(member) { vm.mensaRows(member) }
    val contactRows = remember(member) { vm.contactRows(member) }
    val sigRows = remember(member) { vm.sigRows(member) }

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val heroScale by animateFloatAsState(
        if (appeared) 1f else 0.85f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow), label = "hs",
    )
    val heroAlpha by animateFloatAsState(
        if (appeared) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), label = "ha",
    )
    val sectionsAlpha by animateFloatAsState(
        if (appeared) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), label = "sa",
    )
    val sectionsOffset by animateFloatAsState(
        if (appeared) 0f else 12f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), label = "so",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Hero: avatar + name + ID + birthday
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = heroScale
                    scaleY = heroScale
                    alpha = heroAlpha
                }
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MemberHeroAvatar(
                member = member,
                size = 120.dp,
                modifier = Modifier.shadow(
                    elevation = 18.dp,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.18f),
                    spotColor = Color.Black.copy(alpha = 0.18f),
                ),
            )

            val displayName = member.name.split(" ").joinToString(" ") { w ->
                w.lowercase().replaceFirstChar { it.uppercaseChar() }
            }
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            if (member.id.isNotEmpty()) {
                Text(
                    text = member.id,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            member.birthdate?.let { bd ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF9C27B0), Color(0xFFE91E63))
                                ),
                                RoundedCornerShape(50),
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Cake,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = formatBirthdate(bd.toEpochMilliseconds()),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Color.White,
                        )
                    }
                }
            }
        }

        // Sections with staggered entrance
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = sectionsOffset * density
                    alpha = sectionsAlpha
                },
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (profileRows.isNotEmpty()) {
                DataSection(
                    title = tr("members.section.profile", fallback = "Anagrafica"),
                    icon = Icons.Outlined.Person,
                    rows = profileRows,
                )
            }

            if (mensaRows.isNotEmpty()) {
                DataSection(
                    title = tr("members.section.mensa", fallback = "Mensa"),
                    icon = Icons.Outlined.StarRate,
                    rows = mensaRows,
                )
            }

            if (contactRows.isNotEmpty()) {
                DataSection(
                    title = tr("members.section.contacts", fallback = "Contatti"),
                    icon = Icons.Outlined.ContactMail,
                    rows = contactRows,
                    onRowClick = { _, value ->
                        when {
                            value.startsWith("http") -> onOpenUrl(value)
                            value.contains("@") -> onEmail(value)
                            value.matches(Regex("[0-9 +()\\-]+")) -> onDial(value)
                        }
                    },
                )
            }

            if (sigRows.isNotEmpty()) {
                DataSection(
                    title = tr("members.section.sig", fallback = "Community"),
                    icon = Icons.Outlined.Groups,
                    rows = sigRows,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DataSection(
    title: String,
    icon: ImageVector,
    rows: List<Pair<String, String>>,
    onRowClick: ((String, String) -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            rows.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(110.dp),
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = if (onRowClick != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                    )
                }
                if (index < rows.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

private fun formatBirthdate(ms: Long): String {
    val date = java.util.Date(ms)
    val fmt = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.ITALIAN)
    return fmt.format(date)
}
