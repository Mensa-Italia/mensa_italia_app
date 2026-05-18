package it.mensa.app.features.profile.sub

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.stripe.android.paymentsheet.rememberPaymentSheet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MakeDonationScreen(
    onBack: () -> Unit,
    vm: MakeDonationViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme

    // Stripe PaymentSheet must be created in a Composable scope (it registers
    // an ActivityResultLauncher). We feed the result back to the VM.
    val paymentSheet = rememberPaymentSheet(vm::onPaymentResult)
    LaunchedEffect(uiState.pending) {
        val pending = uiState.pending ?: return@LaunchedEffect
        paymentSheet.presentWithPaymentIntent(
            paymentIntentClientSecret = pending.clientSecret,
            configuration = pending.configuration,
        )
        vm.consumePending()
    }

    if (uiState.showPicker) {
        PaymentMethodPickerSheet(
            amountLabel = "€${uiState.amount}",
            onDismiss = vm::dismissPicker,
            onConfirm = { methodId -> vm.runDonation(methodId) },
        )
    }

    val heartTransition = rememberInfiniteTransition(label = "heart_beat")
    val heartScale by heartTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "heart_scale",
    )
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(tr("views.make_donation.title", fallback = "Donazione")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Hero (M3 tertiary semantic for support/donation accent) ──────
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(
                        colorScheme.tertiaryContainer,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Favorite,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .scale(heartScale),
                    tint = colorScheme.onTertiaryContainer,
                )
            }
            Spacer(Modifier.height(16.dp))

            // Section header — titleSmall colore primary (SectionHeader eliminato)
            Text(
                text = tr("app.donate.headline", fallback = "Supporta Mensa Italia"),
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                tr(
                    "app.donate.subhead",
                    fallback = "Il tuo contributo aiuta a sostenere eventi, community e l'associazione.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            // ── Amount presets ────────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    tr("app.donate.amount", fallback = "Seleziona importo"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    vm.presets.forEach { preset ->
                        val selected = uiState.amount == preset && !uiState.usingCustom
                        FilterChip(
                            selected = selected,
                            onClick = { vm.selectPreset(preset) },
                            label = {
                                Text(
                                    text = "€$preset",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Custom amount ─────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.customAmountText,
                onValueChange = vm::onCustomAmountChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("app.donate.custom", fallback = "Importo personalizzato")) },
                prefix = { Text("€", fontWeight = FontWeight.SemiBold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))

            // ── Payment method picker ─────────────────────────────────────────
            if (uiState.methods.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        tr("app.donate.method", fallback = "Metodo di pagamento"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    uiState.methods.forEach { method ->
                        val selected = method.id == uiState.selectedMethodId
                        Card(
                            onClick = { vm.selectMethod(method.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected)
                                    colorScheme.secondaryContainer
                                else
                                    colorScheme.surfaceContainerLow,
                                contentColor = if (selected)
                                    colorScheme.onSecondaryContainer
                                else
                                    colorScheme.onSurface,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                        ) {
                            Text(
                                text = "${method.brand.replaceFirstChar { it.uppercase() }} ${method.display}",
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Donate button ─────────────────────────────────────────────────
            val donateVerb = tr("app.donate.cta_verb", fallback = "Dona")
            Button(
                onClick = vm::submitDonation,
                enabled = uiState.amount > 0 && !uiState.submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp),
            ) {
                if (uiState.submitting) {
                    CircularProgressIndicator(
                        Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Outlined.Favorite, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("$donateVerb ${uiState.amount}€")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (uiState.showResult) {
        AlertDialog(
            onDismissRequest = vm::dismissResult,
            title = { Text(tr("views.make_donation.title", fallback = "Donazione")) },
            text = { Text(uiState.resultMessage) },
            confirmButton = { TextButton(onClick = vm::dismissResult) { Text("OK") } },
        )
    }
}
