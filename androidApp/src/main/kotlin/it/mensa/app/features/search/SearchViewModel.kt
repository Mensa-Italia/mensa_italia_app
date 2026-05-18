package it.mensa.app.features.search

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.Logger
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.AddonModel
import it.mensa.shared.model.BoutiqueModel
import it.mensa.shared.model.DealModel
import it.mensa.shared.model.DocumentModel
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.OrgChartGroup
import it.mensa.shared.model.OrgChartMember
import it.mensa.shared.model.RegSociModel
import it.mensa.shared.model.SigModel
import it.mensa.shared.model.search.SearchHit
import it.mensa.shared.model.search.SearchResponse
import it.mensa.shared.repository.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── Domain types ─────────────────────────────────────────────────────────────

/** Role inside a local office — rendered as a chip on PersonSearchResultRow */
data class LocalOfficeAffiliation(
    val label: String,
    val slug: String,
    val kind: Kind,
) {
    enum class Kind { Admin, TestAssistant }
}

// ─── UI State ─────────────────────────────────────────────────────────────────

sealed class SearchPhase {
    object Idle : SearchPhase()
    data class Loading(val query: String) : SearchPhase()
    data class Results(val sections: List<HydratedSection>) : SearchPhase()
    data class Error(val message: String) : SearchPhase()
}

data class SearchUiState(
    val query: String = "",
    val selectedType: String? = null,
    val phase: SearchPhase = SearchPhase.Idle,
    val recent: List<String> = emptyList(),
)

// ─── Section / hit model ──────────────────────────────────────────────────────

data class HydratedSection(
    val type: String,
    val hits: List<HydratedHit>,
)

