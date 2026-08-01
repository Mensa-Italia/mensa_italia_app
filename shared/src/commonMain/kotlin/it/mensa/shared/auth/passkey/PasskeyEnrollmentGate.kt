package it.mensa.shared.auth.passkey

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.auth.LoginMethod
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.demo.DemoIdentity
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Decide se proporre l'attivazione della passkey dopo un accesso con password.
 *
 * Stesso schema di [it.mensa.shared.onboarding.OnboardingState]: un marker per
 * utente sul key-value store locale, piu' un `reset` che rende il gate
 * verificabile senza reinstallare l'app.
 *
 * Il marker viene scritto **quando il prompt viene mostrato**, non quando viene
 * accettato. E' questo che rende vero "una volta sola": chi rifiuta non se lo
 * ritrova al login dopo. Da quel momento l'unica strada e' la schermata di
 * gestione nei settings, che e' il comportamento giusto — una proposta rifiutata
 * non si ripete, ma resta raggiungibile.
 */
class PasskeyEnrollmentGate internal constructor(
    private val db: MensaDatabase,
    private val auth: AuthRepository,
    private val passkeys: PasskeyRepository,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * True solo quando valgono tutte:
     *
     *  - l'accesso corrente e' avvenuto con la password (proporre una passkey a
     *    chi e' appena entrato *con* una passkey non ha senso, e un cold start
     *    con sessione ripristinata non e' un accesso);
     *  - il marker per questo utente non c'e';
     *  - il dispositivo sa gestire le passkey — [deviceSupportsPasskeys] arriva
     *    dall'app module, perche' lo shared non ha modo di saperlo;
     *  - l'utente non ha ancora nessuna passkey. Si usa [PasskeyRepository.listOrNull]
     *    e non `list`, perche' una lista vuota per errore di rete non deve far
     *    partire la proposta;
     *  - non siamo in modalita' screenshot, dove un prompt inatteso finirebbe
     *    dentro le immagini per gli store.
     */
    suspend fun shouldShow(user: UserModel, deviceSupportsPasskeys: Boolean): Boolean {
        if (!deviceSupportsPasskeys) return false
        if (DemoIdentity.enabled) return false
        if (user.id.isEmpty()) return false
        if (auth.lastLoginMethod.value != LoginMethod.PASSWORD) return false

        val alreadyShown = try {
            db.keyValueQueries.selectById(keyFor(user.id)).awaitAsOneOrNull()?.value_
        } catch (_: Throwable) {
            null
        } == "1"
        if (alreadyShown) return false

        val existing = passkeys.listOrNull() ?: return false
        return existing.isEmpty()
    }

    /** Da chiamare nel momento in cui il prompt viene presentato. */
    fun markShown(userId: String) {
        if (userId.isEmpty()) return
        scope.launch {
            try {
                db.keyValueQueries.insertOrReplace(keyFor(userId), "1")
            } catch (_: Throwable) {
            }
        }
    }

    /**
     * Azzera il marker, per far riscattare il gate senza reinstallare l'app.
     *
     * Non serve chiamarlo quando una passkey viene revocata o rifiutata: in quel
     * caso [shouldShow] si autocorregge, perche' interroga comunque la lista sul
     * server e la trova vuota. Il marker resta scritto, quindi la proposta non
     * ricompare da sola — ed e' giusto cosi': chi ha detto no una volta la
     * riattiva dai settings.
     */
    fun reset(userId: String) {
        if (userId.isEmpty()) return
        scope.launch {
            try {
                db.keyValueQueries.deleteById(keyFor(userId))
            } catch (_: Throwable) {
            }
        }
    }

    private fun keyFor(id: String) = "passkey.prompt.$id"
}
