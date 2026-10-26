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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Surface
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.model.markdownDimens
import com.mikepenz.markdown.model.rememberMarkdownState
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
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Text(
                    text = tr("developer_message.title", "Un messaggio dagli sviluppatori"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scroll)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                MessageMarkdown(message = message, onTextHeight = { textHeight = it })
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

/**
 * Il testo, per conto suo: quando "Avanti" si accende la scheda si ridisegna,
 * e con il Markdown dentro lo stesso blocco la libreria lo rianalizzava da
 * capo. Per un attimo il testo era vuoto e lo scorrimento tornava in cima.
 * Qui non si ridisegna (parametri uguali) e l'analisi resta ricordata.
 */
@Composable
private fun MessageMarkdown(message: String, onTextHeight: (Int) -> Unit) {
    val state = rememberMarkdownState(message)
    SelectionContainer {
        Markdown(
            markdownState = state,
            colors = developerMessageColors(),
            typography = developerMessageTypography(),
            padding = markdownPadding(
                // Senza, i paragrafi stanno attaccati.
                block = 6.dp,
                listItemTop = 3.dp,
                listItemBottom = 3.dp,
                listIndent = 6.dp,
                codeBlock = PaddingValues(14.dp),
                blockQuoteText = PaddingValues(start = 12.dp, top = 2.dp, bottom = 2.dp),
            ),
            dimens = markdownDimens(
                blockQuoteThickness = 3.dp,
                codeBackgroundCornerSize = 12.dp,
                dividerThickness = 1.dp,
            ),
            modifier = Modifier.fillMaxWidth().onSizeChanged { onTextHeight(it.height) },
        )
    }
}

private const val END_SLACK_PX = 8

/**
 * Il markdown del messaggio: quello delle chat, non quello di un documento.
 * Di serie i titoli usano i display di Material (un `#` prendeva mezzo
 * schermo) e i link sono neri sottolineati; qui titoli poco piu' grandi del
 * testo, interlinea larga, link nel colore del brand, codice su fondo grigio.
 * Mirrors iOS `Theme.developerMessage`.
 */
@Composable
private fun developerMessageTypography(): MarkdownTypography {
    val body = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp)
    val heading = body.copy(fontWeight = FontWeight.SemiBold)
    val mono = body.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 21.sp)
    val brand = MaterialTheme.colorScheme.primary
    return markdownTypography(
        h1 = heading.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
        h2 = heading.copy(fontSize = 19.sp, lineHeight = 26.sp),
        h3 = heading.copy(fontSize = 17.sp, lineHeight = 24.sp),
        h4 = heading,
        h5 = heading,
        h6 = heading.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        text = body,
        code = mono,
        inlineCode = mono.copy(fontSize = 14.5.sp),
        quote = body.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        paragraph = body,
        ordered = body,
        bullet = body,
        list = body,
        textLink = TextLinkStyles(style = SpanStyle(color = brand, fontWeight = FontWeight.Medium)),
    )
}

@Composable
private fun developerMessageColors(): MarkdownColors = markdownColor(
    text = MaterialTheme.colorScheme.onSurface,
    codeText = MaterialTheme.colorScheme.onSurface,
    inlineCodeText = MaterialTheme.colorScheme.onSurface,
    linkText = MaterialTheme.colorScheme.primary,
    codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest,
    inlineCodeBackground = MaterialTheme.colorScheme.surfaceContainerHighest,
    dividerColor = MaterialTheme.colorScheme.outlineVariant,
)
