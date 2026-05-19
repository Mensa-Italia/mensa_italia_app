import SwiftUI
import Shared

/// Global search screen. iOS 26 Liquid Glass, inset-grouped sectioned `List`,
/// per-type canonical rows. Mirrors how the rest of the app renders each
/// entity, so a search result for an event looks exactly like an event row in
/// the Events list, a person looks like a directory cell, and so on.
struct SearchView: View {
    @State private var vm = SearchViewModel()
    /// Toggle in chip-Eventi view to opt-into past events (default hidden).
    /// In the "Tutti" overview past events are always hidden — see `applyEventFilter`.
    @State private var showPastEvents = false

    /// Max rows per section in the "Tutti" overview before collapsing into a
    /// "Mostra tutto" footer that selects the dedicated chip. Apple HIG-ish
    /// — keeps the unified scroll digestible without forcing the user to
    /// scroll past 50 deals to see boutique.
    private let previewLimit: Int = 6

    // Wrapped Hashable navigation values so each detail destination is
    // resolved by an unambiguous type and we don't collide with global
    // `String.self` destinations from other navigation stacks.
    private struct EventRoute: Hashable { let id: String }
    private struct DealRoute: Hashable { let id: String }
    private struct DocumentRoute: Hashable { let id: String }
    private struct SigRoute: Hashable { let id: String }
    /// Navigazione verso l'organigramma con la search interna pre-popolata
    /// sul titolo del gruppo selezionato.
    private struct OrgGroupRoute: Hashable { let groupTitle: String }

    private struct FilterChip: Identifiable, Hashable {
        let id: String  // "all" | type key
        let label: String
        let key: String?
        let systemImage: String
    }

    private var filterChips: [FilterChip] {
        [
            FilterChip(id: "all", label: tr("views.community.chip.all", fallback: "Tutti"), key: nil, systemImage: "circle.grid.2x2"), // i18n
            FilterChip(id: "user", label: tr("app.search.filter.people", fallback: "Persone"), key: "user", systemImage: "person.2"), // i18n
            FilterChip(id: "event", label: tr("views.events.title", fallback: "Eventi"), key: "event", systemImage: "calendar"), // i18n
            FilterChip(id: "deal", label: tr("app.search.filter.deals", fallback: "Deals"), key: "deal", systemImage: "tag"), // i18n
            FilterChip(id: "sig", label: tr("app.discover.groups", fallback: "Gruppi e interessi"), key: "sig", systemImage: "person.3"), // i18n
            FilterChip(id: "document", label: tr("addons.documents.title", fallback: "Documenti"), key: "document", systemImage: "doc.text"), // i18n
            FilterChip(id: "boutique", label: tr("addons.boutique.title", fallback: "Boutique"), key: "boutique", systemImage: "bag"), // i18n
            FilterChip(id: "quid_issue", label: tr("app.search.filter.quid_issues", fallback: "Numeri Quid"), key: "quid_issue", systemImage: "books.vertical"), // i18n
            FilterChip(id: "quid_article", label: tr("app.search.filter.quid_articles", fallback: "Articoli Quid"), key: "quid_article", systemImage: "newspaper"), // i18n
            FilterChip(id: "linktree_link", label: tr("app.search.filter.linktree_link", fallback: "Gruppi locali"), key: "linktree_link", systemImage: "building.2"), // i18n
            // Organigramma — tipo composito: gruppi e cariche convivono nella
            // stessa sezione perché concettualmente sono la stessa entità
            // ("chi fa cosa in Mensa"). Vedi `SearchViewModel.orgMatches`.
            FilterChip(id: "org", label: tr("app.search.filter.org", fallback: "Organigramma"), key: "org", systemImage: "rectangle.connected.to.line.below") // i18n
        ]
    }

