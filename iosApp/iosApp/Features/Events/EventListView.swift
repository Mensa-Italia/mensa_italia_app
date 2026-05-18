import SwiftUI
import Shared

@MainActor @Observable
final class EventListViewModel {
    var events: [EventModel] = []
    var loading = false
    var refreshing = false
    var error: String? = nil
    var query: String = ""

    /// Active filter state, set by the host view. Kept as a value to avoid
    /// observation churn — the host owns the `@AppStorage` truth.
    var filter: EventFilterState = EventFilterState()

    private var sub: Closeable?

    func start() {
        guard sub == nil else { return }
        let flow = koin.events.observeAll() as Kotlinx_coroutines_coreFlow
        sub = subscribeFlow(flow) { [weak self] (list: NSArray) in
            Task { @MainActor [weak self] in
                self?.events = (list as? [EventModel]) ?? []
                self?.loading = false
            }
        }
        // Initial DB snapshot is delivered by the flow immediately; refresh remote in background.
        Task { await self.refresh(showSpinner: false) }
    }

    func stop() {
        sub?.close(); sub = nil
    }

    func refresh(showSpinner: Bool = true) async {
        if showSpinner { refreshing = true }
        defer { refreshing = false }
        do {
            try await koin.events.refresh(filter: nil, sort: "when_end")
        } catch {
            self.error = (error as NSError).localizedDescription
        }
    }

    // MARK: - Derived

    private var nowSeconds: Int64 { Int64(Date().timeIntervalSince1970) }

    private func matchesQuery(_ e: EventModel) -> Bool {
        guard !query.trimmingCharacters(in: .whitespaces).isEmpty else { return true }
        let q = query.lowercased()
        if e.name.lowercased().contains(q) { return true }
        if e.description_.lowercased().contains(q) { return true }
        if let pos = e.position {
            if pos.name.lowercased().contains(q) { return true }
            if pos.address.lowercased().contains(q) { return true }
        }
        return false
    }

    private func passesAll(_ e: EventModel) -> Bool {
        EventFilterHelpers.matches(event: e, state: filter) && matchesQuery(e)
    }

    var upcoming: [EventModel] {
        events
            .filter { $0.whenEnd.epochSeconds >= nowSeconds }
            .filter { passesAll($0) }
            .sorted { $0.whenStart.epochSeconds < $1.whenStart.epochSeconds }
    }

    var past: [EventModel] {
        events
            .filter { $0.whenEnd.epochSeconds < nowSeconds }
            .filter { passesAll($0) }
            .sorted { $0.whenStart.epochSeconds > $1.whenStart.epochSeconds }
    }
}

struct EventListView: View {
    @State private var vm = EventListViewModel()
    @State private var showCalendar = false
    @State private var showMap = false
    @State private var showAdd = false

    /// Letto sincrono dall'auth state alla costruzione della view. I powers
    /// cambiano solo a login/logout — in entrambi i casi la EventListView
    /// viene smontata, quindi un @State + flow subscription è inutile
    /// (e introduceva snap visivo all'arrivo della prima emit).
    private var canAddEvent: Bool {
        let user = koin.auth.currentUser.value as? UserModel
        let powers = user?.powers ?? []
        return powers.contains("super") || powers.contains("canAddEvent")
    }

    // Filter sheet state. Persisted as JSON so it survives app launches.
    @State private var showFilters = false
    @State private var filterState = EventFilterState()
    @AppStorage("events.filter.json") private var filterStorage: String = ""

