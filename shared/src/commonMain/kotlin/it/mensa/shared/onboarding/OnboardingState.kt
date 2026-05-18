package it.mensa.shared.onboarding

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class OnboardingState internal constructor(
    private val db: MensaDatabase,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val windowMs = 24L * 60 * 60 * 1000
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Returns true when: the user record carries a meaningful `created` date,
     * the user was created within the last 24h, AND we haven't already
     * recorded `onboarding.shown.<userId>` locally.
     */
    suspend fun shouldShow(user: UserModel): Boolean {
        if (user.id.isEmpty()) return false
        val key = keyFor(user.id)
        val alreadyShown = try {
            db.keyValueQueries.selectById(key).awaitAsOneOrNull()?.value_
        } catch (_: Throwable) { null } == "1"
        if (alreadyShown) return false
        val createdMs = user.created.toEpochMilliseconds()
        if (createdMs <= 0) return false
        return (clock() - createdMs) < windowMs
    }

    fun markShown(userId: String) {
        if (userId.isEmpty()) return
        scope.launch {
            try { db.keyValueQueries.insertOrReplace(keyFor(userId), "1") } catch (_: Throwable) {}
        }
    }

    /** Test/dev helper: reset the marker so the gate triggers again. */
    fun reset(userId: String) {
        if (userId.isEmpty()) return
        scope.launch {
            try { db.keyValueQueries.deleteById(keyFor(userId)) } catch (_: Throwable) {}
        }
    }

    private fun keyFor(id: String) = "onboarding.shown.$id"
}
