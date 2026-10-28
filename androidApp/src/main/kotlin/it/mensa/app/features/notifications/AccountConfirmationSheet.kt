package it.mensa.app.features.notifications

import it.mensa.app.support.userMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.koinAccess
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.shared.api.endpoints.ExportRequestGoneException
import it.mensa.shared.model.ExAppModel
import it.mensa.shared.notifications.DataExportScope
import kotlinx.coroutines.launch

/**
 * AccountConfirmationSheet — third-party approval prompt.
 *
 * Android equivalent of `iosApp/.../AccountConfirmationSheet.swift`.
 * Shows the requesting [ExAppModel] (name, image) and offers Approva / Rifiuta,
 * with a body that depends on the [AccountConfirmationController.Request]:
 *
 *  - [AccountConfirmationController.Request.Identity]: Approva grants the
 *    request's permissions (`CHECK_USER_EXISTENCE`), then POSTs `{accepted}` to
 *    the caller's callback URL;
 *  - [AccountConfirmationController.Request.DataExport]: one-time export of
 *    stamps or medals. Either answer goes to the server first
 *    (`export/decide`), then the third party is told `{accepted, request_id}`.
 *
 * Either decision marks the originating notification (when present) as seen.
 * Both work for a suspended account: the server keeps them on its allowlist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountConfirmationSheet(
    request: AccountConfirmationController.Request,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val koin = remember { koinAccess() }

    var exApp by remember { mutableStateOf<ExAppModel?>(null) }
    var loading by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // Richiesta di esportazione scaduta o gia' decisa: riprovare darebbe lo stesso 404/409.
    var gone by remember { mutableStateOf(false) }

    suspend fun load() {
        loading = true
        errorMessage = null
        try {
            exApp = koin.exApps.getExApp(request.exAppId)
        } catch (e: Exception) {
            exApp = null
            errorMessage = e.userMessage
        } finally {
            loading = false
        }
    }

    LaunchedEffect(request.exAppId) { load() }

    suspend fun markSeen() {
        request.notificationId?.let { id ->
            runCatching { koin.notifications.markSeen(id) }
        }
    }

    /** Esegue la risposta [accepted] per il tipo di richiesta, poi chiude. */
    fun answer(accepted: Boolean) {
        if (submitting) return
        submitting = true
        scope.launch {
            runCatching {
                when (request) {
                    is AccountConfirmationController.Request.Identity -> {
                        if (accepted) koin.exApps.addPermissions(request.exAppId, request.permissions)
                        koin.exApps.postCallback(request.callbackUrl, accepted)
                    }
                    is AccountConfirmationController.Request.DataExport ->
                        koin.exApps.answerDataExport(request.requestId, request.callbackUrl, accepted)
                }
                markSeen()
            }.onFailure { e ->
                errorMessage = e.userMessage
                submitting = false
                // Non c'e' piu' niente da rifare: si passa all'errore con solo
                // "Chiudi", non al pulsante Approva.
                if (e is ExportRequestGoneException) {
                    gone = true
                    exApp = null
                }
                return@launch
            }
            submitting = false
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!submitting) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        when {
            loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            exApp != null -> ApprovalContent(
                app = exApp!!,
                submitting = submitting,
                request = request,
                onApprove = { answer(true) },
                onDeny = { answer(false) },
            )

            else -> ErrorContent(
                message = errorMessage,
                gone = gone,
                onRetry = if (gone) null else ({ scope.launch { load() } }),
                onDismiss = onDismiss,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ApprovalContent(
    app: ExAppModel,
    request: AccountConfirmationController.Request,
    submitting: Boolean,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = when (request) {
                is AccountConfirmationController.Request.Identity ->
                    tr("ex_app.confirm.title", fallback = "Conferma identità")
                is AccountConfirmationController.Request.DataExport ->
                    tr("ex_app.export.title", fallback = "Esporta i tuoi dati")
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = app.name.orEmpty(),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        val imageUrl = remember(app.id, app.image) {
            val id = app.id
            val img = app.image
            if (id != null && !img.isNullOrEmpty()) FilesUrl.build("ex_apps", id, img) else null
        }
        if (imageUrl != null) {
            CachedAsyncImage(
                model = imageUrl,
                contentDescription = app.name,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentScale = ContentScale.Fit,
            )
        }

        when (request) {
            is AccountConfirmationController.Request.Identity -> Text(
                text = app.description ?: tr(
                    "ex_app.confirm.description.fallback",
                    fallback = "L'app richiede di verificare la tua identità Mensa.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            is AccountConfirmationController.Request.DataExport -> DataExportBody(app, request)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onApprove,
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = when (request) {
                            is AccountConfirmationController.Request.Identity ->
                                tr("ex_app.confirm.approve", fallback = "Approva")
                            is AccountConfirmationController.Request.DataExport ->
                                tr("ex_app.export.approve", fallback = "Consenti l'estrazione")
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
            OutlinedButton(
                onClick = onDeny,
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = tr("ex_app.confirm.deny", fallback = "Rifiuta"),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

/**
 * Cosa lascia l'app, detto chiaramente, e che l'estrazione e' una sola.
 * Aggiungere un tipo di dato vuol dire aggiungere un caso a [DataExportScope] e
 * qui la sua riga.
 */
@Composable
private fun DataExportBody(
    app: ExAppModel,
    request: AccountConfirmationController.Request.DataExport,
) {
    val what = when (request.scope) {
        DataExportScope.STAMPS -> tr("ex_app.export.scope.stamps", fallback = "i tuoi timbri")
        DataExportScope.MEDALS -> tr("ex_app.export.scope.medals", fallback = "le tue medaglie")
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = tr(
                "ex_app.export.description",
                "{app} chiede di ricevere {what}.",
                "app" to app.name.orEmpty(),
                "what" to what,
            ),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = tr(
                "ex_app.export.one_time",
                "È un'estrazione una tantum: i dati vengono consegnati una sola volta, e per ogni nuova richiesta ti chiederemo di nuovo il consenso. La richiesta scade tra pochi minuti.",
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorContent(
    message: String?,
    gone: Boolean,
    onRetry: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = if (gone) {
                tr("ex_app.export.error.title", fallback = "Richiesta non più valida")
            } else {
                tr("ex_app.confirm.error.title", fallback = "Impossibile caricare l'app")
            },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = message ?: tr("ex_app.confirm.error.body", fallback = "Riprova più tardi."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(tr("app.retry", fallback = "Riprova"))
            }
        }
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(tr("app.close", fallback = "Chiudi"))
        }
    }
}