    var body: some View {
        rootContent
            .navigationTitle(tr("app.search.title", fallback: "Cerca")) // i18n
            .searchable(
                text: Binding(
                    get: { vm.query },
                    set: { vm.updateQuery($0) }
                ),
                placement: .navigationBarDrawer(displayMode: .always),
                prompt: tr("app.search.placeholder", fallback: "Cerca persone, eventi, deal…") // i18n
            )
            .toolbar {
                if vm.selectedType != nil {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(tr("app.search.filter.clear", fallback: "Tutti")) { // i18n
                            vm.pickType(nil)
                        }
                        .tint(AppTheme.Colors.brandTintAdaptive)
                    }
                }
            }
            .navigationDestination(for: RegSociRoute.self) { route in
                MemberDetailView(memberId: route.id)
            }
            .navigationDestination(for: EventRoute.self) { route in
                EventDetailView(eventId: route.id)
            }
            .navigationDestination(for: DealRoute.self) { route in
                DealDetailView(dealId: route.id)
            }
            .navigationDestination(for: DocumentRoute.self) { route in
                DocumentDetailView(documentId: route.id)
            }
            .navigationDestination(for: SigRoute.self) { route in
                SigDetailView(sigId: route.id)
            }
            .navigationDestination(for: OrgGroupRoute.self) { route in
                OrgChartView(initialSearchText: route.groupTitle)
            }
            .navigationDestination(for: QuidIssueRoute.self) { route in
                QuidIssueView(issueId: route.issueId, issueName: route.issueName)
            }
            .navigationDestination(for: QuidArticleRoute.self) { route in
                QuidArticleView(articleId: route.articleId)
            }
            .navigationDestination(for: QuidPDFDeepLinkRoute.self) { route in
                QuidPDFDeepLinkLoader(recordId: route.recordId)
            }
            .navigationDestination(for: LocalOfficeSlugRoute.self) { route in
                LocalOfficeBySlugLoader(slug: route.slug)
            }
            .task {
                // start() is idempotent — subscriptions live for the lifetime of
                // the view's @State `vm`. We intentionally do NOT call stop() on
                // disappear, because in a NavigationStack pushing a detail fires
                // .onDisappear on the parent, and tearing down here would force
                // a full re-subscribe-re-emit cycle on return: that's what was
                // causing the scroll-reset flicker after coming back from a
                // person/event detail. The VM is cleaned up via its `deinit`
                // when the host view is actually destroyed (tab swap, pop).
                vm.start()
                if let q = ProcessInfo.processInfo.environment["MENSA_AUTOSEARCH"], !q.isEmpty {
                    vm.updateQuery(q)
                }
            }
    }

    // MARK: - Root content switch

    @ViewBuilder private var rootContent: some View {
        // Chip bar lives OUTSIDE the List so it can scroll edge-to-edge,
        // ignoring the `.insetGrouped` section margins. The phase content
        // sits below it and keeps its native list inset styling.
        VStack(spacing: 0) {
            chipBar
                .padding(.bottom, 4)
            switch vm.phase {
            case .idle:
                idleList
            case .loading:
                loadingList
            case .results(let sections) where sections.isEmpty:
                emptyResultsView
            case .results(let sections):
                resultsList(sections)
            case .error(let msg):
                errorView(message: msg)
            }
        }
    }

    // MARK: - Edge-to-edge chip bar

    private var chipBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(filterChips) { chip in
                    let isSelected = (vm.selectedType ?? "all") == (chip.key ?? "all")
                    Button {
                        vm.pickType(chip.key)
                    } label: {
                        Label(chip.label, systemImage: chip.systemImage)
                            .labelStyle(.titleAndIcon)
                            .font(.subheadline.weight(.medium))
                    }
                    .buttonStyle(.bordered)
                    .tint(isSelected ? AppTheme.Colors.brandTintAdaptive : .secondary)
                    .controlSize(.regular)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 6)
        }
    }

    // MARK: - Idle (no query)

    private var idleList: some View {
        // Empty state honesto: niente "suggeriti" finti agganciati a query
        // pre-compilate. Invece:
        //  - un hero educational che spiega lo scope (ricerca davvero
        //    globale — pochi utenti se lo aspettano);
        //  - una riga di esempi di query (NON suggerimenti rankati): tap
        //    popola la search bar così l'utente vede subito cosa la ricerca
        //    produce su domini diversi;
        //  - i Recenti, se ce ne sono.
        ScrollView {
            VStack(spacing: 16) {
                searchHeroCard
                    .padding(.horizontal, 16)
                    .padding(.top, 12)

                searchExamplesRow
                    .padding(.top, 4)

                if !vm.recent.isEmpty {
                    recentSection
                        .padding(.top, 8)
                }

                Spacer(minLength: 24)
            }
        }
        .scrollIndicators(.hidden)
    }

    /// Hero esplicativo dello scope. Mostrato solo nello stato idle, una
    /// volta sola (non swipea via, è statico). L'icona usa SF Symbols
    /// `sparkle.magnifyingglass` per richiamare visivamente la ricerca.
    private var searchHeroCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 10) {
                Image(systemName: "sparkle.magnifyingglass")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                Text(tr("app.search.empty.hero.title", fallback: "Una sola barra, tutto Mensa"))
                    .font(.headline)
                    .foregroundStyle(.primary)
            }
            Text(tr(
                "app.search.empty.hero.body",
                fallback: "Cerca soci, eventi, deal, gruppi, documenti, boutique e addon. Tutto da qui."
            ))
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(.regularMaterial)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(Color.primary.opacity(0.06), lineWidth: 0.5)
        )
    }

    /// Esempi di query. NON sono suggerimenti — sono spunti per mostrare la
    /// varietà di domini ricercabili. La copy "Prova a cercare" è esplicita
    /// per non confondere l'utente con un'aspettativa di rilevanza.
    private var searchExamplesRow: some View {
        let examples: [String] = [
            tr("app.search.empty.example.council", fallback: "consiglio"),
            tr("app.search.empty.example.city", fallback: "milano"),
            tr("app.search.empty.example.card", fallback: "tessera"),
            tr("app.search.empty.example.balance", fallback: "bilancio")
        ]
        return VStack(alignment: .leading, spacing: 8) {
            Text(tr("app.search.empty.examples.title", fallback: "Prova a cercare"))
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.secondary)
                .textCase(.uppercase)
                .padding(.horizontal, 16)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(examples, id: \.self) { example in
                        Button {
                            vm.updateQuery(example)
                        } label: {
                            Text(example)
                                .font(.subheadline.weight(.medium))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(
                                    Capsule(style: .continuous)
                                        .fill(.regularMaterial)
                                )
                                .overlay(
                                    Capsule(style: .continuous)
                                        .strokeBorder(Color.primary.opacity(0.06), lineWidth: 0.5)
                                )
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(.primary)
                    }
                }
                .padding(.horizontal, 16)
            }
        }
    }

    private var recentSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(tr("app.search.recent", fallback: "Recenti"))
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .textCase(.uppercase)
                Spacer()
                Button(tr("app.search.recent.clear", fallback: "Cancella")) {
                    vm.clearRecent()
                }
                .font(.footnote)
                .tint(AppTheme.Colors.brandTintAdaptive)
            }
            .padding(.horizontal, 16)

            VStack(spacing: 0) {
                ForEach(Array(vm.recent.enumerated()), id: \.element) { idx, q in
                    Button {
                        vm.updateQuery(q)
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: "clock.arrow.circlepath")
                                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                                .frame(width: 22)
                            Text(q)
                                .foregroundStyle(.primary)
                            Spacer()
                        }
                        .padding(.vertical, 12)
                        .padding(.horizontal, 16)
                    }
                    .buttonStyle(.plain)
                    if idx < vm.recent.count - 1 {
                        Divider().padding(.leading, 50)
                    }
                }
            }
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(.regularMaterial)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .strokeBorder(Color.primary.opacity(0.06), lineWidth: 0.5)
            )
            .padding(.horizontal, 16)
        }
    }

    // MARK: - Loading

    private var loadingList: some View {
        List {
            Section {
                HStack {
                    Spacer()
                    ProgressView()
                        .controlSize(.regular)
                    Spacer()
                }
                .listRowBackground(Color.clear)
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
    }

    // MARK: - Empty results

    private var emptyResultsView: some View {
        // Chip bar is rendered above by `rootContent` — empty state just
        // shows the standard search-empty placeholder, full-bleed.
        ContentUnavailableView.search(text: vm.query)
    }

    // MARK: - Error

    private func errorView(message: String) -> some View {
        ContentUnavailableView {
            Label(tr("app.search.error.title", fallback: "Ricerca non disponibile"), systemImage: "wifi.exclamationmark") // i18n
        } description: {
            Text(message)
        } actions: {
            Button(tr("app.retry", fallback: "Riprova")) { // i18n
                vm.updateQuery(vm.query)
            }
            .buttonStyle(.borderedProminent)
            .tint(AppTheme.Colors.brandPrimary)
        }
    }

    // MARK: - Results

    @ViewBuilder
    private func resultsList(_ sections: [SearchViewModel.HydratedSection]) -> some View {
        List {
            ForEach(sections) { section in
                if section.type == "user" {
                    peopleSection(section)
                } else {
                    sectionBlock(section)
                }
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .scrollDismissesKeyboard(.interactively)
        // Resetta il toggle "eventi passati" quando si esce dalla chip Eventi
        // (es. switch a "Tutti" o a "Persone"): tornandoci ci si aspetta lo
        // stato pulito.
        .onChange(of: vm.selectedType) { _, new in
            if new != "event" { showPastEvents = false }
        }
    }

    // MARK: - Generic section (non-user)

    @ViewBuilder
    private func sectionBlock(_ section: SearchViewModel.HydratedSection) -> some View {
        let snapshot = visibleHits(for: section)
        let headerCount: Int = {
            if snapshot.onlyPastInTutti { return snapshot.pastEventsHidden }
            return snapshot.visible.count
        }()

        Section {
            if snapshot.onlyPastInTutti {
                // Single CTA row that opens the Eventi chip with past events visible.
                showPastEventsCardRow(hidden: snapshot.pastEventsHidden)
            } else {
                ForEach(snapshot.visible) { hit in
                    row(for: hit, type: section.type)
                }
                if snapshot.hasMorePreview {
                    if isCardStyleSection(section.type) {
                        cardShowAllRow(type: section.type, total: snapshot.totalAfterEventFilter)
                    } else {
                        listShowAllRow(type: section.type, total: snapshot.totalAfterEventFilter)
                    }
                }
                if section.type == "event" && vm.selectedType == "event" {
                    if !showPastEvents && snapshot.pastEventsHidden > 0 {
                        togglePastEventsRow(hidden: snapshot.pastEventsHidden, showing: false)
                    } else if showPastEvents {
                        togglePastEventsRow(hidden: 0, showing: true)
                    }
                }
            }
        } header: {
            sectionHeader(section, displayedCount: headerCount)
        }
    }

    // MARK: - People section (role-holders first + same preview-cap behaviour)

    @ViewBuilder
    private func peopleSection(_ section: SearchViewModel.HydratedSection) -> some View {
        // Role-holders are forced to the top of the section so they're the
        // first faces a user sees when searching by name. The same preview
        // cap as the other sections is applied AFTER the sort.
        let sortedHits = section.hits.enumerated()
            .sorted { lhs, rhs in
                let lr = isRoleHolder(lhs.element)
                let rr = isRoleHolder(rhs.element)
                if lr != rr { return lr }       // role-holders first
                return lhs.offset < rhs.offset  // stable order within group
            }
            .map { $0.element }

        let snapshot = previewSlice(sortedHits, sectionType: "user")

        Section {
            ForEach(snapshot.visible) { hit in
                row(for: hit, type: "user")
            }
            if snapshot.hasMorePreview {
                listShowAllRow(type: "user", total: sortedHits.count)
            }
        } header: {
            sectionHeader(section, displayedCount: snapshot.visible.count)
        }
    }

    // MARK: - Visibility / preview helpers

    /// Result of applying both the past-event filter and the preview cap to
    /// a section. Exposed as a struct so the call sites stay readable.
    private struct VisibleHitsSnapshot {
        let visible: [SearchViewModel.HydratedHit]
        let hasMorePreview: Bool       // capped in Tutti → show "Mostra tutto"
        let totalAfterEventFilter: Int // count used by the "Mostra tutto" subtitle
        let pastEventsHidden: Int      // > 0 only when section is "event" and
                                       // past events were filtered out
        /// Edge case: in "Tutti" the event filter wiped out all results because
        /// every match was a past event. We still render the section with a
        /// single CTA row that jumps the user into the Eventi chip with
        /// `showPastEvents = true`. Empty otherwise the section would just
        /// show a lonely "Eventi" header with no content.
        let onlyPastInTutti: Bool
    }

    private func visibleHits(for section: SearchViewModel.HydratedSection) -> VisibleHitsSnapshot {
        // Step 1: events filter — past events are hidden in "Tutti" always,
        // and in the event chip until the user opts in.
        var hits = section.hits
        var pastHidden = 0
        var onlyPastInTutti = false
        if section.type == "event" {
            let upcoming = hits.filter { isUpcomingEvent($0) }
            let hidingPast = vm.selectedType == nil || !showPastEvents
            if hidingPast {
                pastHidden = hits.count - upcoming.count
                hits = upcoming
                if vm.selectedType == nil && hits.isEmpty && pastHidden > 0 {
                    onlyPastInTutti = true
                }
            }
        }
        // Step 2: preview cap — only in "Tutti".
        let slice = previewSlice(hits, sectionType: section.type)
        return VisibleHitsSnapshot(
            visible: slice.visible,
            hasMorePreview: slice.hasMorePreview,
            totalAfterEventFilter: hits.count,
            pastEventsHidden: pastHidden,
            onlyPastInTutti: onlyPastInTutti
        )
    }

    /// Sections rendered as floating cards (transparent List background +
    /// hidden separator). The "Mostra tutti" footer needs to match that
    /// shape — a normal inset-grouped row would float visually disconnected
    /// from the cards above.
    private func isCardStyleSection(_ type: String) -> Bool {
        switch type {
        case "event", "quid_issue", "quid_article": return true
        default: return false
        }
    }

    /// Slice helper without the event-specific filtering — used by the
    /// people section that does its own role-holder sort first.
    private struct SliceSnapshot {
        let visible: [SearchViewModel.HydratedHit]
        let hasMorePreview: Bool
    }

    private func previewSlice(
        _ hits: [SearchViewModel.HydratedHit],
        sectionType: String
    ) -> SliceSnapshot {
        let isAllChip = vm.selectedType == nil
        if isAllChip && hits.count > previewLimit {
            return SliceSnapshot(visible: Array(hits.prefix(previewLimit)), hasMorePreview: true)
        }
        return SliceSnapshot(visible: hits, hasMorePreview: false)
    }

    private var nowSeconds: Int64 { Int64(Date().timeIntervalSince1970) }

    private func isUpcomingEvent(_ hit: SearchViewModel.HydratedHit) -> Bool {
        if case .event(let e) = hit.payload {
            return e.whenEnd.epochSeconds >= nowSeconds
        }
        return true
    }

    // MARK: - "Mostra tutto" / "Mostra eventi passati" rows

    /// Footer-style row that switches the chip filter to the section's type.
    /// Used for sections that already live inside the default inset-grouped
    /// card (users / deals / sigs / documents / boutique / addon / org) —
    /// renders as an inline row and shares the section's rounded card.
    @ViewBuilder
    private func listShowAllRow(type: String, total: Int) -> some View {
        Button {
            vm.pickType(type)
        } label: {
            HStack(spacing: 8) {
                Text(tr("app.search.show_all", fallback: "Mostra tutti"))
                    .font(.subheadline.weight(.semibold))
                Text("(\(total))")
                    .font(.subheadline.monospacedDigit())
                    .foregroundStyle(.secondary)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
            }
            .contentShape(Rectangle())
            .padding(.vertical, 2)
        }
        .buttonStyle(.plain)
        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
    }

    /// Card-style footer used for sections whose tiles are floating cards
    /// (events / quid issues / quid articles). Mimics the shape of those
    /// tiles — rounded glass-effect surface, full-width tappable — so the
    /// "Mostra tutti" sits visually inside the same column of cards.
    @ViewBuilder
    private func cardShowAllRow(type: String, total: Int) -> some View {
        Button {
            vm.pickType(type)
        } label: {
            HStack(spacing: 8) {
                Text(tr("app.search.show_all", fallback: "Mostra tutti"))
                    .font(.subheadline.weight(.semibold))
                Text("(\(total))")
                    .font(.subheadline.monospacedDigit())
                    .foregroundStyle(.secondary)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            }
            .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity)
            .glassEffect(.regular, in: .rect(cornerRadius: 16))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .listRowInsets(EdgeInsets(top: 6, leading: 12, bottom: 6, trailing: 12))
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
    }

    /// Special card-row shown in the "Tutti" overview when the Eventi
    /// section matched only past events. Tap → switch to the Eventi chip
    /// AND opt into past events so the user sees them immediately.
    @ViewBuilder
    private func showPastEventsCardRow(hidden: Int) -> some View {
        Button {
            showPastEvents = true
            vm.pickType("event")
        } label: {
            HStack(spacing: 10) {
                Image(systemName: "clock.arrow.circlepath")
                    .font(.subheadline.weight(.semibold))
                VStack(alignment: .leading, spacing: 2) {
                    Text(tr("app.search.events.show_past", fallback: "Mostra eventi passati"))
                        .font(.subheadline.weight(.semibold))
                    Text(hidden == 1
                         ? tr("app.search.events.past_count_one", fallback: "1 evento")
                         : String.localizedStringWithFormat(
                            tr("app.search.events.past_count_other", fallback: "%lld eventi"), hidden))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            }
            .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity)
            .glassEffect(.regular, in: .rect(cornerRadius: 16))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .listRowInsets(EdgeInsets(top: 6, leading: 12, bottom: 6, trailing: 12))
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
    }

    /// Toggle row only shown in the Eventi chip. Hidden in the "Tutti" overview
    /// because past events are filtered out unconditionally there.
    @ViewBuilder
    private func togglePastEventsRow(hidden: Int, showing: Bool) -> some View {
        Button {
            withAnimation(.easeInOut(duration: 0.2)) { showPastEvents.toggle() }
        } label: {
            HStack(spacing: 8) {
                Image(systemName: showing ? "eye.slash" : "clock.arrow.circlepath")
                    .font(.subheadline.weight(.semibold))
                Text(showing
                     ? tr("app.search.events.hide_past", fallback: "Nascondi eventi passati")
                     : tr("app.search.events.show_past", fallback: "Mostra eventi passati"))
                    .font(.subheadline.weight(.semibold))
                if !showing && hidden > 0 {
                    Text("(\(hidden))")
                        .font(.subheadline.monospacedDigit())
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }
            .contentShape(Rectangle())
            .padding(.vertical, 2)
        }
        .buttonStyle(.plain)
        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
    }

    private func isRoleHolder(_ hit: SearchViewModel.HydratedHit) -> Bool {
        if case .user(_, let role, _, _) = hit.payload, let r = role, !r.isEmpty {
            return true
        }
        return false
    }

    private func sectionHeader(
        _ section: SearchViewModel.HydratedSection,
        displayedCount: Int
    ) -> some View {
        HStack {
            Text(headerLabel(for: section.type))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.primary)
                .textCase(nil)
            Spacer()
            Text("\(displayedCount)")
                .font(.subheadline.monospacedDigit())
                .foregroundStyle(.secondary)
        }
    }

    @ViewBuilder
    private func row(for hit: SearchViewModel.HydratedHit, type: String) -> some View {
        switch hit.payload {
        case .user(let member, let role, let group, let affiliations):
            // Single uniform row for every person, role-holder or not.
            // The org role + group (when present), plus any local-office
            // affiliations (segretario / cosegretario / assistente al test
            // pulled from the dedicated search hit types), are surfaced as
            // small chips inside the row.
            NavigationLink(value: RegSociRoute(id: member.id)) {
                PersonSearchResultRow(
                    member: member, role: role, group: group,
                    localOfficeAffiliations: affiliations
                )
            }

        case .event(let event):
            // EventRowCard is a tall hero card; the disclosure chevron looks
            // silly next to it. Hide it with the ZStack + EmptyView-label
            // NavigationLink trick — the link sits underneath the card with
            // no detectable label, so List doesn't render an accessory.
            ZStack {
                EventRowCard(event: event)
                NavigationLink(value: EventRoute(id: event.id)) {
                    EmptyView()
                }
                .opacity(0)
            }
            .listRowInsets(EdgeInsets(top: 6, leading: 12, bottom: 6, trailing: 12))
            .listRowBackground(Color.clear)
            // Le card si autoseparano visivamente: la riga sottile di List
            // tra due card "fluttuanti" diventa una linea fuori contesto.
            .listRowSeparator(.hidden)

        case .deal(let deal):
            // Same approach as `BoutiqueSearchResultRow`: a compact HStack-only
            // row works cleanly inside the search `List`, while the full
            // `DealCardView` (with `.glassEffect`) is reserved for `DealListView`
            // (which lives in a `LazyVStack`, where the card hit-tests fine).
            NavigationLink(value: DealRoute(id: deal.id)) {
                DealSearchResultRow(deal: deal)
            }

        case .sig(let sig):
            NavigationLink(value: SigRoute(id: sig.id)) {
                SigSearchResultRow(sig: sig)
            }

        case .document(let doc):
            NavigationLink(value: DocumentRoute(id: doc.id)) {
                DocumentRow(doc: doc, dateString: documentDateString(doc))
            }

        case .boutique(let product):
            NavigationLink(value: BoutiqueProductRoute(productId: product.id)) {
                BoutiqueSearchResultRow(product: product)
            }

        case .quidIssue(let issue):
            // Riusa la stessa card che si vede nell'hub dei numeri.
            // Niente disclosure chevron sulla card → trucco ZStack come per gli eventi.
            // Se l'issue è un numero PDF (`pdfUrl != nil`), il tap risolve via
            // `QuidPDFDeepLinkLoader` invece di aprire `QuidIssueView`.
            ZStack {
                QuidIssueCard(issue: issue)
                if issue.pdfUrl != nil {
                    NavigationLink(value: QuidPDFDeepLinkRoute(recordId: -issue.id)) {
                        EmptyView()
                    }
                    .opacity(0)
                } else {
                    NavigationLink(value: QuidIssueRoute(issueId: issue.id, issueName: issue.name)) {
                        EmptyView()
                    }
                    .opacity(0)
                }
            }
            .listRowInsets(EdgeInsets(top: 6, leading: 12, bottom: 6, trailing: 12))
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)

        case .quidArticle(let article):
            // Riusa la card dell'articolo (la stessa della lista del numero).
            ZStack {
                QuidArticleCard(article: article)
                NavigationLink(value: QuidArticleRoute(articleId: article.id)) {
                    EmptyView()
                }
                .opacity(0)
            }
            .listRowInsets(EdgeInsets(top: 6, leading: 12, bottom: 6, trailing: 12))
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)

        case .linktreeLink(let slug, let linkTitle, let linkSubtitle, let imageURL, _):
            // Lean row — un link del linktree di un gruppo locale. Tap apre la
            // pagina del gruppo (`LocalOfficeView`). Layout compatto in stile
            // BoutiqueSearchResultRow.
            NavigationLink(value: LocalOfficeSlugRoute(slug: slug)) {
                LinktreeLinkSearchResultRow(
                    title: linkTitle,
                    subtitle: linkSubtitle,
                    imageURL: imageURL
                )
            }

        case .orgGroup(let group):
            // Tap → OrgChartView con il filtro pre-impostato sul titolo del
            // gruppo (la search bar interna farà il resto). HIG-style row:
            // leading capsule con icona "building.2.fill" in brand tint,
            // titolo localizzato, sottotitolo con conta membri.
            NavigationLink(value: OrgGroupRoute(groupTitle: group.title)) {
                OrgGroupSearchResultRow(group: group)
            }

        case .orgRole(let role, let groupTitle, _, let member):
            // Tap → scheda socio: la carica è "chi" prima ancora che "ruolo",
            // e la scheda socio è la destination naturale (e già esistente).
            NavigationLink(value: RegSociRoute(id: member.userId)) {
                OrgRoleSearchResultRow(role: role, groupTitle: groupTitle, member: member)
            }

        case .addon, .lean:
            // Per il tipo "user" non vogliamo mai cadere nella riga "lean"
            // generica: l'utente si aspetta foto + tap → scheda socio anche
            // se il cache locale non ha ancora il record. SearchViewModel
            // intanto scarica il record completo in background, ma fino
            // all'arrivo della prossima emissione mostriamo una versione
            // light tappabile con i campi forniti dal backend.
            if type == "user" {
                NavigationLink(value: RegSociRoute(id: hit.id)) {
                    LeanPersonSearchResultRow(
                        id: hit.id,
                        name: hit.leanTitle,
                        subtitle: hit.leanSubtitle,
                        imageFilename: hit.leanImage
                    )
                }
            } else {
                LeanSearchResultRow(
                    title: hit.leanTitle,
                    subtitle: hit.leanSubtitle,
                    icon: iconFor(type: type)
                )
            }
        }
    }

    // MARK: - Helpers

    private func headerLabel(for type: String) -> String {
        switch type {
        case "user":     return tr("app.search.filter.people", fallback: "Persone") // i18n
        case "event":    return tr("views.events.title", fallback: "Eventi") // i18n
        case "deal":     return tr("app.search.filter.deals", fallback: "Deals") // i18n
        case "sig":      return tr("app.discover.groups", fallback: "Gruppi e interessi") // i18n
        case "document": return tr("addons.documents.title", fallback: "Documenti") // i18n
        case "boutique": return tr("addons.boutique.title", fallback: "Boutique") // i18n
        case "addon":    return tr("addons.title", fallback: "Addon") // i18n
        case "quid_issue":   return tr("app.search.header.quid_issues", fallback: "Numeri Quid") // i18n
        case "quid_article": return tr("app.search.header.quid_articles", fallback: "Articoli Quid") // i18n
        case "linktree_link": return tr("app.search.header.linktree_link", fallback: "Gruppi locali") // i18n
        case "org":      return tr("app.search.header.org", fallback: "Organigramma") // i18n
        default:         return type.capitalized
        }
    }

    private func iconFor(type: String) -> String {
        switch type {
        case "user":     return "person"
        case "event":    return "calendar"
        case "deal":     return "tag"
        case "sig":      return "person.3"
        case "document": return "doc.text"
        case "boutique": return "bag"
        case "addon":    return "puzzlepiece.extension"
        case "quid_issue":   return "books.vertical"
        case "quid_article": return "newspaper"
        case "linktree_link": return "building.2"
        case "org":      return "rectangle.connected.to.line.below"
        default:         return "magnifyingglass"
        }
    }

    private func documentDateString(_ doc: DocumentModel) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.dateStyle = .medium
        return f.string(from: Date(timeIntervalSince1970: Double(doc.created.epochSeconds)))
    }
}

// MARK: - Lean fallback row

/// Generic search-result row used when we don't have a canonical view for
/// a type (currently: `addon`) or when the local cache hasn't resolved a hit.
struct LeanSearchResultRow: View {
    let title: String
    let subtitle: String
    let icon: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                .frame(width: 36, height: 36)
                .background(
                    AppTheme.Colors.brandPrimary.opacity(0.12),
                    in: RoundedRectangle(cornerRadius: 10, style: .continuous)
                )

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(2)
                if !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }

            Spacer(minLength: 0)
        }
        .padding(.vertical, 2)
        .contentShape(Rectangle())
    }
}

