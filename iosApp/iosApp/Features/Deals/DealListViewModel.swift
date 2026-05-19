import SwiftUI
import Shared

@MainActor
@Observable
final class DealListViewModel {
    /// Raw, unfiltered list straight from the cache flow.
    var allDeals: [DealModel] = []

    /// True only when we have nothing on screen yet AND a refresh is in flight.
    /// Cache-first: if `allDeals` is non-empty we never show a spinner.
    var refreshing = false
    var error: String?

    /// Active sector chip (`nil` = "all").
    var selectedSector: String?
    var searchText: String = ""

    /// Current authenticated user — letto sincrono dall'auth (session-stable,
    /// cambia solo a login/logout e in quel caso la view viene smontata).
    var currentUser: UserModel? {
        koin.auth.currentUser.value as? UserModel
    }

    private var dealsSub: Closeable?

    // MARK: - Lifecycle

    func start() {
        dealsSub?.close()
        dealsSub = FlowBridgeKt.subscribe(
            flow: koin.deals.observeAll(),
            onEach: { [weak self] value in
                Task { @MainActor in
                    self?.allDeals = (value as? [DealModel]) ?? []
                }
            },
            onError: { _ in }
        )

        Task { await refresh() }
    }

    func stop() {
        dealsSub?.close()
        dealsSub = nil
    }

    func refresh() async {
        refreshing = true
        defer { refreshing = false }
        do {
            try await koin.deals.refresh(filter: nil, sort: "created")
        } catch {
            self.error = (error as NSError).localizedDescription
        }
    }

    // MARK: - Derived

    /// All distinct, non-empty sectors present in the dataset, alphabetically.
    var sectors: [String] {
        let set = Set(allDeals.map { $0.commercialSector }.filter { !$0.isEmpty })
        return set.sorted()
    }

    /// Filtered by sector + search.
    var filteredDeals: [DealModel] {
        let trimmedQuery = searchText
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
        return allDeals.filter { d in
            if let sector = selectedSector, d.commercialSector != sector { return false }
            guard !trimmedQuery.isEmpty else { return true }
            if d.name.lowercased().contains(trimmedQuery) { return true }
            if let det = d.details?.lowercased(), det.contains(trimmedQuery) { return true }
            if d.commercialSector.lowercased().contains(trimmedQuery) { return true }
            return false
        }
    }

    /// Empty-state copy depends on whether we filtered ourselves into a corner
    /// or really have no data.
    var isFilterActive: Bool {
        selectedSector != nil ||
            !searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var canAddDeal: Bool {
        guard let user = currentUser else { return false }
        // Flutter app uses 'super' / 'deals_admin' / 'admin' interchangeably.
        let allowed: Set<String> = ["super", "admin", "deals_admin"]
        return !user.powers.filter { allowed.contains($0) }.isEmpty
    }
}
