import Foundation
import Shared

/// Global-search view model.
///
/// Strategy:
/// 1. Subscribe to `SearchRepository.state` — that gives us the backend index hits.
/// 2. Hold local snapshots of every entity cache (members, events, deals, sigs,
///    documents, boutique). They are kept fresh by the existing observe flows.
/// 3. When the backend returns, *merge* its hits with local substring matches so
///    we don't miss anything the index hasn't ranked. This is what fixes the
///    "few people" bug — the local `members_registry` SQLDelight cache contains
///    the full directory, so the union is effectively the full directory filtered
///    by query, sorted by backend score first then by name.
/// 4. Hydrate each hit id into a typed payload (`HydratedHit.Payload`) so the
///    UI can render the canonical per-type row exactly as the rest of the app does.
/// 5. Cache the orgchart once per session and attach role/group to user payloads.
/// Membership of a member inside a local office, derived from the
/// `local_office_admin` / `test_assistant` search hits. Rendered as a small
/// brand-tint chip below the person's name in the People section.
struct LocalOfficeAffiliation: Hashable {
    /// Display label — taken verbatim from the hit's `title`
    /// (e.g. "Segretario di Lombardia", "Assistente al test di Toscana").
    let label: String
    /// Office slug parsed from the hit's `deep_link`
    /// (`mensa://local-office/<slug>`). Used for tap-through.
    let slug: String
    /// Distinguishes admin (segretario/co-segretario) vs test-assistant —
    /// useful if we ever want to style the two differently.
    let kind: Kind

    enum Kind: Hashable { case admin, testAssistant }
}

@MainActor @Observable
final class SearchViewModel {
    enum Phase {
        case idle
        case loading(String)
        case results([HydratedSection])
        case error(String)
    }

    /// One section per entity type, already hydrated.
    struct HydratedSection: Identifiable {
        let type: String
        let hits: [HydratedHit]
        var id: String { type }
    }

    /// A single result row with a typed payload that drives which canonical
    /// row gets rendered. The lean fields (id/title/subtitle/image) cover the
    /// fallback path when a hit can't be resolved against a local cache.
    struct HydratedHit: Identifiable {
        let id: String
        let leanTitle: String
        let leanSubtitle: String
        let leanImage: String
        let payload: Payload

        enum Payload {
            case user(
                RegSociModel,
                orgRole: String?,
                orgGroup: String?,
                /// Local-office affiliations derived from `local_office_admin` /
                /// `test_assistant` search hits keyed on the same user. Each
                /// entry surfaces a quiet brand-tint badge on the row
                /// (e.g. "Segretario di Lombardia").
                localOfficeAffiliations: [LocalOfficeAffiliation]
            )
            case event(EventModel)
            case deal(DealModel)
            case sig(SigModel)
            case document(DocumentModel)
            case boutique(BoutiqueModel)
            case addon(AddonModel)
            /// Numero di Quid — reusa `QuidIssueCard` come row di ricerca.
            /// L'id di routing (WP category id) viene parsato da `deep_link`
            /// (forma `mensa://quid/<category_id>`).
            case quidIssue(QuidIssue)
            /// Articolo di Quid — reusa `QuidArticleCard`. L'id di routing
            /// (WP post id) viene parsato da `deep_link`
            /// (forma `mensa://quid-article/<wp_post_id>`).
            case quidArticle(QuidArticle)
            /// Link di un gruppo locale (linktree). Lean payload — l'hit del
            /// backend porta direttamente title/subtitle/image + deep_link
            /// (`mensa://local-office/<slug>`); il tap apre `LocalOfficeView`
            /// con lo slug parsato.
            case linktreeLink(slug: String, title: String, subtitle: String, imageURL: String, externalURL: String)
            /// Gruppo dell'organigramma — porta l'intero modello così la
            /// row può mostrare conto membri e, in futuro, il role-master.
            case orgGroup(OrgChartGroup)
            /// Singola carica all'interno di un gruppo. Tap → scheda socio.
            case orgRole(role: String, groupTitle: String, groupId: String, member: OrgChartMember)
            /// Local cache miss — render a lean fallback from the lean fields.
            case lean
        }
    }

    // MARK: - Observable state

    var query: String = ""
    var selectedType: String? = nil   // nil = all
    var phase: Phase = .idle
    var recent: [String] = []

    // MARK: - Local caches (fed by Kotlin Flow subscriptions)

    private var members: [RegSociModel] = []
    private var events: [EventModel] = []
    private var deals: [DealModel] = []
    private var sigs: [SigModel] = []
    private var documents: [DocumentModel] = []
    private var boutique: [BoutiqueModel] = []
    private var addons: [AddonModel] = []

    /// userId → (role, groupTitle) — populated once per session.
    private var orgIndex: [String: (role: String, group: String)] = [:]

