package it.mensa.app.features.publicarea

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.support.tr
import it.mensa.app.ui.root.MensaLogoMark

/**
 * "Chi siamo" pre-login info page. Content parity with iOS
 * `ChiSiamoView.swift`: hero + 6 grouped sections (Numbers, What is Mensa,
 * Mensa Italia, What we do, How to join, Contacts).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChiSiamoScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("public.chi_siamo.title", fallback = "Chi siamo")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = tr("common.back", fallback = "Indietro"),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { Hero() }

            // ── In numeri ───────────────────────────────────────────────────
            item {
                CSSectionHeader(tr("public.chi_siamo.section.numbers", fallback = "In numeri"))
                CSGroupedCard {
                    StatRow(value = "2%", caption = tr("public.chi_siamo.stat.population", fallback = "della popolazione"))
                    CSRowDivider()
                    StatRow(value = "150.000+", caption = tr("public.chi_siamo.stat.members_world", fallback = "soci nel mondo"))
                    CSRowDivider()
                    StatRow(value = "100+", caption = tr("public.chi_siamo.stat.countries", fallback = "Paesi"))
                    CSRowDivider()
                    StatRow(value = "1946", caption = tr("public.chi_siamo.stat.founded", fallback = "anno di fondazione"))
                }
            }

            // ── Cos'è il Mensa ──────────────────────────────────────────────
            item {
                CSSectionHeader(tr("public.chi_siamo.section.what_is", fallback = "Cos'è il Mensa"))
                CSGroupedCard {
                    ParagraphRow(
                        tr(
                            "public.chi_siamo.what_is.body",
                            fallback = "Mensa è un club internazionale di persone curiose. L'unico requisito per entrare è un test di logica: se rientri nel 2% più alto della popolazione, sei dentro. Da lì in poi non conta più cosa hai studiato, che lavoro fai o quanti anni hai. Il nome viene dal latino mensa, la tavola rotonda: tra soci si sta tutti allo stesso livello.",
                        ),
                    )
                }
            }

            // ── Mensa Italia ────────────────────────────────────────────────
            item {
                CSSectionHeader("Mensa Italia")
                CSGroupedCard {
                    ParagraphRow(
                        tr(
                            "public.chi_siamo.mensa_italia.body",
                            fallback = "Dal 1983 siamo la sezione italiana di Mensa. Oltre 2.600 soci in venti gruppi locali, dal Trentino alla Sicilia, che si incontrano per cene, conferenze, weekend di giochi e progetti comuni. Niente politica, niente religione, niente scopo di lucro.",
                        ),
                    )
                }
            }

            // ── Cosa facciamo ───────────────────────────────────────────────
            item {
                CSSectionHeader(tr("public.chi_siamo.section.what_we_do", fallback = "Cosa facciamo"))
                CSGroupedCard {
                    ActivityRow(
                        icon = Icons.Outlined.Group,
                        title = tr("public.chi_siamo.do.local.title", fallback = "Gruppi locali"),
                        subtitle = tr("public.chi_siamo.do.local.subtitle", fallback = "Aperitivi, cene, gite, vicino a te"),
                    )
                    CSRowDivider()
                    ActivityRow(
                        icon = Icons.Outlined.EmojiEvents,
                        title = tr("public.chi_siamo.do.contests.title", fallback = "Concorsi"),
                        subtitle = tr("public.chi_siamo.do.contests.subtitle", fallback = "Il Brain e Mensa Ludo, ogni anno"),
                    )
                    CSRowDivider()
                    ActivityRow(
                        icon = Icons.Outlined.Headphones,
                        title = tr("public.chi_siamo.do.podcasts.title", fallback = "Podcast"),
                        subtitle = "Brainwaves, She Talks, Mensa Talk",
                    )
                    CSRowDivider()
                    ActivityRow(
                        icon = Icons.Outlined.Book,
                        title = tr("public.chi_siamo.do.quid.title", fallback = "Rivista QUID"),
                        subtitle = tr("public.chi_siamo.do.quid.subtitle", fallback = "Approfondimenti scritti dai soci"),
                    )
                    CSRowDivider()
                    ActivityRow(
                        icon = Icons.Outlined.Lightbulb,
                        title = tr("public.chi_siamo.do.research.title", fallback = "Ricerca"),
                        subtitle = tr("public.chi_siamo.do.research.subtitle", fallback = "Cosa significa davvero \"essere intelligenti\""),
                    )
                }
            }

            // ── Come si entra ───────────────────────────────────────────────
            item {
                CSSectionHeader(tr("public.chi_siamo.section.how_to_join", fallback = "Come si entra"))
                CSGroupedCard {
                    ParagraphRow(
                        tr(
                            "public.chi_siamo.how_to.test_body",
                            fallback = "Il test ufficiale dura circa 20 minuti: 45 sequenze di figure, niente matematica, niente cultura generale. L'esito arriva in due settimane. Se sei nel 2% più alto, ti scriviamo per darti il benvenuto.",
                        ),
                    )
                    CSRowDivider()
                    ParagraphRow(
                        tr(
                            "public.chi_siamo.how_to.bypass_body",
                            fallback = "Hai già fatto un test del QI riconosciuto altrove? Puoi chiedere l'ammissione diretta senza rifarlo.",
                        ),
                    )
                    CSRowDivider()
                    LabeledRow(
                        label = tr("public.chi_siamo.how_to.fees_label", fallback = "Quote annuali"),
                        value = "€25 primo anno · €50 standard · €25 under 26",
                    )
                }
            }

            // ── Contatti ────────────────────────────────────────────────────
            item {
                CSSectionHeader(tr("public.chi_siamo.section.contacts", fallback = "Contatti"))
                CSGroupedCard {
                    ContactRow(
                        icon = Icons.Outlined.Mail,
                        title = tr("public.chi_siamo.contact.email.title", fallback = "Scrivici una mail"),
                        subtitle = "info@mensa.it",
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:info@mensa.it")),
                                )
                            }
                        },
                    )
                    CSRowDivider()
                    ContactRow(
                        icon = Icons.Outlined.LocationOn,
                        title = tr("public.chi_siamo.contact.address.title", fallback = "Sede nazionale, Milano"),
                        subtitle = "Viale Lunigiana 7",
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("geo:0,0?q=Viale+Lunigiana+7+20125+Milano"),
                                    ),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

// ─── Hero ─────────────────────────────────────────────────────────────────────

@Composable
private fun Hero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MensaLogoMark(size = 56, inBlueBadge = false)
        Text(
            text = "Mensa",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = tr("public.chi_siamo.hero.tagline", fallback = "Persone curiose. Un test in comune."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Grouped-list primitives ──────────────────────────────────────────────────

@Composable
private fun CSSectionHeader(text: String) {
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
private fun CSGroupedCard(content: @Composable () -> Unit) {
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
private fun CSRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun StatRow(value: String, caption: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = caption,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ParagraphRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ActivityRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(28.dp),
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(28.dp),
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
