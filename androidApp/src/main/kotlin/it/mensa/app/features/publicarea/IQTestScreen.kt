package it.mensa.app.features.publicarea

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import it.mensa.app.support.koinAccess
import it.mensa.app.support.tr
import it.mensa.shared.iqtest.MensaAgeGroup
import it.mensa.shared.iqtest.MensaTestPayload
import it.mensa.shared.iqtest.MensaTestQuestion
import it.mensa.shared.iqtest.MensaTestResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Native IQ-test flow — content/behavior mirror of `IQTestView.swift`.
 * Drives the shared [it.mensa.shared.iqtest.MensaTestClient] through the
 * phases (loading → ageGate → instructions → taking → submitting → result).
 */
private sealed class TestPhase {
    data object Loading : TestPhase()
    data object AgeGate : TestPhase()
    data object Instructions : TestPhase()
    data object Taking : TestPhase()
    data object Submitting : TestPhase()
    data class Result(val result: MensaTestResult) : TestPhase()
    data class Failed(val message: String) : TestPhase()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IQTestScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val client = remember { koinAccess().mensaTest }

    var phase by remember { mutableStateOf<TestPhase>(TestPhase.Loading) }
    var payload by remember { mutableStateOf<MensaTestPayload?>(null) }
    var selectedAge by remember { mutableStateOf(MensaAgeGroup.Y1850) }
    val answers = remember { mutableStateMapOf<Int, Int>() }
    var currentQuestion by remember { mutableIntStateOf(0) }
    var startedAtMs by remember { mutableStateOf(0L) }
    var secondsRemaining by remember { mutableIntStateOf(1500) }

    suspend fun startLoading() {
        phase = TestPhase.Loading
        runCatching { client.loadTest() }
            .onSuccess { p ->
                payload = p
                secondsRemaining = p.durationSeconds
                phase = TestPhase.AgeGate
            }
            .onFailure { e -> phase = TestPhase.Failed(e.message ?: "Errore sconosciuto") }
    }

    suspend fun submit() {
        val p = payload ?: return
        phase = TestPhase.Submitting
        val finishedMs = Clock.System.now().toEpochMilliseconds()
        runCatching {
            val stringKeyed = answers.mapKeys { it.key.toString() }
            client.submit(
                payload = p,
                answers = stringKeyed,
                ageGroup = selectedAge,
                startedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(startedAtMs),
                finishedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(finishedMs),
            )
        }.onSuccess { phase = TestPhase.Result(it) }
            .onFailure { e -> phase = TestPhase.Failed(e.message ?: "Errore sconosciuto") }
    }

    LaunchedEffect(Unit) { startLoading() }

    // Countdown timer for Taking phase.
    LaunchedEffect(phase) {
        if (phase !is TestPhase.Taking) return@LaunchedEffect
        while (secondsRemaining > 0 && phase is TestPhase.Taking) {
            delay(1000)
            secondsRemaining -= 1
        }
        if (secondsRemaining == 0 && phase is TestPhase.Taking) submit()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("iqtest.title", fallback = "Test di prova")) },
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
    ) { innerPadding ->
        AnimatedContent(
            targetState = phase,
            transitionSpec = { fadeIn(tween(250)).togetherWith(fadeOut(tween(150))) },
            label = "IQTestPhase",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { currentPhase ->
            when (currentPhase) {
                is TestPhase.Loading -> LoadingView()
                is TestPhase.AgeGate -> AgeGateView(
                    selected = selectedAge,
                    onSelect = { selectedAge = it },
                    onNext = { phase = TestPhase.Instructions },
                )
                is TestPhase.Instructions -> InstructionsView(
                    onStart = {
                        val p = payload ?: return@InstructionsView
                        answers.clear()
                        currentQuestion = 0
                        secondsRemaining = p.durationSeconds
                        startedAtMs = Clock.System.now().toEpochMilliseconds()
                        phase = TestPhase.Taking
                    },
                )
                is TestPhase.Taking -> {
                    val p = payload
                    if (p != null && currentQuestion < p.questions.size) {
                        TakingView(
                            payload = p,
                            currentQuestion = currentQuestion,
                            secondsRemaining = secondsRemaining,
                            selectedAnswer = answers[currentQuestion],
                            onAnswerSelect = { idx -> answers[currentQuestion] = idx },
                            onPrev = { if (currentQuestion > 0) currentQuestion -= 1 },
                            onNext = { if (currentQuestion < p.questions.size - 1) currentQuestion += 1 },
                            onSubmit = { scope.launch { submit() } },
                        )
                    }
                }
                is TestPhase.Submitting -> SubmittingView()
                is TestPhase.Result -> ResultView(
                    result = currentPhase.result,
                    onRetry = { scope.launch { startLoading() } },
                )
                is TestPhase.Failed -> FailedView(
                    message = currentPhase.message,
                    onRetry = { scope.launch { startLoading() } },
                )
            }
        }
    }
}

// ─── Loading ──────────────────────────────────────────────────────────────────

@Composable
private fun LoadingView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(tr("iqtest.loading", fallback = "Caricamento test in corso…"))
    }
}

// ─── Age gate ─────────────────────────────────────────────────────────────────

private val ageOptions = listOf(
    MensaAgeGroup.Y1617 to "16–17 anni",
    MensaAgeGroup.Y1850 to "18–50 anni",
    MensaAgeGroup.Y5160 to "51–60 anni",
    MensaAgeGroup.Y6199 to "61–99 anni",
)

