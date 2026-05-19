import SwiftUI
import Shared

@MainActor
@Observable
final class TicketDetailViewModel {
    var ticket: TicketModel?
    var loading = true
    var error: String?

    func load(id: String) async {
        loading = true
        defer { loading = false }
        do {
            ticket = try await koin.tickets.getById(id: id)
        } catch {
            self.error = error.localizedDescription
        }
    }
}

struct TicketDetailView: View {
    let ticketId: String
    @State private var vm = TicketDetailViewModel()

    var body: some View {
        Group {
            if vm.loading && vm.ticket == nil {
                ProgressView()
            } else if let t = vm.ticket {
                content(t)
            } else if let err = vm.error {
                ContentUnavailableView(err, systemImage: "exclamationmark.triangle")
            } else {
                ContentUnavailableView(
                    tr("tickets.not_found", fallback: "Ticket non trovato"),
                    systemImage: "ticket"
                )
            }
        }
        .navigationTitle(tr("tickets.detail.title", fallback: "Ticket"))
        .navigationBarTitleDisplayMode(.inline)
        .task { await vm.load(id: ticketId) }
    }

    @ViewBuilder
    private func content(_ t: TicketModel) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(t.name ?? tr("tickets.no_name", fallback: "Ticket"))
                        .font(.largeTitle.bold())
                    HStack(spacing: 6) {
                        Circle()
                            .fill(t.statusComputed.badgeColor)
                            .frame(width: 8, height: 8)
                        Text(tr(t.statusComputed.labelKey, fallback: t.statusComputed.fallback))
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(t.statusComputed.badgeColor)
                    }
                }

                if let d = t.description_, !d.isEmpty {
                    Text(d).font(.body)
                }

                if let qr = t.qr, !qr.isEmpty {
                    VStack(spacing: 10) {
                        Text(tr("tickets.qr_label", fallback: "Mostra all'ingresso").uppercased())
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.secondary)
                            .tracking(0.5)
                        QRCodeView(payload: qr, size: 220)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(20)
                    .glassEffect(.regular, in: .rect(cornerRadius: 20))
                }

                VStack(alignment: .leading, spacing: 12) {
                    if let dl = t.deadline {
                        infoRow(
                            icon: "calendar",
                            label: tr("tickets.deadline", fallback: "Scadenza"),
                            value: formatItalianDate(dl)
                        )
                    }
                    if let ref = t.internalRefId, !ref.isEmpty {
                        infoRow(icon: "number", label: tr("tickets.ref", fallback: "Riferimento"), value: ref)
                    }
                    infoRow(icon: "clock", label: tr("tickets.created", fallback: "Creato"), value: formatItalianDate(t.created))
                }
                .padding(16)
                .glassEffect(.regular, in: .rect(cornerRadius: 16))

                if let link = t.link, let url = URL(string: link) {
                    Link(destination: url) {
                        Label(tr("tickets.go_event", fallback: "Vai all'evento"), systemImage: "arrow.up.right.square")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppTheme.Colors.brandPrimary)
                }
            }
            .padding(20)
        }
    }

    private func infoRow(icon: String, label: String, value: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .frame(width: 24)
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            VStack(alignment: .leading, spacing: 2) {
                Text(label).font(.caption).foregroundStyle(.secondary)
                Text(value).font(.subheadline)
            }
            Spacer()
        }
    }
}
