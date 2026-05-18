import SwiftUI
import Shared

@MainActor
@Observable
final class DealDetailViewModel {
    var deal: DealModel? = nil
    var contacts: [DealsContactModel] = []
    var loading = false
    var loadingContacts = false
    var error: String? = nil
    var deleting = false

    private var listSub: Closeable?

    func start(id: String) {
        // Subscribe to the cached list flow so the detail view updates
        // automatically when a background `refresh()` brings in a fresh copy
        // of this specific deal.
        listSub?.close()
        listSub = FlowBridgeKt.subscribe(
            flow: koin.deals.observeAll(),
            onEach: { [weak self] value in
                Task { @MainActor in
                    guard let list = value as? [DealModel] else { return }
                    if let match = list.first(where: { $0.id == id }) {
                        self?.deal = match
                    }
                }
            },
            onError: { _ in }
        )

        Task { await loadIfNeeded(id: id) }
        Task { await loadContacts(id: id) }
    }

    func stop() {
        listSub?.close()
        listSub = nil
    }

    /// Deletes the current deal. Mirrors Android (delete in edit form) and
    /// web (`Mensa.deals.delete` in EditDealForm). Returns true on success
    /// so the view can dismiss / navigate back.
    func delete() async -> Bool {
        guard let id = deal?.id else { return false }
        deleting = true
        defer { deleting = false }
        do {
            try await koin.deals.delete(id: id)
            return true
        } catch {
            self.error = (error as NSError).localizedDescription
            return false
        }
    }

    private func loadIfNeeded(id: String) async {
        guard deal == nil else { return }
        loading = true
        defer { loading = false }
        do {
            // 1. Fast path: legge dalla cache SQLDelight.
            if let cached = try await koin.deals.getById(id: id) {
                deal = cached
                return
            }
            // 2. Cache miss (cold-launch via deep-link / push): rifresca
            //    la lista dal network, poi ritenta il lookup. Senza
            //    questa retry l'utente vede LoadingDots all'infinito.
            // K/N non bridge i default args di Kotlin: passa esplicito.
            try await koin.deals.refresh(filter: nil, sort: "created")
            if let fresh = try await koin.deals.getById(id: id) {
                deal = fresh
            } else {
                self.error = tr(
                    "addons.deals.detail.not_found",
                    fallback: "Convenzione non trovata."
                )
            }
        } catch {
            self.error = (error as NSError).localizedDescription
        }
    }

    func loadContacts(id: String) async {
        loadingContacts = true
        defer { loadingContacts = false }
        do {
            let list = try await koin.deals.contacts(dealId: id)
            self.contacts = list
        } catch {
            self.contacts = []
        }
    }
}