data class HydratedHit(
    val id: String,
    val leanTitle: String,
    val leanSubtitle: String,
    val leanImage: String,
    val payload: Payload,
) {
    sealed class Payload {
        data class User(
            val member: RegSociModel,
            val orgRole: String?,
            val orgGroup: String?,
            val localOfficeAffiliations: List<LocalOfficeAffiliation>,
        ) : Payload()
        data class Event(val event: EventModel) : Payload()
        data class Deal(val deal: DealModel) : Payload()
        data class Sig(val sig: SigModel) : Payload()
        data class Document(val document: DocumentModel) : Payload()
        data class Boutique(val product: BoutiqueModel) : Payload()
        data class Addon(val addon: AddonModel) : Payload()
        data class OrgGroup(val group: OrgChartGroup) : Payload()
        data class OrgRole(
            val role: String,
            val groupTitle: String,
            val groupId: String,
            val member: OrgChartMember,
        ) : Payload()
        /** Backend hit that doesn't resolve to a local cache entry */
        object Lean : Payload()
    }
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class SearchViewModel(private val context: Context) : ViewModel() {

    private val koin = koinAccess()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // ─── Local caches ─────────────────────────────────────────────────────────

    private var members: List<RegSociModel> = emptyList()
    private var events: List<EventModel> = emptyList()
    private var deals: List<DealModel> = emptyList()
    private var sigs: List<SigModel> = emptyList()
    private var documents: List<DocumentModel> = emptyList()
    private var boutique: List<BoutiqueModel> = emptyList()
    private var addons: List<AddonModel> = emptyList()

    /** userId → (role, groupTitle) */
    private var orgIndex: Map<String, Pair<String, String>> = emptyMap()
    private var orgGroups: List<OrgChartGroup> = emptyList()

    /** Last backend success payload — kept for re-hydration when caches update */
    private var lastSuccess: Pair<String, SearchResponse>? = null

    /** Dedup guard for in-flight member hydration requests */
    private val hydratingMembers = mutableSetOf<String>()

    /** Affiliate index built on each buildSections pass */
    private var currentAffiliations: Map<String, List<LocalOfficeAffiliation>> = emptyMap()

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("mensa_search", Context.MODE_PRIVATE)
    }
    private val recentKey = "recent_ordered"

    init {
        loadRecent()
        subscribeCaches()
        subscribeSearchState()
        viewModelScope.launch { loadOrgChart() }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    fun onQueryChange(q: String) {
        _uiState.update { it.copy(query = q) }
        val trimmed = q.trim()
        if (trimmed.isEmpty()) {
            koin.search.clear()
            lastSuccess = null
            _uiState.update { it.copy(phase = SearchPhase.Idle) }
            return
        }
        val params = SearchRepository.Params(
            q = q,
            types = _uiState.value.selectedType?.let { listOf(it) },
            region = null,
            limitPerType = 200,
            hydrate = true,
        )
        koin.search.update(params)
    }

    fun onClearQuery() = onQueryChange("")

    fun pickType(type: String?) {
        _uiState.update { it.copy(selectedType = type) }
        val q = _uiState.value.query
        if (q.trim().isNotEmpty()) {
            onQueryChange(q)
        } else {
            lastSuccess?.let { (query, response) ->
                _uiState.update { it.copy(phase = SearchPhase.Results(buildSections(query, response))) }
            }
        }
    }

    fun clearRecent() {
        _uiState.update { it.copy(recent = emptyList()) }
        prefs.edit().remove(recentKey).apply()
    }

    fun onItemClick(item: HydratedHit) {
        // TODO: wire navigation
        Logger.i("SearchViewModel", "item clicked: ${item.id} type=${item.payload::class.simpleName}")
    }

    // ─── Recent searches ──────────────────────────────────────────────────────

    private fun loadRecent() {
        val raw = prefs.getString(recentKey, "") ?: ""
        val arr = raw.split("\n").filter { it.isNotBlank() }.take(8)
        _uiState.update { it.copy(recent = arr) }
    }

    private fun saveRecent(q: String) {
        val trimmed = q.trim()
        if (trimmed.isEmpty()) return
        val key = normalize(trimmed)
        val current = _uiState.value.recent

        // Prefix-dedup: skip if a longer existing entry already covers this key
        if (current.any { existing ->
                val ek = normalize(existing)
                ek != key && ek.startsWith(key)
            }) return

        var arr = current.filter { existing ->
            val ek = normalize(existing)
            ek != key && !key.startsWith(ek)
        }.toMutableList()
        arr.add(0, trimmed)
        if (arr.size > 8) arr = arr.take(8).toMutableList()
        _uiState.update { it.copy(recent = arr) }
        prefs.edit().putString(recentKey, arr.joinToString("\n")).apply()
    }

    // ─── Search state subscription ────────────────────────────────────────────

    private fun subscribeSearchState() {
        viewModelScope.launch {
            koin.search.state.collect { state ->
                when (state) {
                    is SearchRepository.State.Idle -> {
                        lastSuccess = null
                        _uiState.update { it.copy(phase = SearchPhase.Idle) }
                    }
                    is SearchRepository.State.Loading -> {
                        _uiState.update { it.copy(phase = SearchPhase.Loading(state.query)) }
                    }
                    is SearchRepository.State.Success -> {
                        Logger.i("SearchViewModel", "search success q=${state.query} keys=${state.response.results.keys}")
                        saveRecent(state.query)
                        lastSuccess = state.query to state.response
                        val sections = buildSections(state.query, state.response)
                        _uiState.update { it.copy(phase = SearchPhase.Results(sections)) }
                    }
                    is SearchRepository.State.Error -> {
                        Logger.e("SearchViewModel", "search error: ${state.cause.message}")
                        _uiState.update {
                            it.copy(phase = SearchPhase.Error(state.cause.message ?: "Errore"))
                        }
                    }
                }
            }
        }
    }

    // ─── Cache subscriptions ──────────────────────────────────────────────────

    private fun subscribeCaches() {
        viewModelScope.launch {
            koin.regSoci.observeAll().collect { list ->
                members = list; rebuildIfPossible()
            }
        }
        viewModelScope.launch {
            koin.events.observeAll().collect { list ->
                events = list; rebuildIfPossible()
            }
        }
        viewModelScope.launch {
            koin.deals.observeAll().collect { list ->
                deals = list; rebuildIfPossible()
            }
        }
        viewModelScope.launch {
            koin.sigs.observeAll().collect { list ->
                sigs = list; rebuildIfPossible()
            }
        }
        viewModelScope.launch {
            koin.documents.observeAll().collect { list ->
                documents = list; rebuildIfPossible()
            }
        }
        viewModelScope.launch {
            koin.boutique.observeAll().collect { list ->
                boutique = list; rebuildIfPossible()
            }
        }
        viewModelScope.launch {
            koin.addons.observeAll().collect { list ->
                addons = list; rebuildIfPossible()
            }
        }
    }

    private fun rebuildIfPossible() {
        val (query, response) = lastSuccess ?: return
        val sections = buildSections(query, response)
        _uiState.update { it.copy(phase = SearchPhase.Results(sections)) }
    }

    // ─── Org chart ────────────────────────────────────────────────────────────

    private suspend fun loadOrgChart() {
        try {
            val chart = koin.orgChart.fetch()
            val map = mutableMapOf<String, Pair<String, String>>()
            for (group in chart.groups) {
                for (member in group.members) {
                    if (member.userId.isNotEmpty() && !map.containsKey(member.userId)) {
                        map[member.userId] = member.role to group.title
                    }
                }
            }
            orgIndex = map
            orgGroups = chart.groups.filter { it.members.isNotEmpty() }
            rebuildIfPossible()
        } catch (e: Exception) {
            Logger.e("SearchViewModel", "orgchart fetch failed: ${e.message}")
        }
    }

    // ─── Section building ─────────────────────────────────────────────────────

    private fun buildSections(query: String, response: SearchResponse): List<HydratedSection> {
        val q = normalize(query)
        currentAffiliations = extractAffiliations(response)

        val order = listOf(
            "org", "user", "linktree_link", "event", "deal",
            "sig", "document", "quid_issue", "quid_pdf", "quid_article",
            "boutique", "addon"
        )
        val typeFilter = _uiState.value.selectedType

        val sections = mutableListOf<HydratedSection>()
        for (type in order) {
            if (typeFilter != null && typeFilter != type) continue
            val backendHits = backendHitsFor(type, response)
            val merged = mergeWithLocal(type, backendHits, q)
            if (merged.isEmpty()) continue
            sections.add(HydratedSection(type = type, hits = merged))
        }
        return sections
    }

    private fun extractAffiliations(response: SearchResponse): Map<String, List<LocalOfficeAffiliation>> {
        val byUser = mutableMapOf<String, MutableList<LocalOfficeAffiliation>>()

        val adminHits = response.hitsFor("local_office_admin")
        val assistantHits = response.hitsFor("local_office_test_assistant")

        for ((hit, kind) in (adminHits.map { it to LocalOfficeAffiliation.Kind.Admin } +
                assistantHits.map { it to LocalOfficeAffiliation.Kind.TestAssistant })) {
            val userId = parseMemberIdFromImageUrl(hit.image) ?: continue
            val slug = parseDeepLinkSlug(hit.deepLink, "local-office") ?: continue
            byUser.getOrPut(userId) { mutableListOf() }
                .add(LocalOfficeAffiliation(label = hit.title, slug = slug, kind = kind))
        }
        return byUser
    }

    private fun parseMemberIdFromImageUrl(raw: String): String? {
        if (raw.isEmpty()) return null
        val pattern = Regex("/members_registry/([^/]+)/")
        return pattern.find(raw)?.groupValues?.getOrNull(1)
    }

    private fun backendHitsFor(canonical: String, response: SearchResponse): List<SearchHit> {
        val aliases = when (canonical) {
            "user" -> listOf("user", "members_registry", "member", "reg_soci")
            else -> listOf(canonical)
        }
        val seen = mutableSetOf<String>()
        val merged = mutableListOf<SearchHit>()
        for (key in aliases) {
            for (hit in response.hitsFor(key)) {
                if (seen.add(hit.id)) merged.add(hit)
            }
        }
        return merged
    }

    // ─── Merge backend + local cache ──────────────────────────────────────────

    private fun mergeWithLocal(
        type: String,
        backendHits: List<SearchHit>,
        q: String,
    ): List<HydratedHit> {
        val ordered = backendHits.map { hydrate(it, type) }.toMutableList()
        val seen = ordered.map { it.id }.toMutableSet()

        if (q.isEmpty()) return ordered

        val localMatches: List<HydratedHit> = when (type) {
            "user" -> members
                .filter { it.matchesQuery(q) }
                .sortedBy { it.name }
                .map { m ->
                    if (m.image.isEmpty()) enqueueMemberHydration(m.id)
                    val org = orgIndex[m.id]
                    val affs = currentAffiliations[m.id] ?: emptyList()
                    HydratedHit(
                        id = m.id, leanTitle = m.name,
                        leanSubtitle = org?.first ?: m.city, leanImage = m.image,
                        payload = HydratedHit.Payload.User(m, org?.first, org?.second, affs),
                    )
                }
            "event" -> events
                .filter { it.matchesQuery(q) }
                .sortedByDescending { it.whenStart.toEpochMilliseconds() }
                .map { e ->
                    HydratedHit(e.id, e.name, "", e.image, HydratedHit.Payload.Event(e))
                }
            "deal" -> deals
                .filter { it.matchesQuery(q) }
                .map { d ->
                    HydratedHit(d.id, d.name, d.commercialSector, "", HydratedHit.Payload.Deal(d))
                }
            "sig" -> sigs
                .filter { it.matchesQuery(q) }
                .map { s ->
                    HydratedHit(s.id, s.name, s.description, s.image, HydratedHit.Payload.Sig(s))
                }
            "document" -> documents
                .filter { it.matchesQuery(q) }
                .map { doc ->
                    HydratedHit(doc.id, doc.name, doc.description ?: "", "", HydratedHit.Payload.Document(doc))
                }
            "boutique" -> boutique
                .filter { it.matchesQuery(q) }
                .map { p ->
                    HydratedHit(p.id, p.name, p.description, p.image.firstOrNull() ?: "", HydratedHit.Payload.Boutique(p))
                }
            "addon" -> addons
                .filter { it.matchesQuery(q) }
                .map { a ->
                    HydratedHit(a.id, a.name, a.description, a.icon, HydratedHit.Payload.Addon(a))
                }
            "org" -> orgMatches(q)
            else -> emptyList()
        }

        for (m in localMatches) {
            if (seen.add(m.id)) ordered.add(m)
        }
        return if (ordered.size > 500) ordered.take(500) else ordered
    }

    // ─── Hydrate a single backend hit ─────────────────────────────────────────

    private fun hydrate(hit: SearchHit, type: String): HydratedHit {
        val id = hit.id
        val title = hit.title
        val subtitle = hit.subtitle
        val image = hit.image

        return when (type) {
            "user" -> {
                val m = members.firstOrNull { it.id == id }
                if (m != null) {
                    if (m.image.isEmpty()) enqueueMemberHydration(id)
                    val org = orgIndex[m.id]
                    val affs = currentAffiliations[m.id] ?: emptyList()
                    HydratedHit(
                        id, title, subtitle, image,
                        HydratedHit.Payload.User(m, org?.first, org?.second, affs)
                    )
                } else {
                    enqueueMemberHydration(id)
                    HydratedHit(id, title, subtitle, image, HydratedHit.Payload.Lean)
                }
            }
            "event" -> {
                val e = events.firstOrNull { it.id == id }
                if (e != null) HydratedHit(id, title, subtitle, image, HydratedHit.Payload.Event(e))
                else HydratedHit(id, title, subtitle, image, HydratedHit.Payload.Lean)
            }
            "deal" -> {
                val d = deals.firstOrNull { it.id == id }
                if (d != null) HydratedHit(id, title, subtitle, image, HydratedHit.Payload.Deal(d))
                else HydratedHit(id, title, subtitle, image, HydratedHit.Payload.Lean)
            }
            "sig" -> {
                val s = sigs.firstOrNull { it.id == id }
                if (s != null) HydratedHit(id, title, subtitle, image, HydratedHit.Payload.Sig(s))
                else HydratedHit(id, title, subtitle, image, HydratedHit.Payload.Lean)
            }
            "document" -> {
                val doc = documents.firstOrNull { it.id == id }
                if (doc != null) HydratedHit(id, title, subtitle, image, HydratedHit.Payload.Document(doc))
                else HydratedHit(id, title, subtitle, image, HydratedHit.Payload.Lean)
            }
            "boutique" -> {
                val p = boutique.firstOrNull { it.id == id }
                if (p != null) HydratedHit(id, title, subtitle, image, HydratedHit.Payload.Boutique(p))
                else HydratedHit(id, title, subtitle, image, HydratedHit.Payload.Lean)
            }
            "addon" -> {
                val a = addons.firstOrNull { it.id == id }
                if (a != null) HydratedHit(id, title, subtitle, image, HydratedHit.Payload.Addon(a))
                else HydratedHit(id, title, subtitle, image, HydratedHit.Payload.Lean)
            }
            else -> HydratedHit(id, title, subtitle, image, HydratedHit.Payload.Lean)
        }
    }

    // ─── Org chart local search ───────────────────────────────────────────────

    private fun orgMatches(q: String): List<HydratedHit> {
        if (q.isEmpty() || orgGroups.isEmpty()) return emptyList()

        val groupHits = mutableListOf<HydratedHit>()
        val roleHits = mutableListOf<HydratedHit>()

        for (group in orgGroups) {
            val titleKey = normalize(group.title)
            val titlePretty = normalize(localizedGroupTitle(group.title))
            if (titleKey.contains(q) || titlePretty.contains(q)) {
                groupHits.add(
                    HydratedHit(
                        id = "org_group_${group.id}",
                        leanTitle = localizedGroupTitle(group.title),
                        leanSubtitle = "",
                        leanImage = "",
                        payload = HydratedHit.Payload.OrgGroup(group),
                    )
                )
            }

            for (member in group.members.filter { !it.inactive }) {
                val roleKey = normalize(member.role)
                val nameKey = normalize(member.name)
                if (!roleKey.contains(q) && !nameKey.contains(q)) continue
                roleHits.add(
                    HydratedHit(
                        id = "org_role_${group.id}_${member.userId}",
                        leanTitle = member.role,
                        leanSubtitle = member.name,
                        leanImage = member.image,
                        payload = HydratedHit.Payload.OrgRole(
                            role = member.role,
                            groupTitle = localizedGroupTitle(group.title),
                            groupId = group.id,
                            member = member,
                        ),
                    )
                )
            }
        }
        // Master roles first
        roleHits.sortByDescending { hit ->
            (hit.payload as? HydratedHit.Payload.OrgRole)?.member?.isMaster ?: false
        }
        return groupHits + roleHits
    }

    // ─── Member hydration (background, deduped) ───────────────────────────────

    private fun enqueueMemberHydration(id: String) {
        if (id.isEmpty() || hydratingMembers.contains(id)) return
        hydratingMembers.add(id)
        viewModelScope.launch {
            try {
                koin.regSoci.getById(id)
            } catch (e: Exception) {
                Logger.e("SearchViewModel", "member hydration failed id=$id: ${e.message}")
            } finally {
                hydratingMembers.remove(id)
            }
        }
    }

    // ─── Deep link / URL helpers ──────────────────────────────────────────────

    private fun parseDeepLinkSlug(deepLink: String, expectedHost: String): String? {
        return try {
            val uri = android.net.Uri.parse(deepLink)
            if (uri.scheme != "mensa" || uri.host != expectedHost) return null
            uri.lastPathSegment?.takeIf { it.isNotBlank() }
        } catch (e: Exception) { null }
    }

    // ─── String normalization ─────────────────────────────────────────────────

    private fun normalize(s: String): String =
        s.trim()
            .lowercase()
            .let { str ->
                str.replace("à", "a").replace("è", "e").replace("é", "e")
                    .replace("ì", "i").replace("ò", "o").replace("ù", "u")
            }

    private fun localizedGroupTitle(raw: String): String {
        val pretty = raw
            .replace("_", " ")
            .replace("-", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        return try {
            koin.i18n.t(raw, pretty, emptyMap())
        } catch (e: Exception) { pretty }
    }
}

// ─── Local match predicates ───────────────────────────────────────────────────

private fun String.folded(): String = this.lowercase()
    .replace("à", "a").replace("è", "e").replace("é", "e")
    .replace("ì", "i").replace("ò", "o").replace("ù", "u")

private fun RegSociModel.matchesQuery(q: String): Boolean {
    if (q.isEmpty()) return false
    return name.folded().contains(q) || city.folded().contains(q) || id.lowercase().contains(q)
}

private fun EventModel.matchesQuery(q: String): Boolean {
    if (q.isEmpty()) return false
    if (name.folded().contains(q)) return true
    if (description.folded().contains(q)) return true
    val pos = position
    if (pos != null) {
        if (pos.name.folded().contains(q)) return true
        if (pos.address.folded().contains(q)) return true
    }
    return false
}

private fun DealModel.matchesQuery(q: String): Boolean {
    if (q.isEmpty()) return false
    if (name.folded().contains(q)) return true
    if (commercialSector.folded().contains(q)) return true
    if (details?.folded()?.contains(q) == true) return true
    if (who?.folded()?.contains(q) == true) return true
    val pos = position
    if (pos != null) {
        if (pos.name.folded().contains(q)) return true
        if (pos.address.folded().contains(q)) return true
    }
    return false
}

private fun SigModel.matchesQuery(q: String): Boolean {
    if (q.isEmpty()) return false
    return name.folded().contains(q) || description.folded().contains(q) || groupType.folded().contains(q)
}

private fun DocumentModel.matchesQuery(q: String): Boolean {
    if (q.isEmpty()) return false
    if (name.folded().contains(q)) return true
    if (description?.folded()?.contains(q) == true) return true
    return category.folded().contains(q)
}

private fun BoutiqueModel.matchesQuery(q: String): Boolean {
    if (q.isEmpty()) return false
    return name.folded().contains(q) || description.folded().contains(q)
}

private fun AddonModel.matchesQuery(q: String): Boolean {
    if (q.isEmpty()) return false
    return name.folded().contains(q) || description.folded().contains(q)
}
