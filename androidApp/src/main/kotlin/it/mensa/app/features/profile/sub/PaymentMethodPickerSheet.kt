package it.mensa.app.features.profile.sub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stripe.android.paymentsheet.rememberPaymentSheet
import it.mensa.app.support.tr
import org.koin.androidx.compose.koinViewModel

/**
 * Inline payment-method picker. Mirrors Flutter's `PaymentMethodPicker`:
 *
 *   - On open, shows the default method + a "Pay {amount}" CTA. The
 *     "Cambia metodo" link toggles to a radio list of saved methods.
 *   - "Aggiungi metodo" launches Stripe's PaymentSheet in setup-intent
 *     mode and reloads on success.
 *   - The caller passes `onConfirm` to receive the selected method id —
 *     the donation screen then triggers the actual donate flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodPickerSheet(
    amountLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (paymentMethodId: String) -> Unit,
    vm: PaymentMethodPickerViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme

    // SetupIntent → PaymentSheet for adding a new method. Compose hosts
    // the launcher and pipes results back into the VM.
    val paymentSheet = rememberPaymentSheet(vm::onAddMethodResult)
    LaunchedEffect(uiState.addPending) {
        val pending = uiState.addPending ?: return@LaunchedEffect
        paymentSheet.presentWithSetupIntent(
            setupIntentClientSecret = pending.clientSecret,
            configuration = pending.configuration,
        )
        vm.consumeAddPending()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                tr("app.payments.title", fallback = "Metodo di pagamento"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(16.dp))

            if (uiState.loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            } else {
                val default = vm.defaultMethod()

                if (default == null || uiState.showPicker) {
                    // Flutter shows the add-CTA whenever there's no default
                    // OR when the user is browsing the picker — same here.
                    AddMethodTile(
                        enabled = !uiState.adding,
                        onClick = vm::addMethod,
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (default != null && !uiState.showPicker) {
                    PaymentMethodRow(
                        method = default,
                        selected = true,
                        showRadio = false,
                        onClick = {},
                    )
                }

                if (default != null && uiState.showPicker) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        uiState.methods.forEach { m ->
                            PaymentMethodRow(
                                method = m,
                                selected = m.id == uiState.defaultId,
                                showRadio = true,
                                onClick = { vm.selectMethod(m.id) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (!uiState.showPicker) {
                    Button(
                        onClick = {
                            default?.let { onConfirm(it.id) }
                        },
                        enabled = default != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                    ) {
                        Text(
                            text = tr(
                                "app.payments.pay_amount",
                                fallback = "Paga {amount}",
                                "amount" to amountLabel,
                            ),
                        )
                    }
                    if (uiState.methods.size > 1 || default != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = vm::toggleShowPicker) {
                                Text(
                                    tr(
                                        "app.payments.change_method",
                                        fallback = "Cambia metodo di pagamento",
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                } else {
                    TextButton(
                        onClick = vm::toggleShowPicker,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(tr("app.common.back", fallback = "Indietro"))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AddMethodTile(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                tr("app.payments.add", fallback = "Aggiungi metodo di pagamento"),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Outlined.AddCircle,
                contentDescription = null,
                tint = colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PaymentMethodRow(
    method: it.mensa.shared.model.PaymentMethodModel,
    selected: Boolean,
    showRadio: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colorScheme.secondaryContainer
                             else colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.CreditCard,
                        contentDescription = null,
                        tint = colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    method.brand.ifEmpty { "Carta" }.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    method.display.ifEmpty { "•••• ••••" },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
            if (showRadio) {
                RadioButton(selected = selected, onClick = onClick)
            }
        }
    }
}