    /// Snapshot completo dell'organigramma — usato per indicizzare gruppi
    /// (`org_group`) e cariche (`org_role`) come tipi di ricerca aggiuntivi.
    /// Resta in memoria per tutta la sessione (poche decine di KB).
    private var orgGroups: [OrgChartGroup] = []

    // MARK: - Subscriptions

    private var stateSub: Closeable?
    private var membersSub: Closeable?
    private var eventsSub: Closeable?
    private var dealsSub: Closeable?
    private var sigsSub: Closeable?
    private var documentsSub: Closeable?
    private var boutiqueSub: Closeable?
    private var addonsSub: Closeable?

    /// Last backend `Success` payload — kept so we can re-hydrate when caches
    /// stream in late.
    private var lastSuccess: (query: String, response: SearchResponse)?

    /// Guard against double-start. The view's `.task` re-fires every time it
    /// reappears (e.g. coming back from a pushed detail). Without this guard
    /// we'd re-subscribe to every Kotlin Flow on each return, double-handling
    /// every emission and causing the search list to flicker / reset scroll.
    private var started = false

    /// IDs whose `getById` hydration is in flight. Used by `enqueueMemberHydration`
    /// to dedupe repeated requests (the same member can appear in many hits and
    /// rebuildIfPossible runs frequently as caches stream in).
    private var hydratingMembers: Set<String> = []

    func start() {
        guard !started else { return }
        started = true

        loadRecent()
        subscribeCaches()
        Task { await loadOrgChart() }

        stateSub = FlowBridgeKt.subscribe(
            flow: koin.search.state,
            onEach: { [weak self] state in
                Task { @MainActor in self?.handle(state: state) }
            },
            onError: { [weak self] err in
                let msg = err.message ?? "unknown"
                Log.auth.error("[search] state error: \(msg)")
                Task { @MainActor in self?.phase = .error(tr("app.search.error", fallback: "Errore di connessione")) }
            }
        )
    }

    /// Manual teardown — kept for symmetry but not called from `.onDisappear`
    /// (that path was the source of the flicker / scroll-reset bug). The
    /// subscriptions live for the lifetime of the @State-owned VM; when the
    /// host view is actually destroyed (e.g. tab swap), ARC drops the VM and
    /// the `[weak self]` closures held by the Kotlin flow subscribers become
    /// effectively no-ops. Not closing the Closeables explicitly is acceptable
    /// for a long-lived tab-level VM.
    func stop() {
        started = false
        stateSub?.close();     stateSub = nil
        membersSub?.close();   membersSub = nil
        eventsSub?.close();    eventsSub = nil
        dealsSub?.close();     dealsSub = nil
        sigsSub?.close();      sigsSub = nil
        documentsSub?.close(); documentsSub = nil
        boutiqueSub?.close();  boutiqueSub = nil
        addonsSub?.close();    addonsSub = nil
    }

    // MARK: - Query / filter

