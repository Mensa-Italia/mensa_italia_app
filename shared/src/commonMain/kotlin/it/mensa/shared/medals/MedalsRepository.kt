package it.mensa.shared.medals

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.model.MedalStatsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * The signed-in member's medals: `view_medals` read and run through
 * [MedalRules]. Kept in memory only — it's a single small row, refetched
 * whenever a screen that shows medals appears — and tied to the user it was
 * read for, so another account never sees the previous one's medals.
 */
class MedalsRepository(
    private val pb: PocketBaseClient,
    private val auth: AuthRepository,
) {
    private val cache = MutableStateFlow<Pair<String, List<MedalStatus>>?>(null)

    /** The medals, once read for the current user; empty until then. */
    fun observe(): Flow<List<MedalStatus>> =
        combine(cache, auth.currentUser) { cached, user ->
            if (cached != null && user != null && cached.first == user.id) cached.second else emptyList()
        }

    /** Rereads the counters. Throws on network errors; the last read stays. */
    suspend fun refresh(): List<MedalStatus> {
        val userId = auth.currentUser.value?.id ?: return emptyList()
        val stats: MedalStatsModel = pb.getOne(COLLECTION, userId)
        val medals = MedalRules.evaluate(stats)
        cache.value = userId to medals
        return medals
    }

    private companion object {
        const val COLLECTION = "view_medals"
    }
}
