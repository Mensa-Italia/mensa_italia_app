import SwiftUI
import Shared

enum ReceiptKind {
    case donation, renewal, purchase, unknown

    var labelKey: String {
        switch self {
        case .donation: return "receipts.kind.donation"
        case .renewal: return "receipts.kind.renewal"
        case .purchase: return "receipts.kind.purchase"
        case .unknown: return "receipts.kind.unknown"
        }
    }
    var fallback: String {
        switch self {
        case .donation: return "Donazione"
        case .renewal: return "Rinnovo"
        case .purchase: return "Acquisto"
        case .unknown: return "Transazione"
        }
    }
    var icon: String {
        switch self {
        case .donation: return "heart.fill"
        case .renewal: return "arrow.clockwise.circle.fill"
        case .purchase: return "bag.fill"
        case .unknown: return "creditcard.fill"
        }
    }
}

extension ReceiptModel {
    var kind: ReceiptKind {
        let d = (description ?? "").lowercased()
        if d.contains("donaz") || d.contains("donation") { return .donation }
        if d.contains("rinnov") || d.contains("renewal") || d.contains("membership") { return .renewal }
        if d.contains("acquist") || d.contains("purchase") || d.contains("boutique") { return .purchase }
        return .unknown
    }

    var statusColor: Color {
        switch status.lowercased() {
        case "completed", "paid", "success", "succeeded": return .green
        case "pending", "processing": return .orange
        case "failed", "error", "canceled": return .red
        default: return .gray
        }
    }

    var amountFormatted: String {
        let euros = Double(amount) / 100.0
        let f = NumberFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.numberStyle = .currency
        f.currencyCode = "EUR"
        return f.string(from: NSNumber(value: euros)) ?? "€\(euros)"
    }
}

@MainActor
@Observable
final class ReceiptsListViewModel {
    var receipts: [ReceiptModel] = []
    var loading = true
    var error: String? = nil

    private var sub: Closeable?

    func load() async {
        loading = true
        defer { loading = false }
        sub?.close()
        let flow = koin.receipts.observeAll() as Kotlinx_coroutines_coreFlow
        sub = subscribeFlow(flow) { [weak self] (list: NSArray) in
            Task { @MainActor [weak self] in
                self?.receipts = (list as? [ReceiptModel]) ?? []
            }
        }
        await refresh()
    }

    func refresh() async {
        do { try await koin.receipts.refresh() }
        catch { self.error = error.localizedDescription }
    }
}
