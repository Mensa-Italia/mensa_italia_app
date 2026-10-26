package it.mensa.app.features.today._components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.mensa.app.features.members._components.MemberAvatar
import it.mensa.app.support.tr
import it.mensa.shared.members.Birthday
import kotlinx.coroutines.launch

private const val MAX_FACES = 5

/**
 * I soci che compiono gli anni oggi, come nella Home della vecchia app: gli
 * avatar sovrapposti e, toccandoli, l'elenco con gli anni e il profilo di
 * ciascuno. Chi festeggia lo decide `Birthdays` (:shared). Mirrors iOS
 * `BirthdaysCard`.
 */
@Composable
fun BirthdaysCard(
    birthdays: List<Birthday>,
    onMemberClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showList by remember { mutableStateOf(false) }

    Card(
        onClick = { showList = true },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Faces(birthdays)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline(birthdays),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tr("today.birthdays.cta", fallback = "Fagli gli auguri"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showList) {
        BirthdaysListSheet(
            birthdays = birthdays,
            onDismiss = { showList = false },
            onMemberClick = { id ->
                showList = false
                onMemberClick(id)
            },
        )
    }
}

/** Fino a cinque volti, ognuno sopra il precedente per meta'; oltre, un "+N". */
@Composable
private fun Faces(birthdays: List<Birthday>) {
    val shown = birthdays.take(MAX_FACES)
    val extra = birthdays.size - shown.size
    val ring = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
        shown.forEach { b ->
            MemberAvatar(
                member = b.member,
                size = 36.dp,
                modifier = Modifier.border(2.dp, ring, CircleShape),
            )
        }
        if (extra > 0) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(36.dp).border(2.dp, ring, CircleShape),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+$extra",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun headline(birthdays: List<Birthday>): String {
    val only = birthdays.singleOrNull()
    return if (only != null) {
        tr("today.birthdays.one", "{name} compie gli anni oggi", "name" to memberDisplayName(only.member.name))
    } else {
        tr("today.birthdays.many", "{count} soci compiono gli anni oggi", "count" to birthdays.size)
    }
}

/** L'elenco dei festeggiati, con gli anni; ognuno porta al suo profilo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthdaysListSheet(
    birthdays: List<Birthday>,
    onDismiss: () -> Unit,
    onMemberClick: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = tr("today.birthdays.title", fallback = "Compleanni di oggi"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp),
        )
        LazyColumn(modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            items(birthdays, key = { it.member.id }) { b ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { sheetState.hide() }
                                .invokeOnCompletion { onMemberClick(b.member.id) }
                        }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    MemberAvatar(member = b.member, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = memberDisplayName(b.member.name),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val age = b.age
                        val subtitle = when {
                            age != null -> tr("today.birthdays.age", "Compie {age} anni", "age" to age)
                            else -> b.member.state
                        }
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Il registro scrive i nomi in maiuscolo: "MARIO ROSSI" → "Mario Rossi". */
private fun memberDisplayName(raw: String): String =
    raw.split(" ").filter { it.isNotEmpty() }
        .joinToString(" ") { word -> word.lowercase().replaceFirstChar { it.uppercaseChar() } }