    func updateQuery(_ s: String) {
        query = s
        let trimmed = s.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            koin.search.clear()
            lastSuccess = nil
            phase = .idle
            return
        }
        // Niente cap stretto sul "Tutti": il backend pagina già a perPage
        // ragionevole, e la merge con la cache locale può produrre molte
        // persone valide per query frequenti (cognomi corti, città).
        // Manteniamo un upper bound coerente col single-type filter.
        let limit: Int32 = 200
        let params = SearchRepository.Params(
            q: s,
            types: selectedType.map { [$0] },
            region: nil,
            limitPerType: limit,
            hydrate: true
        )
        koin.search.update(params: params)
    }

    func pickType(_ t: String?) {
        selectedType = t
        if !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            updateQuery(query)
        } else if let success = lastSuccess {
            // Re-render with the new filter applied locally.
            phase = .results(buildSections(query: success.query, response: success.response))
        }
    }

    // MARK: - State handler

    private func handle(state: Any) {
        if state is SearchRepositoryStateIdle {
            phase = .idle
            lastSuccess = nil
        } else if let l = state as? SearchRepositoryStateLoading {
            phase = .loading(l.query)
        } else if let s = state as? SearchRepositoryStateSuccess {
            // Diagnostic: traccia le chiavi che il backend ritorna così se
            // server-side rinominano un tipo lo notiamo subito (sintomo:
            // sezione vuota su un canonical type). Da rimuovere quando lo
            // schema sarà stabile.
            let keys = (s.response.results.keys as? Set<AnyHashable>)?
                .compactMap { $0.base as? String }.sorted() ?? []
            Log.auth.info("[search] q=\(s.query) backend keys=\(keys)")
            saveRecent(s.query)
            lastSuccess = (s.query, s.response)
            phase = .results(buildSections(query: s.query, response: s.response))
        } else if let e = state as? SearchRepositoryStateError {
            phase = .error(e.cause.message ?? tr("app.search.error", fallback: "Errore"))
        } else {
            phase = .idle
        }
    }

    // MARK: - Hydration + local supplementation

    private func buildSections(query: String, response: SearchResponse) -> [HydratedSection] {
        let q = normalize(query)
        let typeFilter = selectedType
        // `local_office_admin` and `test_assistant` hits are NOT rendered as
        // their own sections — they enrich the People rows with badge chips
        // showing each member's role inside their local office. Compute the
        // affiliations index up front so the user-hit hydration can attach it.
        currentAffiliations = extractAffiliations(from: response)
        // Canonical order across sections.
        // "org" è un tipo composito (gruppi + cariche organigramma) che la
        // search ricostruisce SOLO lato client a partire dallo snapshot
        // `orgGroups`. Il backend non indicizza queste entità: niente
        // `SearchHit` arriverà per "org", e va bene così — il matcher locale
        // produce risultati comunque.
        // Organigramma in cima: chi cerca un cognome che è anche un ruolo
        // (es. "vacca" → Presidente) vede prima la carica e poi tutti i soci
        // con quel cognome. Le entità "macro" precedono quelle puntuali.
        let order = ["org", "user", "linktree_link", "event", "deal", "sig", "document", "quid_issue", "quid_pdf", "quid_article", "boutique", "addon"]

        var sections: [HydratedSection] = []
        for type in order {
            if let typeFilter, typeFilter != type { continue }
            let backendHits = backendHits(for: type, in: response)
            let merged = mergeWithLocal(type: type, backendHits: backendHits, normalizedQuery: q)
            if merged.isEmpty { continue }
            sections.append(HydratedSection(type: type, hits: merged))
        }
        return sections
    }

    /// userId → affiliations, refreshed on every `buildSections` pass.
    private var currentAffiliations: [String: [LocalOfficeAffiliation]] = [:]

    /// Produces a `userId → affiliations` map from the search response.
    ///
    /// We don't depend on a specific top-level key name (`local_office_admin`
    /// vs `LOCAL_OFFICE_ADMIN` vs whatever the backend ends up emitting) —
    /// instead we scan EVERY hit in EVERY result bucket and pattern-match:
    ///   - `image` URL contains `/members_registry/<userId>/...`
    ///   - `deep_link` is `mensa://local-office/<slug>`
    /// Both conditions together unambiguously identify an "affiliation" hit
    /// (a member shown together with the office they're tied to).
    ///
    /// Admin vs test-assistant is inferred from the localised title prefix:
    ///   - "Assistente …" → `.testAssistant`
    ///   - anything else  → `.admin`  (Segretario / Co-segretario / etc.)
    private func extractAffiliations(
        from response: SearchResponse
    ) -> [String: [LocalOfficeAffiliation]] {
        var byUser: [String: [LocalOfficeAffiliation]] = [:]

        // Pull both buckets via the typed Kotlin helper — it returns
        // `List<SearchHit>` directly, sidestepping any Map<String, ...>
        // bridging quirks. Keys are the exact strings the backend emits.
        let adminHits = response.hitsFor(type: "local_office_admin")
        let assistantHits = response.hitsFor(type: "local_office_test_assistant")

        let adminPairs = adminHits.map { ($0, LocalOfficeAffiliation.Kind.admin) }
        let assistantPairs = assistantHits.map { ($0, LocalOfficeAffiliation.Kind.testAssistant) }
        for (hit, kind) in adminPairs + assistantPairs {
            guard let userId = SearchParsers.shared.parseMemberIdFromImageURL(raw: hit.image),
                  let slug = SearchParsers.shared.parseDeepLinkSlug(deepLink: hit.deepLink, expectedHost: "local-office") else {
                Log.auth.info("[aff] skip hit id=\(hit.id) image='\(hit.image)' link='\(hit.deepLink)'")
                continue
            }
            let aff = LocalOfficeAffiliation(label: hit.title, slug: slug, kind: kind)
            byUser[userId, default: []].append(aff)
            Log.auth.info("[aff] add userId=\(userId) slug=\(slug) label='\(hit.title)'")
        }
        Log.auth.info("[aff] admins=\(adminHits.count) assistants=\(assistantHits.count) → \(byUser.count) user(s) enriched")
        return byUser
    }

    /// Estrae gli hit per un dato canonical type dalla response, tollerando
    /// alias del backend. Esempio concreto: dopo che il campo
    /// `org_chart_members.user` è stato ricablato per puntare a
    /// `members_registry`, il server ha (presumibilmente) iniziato a
    /// emettere la chiave "members_registry" invece di "user" — qui
    /// fondiamo le due in un'unica lista per il tipo canonical "user".
    private func backendHits(for canonical: String, in response: SearchResponse) -> [SearchHit] {
        let aliases: [String]
        switch canonical {
        case "user":
            aliases = ["user", "members_registry", "member", "reg_soci"]
        default:
            aliases = [canonical]
        }
        var seen = Set<String>()
        var merged: [SearchHit] = []
        for key in aliases {
            let hits = (response.results[key] as? [SearchHit]) ?? []
            for h in hits where !seen.contains(h.id) {
                merged.append(h)
                seen.insert(h.id)
            }
        }
        return merged
    }

    private func mergeWithLocal(
        type: String,
        backendHits: [SearchHit],
        normalizedQuery q: String
    ) -> [HydratedHit] {
        // Backend order first (already ranked by score).
        var ordered: [HydratedHit] = backendHits.map { hydrate(hit: $0, type: type) }
        var seen = Set(ordered.map(\.id))

        // Local substring supplementation — only when we have a non-empty query.
        guard !q.isEmpty else { return ordered }

        let localMatches: [HydratedHit]
        switch type {
        case "user":
            localMatches = members
                .filter { $0.matchesQuery(q) }
                .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
                .map { m in
                    // Mirror the auto-hydration done in `hydrate()` for backend hits:
                    // the `members_registry` LIST endpoint frequently stores image=""
                    // locally, only `getById` returns the canonical filename.
                    // Without this the avatar stays as initials until the user opens
                    // the detail (which fires getById and back-fills the cache).
                    if m.image.isEmpty {
                        enqueueMemberHydration(m.id)
                    }
                    let org = orgIndex[m.id]
                    let affs = currentAffiliations[m.id] ?? []
                    return HydratedHit(
                        id: m.id, leanTitle: m.name,
                        leanSubtitle: org?.role ?? m.city, leanImage: m.image,
                        payload: .user(m, orgRole: org?.role, orgGroup: org?.group,
                                       localOfficeAffiliations: affs)
                    )
                }
        case "event":
            localMatches = events
                .filter { $0.matchesQuery(q) }
                .sorted { $0.whenStart.epochSeconds > $1.whenStart.epochSeconds }
                .map { HydratedHit(id: $0.id, leanTitle: $0.name, leanSubtitle: "", leanImage: $0.image, payload: .event($0)) }
        case "deal":
            localMatches = deals
                .filter { $0.matchesQuery(q) }
                .map { HydratedHit(id: $0.id, leanTitle: $0.name, leanSubtitle: $0.commercialSector, leanImage: "", payload: .deal($0)) }
        case "sig":
            localMatches = sigs
                .filter { $0.matchesQuery(q) }
                .map { HydratedHit(id: $0.id, leanTitle: $0.name, leanSubtitle: $0.description_, leanImage: $0.image, payload: .sig($0)) }
        case "document":
            localMatches = documents
                .filter { $0.matchesQuery(q) }
                .map { HydratedHit(id: $0.id, leanTitle: $0.name, leanSubtitle: $0.description_ ?? "", leanImage: "", payload: .document($0)) }
        case "boutique":
            localMatches = boutique
                .filter { $0.matchesQuery(q) }
                .map { HydratedHit(id: $0.id, leanTitle: $0.name, leanSubtitle: $0.description_, leanImage: $0.image.first ?? "", payload: .boutique($0)) }
        case "addon":
            localMatches = addons
                .filter { $0.matchesQuery(q) }
                .map { HydratedHit(id: $0.id, leanTitle: $0.name, leanSubtitle: $0.description_, leanImage: $0.icon, payload: .addon($0)) }
        case "quid_issue", "quid_article", "quid_pdf", "linktree_link":
            // Niente cache locale interrogabile per Quid: gli hit arrivano
            // solo dal backend (`SearchRepository`). La hydration estrae i
            // modelli da `SearchHit` direttamente.
            localMatches = []
        case "org":
            localMatches = orgMatches(normalizedQuery: q)
        default:
            localMatches = []
        }

        for m in localMatches where !seen.contains(m.id) {
            ordered.append(m)
            seen.insert(m.id)
        }
        // Hard cap to keep the UI snappy even on degenerate queries.
        if ordered.count > 500 { ordered = Array(ordered.prefix(500)) }
        return ordered
    }

    private func hydrate(hit: SearchHit, type: String) -> HydratedHit {
        let id = hit.id
        let title = hit.title
        let subtitle = hit.subtitle
        let image = hit.image

        switch type {
        case "user":
            if let m = members.first(where: { $0.id == id }) {
                let org = orgIndex[m.id]
                // The `members_registry` LIST endpoint sometimes returns image="".
                // Only `getById` brings the canonical filename. Trigger a
                // background hydration so the row swaps from initials to photo
                // without requiring the user to open the detail page first.
                if m.image.isEmpty {
                    enqueueMemberHydration(id)
                }
                let affs = currentAffiliations[m.id] ?? []
                return HydratedHit(id: id, leanTitle: title, leanSubtitle: subtitle, leanImage: image,
                                   payload: .user(m, orgRole: org?.role, orgGroup: org?.group,
                                                  localOfficeAffiliations: affs))
            }
            // Cache miss: il backend ci ha dato l'id, ma il `members_registry`
            // locale non l'ha mai visto (la sync è on-demand). Lanciamo un
            // fetch puntuale così la cache si popola: la successiva
            // emissione del flow `regSoci.observeAll` farà ri-hydratare la
            // row con il modello completo (foto + città + ruolo orgchart).
            enqueueMemberHydration(id)
        case "event":
            if let m = events.first(where: { $0.id == id }) {
                return HydratedHit(id: id, leanTitle: title, leanSubtitle: subtitle, leanImage: image, payload: .event(m))
            }
        case "deal":
            if let m = deals.first(where: { $0.id == id }) {
                return HydratedHit(id: id, leanTitle: title, leanSubtitle: subtitle, leanImage: image, payload: .deal(m))
            }
        case "sig":
            if let m = sigs.first(where: { $0.id == id }) {
                return HydratedHit(id: id, leanTitle: title, leanSubtitle: subtitle, leanImage: image, payload: .sig(m))
            }
        case "document":
            if let m = documents.first(where: { $0.id == id }) {
                return HydratedHit(id: id, leanTitle: title, leanSubtitle: subtitle, leanImage: image, payload: .document(m))
            }
        case "boutique":
            if let m = boutique.first(where: { $0.id == id }) {
                return HydratedHit(id: id, leanTitle: title, leanSubtitle: subtitle, leanImage: image, payload: .boutique(m))
            }
        case "addon":
            if let m = addons.first(where: { $0.id == id }) {
                return HydratedHit(id: id, leanTitle: title, leanSubtitle: subtitle, leanImage: image, payload: .addon(m))
            }
        case "quid_issue":
            // Costruiamo un `QuidIssue` "lean" dai campi del SearchHit così
            // possiamo riusare `QuidIssueCard` invariata. L'id di routing
            // (WP category id) viene dal deep_link, non dall'`id` PocketBase
            // dell'hit. Backend manda anche i numeri PDF sotto `type=quid_issue`
            // ma con deep_link `mensa://quid-pdf/<n>` — riconosciamolo qui.
            if let categoryId = SearchParsers.shared.parseDeepLinkLastInt(deepLink: hit.deepLink, expectedHost: "quid")?.int64Value {
                let issue = QuidIssue(
                    id: categoryId,
                    slug: "",
                    name: title,
                    description: "",
                    articleCount: Int32(SearchParsers.shared.parseArticleCount(subtitle: subtitle)),
                    coverImageUrl: image.isEmpty ? nil : image,
                    pdfUrl: nil
                )
                return HydratedHit(id: id, leanTitle: title, leanSubtitle: subtitle, leanImage: image, payload: .quidIssue(issue))
            }
            // PDF detection — robust to whatever deep-link format the backend
            // chose. Triggers on either:
            //   - host `quid-pdf` or `quid_pdf` in the deep link, OR
            //   - subtitle starting with "PDF" (the backend marks PDF issues that way)
            // Extract the issue number from any of: deep link path, the title
            // (e.g. "Quid 03 - La libertà"), or the slug. If we can pin a
            // number we build a PDF-flagged `QuidIssue` with id `-n`.
            let looksLikePdf = subtitle.uppercased().hasPrefix("PDF")
                || hit.deepLink.contains("quid-pdf")
                || hit.deepLink.contains("quid_pdf")
            if looksLikePdf, let n = quidPdfNumber(deepLink: hit.deepLink, title: title) {
                let issue = QuidIssue(
                    id: -n,
                    slug: "",
                    name: title,
                    description: "",
                    articleCount: 0,
                    coverImageUrl: image.isEmpty ? nil : image,
                    pdfUrl: hit.deepLink.isEmpty ? "mensa://quid-pdf/\(n)" : hit.deepLink
                )
                return HydratedHit(id: id, leanTitle: title, leanSubtitle: subtitle, leanImage: image, payload: .quidIssue(issue))
            }
        case "quid_pdf":
            // Numero PDF di Quid. Il deep link è `mensa://quid-pdf/<n>` con
            // `n` 1..12; lo trasformiamo in un `QuidIssue` "lean" con id
            // negativo (stesso schema usato in `QuidApi.fetchPdfArchive`) e
            // `pdfUrl` non-nil per attivare il badge "PDF" sulla card. L'URL
            // reale del PDF non è disponibile qui — verrà risolto al tap dal
            // `QuidPDFDeepLinkLoader` via `koin.quid.observeIssues()`.
            if let n = SearchParsers.shared.parseDeepLinkLastInt(deepLink: hit.deepLink, expectedHost: "quid-pdf")?.int64Value {
                let issue = QuidIssue(
                    id: -n,
                    slug: "",
                    name: title,
                    description: "",
                    articleCount: 0,
                    coverImageUrl: image.isEmpty ? nil : image,
                    pdfUrl: hit.deepLink
                )
                return HydratedHit(id: id, leanTitle: title, leanSubtitle: subtitle, leanImage: image, payload: .quidIssue(issue))
            }
        case "quid_article":
            // Stesso pattern: costruzione "lean" di QuidArticle dai campi del
            // SearchHit per riusare `QuidArticleCard`.
            if let postId = SearchParsers.shared.parseDeepLinkLastInt(deepLink: hit.deepLink, expectedHost: "quid-article")?.int64Value {
                let article = QuidArticle(
                    id: postId,
                    slug: "",
                    link: "",
                    date: "",
                    modified: "",
                    titleHtml: title,
                    excerptHtml: subtitle,
                    contentHtml: "",
                    coverImageUrl: image.isEmpty ? nil : image,
                    categoryNames: []
                )
                return HydratedHit(id: id, leanTitle: title, leanSubtitle: subtitle, leanImage: image, payload: .quidArticle(article))
            }
        case "linktree_link":
            // Backend search ranks individual linktree links. The deep_link
            // (`mensa://local-office/<slug>`) carries the slug — parse it
            // straight to a string and pass through; navigation opens
            // `LocalOfficeView(slug:)`.
            if let slug = SearchParsers.shared.parseDeepLinkSlug(deepLink: hit.deepLink, expectedHost: "local-office") {
                return HydratedHit(
                    id: id, leanTitle: title, leanSubtitle: subtitle, leanImage: image,
                    payload: .linktreeLink(
                        slug: slug, title: title, subtitle: subtitle,
                        imageURL: image, externalURL: hit.deepLink
                    )
                )
            }
        default: break
        }
        return HydratedHit(id: id, leanTitle: title, leanSubtitle: subtitle, leanImage: image, payload: .lean)
    }

    // MARK: - Member hydration (auto-load missing avatars)

    /// Fire-and-forget hydration of a single member by id. Deduped via
    /// `hydratingMembers` so repeated calls (e.g. rebuildIfPossible firing
    /// on every cache emission) don't spam the network.
    ///
    /// On success `getById` upserts the canonical record into SQLDelight; the
    /// `members_registry` flow then re-emits, `membersSub` catches it, and
    /// `rebuildIfPossible` produces a new `HydratedHit` with the populated
    /// `image` field — the row swaps from initials to the loaded avatar.
    ///
    /// This lives in the viewmodel (rather than relying on
    /// `PersonSearchResultRow.task(id:)`) because List row lifecycles are
    /// noisy under fast scroll / navigation, and we want the fetch to be
    /// resilient to that.
    private func enqueueMemberHydration(_ id: String) {
        guard !id.isEmpty, !hydratingMembers.contains(id) else { return }
        hydratingMembers.insert(id)
        Task { [weak self] in
            _ = try? await koin.regSoci.getById(id: id)
            await MainActor.run { self?.hydratingMembers.remove(id) }
        }
    }

    // MARK: - Deep-link parsing helpers

    /// Cerca il numero del numero PDF Quid (1..16) ovunque possa essere:
    /// 1) ultimo segmento del deep link (`mensa://quid-pdf/3`)
    /// 2) prima sequenza di cifre nel titolo (`Quid 03 - La libertà` → 3)
    private func quidPdfNumber(deepLink: String, title: String) -> Int64? {
        if let url = URL(string: deepLink),
           url.scheme == "mensa",
           let last = url.path.split(separator: "/").last,
           let n = Int64(last) {
            return n
        }
        // Fallback: regex on title.
        let pattern = #"Quid\s+0?(\d{1,2})"#
        if let r = title.range(of: pattern, options: [.regularExpression, .caseInsensitive]) {
            let match = String(title[r])
            let digits = match.unicodeScalars.filter { CharacterSet.decimalDigits.contains($0) }
            if let n = Int64(String(String.UnicodeScalarView(digits))) {
                return n
            }
        }
        return nil
    }

    /// Indicizzazione locale dell'organigramma per il tipo "org".
    ///
    /// Strategia: scorre i gruppi e raccoglie due flussi distinti di hit:
    ///  - **Gruppi** (`org_group_*` id): match sulla key grezza (es.
    ///    "consiglio") O sul titolo localizzato (es. "Consiglio Direttivo").
    ///    Quando un gruppo matcha, NON espande le sue cariche — sarebbe
    ///    rumore: l'utente che cerca "consiglio" vuole il gruppo, non le
    ///    20 persone dentro.
    ///  - **Cariche** (`org_role_*` id): match sul nome del ruolo (es.
    ///    "presidente") O sul nome del socio. Ogni hit porta con sé tutto
    ///    quello che serve a renderizzare la riga senza ulteriori lookup.
    ///
    /// Ordinamento: gruppi prima (più "macro"), poi cariche. I master role
    /// (es. Presidente) vincono fra le cariche.
    private func orgMatches(normalizedQuery q: String) -> [HydratedHit] {
        guard !q.isEmpty, !orgGroups.isEmpty else { return [] }

        var groupHits: [HydratedHit] = []
        var roleHits: [HydratedHit] = []

        for group in orgGroups {
            let titleKey = normalize(group.title)
            let titlePretty = normalize(localizedGroupTitle(group.title))
            if titleKey.contains(q) || titlePretty.contains(q) {
                groupHits.append(HydratedHit(
                    id: "org_group_\(group.id)",
                    leanTitle: localizedGroupTitle(group.title),
                    leanSubtitle: "",
                    leanImage: "",
                    payload: .orgGroup(group)
                ))
            }

            for member in group.members where !member.inactive {
                let roleKey = normalize(member.role)
                let nameKey = normalize(member.name)
                guard roleKey.contains(q) || nameKey.contains(q) else { continue }
                roleHits.append(HydratedHit(
                    id: "org_role_\(group.id)_\(member.userId)",
                    leanTitle: member.role,
                    leanSubtitle: member.name,
                    leanImage: member.image,
                    payload: .orgRole(
                        role: member.role,
                        groupTitle: localizedGroupTitle(group.title),
                        groupId: group.id,
                        member: member
                    )
                ))
            }
        }
        // Master role in cima fra le cariche.
        roleHits.sort { lhs, rhs in
            let lm = isMasterRole(lhs)
            let rm = isMasterRole(rhs)
            if lm != rm { return lm }
            return false  // stable
        }
        return groupHits + roleHits
    }

    private func isMasterRole(_ hit: HydratedHit) -> Bool {
        if case .orgRole(_, _, _, let member) = hit.payload { return member.isMaster }
        return false
    }

    /// Mirror locale di `OrgChartView.localizedGroupTitle` — Tolgee key
    /// (es. "consiglio") → traduzione, con un fallback "Pretty Case" se la
    /// chiave non è ancora stata pushata. Tenuta qui (anziché estratta in
    /// helper condiviso) per non introdurre dipendenze cross-feature.
    private func localizedGroupTitle(_ raw: String) -> String {
        let pretty = raw
            .replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: "-", with: " ")
            .capitalized
        return tr(raw, fallback: pretty)
    }

    /// Lowercase + diacritic-fold — "Persè" matches "perse".
    private func normalize(_ s: String) -> String {
        s.trimmingCharacters(in: .whitespacesAndNewlines)
            .folding(options: .diacriticInsensitive, locale: .current)
            .lowercased()
    }

    // MARK: - Cache subscriptions

    private func subscribeCaches() {
        membersSub = FlowBridgeKt.subscribe(
            flow: koin.regSoci.observeAll(),
            onEach: { [weak self] value in
                let list = (value as? [RegSociModel]) ?? []
                Task { @MainActor in self?.members = list; self?.rebuildIfPossible() }
            },
            onError: { _ in }
        )
        eventsSub = FlowBridgeKt.subscribe(
            flow: koin.events.observeAll(),
            onEach: { [weak self] value in
                let list = (value as? [EventModel]) ?? []
                Task { @MainActor in self?.events = list; self?.rebuildIfPossible() }
            },
            onError: { _ in }
        )
        dealsSub = FlowBridgeKt.subscribe(
            flow: koin.deals.observeAll(),
            onEach: { [weak self] value in
                let list = (value as? [DealModel]) ?? []
                Task { @MainActor in self?.deals = list; self?.rebuildIfPossible() }
            },
            onError: { _ in }
        )
        sigsSub = FlowBridgeKt.subscribe(
            flow: koin.sigs.observeAll(),
            onEach: { [weak self] value in
                let list = (value as? [SigModel]) ?? []
                Task { @MainActor in self?.sigs = list; self?.rebuildIfPossible() }
            },
            onError: { _ in }
        )
        documentsSub = FlowBridgeKt.subscribe(
            flow: koin.documents.observeAll(),
            onEach: { [weak self] value in
                let list = (value as? [DocumentModel]) ?? []
                Task { @MainActor in self?.documents = list; self?.rebuildIfPossible() }
            },
            onError: { _ in }
        )
        boutiqueSub = FlowBridgeKt.subscribe(
            flow: koin.boutique.observeAll(),
            onEach: { [weak self] value in
                let list = (value as? [BoutiqueModel]) ?? []
                Task { @MainActor in self?.boutique = list; self?.rebuildIfPossible() }
            },
            onError: { _ in }
        )
        addonsSub = FlowBridgeKt.subscribe(
            flow: koin.addons.observeAll(),
            onEach: { [weak self] value in
                let list = (value as? [AddonModel]) ?? []
                Task { @MainActor in self?.addons = list; self?.rebuildIfPossible() }
            },
            onError: { _ in }
        )
    }

    /// Re-emit hydrated sections after caches update — keeps the rendered
    /// rows in sync with late-arriving local data and ensures the user-typed
    /// query benefits from the directory as soon as it streams in.
    private func rebuildIfPossible() {
        guard let success = lastSuccess else { return }
        phase = .results(buildSections(query: success.query, response: success.response))
    }

    // MARK: - Org chart enrichment

    private func loadOrgChart() async {
        do {
            let chart = try await koin.orgChart.fetch()
            var map: [String: (role: String, group: String)] = [:]
            for group in chart.groups {
                for member in group.members where !member.userId.isEmpty {
                    // First role wins (Presidente trumps a later Tesoriere entry).
                    if map[member.userId] == nil {
                        map[member.userId] = (member.role, group.title)
                    }
                }
            }
            self.orgIndex = map
            self.orgGroups = chart.groups.filter { !$0.members.isEmpty }
            rebuildIfPossible()
        } catch {
            // Non-fatal — search still works, just without role badges.
            Log.auth.error("[search] orgchart fetch failed: \(error.localizedDescription)")
        }
    }

    // MARK: - Recent searches

    private func loadRecent() {
        recent = UserDefaults.standard.stringArray(forKey: "search.recent") ?? []
    }

    private func saveRecent(_ q: String) {
        let trimmed = q.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        // Prefix-dedup: la search committa la query dopo un debounce, quindi
        // digitando "Cu" → pausa → "Cuni" finivano salvate entrambe come
        // entry separate. Ogni volta che salviamo, eliminiamo i recenti
        // che sono PREFISSO della nuova query (es. "Cu" sparisce quando
        // arriva "Cuni") e viceversa NON salviamo se la nuova è prefisso
        // di una più lunga già presente — il termine corto è quasi sempre
        // una tappa intermedia, non un'intenzione finale dell'utente.
        // Confronto case-insensitive + diacritic-folded per matchare
        // "cafe" vs "Caffè".
        let key = normalize(trimmed)
        if recent.contains(where: { key != normalize($0) && normalize($0).hasPrefix(key) }) {
            // Esiste già una versione più lunga di questa query → la nuova
            // (più corta) è una tappa intermedia: la ignoriamo.
            return
        }
        var arr = recent.filter { existing in
            let ek = normalize(existing)
            // Rimuovi duplicati esatti e prefissi della nuova query.
            return ek != key && !key.hasPrefix(ek)
        }
        arr.insert(trimmed, at: 0)
        if arr.count > 8 { arr = Array(arr.prefix(8)) }
        recent = arr
        UserDefaults.standard.set(arr, forKey: "search.recent")
    }

    func clearRecent() {
        recent = []
        UserDefaults.standard.removeObject(forKey: "search.recent")
    }
}

