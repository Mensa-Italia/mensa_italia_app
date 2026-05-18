package it.mensa.app.features.publicarea

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.podcasts.PodcastEpisodesScreen
import it.mensa.app.features.podcasts.PodcastsListScreen
import it.mensa.app.features.quid.QuidArticleScreen
import it.mensa.app.features.quid.QuidIssueScreen
import it.mensa.app.features.quid.QuidIssuesScreen
import it.mensa.app.support.tr
import it.mensa.app.ui.root.MensaLogoMark
import it.mensa.shared.model.LocalOfficeAssistantModel
import it.mensa.shared.model.LocalOfficeModel
import it.mensa.shared.model.LocalOfficeTestDateModel
import org.koin.androidx.compose.koinViewModel

/**
 * Pre-login entry — content/structure mirror of iOS `PublicAreaView.swift`.
 *
 * Hero + three native grouped sections + a footer login row:
 *  - "Scopri Mensa": Chi siamo, Gruppi locali
 *  - "Diventa socio" (with footer): Mettiti alla prova, Iscriviti per fare il test
 *  - "Esplora": Eventi pubblici, Podcast, Quid
 *  - "Sei socio? Accedi"
 *
 * Sub-routing is local (no NavHost) — matches the iOS pattern that swaps the
 * detail view inside the same shell.
 */
sealed class PublicRoute {
    data object Landing : PublicRoute()
    data object ChiSiamo : PublicRoute()
    data object Events : PublicRoute()
    data object IQTest : PublicRoute()
    data object LocalOffices : PublicRoute()
    data object Podcasts : PublicRoute()
    data class PodcastEpisodes(val podcastId: String, val podcastTitle: String) : PublicRoute()
    data object QuidIssues : PublicRoute()
    data class QuidIssue(val issueId: Long, val issueName: String) : PublicRoute()
    data class QuidArticle(val articleId: Long) : PublicRoute()
    data class LocalOfficeDetail(val officeId: String) : PublicRoute()
    data class TestSession(
        val testDate: LocalOfficeTestDateModel,
        val office: LocalOfficeModel,
        val allAssistants: List<LocalOfficeAssistantModel>,
    ) : PublicRoute()
    data class MemberContact(val contact: PublicMemberContact, val returnTo: PublicRoute) : PublicRoute()
}

private const val REGISTER_TEST_URL = "https://www.mensa.it/ammissione-tramite-test-ufficiale/"

