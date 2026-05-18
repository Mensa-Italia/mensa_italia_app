package it.mensa.app.features.profile.sub

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.model.PaymentMethodModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    onBack: () -> Unit,
    vm: PaymentMethodsViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val paymentSheet = rememberPaymentSheet(vm::onAddMethodResult)
    LaunchedEffect(uiState.pending) {
        val pending = uiState.pending ?: return@LaunchedEffect
        paymentSheet.presentWithSetupIntent(
            setupIntentClientSecret = pending.clientSecret,
            configuration = pending.configuration,
        )
        vm.consumePending()
    }

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(tr("app.payments.title", fallback = "Pagamenti")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (uiState.loading && uiState.methods.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (uiState.methods.isEmpty()) {
                        item {
                            EmptyPaymentMethods(
                                onAddClick = {
                                    vm.addMethod()
                                },
                                modifier = Modifier.padding(32.dp),
                            )
                        }
                    } else {
                        item {
                            // Section header — titleSmall colore primary (SectionHeader eliminato)
                            Text(
                                text = tr("app.payments.section", fallback = "Metodi salvati"),
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
                            )
                        }
                        itemsIndexed(uiState.methods) { _, method ->
                            PaymentMethodCard(
                                method = method,
                                isDefault = method.id == uiState.defaultId,
                                onSetDefault = { vm.setDefault(method.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                            )
                        }

                        // Full-width Add CTA at the end of the list
                        item {
                            Spacer(Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                            ) {
                                Button(
                                    onClick = {
                                        vm.addMethod()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 56.dp),
                                ) {
                                    Icon(Icons.Outlined.AddCircle, null, Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text(tr("app.payments.add", fallback = "Aggiungi metodo"))
                                }
                            }
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }

    uiState.stripeMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = vm::dismissStripeMessage,
            title = { Text(tr("app.payments.add", fallback = "Aggiungi metodo")) },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = vm::dismissStripeMessage) { Text("OK") } },
        )
    }

    uiState.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = vm::dismissError,
            title = { Text(tr("app.error.title", fallback = "Errore")) },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = vm::dismissError) { Text("OK") } },
        )
    }
}

@Composable
private fun EmptyPaymentMethods(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = colorScheme.primaryContainer,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.CreditCard,
                    null,
                    tint = colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            tr("app.payments.empty.title", fallback = "Nessun metodo salvato"),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            tr("app.payments.empty.message", fallback = "Aggiungi una carta per gestire i pagamenti."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(tr("app.payments.add", fallback = "Aggiungi metodo"))
        }
    }
}

@Composable
private fun PaymentMethodCard(
    method: PaymentMethodModel,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        onClick = if (!isDefault) onSetDefault else ({}),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.CreditCard,
                        null,
                        tint = colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        method.brand.ifEmpty { "Carta" }.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (isDefault) {
                        Spacer(Modifier.size(8.dp))
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    tr("app.payments.default", fallback = "Predefinita"),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
                Text(
                    method.display.ifEmpty { "•••• ••••" },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
            if (!isDefault) {
                TextButton(onClick = onSetDefault) {
                    Text(
                        tr("app.payments.make_default", fallback = "Predefinita"),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