// MARK: - Lean person row (cache-miss fallback)

/// Versione "light" della person row usata quando il backend ci dà un id
/// utente che il `members_registry` locale non ha ancora cached. Mostra
/// foto (se filename presente), nome e sottotitolo backend; è tappabile
/// e porta direttamente alla `MemberDetailView`. Mentre l'utente la guarda,
/// `SearchViewModel` sta già scaricando il record completo: la prossima
/// emissione del flow swappa questa riga con la `PersonSearchResultRow`
/// piena (con ring brand-tint per i ruoli, ecc.).
struct LeanPersonSearchResultRow: View {
    let id: String
    let name: String
    let subtitle: String
    let imageFilename: String

    var body: some View {
        HStack(spacing: 12) {
            avatar.frame(width: 40, height: 40)

            VStack(alignment: .leading, spacing: 3) {
                Text(name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(1)
                if !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }

            Spacer(minLength: 0)
        }
        .padding(.vertical, 2)
        .contentShape(Rectangle())
    }

    @ViewBuilder
    private var avatar: some View {
        if !imageFilename.isEmpty,
           let url = Files.url(collection: "members_registry", recordId: id, filename: imageFilename) {
            CachedAsyncImage(url: url) { img in
                img.resizable().scaledToFill()
            } placeholder: {
                avatarFallback
            }
            .clipShape(Circle())
        } else {
            avatarFallback
        }
    }

    private var avatarFallback: some View {
        ZStack {
            Circle().fill(AppTheme.Colors.brandPrimary.opacity(0.12))
            Text(initials)
                .font(.caption.weight(.bold))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
        }
    }

    private var initials: String {
        let parts = name.split(separator: " ").prefix(2)
        return parts.map { $0.first.map(String.init) ?? "" }.joined().uppercased()
    }
}

// MARK: - Org chart rows

/// Row di ricerca per un GRUPPO dell'organigramma (es. "Consiglio
/// Direttivo"). Apple HIG: layout uniforme alle altre row (leading
/// icon-badge in brand tint, titolo, sottotitolo informativo), disclosure
/// chevron implicito dal `NavigationLink` parent.
struct OrgGroupSearchResultRow: View {
    let group: OrgChartGroup

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "building.2.fill")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                .frame(width: 36, height: 36)
                .background(
                    AppTheme.Colors.brandPrimary.opacity(0.12),
                    in: RoundedRectangle(cornerRadius: 10, style: .continuous)
                )