@Composable
fun PublicAreaScreen(
    onLogin: () -> Unit,
) {
    val audioController = org.koin.compose.koinInject<it.mensa.app.services.audio.AudioPlayerController>()
    val currentTrack by audioController.currentTrack.collectAsStateWithLifecycle()
    val isPresentingFullPlayer by audioController.isPresentingFullPlayer.collectAsStateWithLifecycle()

    var route by remember { mutableStateOf<PublicRoute>(PublicRoute.Landing) }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                if (targetState is PublicRoute.Landing) {
                    fadeIn(tween(250)).togetherWith(fadeOut(tween(200)))
                } else {
                    (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 10 })
                        .togetherWith(fadeOut(tween(200)))
                }
            },
            label = "PublicRouteTransition",
        ) { currentRoute ->
        when (currentRoute) {
            is PublicRoute.Landing -> PublicLanding(
                onChiSiamo = { route = PublicRoute.ChiSiamo },
                onLocalOffices = { route = PublicRoute.LocalOffices },
                onIQTest = { route = PublicRoute.IQTest },
                onEvents = { route = PublicRoute.Events },
                onPodcasts = { route = PublicRoute.Podcasts },
                onQuid = { route = PublicRoute.QuidIssues },
                onLogin = onLogin,
            )
            is PublicRoute.ChiSiamo -> ChiSiamoScreen(onBack = { route = PublicRoute.Landing })
            is PublicRoute.Events -> PublicEventsScreen(onBack = { route = PublicRoute.Landing })
            is PublicRoute.IQTest -> IQTestScreen(onBack = { route = PublicRoute.Landing })
            is PublicRoute.LocalOffices -> PublicLocalOfficesListScreen(
                onOfficeClick = { officeId -> route = PublicRoute.LocalOfficeDetail(officeId) },
                onBack = { route = PublicRoute.Landing },
            )
            is PublicRoute.Podcasts -> PodcastsListScreen(
                onNavigateToEpisodes = { id, title -> route = PublicRoute.PodcastEpisodes(id, title) },
                onBack = { route = PublicRoute.Landing },
            )
            is PublicRoute.PodcastEpisodes -> {
                val current = currentRoute
                PodcastEpisodesScreen(
                    podcastId = current.podcastId,
                    podcastTitle = current.podcastTitle,
                    onBack = { route = PublicRoute.Podcasts },
                    vm = org.koin.androidx.compose.koinViewModel(
                        key = "public_podcast_episodes_${current.podcastId}",
                        parameters = { org.koin.core.parameter.parametersOf(current.podcastId) },
                    ),
                )
            }
            is PublicRoute.QuidIssues -> QuidIssuesScreen(
                onNavigateToIssue = { id, name -> route = PublicRoute.QuidIssue(id, name) },
                onNavigateToPdf = { _, _ -> /* PDF viewer skipped pre-login */ },
                onBack = { route = PublicRoute.Landing },
            )
            is PublicRoute.QuidIssue -> {
                val current = currentRoute
                // Force a fresh ViewModel per issueId — Koin caches the VM by
                // class alone and would otherwise hand back the first issue's
                // VM regardless of the new parameters.
                QuidIssueScreen(
                    issueId = current.issueId,
                    issueName = current.issueName,
                    onBack = { route = PublicRoute.QuidIssues },
                    onNavigateToArticle = { articleId -> route = PublicRoute.QuidArticle(articleId) },
                    viewModel = org.koin.androidx.compose.koinViewModel(
                        key = "public_quid_issue_${current.issueId}",
                        parameters = {
                            org.koin.core.parameter.parametersOf(current.issueId, current.issueName)
                        },
                    ),
                )
            }
            is PublicRoute.QuidArticle -> {
                val current = currentRoute
                QuidArticleScreen(
                    articleId = current.articleId,
                    onBack = { route = PublicRoute.QuidIssues },
                    viewModel = org.koin.androidx.compose.koinViewModel(
                        key = "public_quid_article_${current.articleId}",
                        parameters = {
                            org.koin.core.parameter.parametersOf(current.articleId)
                        },
                    ),
                )
            }
            is PublicRoute.LocalOfficeDetail -> {
                val current = currentRoute
                PublicLocalOfficeDetailRouteHost(
                    officeId = current.officeId,
                    onBack = { route = PublicRoute.LocalOffices },
                    onTestSessionClick = { td, office, assistants ->
                        route = PublicRoute.TestSession(td, office, assistants)
                    },
                    onMemberClick = { contact ->
                        route = PublicRoute.MemberContact(contact, current)
                    },
                )
            }
            is PublicRoute.TestSession -> {
                val current = currentRoute
                PublicTestSessionDetailScreen(
                    testDate = current.testDate,
                    office = current.office,
                    allAssistants = current.allAssistants,
                    onMemberClick = { contact ->
                        route = PublicRoute.MemberContact(contact, current)
                    },
                    onBack = { route = PublicRoute.LocalOfficeDetail(current.office.id) },
                )
            }
            is PublicRoute.MemberContact -> {
                val current = currentRoute
                PublicMemberContactScreen(
                    contact = current.contact,
                    onBack = { route = current.returnTo },
                )
            }
        }
        }

        // ── Mini audio player (above whatever screen is on top) ──────────────
        if (currentTrack != null) {
            it.mensa.app.ui.components.MiniAudioPlayer(
                controller = audioController,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp, start = 12.dp, end = 12.dp),
            )
        }
    }

    // ── Full-screen now-playing sheet (Dialog overlay) ───────────────────────
    if (isPresentingFullPlayer) {
        it.mensa.app.ui.components.NowPlayingFullScreenView(
            controller = audioController,
            onDismiss = { audioController.dismissFullPlayer() },
        )
    }
}

