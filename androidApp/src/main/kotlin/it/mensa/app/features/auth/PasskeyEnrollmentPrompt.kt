package it.mensa.app.features.auth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.Logger
import it.mensa.app.support.koinAccess
import it.mensa.app.support.tr
import kotlinx.coroutines.launch

/**
 * Propone **una volta sola** di attivare una passkey, dopo un accesso con
 * password.
 *
 * Tutte le condizioni stanno nel gate condiviso
 * (`PasskeyEnrollmentGate.shouldShow`): accesso avvenuto con password, marker
 * non ancora scritto, nessuna passkey gia' attiva, non in modalita' screenshot.
 * Qui c'e' solo la presentazione.
 *
 * Il marker viene scritto **quando il prompt appare**, non quando viene
 * accettato: e' questo che rende vero "una volta sola" anche se l'utente dice
 * no. Da quel momento l'attivazione resta raggiungibile dai settings.
 *
 * Da comporre nella schermata principale post-login, non nella login: cosi' non
 * si sovrappone all'onboarding.
 */
@Composable
fun PasskeyEnrollmentPrompt() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { koinAccess().auth }
    val passkeys = remember { koinAccess().passkeys }
    val gate = remember { koinAccess().passkeyEnrollment }

    var visible by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    val user by auth.currentUser.collectAsStateWithLifecycle()

    LaunchedEffect(user?.id) {
        val current = user ?: return@LaunchedEffect
        if (!gate.shouldShow(current, PasskeyAuthenticator.isSupported)) return@LaunchedEffect
        gate.markShown(current.id)
        visible = true
    }

    if (visible) {
        AlertDialog(
            onDismissRequest = { visible = false },
            // Proposta di valore, non allarme sicurezza.
            title = {
                Text(
                    // Chiave con suffisso di piattaforma: il nome del sensore
                    // biometrico è diverso su iOS e Android, e una chiave sola
                    // costringerebbe una delle due a una dicitura generica.
                    tr(
                        "app.passkey.enroll.title.android",
                        fallback = "Accedi con l'impronta la prossima volta",
                    )
                )
            },
            text = {
                Text(
                    tr(
                        "app.passkey.enroll.body",
                        fallback = "Puoi entrare senza digitare la password. La password continua a funzionare.",
                    )
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !working,
                    onClick = {
                        working = true
                        scope.launch {
                            val challenge = passkeys.beginRegistration()
                            if (challenge == null) {
                                working = false
                                visible = false
                                failed = true
                                return@launch
                            }
                            when (
                                val prompt = PasskeyAuthenticator(context)
                                    .create(challenge.creationOptionsJson)
                            ) {
                                is PasskeyPromptResult.Success -> {
                                    failed = !passkeys.finishRegistration(
                                        passkeyId = challenge.passkeyId,
                                        credentialJson = prompt.credentialJson,
                                        name = PasskeyAuthenticator.deviceLabel(),
                                    )
                                }
                                // Prompt chiuso dall'utente: nessun messaggio.
                                PasskeyPromptResult.Cancelled -> Unit
                                else -> {
                                    Logger.e("PasskeyEnroll", "create", "$prompt")
                                    failed = true
                                }
                            }
                            working = false
                            visible = false
                        }
                    },
                ) {
                    Text(tr("app.passkey.enroll.confirm", fallback = "Attiva"))
                }
            },
            dismissButton = {
                TextButton(enabled = !working, onClick = { visible = false }) {
                    Text(tr("app.passkey.enroll.later", fallback = "Non ora"))
                }
            },
        )
    }

    if (failed) {
        AlertDialog(
            onDismissRequest = { failed = false },
            text = {
                Text(
                    tr(
                        "views.passkeys.add_failed",
                        fallback = "Attivazione non completata. Riprova.",
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { failed = false }) {
                    Text(tr("common.ok", fallback = "OK"))
                }
            },
        )
    }
}