            VStack(alignment: .leading, spacing: 2) {
                Text(localizedGroupTitle(group.title))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(2)
                Text(memberCountSubtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer(minLength: 0)
        }
        .padding(.vertical, 2)
        .contentShape(Rectangle())
    }

    private var memberCountSubtitle: String {
        let active = group.members.filter { !$0.inactive }.count
        // i18n: lasciamo un fallback con singolare/plurale italiano semplice;
        // Tolgee ICU non è ancora wirato per questa app.
        let key = active == 1 ? "app.search.org.group.member_one" : "app.search.org.group.member_other"
        let fallback = active == 1 ? "1 membro" : "\(active) membri"
        return tr(key, fallback: fallback, ["count": "\(active)"])
    }

    private func localizedGroupTitle(_ raw: String) -> String {
        let pretty = raw
            .replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: "-", with: " ")
            .capitalized
        return tr(raw, fallback: pretty)
    }
}

/// Row di ricerca per una CARICA (es. "Presidente — Consiglio Direttivo").
/// Apple HIG: avatar circolare del socio a sinistra (stessa grammatica
/// delle people row), titolo = ruolo, sottotitolo = nome socio · gruppo.
/// Master role (es. Presidente) ottiene un piccolo accento brand.
struct OrgRoleSearchResultRow: View {
    let role: String
    let groupTitle: String
    let member: OrgChartMember

