import SwiftUI
import Shared

@MainActor @Observable
final class MembersDirectoryViewModel {
    var members: [RegSociModel] = []
    var query: String = ""
    /// La regione scelta nel filtro, nil = tutte. Si cambia con `selectRegion`.
    private(set) var region: String?
    /// La provincia scelta nel sottofiltro, nil = tutte; solo con una regione.
    var province: String?
    var loading = false
    var error: String?

    private var sub: Closeable?
    private var searchTask: Task<Void, Never>?

    func start() {
        sub = FlowBridgeKt.subscribe(
            flow: koin.regSoci.observeAll(),
            onEach: { [weak self] value in
                let list = (value as? [RegSociModel]) ?? []
                Task { @MainActor in
                    self?.members = list
                }
            },
            onError: { _ in }
        )
        Task { await refresh() }
    }

    func stop() {
        sub?.close()
        searchTask?.cancel()
    }

    func refresh() async {
        loading = true
        defer { loading = false }
        do {
            try await koin.regSoci.refresh()
        } catch {
            self.error = error.refreshMessage
        }
    }

    /// Debounced (300ms) remote search.
    func onQueryChange(_ newValue: String) {
        query = newValue
        searchTask?.cancel()
        let trimmed = newValue.trimmingCharacters(in: .whitespaces)
        guard trimmed.count >= 2 else { return }
        searchTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 300_000_000)
            if Task.isCancelled { return }
            do {
                _ = try await koin.regSoci.searchByName(query: trimmed)
            } catch { }
        }
    }

    /// Cambiare o togliere la regione azzera la provincia, che era di quella regione.
    func selectRegion(_ newValue: String?) {
        guard newValue != region else { return }
        region = newValue
        province = nil
    }

    /// Ricerca, regione e provincia insieme, sul registro in locale: subito,
    /// mentre la ricerca sul server aspetta il debounce. Vedi `MemberDirectory.filter`.
    var filtered: [RegSociModel] {
        MemberDirectory.shared.filter(members: members, query: query, region: region, province: province)
    }

    /// Le 20 regioni del menu del filtro, coi soci gia' caricati di ognuna.
    var regions: [MemberPlace] {
        MemberDirectory.shared.regions(members: members)
    }

    /// Le province della regione scelta, fisse: ci sono prima che i soci arrivino.
    var provinces: [MemberPlace] {
        MemberDirectory.shared.provinces(members: members, region: region)
    }

    var isFiltering: Bool {
        !query.trimmingCharacters(in: .whitespaces).isEmpty || region != nil
    }
}

struct MembersDirectoryView: View {
    @State private var vm = MembersDirectoryViewModel()

