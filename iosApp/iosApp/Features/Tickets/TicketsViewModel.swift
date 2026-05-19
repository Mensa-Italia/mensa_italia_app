import SwiftUI
import Shared

enum TicketStatus {
    case pending, completed, failed, unknown

    var badgeColor: Color {
        switch self {
        case .pending: return .orange
        case .completed: return .green
        case .failed: return .red
        case .unknown: return .gray
        }
    }

    var labelKey: String {
        switch self {
        case .pending: return "tickets.status.pending"
        case .completed: return "tickets.status.completed"
        case .failed: return "tickets.status.failed"
        case .unknown: return "tickets.status.unknown"
        }
    }

    var fallback: String {
        switch self {
        case .pending: return "In attesa"
        case .completed: return "Completato"
        case .failed: return "Fallito"
        case .unknown: return "-"
        }
    }
}

extension TicketModel {
    var statusComputed: TicketStatus {
        if qr == nil || qr?.isEmpty == true {
            if let d = deadline, d.epochSeconds < Int64(Date().timeIntervalSince1970) {
                return .failed
            }
            return .pending
        }
        return .completed
    }
}

@MainActor
@Observable
final class TicketsListViewModel {
    var tickets: [TicketModel] = []
    var loading = true
    var error: String?

    private var sub: Closeable?

    func load() async {
        loading = true
        defer { loading = false }
        sub?.close()
        let flow = koin.tickets.observeAll() as Kotlinx_coroutines_coreFlow
        sub = subscribeFlow(flow) { [weak self] (list: NSArray) in
            Task { @MainActor [weak self] in
                self?.tickets = (list as? [TicketModel]) ?? []
            }
        }
        await refresh()
    }

    func refresh() async {
        do { try await koin.tickets.refresh() } catch { self.error = error.localizedDescription }
    }
}
