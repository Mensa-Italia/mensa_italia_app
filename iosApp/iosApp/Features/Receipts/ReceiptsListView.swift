import SwiftUI
import Shared

struct ReceiptsListView: View {
    @State private var vm = ReceiptsListViewModel()

    private let dateFmt: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.dateStyle = .medium
        return f
    }()

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(.systemBackground),
                    AppTheme.Colors.brandPrimary.opacity(0.04),
                    Color(.systemBackground)
                ],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()

            if vm.loading && vm.receipts.isEmpty {
                ProgressView().scaleEffect(1.3)
            } else if vm.receipts.isEmpty {
                ContentUnavailableView(
                    tr("receipts.empty.title", fallback: "Nessuna ricevuta"),
                    systemImage: "doc.text",
                    description: Text(tr("receipts.empty.body", fallback: "Le tue ricevute appariranno qui"))
                )
            } else {
                ScrollView(.vertical, showsIndicators: false) {
                    LazyVStack(spacing: 12) {
                        ForEach(Array(vm.receipts.enumerated()), id: \.element.id) { idx, r in
                            NavigationLink(destination: ReceiptDetailView(receiptId: r.id)) {
                                ReceiptRow(receipt: r, dateString: dateFmt.string(from:
                                    Date(timeIntervalSince1970: Double(r.created.epochSeconds))))
                            }
                            .buttonStyle(.plain)
                            .transition(.opacity.combined(with: .move(edge: .top)))
                            .animation(
                                .easeOut(duration: 0.4).delay(Double(idx) * 0.07),
                                value: vm.receipts.count
                            )
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                }
                .refreshable { await vm.refresh() }
            }
        }
        .navigationTitle(tr("receipts.title", fallback: "Le mie ricevute"))
        .navigationBarTitleDisplayMode(.large)
        .cleanNavBar()
        .task { await vm.load() }
    }
}

struct ReceiptRow: View {
    let receipt: ReceiptModel
    let dateString: String

    var body: some View {
        HStack(alignment: .center, spacing: 14) {
            ZStack {
                Circle()
                    .fill(AppTheme.Colors.brandPrimary.opacity(0.15))
                    .frame(width: 44, height: 44)
                Image(systemName: receipt.kind.icon)
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(tr(receipt.kind.labelKey, fallback: receipt.kind.fallback))
                    .font(.subheadline.weight(.semibold))
                if let desc = receipt.description_, !desc.isEmpty {
                    Text(desc).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                }
                HStack(spacing: 8) {
                    Text(dateString).font(.caption2).foregroundStyle(.tertiary)
                    Circle().fill(receipt.statusColor).frame(width: 6, height: 6)
                    Text(receipt.status.capitalized)
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(receipt.statusColor)
                }
            }
            Spacer()
            Text(receipt.amountFormatted)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(.primary)
        }
        .padding(14)
        .glassEffect(.regular, in: .rect(cornerRadius: 16))
    }
}

#Preview {
    NavigationStack { ReceiptsListView() }
}
