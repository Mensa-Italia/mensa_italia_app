import SwiftUI
import Shared

@MainActor @Observable
final class MembersDirectoryViewModel {
    var members: [RegSociModel] = []
    var query: String = ""
    var loading = false
    var error: String? = nil

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
            self.error = (error as NSError).localizedDescription
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

    /// Client-side filter — instant feedback while debounced remote search runs.
    var filtered: [RegSociModel] {
        let trimmed = query.trimmingCharacters(in: .whitespaces).lowercased()
        guard !trimmed.isEmpty else { return members }
        return members.filter {
            $0.name.lowercased().contains(trimmed) ||
            $0.city.lowercased().contains(trimmed) ||
            $0.id.lowercased().contains(trimmed)
        }
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
                        vm.query.isEmpty
                            ? tr("members.empty", fallback: "Directory vuota")
                            : tr("members.no_results", fallback: "Nessun socio trovato"),
                        systemImage: "person.2.slash",
                        description: Text(vm.query.isEmpty
                            ? tr("members.empty_description", fallback: "Trascina giù per aggiornare la directory.")
                            : tr("members.no_results_description", fallback: "Prova con un altro nome o città."))
                    )
                }
            }
            .searchable(
                text: Binding(
                    get: { vm.query },
                    set: { vm.onQueryChange($0) }
                ),
                placement: .navigationBarDrawer(displayMode: .automatic),
                prompt: tr("members.search_placeholder", fallback: "Cerca un socio…")
            )
            .navigationTitle(tr("members.registry.title", fallback: "Registro Soci"))
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
                        }
                    }
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .refreshable { await vm.refresh() }

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

    @State private var lastLetter: String? = nil

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
