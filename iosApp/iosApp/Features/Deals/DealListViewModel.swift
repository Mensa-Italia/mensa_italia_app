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
    /// Active region (`nil` = every region). See `DealRegions`.
    var selectedRegion: String?
    var searchText: String = ""

    /// Current authenticated user — letto sincrono dall'auth (session-stable,
    /// cambia solo a login/logout e in quel caso la view viene smontata).
    var currentUser: UserModel? {
        koin.auth.currentUser.value as? UserModel
    }

    private var dealsSub: Closeable?

    init(selectedRegion: String? = nil) {
        self.selectedRegion = selectedRegion
    }

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
            self.error = error.refreshMessage
        }
    }

    // MARK: - Derived

    /// All distinct, non-empty sectors present in the dataset, alphabetically.
    var sectors: [String] {
        let set = Set(allDeals.map { $0.commercialSector }.filter { !$0.isEmpty })
        return set.sorted()
    }

    /// The regions that have at least one deal, plus the selected one (the
    /// list may open filtered before the deals arrive, or on a region with no
    /// deal of its own: the picker needs a row to check), alphabetically.
    var regions: [String] {
        let withDeals = DealRegions.shared.regions(deals: allDeals)
        guard let selectedRegion, !withDeals.contains(selectedRegion) else { return withDeals }
        return (withDeals + [selectedRegion]).sorted()
    }

    /// Filtered by region (its deals first, then those valid everywhere) + sector + search.
    var filteredDeals: [DealModel] {
        let trimmedQuery = searchText
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
        return DealRegions.shared.filter(deals: allDeals, regionKey: selectedRegion).filter { d in
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
        selectedSector != nil || selectedRegion != nil ||
            !searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var canAddDeal: Bool {
        guard let user = currentUser else { return false }
        return Powers.shared.canManageDeals(powers: user.powers)
    }
}