// MARK: - Per-type local match predicates (lowercased, diacritic-folded)

private extension String {
    func folded() -> String {
        folding(options: .diacriticInsensitive, locale: .current).lowercased()
    }
}

private extension RegSociModel {
    func matchesQuery(_ q: String) -> Bool {
        if q.isEmpty { return false }
        return name.folded().contains(q)
            || city.folded().contains(q)
            || id.lowercased().contains(q)
    }
}

private extension EventModel {
    func matchesQuery(_ q: String) -> Bool {
        if q.isEmpty { return false }
        if name.folded().contains(q) { return true }
        if description_.folded().contains(q) { return true }
        if let pos = position {
            if pos.name.folded().contains(q) { return true }
            if pos.address.folded().contains(q) { return true }
        }
        return false
    }
}

private extension DealModel {
    func matchesQuery(_ q: String) -> Bool {
        if q.isEmpty { return false }
        if name.folded().contains(q) { return true }
        if commercialSector.folded().contains(q) { return true }
        if let d = details, d.folded().contains(q) { return true }
        if let w = who, w.folded().contains(q) { return true }
        if let pos = position {
            if pos.name.folded().contains(q) { return true }
            if pos.state.folded().contains(q) { return true }
        }
        return false
    }
}

private extension SigModel {
    func matchesQuery(_ q: String) -> Bool {
        if q.isEmpty { return false }
        return name.folded().contains(q)
            || description_.folded().contains(q)
            || groupType.folded().contains(q)
    }
}

private extension DocumentModel {
    func matchesQuery(_ q: String) -> Bool {
        if q.isEmpty { return false }
        if name.folded().contains(q) { return true }
        if let d = description_, d.folded().contains(q) { return true }
        return category.folded().contains(q)
    }
}

private extension BoutiqueModel {
    func matchesQuery(_ q: String) -> Bool {
        if q.isEmpty { return false }
        return name.folded().contains(q)
            || description_.folded().contains(q)
    }
}

private extension AddonModel {
    func matchesQuery(_ q: String) -> Bool {
        if q.isEmpty { return false }
        return name.folded().contains(q)
            || description_.folded().contains(q)
    }
}
