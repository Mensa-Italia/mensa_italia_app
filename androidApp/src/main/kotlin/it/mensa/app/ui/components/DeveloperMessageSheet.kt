package it.mensa.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.markdownPadding
import it.mensa.app.support.tr
import kotlinx.coroutines.flow.first

/**
 * Il messaggio degli sviluppatori (`developer_message` in `configs`), in
 * markdown. Non si chiude e "Avanti" resta spento finche' non lo si e' scorso
 * fino in fondo: e' il modo di essere sicuri che sia stato letto. Quando
 * mostrarlo lo decide `DeveloperMessage` (:shared). Mirrors iOS
 * `DeveloperMessageSheet`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperMessageSheet(
    message: String,
    onDone: () -> Unit,
) {
    // Ne' trascinandola giu', ne' toccando fuori, ne' col tasto indietro.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden },
    )
    val scroll = rememberScrollState()
    var reachedEnd by remember { mutableStateOf(false) }
    // Il testo si prepara in sottofondo: finche' non c'e', la colonna e' vuota
    // e "in fondo" lo sarebbe gia'. Si conta solo con il testo disegnato.
    var textHeight by remember { mutableIntStateOf(0) }

    LaunchedEffect(scroll) {
        // Con un testo corto maxValue e' 0 e si e' in fondo da subito.
        snapshotFlow {
            textHeight > 0 && scroll.maxValue != Int.MAX_VALUE &&
                scroll.value >= scroll.maxValue - END_SLACK_PX
        }.first { it }
        reachedEnd = true
    }

    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                text = tr("developer_message.title", "Un messaggio dagli sviluppatori"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 12.dp),
            )
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scroll)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                SelectionContainer {
                    Markdown(
                        content = message,
                        // Senza, i paragrafi stanno attaccati.
                        padding = markdownPadding(block = 6.dp),
                        modifier = Modifier.fillMaxWidth().onSizeChanged { textHeight = it.height },
                    )
                }
            }
            HorizontalDivider()
            Column(
                modifier = Modifier.padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedVisibility(visible = !reachedEnd) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Filled.ArrowDownward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = tr("developer_message.scroll_hint", "Scorri fino in fondo per continuare"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Button(
                    onClick = onDone,
                    enabled = reachedEnd,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(tr("developer_message.continue", "Avanti"))
                }
            }
        }
    }
}

private const val END_SLACK_PX = 8