@Composable
private fun PublicLocalOfficeDetailRouteHost(
    officeId: String,
    onBack: () -> Unit,
    onTestSessionClick: (
        testDate: LocalOfficeTestDateModel,
        office: LocalOfficeModel,
        allAssistants: List<LocalOfficeAssistantModel>,
    ) -> Unit,
    onMemberClick: (PublicMemberContact) -> Unit,
) {
    val vm: PublicLocalOfficeDetailViewModel = org.koin.androidx.compose.koinViewModel(
        key = "public_office_$officeId",
        parameters = { org.koin.core.parameter.parametersOf(officeId) },
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    PublicLocalOfficeDetailScreen(
        officeId = officeId,
        onBack = onBack,
        onTestSessionClick = { td ->
            val office = state.office ?: return@PublicLocalOfficeDetailScreen
            onTestSessionClick(td, office, state.assistants)
        },
        onMemberClick = onMemberClick,
        vm = vm,
    )
}

// ─── Landing ──────────────────────────────────────────────────────────────────

@Composable
private fun PublicLanding(
    onChiSiamo: () -> Unit,
    onLocalOffices: () -> Unit,
    onIQTest: () -> Unit,
    onEvents: () -> Unit,
    onPodcasts: () -> Unit,
    onQuid: () -> Unit,
    onLogin: () -> Unit,
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        item(key = "hero") { HeroRow() }

        item(key = "section_discover") {
            SectionHeader(tr("public.area.section.discover", fallback = "Scopri Mensa"))
            GroupedCard {
                NavRow(
                    icon = Icons.Outlined.Info,
                    title = tr("public.area.about", fallback = "Chi siamo"),
                    onClick = onChiSiamo,
                )
                RowDivider()
                NavRow(
                    icon = Icons.Outlined.Business,
                    title = tr("public.area.local_offices", fallback = "Gruppi locali"),
                    onClick = onLocalOffices,
                )
            }
        }

        item(key = "section_become_member") {
            SectionHeader(tr("public.area.section.become_member", fallback = "Diventa socio"))
            GroupedCard {
                NavRow(
                    icon = Icons.Outlined.Psychology,
                    title = tr("public.area.try_test", fallback = "Mettiti alla prova"),
                    onClick = onIQTest,
                )
                RowDivider()
                NavRow(
                    icon = Icons.Outlined.PersonAdd,
                    title = tr("public.area.register_test", fallback = "Iscriviti per fare il test"),
                    trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REGISTER_TEST_URL)))
                        }
                    },
                )
            }
            SectionFooter(
                tr(
                    "public.area.section.become_member_footer",
                    fallback = "Prova un test ufficiale d'esempio in-app oppure prenota il test ufficiale sul sito di Mensa Italia.",
                ),
            )
        }

        item(key = "section_explore") {
            SectionHeader(tr("public.area.section.explore", fallback = "Esplora"))
            GroupedCard {
                NavRow(
                    icon = Icons.Outlined.CalendarMonth,
                    title = tr("public.area.events", fallback = "Eventi pubblici"),
                    onClick = onEvents,
                )
                RowDivider()
                NavRow(
                    icon = Icons.Outlined.Headphones,
                    title = tr("public.area.podcasts", fallback = "Podcast"),
                    onClick = onPodcasts,
                )
                RowDivider()
                NavRow(
                    icon = Icons.Outlined.Book,
                    title = tr("public.area.quid", fallback = "Quid"),
                    onClick = onQuid,
                )
            }
        }

        item(key = "section_login") {
            GroupedCard(topSpacing = 24.dp) {
                NavRow(
                    icon = Icons.Outlined.Login,
                    title = tr("public.area.member_login", fallback = "Sei socio? Accedi"),
                    onClick = onLogin,
                )
            }
            SectionFooter(tr("public.area.member_login_footer", fallback = "Torni alla schermata di accesso."))
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Hero ─────────────────────────────────────────────────────────────────────

@Composable
private fun HeroRow() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MensaLogoMark(size = 72, inBlueBadge = false)
        Text(
            text = "Mensa Italia",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "The High I.Q. Society",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Native-feeling grouped list helpers ──────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(top = 24.dp, bottom = 6.dp),
    )
}

@Composable
private fun SectionFooter(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(top = 6.dp, bottom = 4.dp),
    )
}

@Composable
private fun GroupedCard(
    topSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    if (topSpacing > 0.dp) Spacer(Modifier.height(topSpacing))
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column { content() }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun NavRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