    var body: some View {
        directoryList
            .overlay {
                if vm.loading && vm.members.isEmpty {
                    LoadingDots()
                } else if vm.filtered.isEmpty {
                    ContentUnavailableView(
                        vm.isFiltering
                            ? tr("members.no_results", fallback: "Nessun socio trovato")
                            : tr("members.empty", fallback: "Directory vuota"),
                        systemImage: "person.2.slash",
                        description: Text(emptyDescription)
                    )
                }
            }
            .searchable(
                text: Binding(
                    get: { vm.query },
                    set: { vm.onQueryChange($0) }
                ),
                // `.always`, non `.automatic`: con `.refreshable` sulla stessa lista il
                // pull-to-refresh muove insieme rotella e cassetto della ricerca, e su
                // iOS 26/27 a fine refresh il cassetto restava a meta': ricerca sopra il
                // titolo grande e un buco vuoto sotto.
                placement: .navigationBarDrawer(displayMode: .always),
                prompt: tr("members.search_placeholder", fallback: "Cerca un socio…")
            )
            .navigationTitle(tr("members.registry.title", fallback: "Registro Soci"))
            .modifier(PlaceSubtitle(places: [vm.region, vm.province].compactMap { $0 }))
            .toolbar {
                // Sempre, anche mentre il registro carica: regioni e province sono fisse.
                ToolbarItemGroup(placement: .topBarTrailing) {
                    if !vm.provinces.isEmpty {
                        placeMenu(
                            title: tr("members.filter.province", fallback: "Provincia"),
                            allTitle: tr("members.filter.province.all", fallback: "Tutte le province"),
                            places: vm.provinces,
                            selected: vm.province,
                            symbol: "mappin.circle",
                            select: { province in withAnimation { vm.province = province } }
                        )
                    }
                    placeMenu(
                        title: tr("members.filter.region", fallback: "Regione"),
                        allTitle: tr("members.filter.region.all", fallback: "Tutte le regioni"),
                        places: vm.regions,
                        selected: vm.region,
                        symbol: "line.3.horizontal.decrease.circle",
                        // Animato: il pulsante della provincia entra ed esce dal
                        // gruppo della toolbar, e il vetro si allarga e si
                        // stringe con lui solo dentro una transazione animata.
                        select: { region in withAnimation { vm.selectRegion(region) } }
                    )
                }
            }
            .cleanNavBar()
            .navigationDestination(for: RegSociRoute.self) { route in
                MemberDetailView(memberId: route.id)
            }
            .task { vm.start() }
            .onDisappear { vm.stop() }
            .alert(tr("app.error.title", fallback: "Errore"), isPresented: Binding(
                get: { vm.error != nil },
                set: { if !$0 { vm.error = nil } }
            )) {
                Button("OK") { vm.error = nil }
            } message: {
                Text(vm.error ?? "")
            }
    }

    private var emptyDescription: String {
        if !vm.isFiltering {
            return tr("members.empty_description", fallback: "Trascina giù per aggiornare la directory.")
        }
        if vm.region != nil {
            return tr("members.no_results_region_description", fallback: "Prova con un altro nome, regione o provincia.")
        }
        return tr("members.no_results_description", fallback: "Prova con un altro nome o città.")
    }

    // MARK: - Region and province filters

    /// Un filtro per regione o provincia: i `places`, ognuno col suo numero di
    /// soci a registro caricato. Il glifo si riempie quando uno e' scelto.
    ///
    /// Le voci sono Toggle e non un Picker perche' solo loro mostrano il
    /// sottotitolo. Scelta singola come in un Picker: la voce gia' spuntata
    /// resta scelta.
    private func placeMenu(
        title: String,
        allTitle: String,
        places: [MemberPlace],
        selected: String?,
        symbol: String,
        select: @escaping (String?) -> Void
    ) -> some View {
        Menu {
            Section {
                Toggle(isOn: Binding(get: { selected == nil }, set: { _ in select(nil) })) {
                    Text(allTitle)
                }
            }
            Section {
                ForEach(places, id: \.name) { place in
                    // Due testi: il menu li mostra come titolo e sottotitolo.
                    Toggle(isOn: Binding(get: { selected == place.name }, set: { _ in select(place.name) })) {
                        Text(place.name)
                        // Il numero solo a registro caricato: prima sarebbe 0 ovunque.
                        if !vm.members.isEmpty {
                            Text(membersCount(Int(place.count)))
                        }
                    }
                }
            }
        } label: {
            // Con una scelta il pulsante dice quale: il filtro attivo resta
            // visibile senza riaprire il menu (su iOS 26 lo dice il sottotitolo).
            if let selected {
                Label(selected, systemImage: symbol + ".fill")
                    .labelStyle(.titleAndIcon)
            } else {
                Label(title, systemImage: symbol)
            }
        }
        .accessibilityLabel(title)
        .accessibilityValue(selected ?? allTitle)
    }

    private func membersCount(_ count: Int) -> String {
        count == 1
            ? tr("members.filter.region.count_one", fallback: "1 socio")
            : String(format: tr("members.filter.region.count_other", fallback: "%d soci"), count)
    }

    // MARK: - Sectioned list (Contacts-style)