@Composable
private fun AgeGateView(
    selected: MensaAgeGroup,
    onSelect: (MensaAgeGroup) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeaderText(tr("iqtest.age.header", fallback = "Quanti anni hai?"))
        GroupedCard {
            ageOptions.forEachIndexed { index, (group, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(group) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (group == selected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (index < ageOptions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
        SectionFooterText(
            tr(
                "iqtest.age.footer",
                fallback = "Seleziona la tua fascia d'età per ottenere un risultato accurato.",
            ),
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) { Text(tr("iqtest.cta.next", fallback = "Avanti")) }

        SectionFooterText(
            tr(
                "iqtest.disclaimer.context",
                fallback = "Test ufficiale di esempio realizzato da Mensa Norge. Mensa Italia ospita solo l'interfaccia: domande, calcolo del punteggio e percentile arrivano da test.mensa.no.",
            ),
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ─── Instructions ─────────────────────────────────────────────────────────────

@Composable
private fun InstructionsView(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeaderText(tr("iqtest.howto.header", fallback = "Come funziona"))
        GroupedCard {
            HowToRow(Icons.Outlined.GridView, tr("iqtest.howto.questions", fallback = "35 domande figurative"))
            CSDivider()
            HowToRow(Icons.Outlined.AccessTime, tr("iqtest.howto.duration", fallback = "20–25 minuti di tempo"))
            CSDivider()
            HowToRow(Icons.Outlined.Calculate, tr("iqtest.howto.no_math", fallback = "Nessuna matematica richiesta"))
            CSDivider()
            HowToRow(Icons.Outlined.TouchApp, tr("iqtest.howto.tap_answer", fallback = "Tocca l'opzione che completa la matrice"))
            CSDivider()
            HowToRow(Icons.Outlined.CheckCircle, tr("iqtest.howto.skip", fallback = "Puoi lasciare domande senza risposta"))
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) { Text(tr("iqtest.cta.start", fallback = "Inizia il test")) }

        SectionFooterText(
            tr(
                "iqtest.disclaimer.privacy",
                fallback = "Le tue risposte vengono inviate direttamente a Mensa Norge, che calcola e restituisce il risultato. Mensa Italia non vede, non conserva e non elabora i dati del test.",
            ),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HowToRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// ─── Taking ───────────────────────────────────────────────────────────────────

@Composable
private fun TakingView(
    payload: MensaTestPayload,
    currentQuestion: Int,
    secondsRemaining: Int,
    selectedAnswer: Int?,
    onAnswerSelect: (Int) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
) {
    val question = payload.questions[currentQuestion]
    val isLast = currentQuestion == payload.questions.lastIndex
    val isLowTime = secondsRemaining < 60

    Column(modifier = Modifier.fillMaxSize()) {
        // Top progress bar + timer
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = if (isLowTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = formatTime(secondsRemaining),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isLowTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${currentQuestion + 1} / ${payload.questions.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (currentQuestion + 1f) / payload.questions.size.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Question image (square)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = question.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            )
        }

        // Answer grid 3 columns
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(question.options) { opt ->
                val idx = question.options.indexOf(opt)
                val isSelected = selectedAnswer == idx
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                        )
                        .clickable { onAnswerSelect(idx) },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = opt,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                    )
                }
            }
        }

        // Bottom nav bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onPrev,
                    enabled = currentQuestion > 0,
                ) { Text("‹") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { if (isLast) onSubmit() else onNext() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(
                        if (isLast) tr("iqtest.cta.submit", fallback = "Concludi")
                        else tr("iqtest.cta.next", fallback = "Avanti"),
                    )
                }
            }
        }
    }
}

// ─── Submitting ───────────────────────────────────────────────────────────────

@Composable
private fun SubmittingView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(tr("iqtest.submitting", fallback = "Calcolo del risultato…"))
    }
}

// ─── Result ───────────────────────────────────────────────────────────────────

@Composable
private fun ResultView(result: MensaTestResult, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = tr("iqtest.result.iq_label", fallback = "Quoziente Intellettivo"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = result.iq?.toString() ?: "—",
            fontSize = 64.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        val percentile = result.percentile
        if (percentile != null && percentile > 0) {
            Spacer(Modifier.height(24.dp))
            SectionHeaderText(tr("iqtest.result.percentile_section", fallback = "Percentile"))
            GroupedCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = tr("iqtest.result.your_percentile", fallback = "Tuo percentile"),
                        modifier = Modifier.weight(1f),
                    )
                    val suffix = if (result.orMore == true) "° o superiore" else "°"
                    Text(
                        text = "$percentile$suffix",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    LinearProgressIndicator(
                        progress = { (percentile.coerceIn(0, 100)) / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) { Text(tr("iqtest.cta.retry", fallback = "Ripeti il test")) }

        SectionFooterText(
            tr(
                "iqtest.result.footer",
                fallback = "Test ufficiale di esempio fornito da Mensa Norge. L'app fa da contenitore grafico; punteggio e domande arrivano da test.mensa.no.",
            ),
        )
    }
}

// ─── Failed ───────────────────────────────────────────────────────────────────

@Composable
private fun FailedView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = tr("iqtest.failed.title", fallback = "Qualcosa è andato storto"),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) { Text(tr("common.retry", fallback = "Riprova")) }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

@Composable
private fun SectionHeaderText(text: String) {
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
            .padding(top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun SectionFooterText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun GroupedCard(content: @Composable () -> Unit) {
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
private fun CSDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 50.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}
