package it.mensa.shared.members

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.repository.RegSociRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/** Gli auguri al socio che compie gli anni. [age] e' null se la data di nascita non e' credibile. */
data class BirthdayWish(
    val firstName: String,
    val age: Int?,
)

/**
 * La scheda degli auguri, il giorno del compleanno del socio che ha fatto
 * l'accesso. Il giorno lo decide [Birthdays], con le sue regole sul fuso e sul
 * 29 febbraio; la data di nascita e' quella del registro soci, dove l'id del
 * socio e' lo stesso dell'utente.
 *
 * Si mostra una volta l'anno, all'apertura. Toccare la push `birthday` che il
 * backend manda la mattina la rimostra anche se gia' vista, come per
 * [it.mensa.shared.config.DeveloperMessage].
 */
class BirthdayGreeting internal constructor(
    private val auth: AuthRepository,
    private val regSoci: RegSociRepository,
    private val db: MensaDatabase,
) {
    /** Gli auguri da mostrare adesso: null se oggi non e' il compleanno o li ha gia' visti quest'anno. */
    @Throws(Exception::class)
    suspend fun pending(): BirthdayWish? {
        val today = Birthdays.today()
        val (name, birthdate) = me() ?: return null
        return pendingOf(name, birthdate, today, lastSeenYear())
    }

    /**
     * Gli auguri da rimostrare al tocco della notifica, anche se gia' visti.
     * Solo se oggi e' davvero il compleanno: una push toccata giorni dopo non
     * fa gli auguri fuori tempo.
     */
    @Throws(Exception::class)
    suspend fun current(): BirthdayWish? {
        val today = Birthdays.today()
        val (name, birthdate) = me() ?: return null
        return pendingOf(name, birthdate, today, lastSeenYear = null)
    }

    /**
     * Toccata una notifica `birthday`: la scheda va mostrata. E' uno stato e
     * non un evento perche' la notifica puo' aprire l'app da chiusa, prima che
     * la scheda esista: la richiesta aspetta finche' qualcuno non la prende
     * con [consumeShowRequest].
     */
    private val _showRequested = MutableStateFlow(false)
    val showRequested: StateFlow<Boolean> = _showRequested.asStateFlow()

    fun requestShow() {
        _showRequested.value = true
    }

    fun consumeShowRequest() {
        _showRequested.value = false
    }

    /** Visti: fino al compleanno dell'anno prossimo non risale da sola. */
    @Throws(Exception::class)
    suspend fun markSeen() {
        runCatching {
            db.keyValueQueries.insertOrReplace(key = KEY_SEEN_YEAR, value_ = Birthdays.today().year.toString())
        }
    }

    private suspend fun me(): Pair<String, Instant>? {
        val user = auth.currentUser.value ?: return null
        if (user.id.isEmpty()) return null
        val member = runCatching { regSoci.getById(user.id) }.getOrNull()
        val birthdate = member?.birthdate ?: return null
        return (member.name.ifBlank { user.name }) to birthdate
    }

    private suspend fun lastSeenYear(): Int? =
        runCatching { db.keyValueQueries.selectById(KEY_SEEN_YEAR).awaitAsOneOrNull()?.value_ }
            .getOrNull()?.toIntOrNull()

    companion object {
        private const val KEY_SEEN_YEAR = "birthday_greeting.seen_year"

        /** La regola, separata per i test: e' il compleanno e quest'anno non li ha ancora visti. */
        fun pendingOf(name: String, birthdate: Instant, today: LocalDate, lastSeenYear: Int?): BirthdayWish? {
            if (!Birthdays.isBirthday(birthdate, today)) return null
            if (lastSeenYear == today.year) return null
            return BirthdayWish(firstName = firstName(name), age = Birthdays.age(birthdate, today))
        }

        /** Il registro scrive "Nome Cognome", spesso tutto maiuscolo: il primo nome, con la maiuscola. */
        fun firstName(fullName: String): String =
            fullName.trim().substringBefore(' ').lowercase().replaceFirstChar { it.uppercase() }
    }
}
