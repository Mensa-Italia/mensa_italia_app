import SwiftUI
import Shared

struct TicketsListView: View {
    @State private var vm = TicketsListViewModel()

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

            if vm.loading && vm.tickets.isEmpty {
                ProgressView().scaleEffect(1.3)
            } else if vm.tickets.isEmpty {
                ContentUnavailableView(
                    tr("tickets.empty.title", fallback: "Nessun ticket"),
                    systemImage: "ticket",
                    description: Text(tr("tickets.empty.body", fallback: "I tuoi ticket appariranno qui"))
                )
            } else {
                ScrollView(.vertical, showsIndicators: false) {
                    LazyVStack(spacing: 12) {
                        ForEach(Array(vm.tickets.enumerated()), id: \.element.id) { idx, t in
                            NavigationLink(destination: TicketDetailView(ticketId: t.id)) {
                                TicketRow(ticket: t)
                            }
                            .buttonStyle(.plain)
                            .transition(.opacity.combined(with: .move(edge: .top)))
                            .animation(
                                .easeOut(duration: 0.4).delay(Double(idx) * 0.07),
                                value: vm.tickets.count
                            )
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                }
                .refreshable { await vm.refresh() }
            }
        }
        .navigationTitle(tr("tickets.title", fallback: "I miei ticket"))
        .navigationBarTitleDisplayMode(.large)
        .cleanNavBar()
        .task { await vm.load() }
    }
}

struct TicketRow: View {
    let ticket: TicketModel

    private var status: TicketStatus { ticket.statusComputed }

    var body: some View {
        HStack(alignment: .center, spacing: 14) {
            ZStack {
                Circle()
                    .fill(AppTheme.Colors.brandPrimary.opacity(0.15))
                    .frame(width: 44, height: 44)
                Image(systemName: "ticket.fill")
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(ticket.name ?? tr("tickets.no_name", fallback: "Ticket"))
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                if let d = ticket.description_, !d.isEmpty {
                    Text(d)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                HStack(spacing: 6) {
                    Circle()
                        .fill(status.badgeColor)
                        .frame(width: 7, height: 7)
                    Text(tr(status.labelKey, fallback: status.fallback))
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(status.badgeColor)
                }
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
        .padding(14)
        .glassEffect(.regular, in: .rect(cornerRadius: 16))
    }
}

#Preview {
    NavigationStack { TicketsListView() }
}