    var body: some View {
        HStack(spacing: 12) {
            avatar
                .frame(width: 36, height: 36)

            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(role)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.primary)
                        .lineLimit(1)
                    if member.isMaster {
                        Image(systemName: "star.fill")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                    }
                }
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer(minLength: 0)
        }
        .padding(.vertical, 2)
        .contentShape(Rectangle())
    }

    private var subtitle: String {
        let name = member.name.trimmingCharacters(in: .whitespaces)
        if name.isEmpty { return groupTitle }
        return "\(name) · \(groupTitle)"
    }

    @ViewBuilder
    private var avatar: some View {
        if !member.image.isEmpty,
           let url = Files.url(collection: "members_registry", recordId: member.userId, filename: member.image) {
            CachedAsyncImage(url: url) { img in
                img.resizable().scaledToFill()
            } placeholder: {
                avatarFallback
            }
            .clipShape(Circle())
        } else {
            avatarFallback
        }
    }

    private var avatarFallback: some View {
        ZStack {
            Circle().fill(AppTheme.Colors.brandPrimary.opacity(0.12))
            Text(initials)
                .font(.caption2.weight(.bold))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
        }
    }

    private var initials: String {
        let parts = member.name.split(separator: " ").prefix(2)
        return parts.map { $0.first.map(String.init) ?? "" }.joined().uppercased()
    }
}

// MARK: - Linktree link row

/// Compact row for a `linktree_link` search hit (a single link inside a local
/// office's linktree). Title = the link label (e.g. "Instagram"), subtitle =
/// office name (e.g. "Lombardia"), leading image = office cover.
struct LinktreeLinkSearchResultRow: View {
    let title: String
    let subtitle: String
    let imageURL: String   // absolute https URL from the backend hit

    private var url: URL? { URL(string: imageURL) }

    var body: some View {
        HStack(spacing: 12) {
            CachedAsyncImage(url: url) { img in
                img.resizable().scaledToFill()
            } placeholder: {
                placeholder
            }
            .frame(width: 56, height: 56)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(2)
                if !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }

            Spacer(minLength: 0)
        }
        .padding(.vertical, 2)
        .contentShape(Rectangle())
    }

    private var placeholder: some View {
        ZStack {
            AppTheme.brandGradient
            Image(systemName: "building.2.fill")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(.white.opacity(0.9))
        }
    }
}

#Preview {
    NavigationStack {
        SearchView()
    }
}