    var body: some View {
        content
            .overlay {
                if vm.events.isEmpty && vm.refreshing {
                    LoadingDots()
                } else if vm.events.isEmpty {
                    ContentUnavailableView(
                        tr("events.empty.title", fallback: "Nessun evento"),
                        systemImage: "calendar.badge.exclamationmark",
                        description: Text(tr("events.empty.description", fallback: "Non ci sono eventi disponibili al momento."))
                    )
                } else if vm.upcoming.isEmpty && vm.past.isEmpty {
                    ContentUnavailableView(
                        tr("events.search.empty", fallback: "Nessun risultato"),
                        systemImage: "magnifyingglass",
                        description: Text(tr("events.search.empty.description", fallback: "Prova a cambiare filtro o termine di ricerca."))
                    )
                }
            }
            .navigationTitle(tr("events.list.title", fallback: "Eventi"))
            .navigationBarTitleDisplayMode(.large)
            .cleanNavBar()
            .searchable(
                text: $vm.query,
                placement: .navigationBarDrawer(displayMode: .automatic),
                prompt: Text(tr("events.search.placeholder", fallback: "Cerca eventi"))
            )
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button {
                        showFilters = true
                    } label: {
                        Image(systemName: filterState.isEmpty
                              ? "line.3.horizontal.decrease.circle"
                              : "line.3.horizontal.decrease.circle.fill")
                    }
                    .accessibilityLabel(tr("events.filter.button", fallback: "Filtri"))
                    Button { showMap = true } label: { Image(systemName: "map") }
                    Button { showCalendar = true } label: { Image(systemName: "calendar") }
                    if canAddEvent {
                        Button { showAdd = true } label: { Image(systemName: "plus") }
                    }
                }
            }
        .navigationDestination(for: String.self) { id in
            EventDetailView(eventId: id)
        }
        .sheet(isPresented: $showCalendar) {
            NavigationStack { EventCalendarView() }
        }
        .sheet(isPresented: $showMap) {
            NavigationStack { EventMapView() }
        }
        .sheet(isPresented: $showAdd) {
            NavigationStack { AddEventView() }
        }
        .sheet(isPresented: $showFilters) {
            EventFiltersSheet(state: $filterState)
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
        }
        .task {
            loadFilterFromStorage()
            vm.filter = filterState
            vm.start()
        }
        .onChange(of: filterState) { _, newValue in
            vm.filter = newValue
            saveFilterToStorage(newValue)
        }
        .onDisappear {
            vm.stop()
        }
        .alert(tr("app.error.title", fallback: "Errore"), isPresented: Binding(
            get: { vm.error != nil },
            set: { if !$0 { vm.error = nil } }
        )) {
            Button("OK") { vm.error = nil }
        } message: { Text(vm.error ?? "") }
    }

    // MARK: - Persistence

    private func loadFilterFromStorage() {
        guard !filterStorage.isEmpty,
              let data = filterStorage.data(using: .utf8),
              let decoded = try? JSONDecoder().decode(EventFilterState.self, from: data)
        else { return }
        filterState = decoded
    }

    private func saveFilterToStorage(_ state: EventFilterState) {
        if let data = try? JSONEncoder().encode(state),
           let json = String(data: data, encoding: .utf8) {
            filterStorage = json
        }
    }

    @ViewBuilder
    private var content: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if !vm.upcoming.isEmpty {
                    sectionHeader(tr("events.section.upcoming", fallback: "Imminenti"),
                                  count: vm.upcoming.count)
                    LazyVStack(spacing: 12) {
                        ForEach(Array(vm.upcoming.enumerated()), id: \.element.id) { idx, ev in
                            NavigationLink(value: ev.id) {
                                EventRowCard(event: ev)
                            }
                            .buttonStyle(.plain)
                            .padding(.horizontal)
                            .transition(.opacity)
                            .modifier(StaggerAppear(index: idx))
                        }
                    }
                }

                if !vm.past.isEmpty {
                    sectionHeader(tr("events.section.past", fallback: "Passati"),
                                  count: vm.past.count)
                    LazyVStack(spacing: 12) {
                        ForEach(Array(vm.past.enumerated()), id: \.element.id) { idx, ev in
                            NavigationLink(value: ev.id) {
                                EventRowCard(event: ev)
                                    .opacity(0.7)
                            }
                            .buttonStyle(.plain)
                            .padding(.horizontal)
                            .modifier(StaggerAppear(index: idx))
                        }
                    }
                }

                Color.clear.frame(height: 24)
            }
            .padding(.top, 4)
        }
        .refreshable { await vm.refresh() }
    }

    private func sectionHeader(_ title: String, count: Int) -> some View {
        HStack {
            Text(title).font(.title3.bold())
            Text("\(count)")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
                .padding(.horizontal, 8).padding(.vertical, 2)
                .background(.thinMaterial, in: Capsule())
            Spacer()
        }
        .padding(.horizontal)
        .padding(.top, 6)
    }
}

/// Subtle, fast scroll-aware appear. Avoids long staggered delays that leave
/// the screen visually empty while the user scrolls fast. Cards fade & slide
/// in over ~250ms with a tiny per-batch offset (capped at 3 indices).
struct StaggerAppear: ViewModifier {
    let index: Int
    @State private var appeared = false

    func body(content: Content) -> some View {
        content
            .opacity(appeared ? 1 : 0)
            .offset(y: appeared ? 0 : 8)
            .onAppear {
                let delay = Double(min(index, 3)) * 0.03
                withAnimation(.easeOut(duration: 0.25).delay(delay)) {
                    appeared = true
                }
            }
    }
}

#Preview {
    NavigationStack { EventListView() }
}