    /// Groups by first alphabetic letter; non-letters → "#". Sorted by name within each section.
    private var sectioned: [(letter: String, members: [RegSociModel])] {
        let grouped = Dictionary(grouping: vm.filtered) { m -> String in
            let trimmed = m.name.trimmingCharacters(in: .whitespaces)
            guard let first = trimmed.first else { return "#" }
            let folded = String(first).folding(options: .diacriticInsensitive, locale: .current).uppercased()
            return folded.range(of: "^[A-Z]$", options: .regularExpression) != nil ? folded : "#"
        }
        return grouped
            .map { (letter: $0.key, members: $0.value.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }) }
            .sorted { a, b in
                if a.letter == "#" { return false }
                if b.letter == "#" { return true }
                return a.letter < b.letter
            }
    }

    @ViewBuilder
    private var directoryList: some View {
        ScrollViewReader { proxy in
            ZStack(alignment: .trailing) {
                List {
                    ForEach(sectioned, id: \.letter) { section in
                        // Inline (non-sticky) letter — scrolls away with the section.
                        Text(section.letter)
                            .font(.title3.weight(.bold))
                            .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                            .listRowSeparator(.hidden)
                            .listRowInsets(EdgeInsets(top: 16, leading: 16, bottom: 2, trailing: 16))
                            .listRowBackground(Color.clear)
                            .id(section.letter)

                        ForEach(section.members, id: \.id) { member in
                            ZStack {
                                NavigationLink(value: RegSociRoute(id: member.id)) {
                                    EmptyView()
                                }
                                .opacity(0)
                                MemberCellCompact(member: member)
                            }
                            .listRowSeparator(.visible, edges: .bottom)
                            .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 22))
                            // A plain list paints every row white even with its
                            // own background hidden: let the page's show through.
                            .listRowBackground(Color.clear)
                        }
                    }
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .refreshableOnReconnect { await vm.refresh() }

                if sectioned.count > 1 {
                    AlphabetIndex(
                        letters: sectioned.map(\.letter),
                        onSelect: { letter in
                            withAnimation(.easeOut(duration: 0.16)) {
                                proxy.scrollTo(letter, anchor: .top)
                            }
                        }
                    )
                    .padding(.trailing, 4)
                }
            }
        }
    }
}

/// iPhone Contacts-style alphabet index: skinny vertical strip of plain
/// brand-colored letters, compressed (fixed line height), drag-to-scrub with
/// haptic feedback only. No glass, no preview bubble.
private struct AlphabetIndex: View {
    let letters: [String]
    let onSelect: (String) -> Void

    @State private var lastLetter: String?

    var body: some View {
        GeometryReader { geo in
            VStack(spacing: 1) {
                ForEach(letters, id: \.self) { l in
                    Text(l)
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                        .frame(height: 12)
                }
            }
            .frame(width: 14)
            .frame(maxHeight: .infinity, alignment: .center)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        handleDrag(at: value.location.y, geo: geo)
                    }
                    .onEnded { _ in lastLetter = nil }
            )
        }
        .frame(width: 14)
        .frame(maxHeight: .infinity)
        .padding(.vertical, 12)
    }

    private func handleDrag(at y: CGFloat, geo: GeometryProxy) {
        let usable = max(geo.size.height, 1)
        let clamped = min(max(y, 0), usable)
        let idx = min(letters.count - 1,
                      max(0, Int(clamped / usable * CGFloat(letters.count))))
        let l = letters[idx]
        if l != lastLetter {
            UIImpactFeedbackGenerator(style: .soft).impactOccurred(intensity: 0.55)
            lastLetter = l
            onSelect(l)
        }
    }
}

/// Regione e provincia scelte sotto il titolo, da iOS 26: li' la barra mostra
/// i pulsanti dei filtri solo come icona. Prima resta il testo dei pulsanti.
private struct PlaceSubtitle: ViewModifier {
    let places: [String]

    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content.navigationSubtitle(places.joined(separator: " · "))
        } else {
            content
        }
    }
}

/// A distinct value type so `navigationDestination(for: String.self)` defined
/// elsewhere doesn't collide with our member routes.
struct RegSociRoute: Hashable {
    let id: String
}

#Preview {
    NavigationStack {
        MembersDirectoryView()
    }
}
