import Shared
import SwiftUI

/// Board only: the association's figures. The season's totals and the
/// monthly charts over every office (the blocks of `LocalGroupStatsView`),
/// then every office's members, events and attendance, most members first.
/// Tapping an office shows it in the tab. Mirrors Android
/// `LocalGroupAssociationScreen`.
struct LocalGroupAssociationView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var store = LocalGroupStore.shared

    /// One office of the comparison: its figures and its name.
    private struct OfficeRow: Identifiable {
        let id: String
        let name: String
        let stats: LocalOfficeStatsModel
    }

    @State private var stats: LocalOfficeStatsModel?
    @State private var rows: [LocalOfficeMonthlyStatsModel] = []
    @State private var offices: [OfficeRow] = []
    @State private var loaded = false
    @State private var failed = false

    var body: some View {
        content
            .navigationTitle(tr("local_group.association.title", fallback: "Associazione"))
            .task { await load() }
    }

    @ViewBuilder
    private var content: some View {
        if !loaded {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if stats == nil && rows.isEmpty && offices.isEmpty {
            ContentUnavailableView(
                tr("local_group.association.title", fallback: "Associazione"),
                systemImage: "chart.bar",
                description: Text(failed
                                  ? tr("local_group.state.failed", fallback: "Non siamo riusciti a caricare il gruppo locale.")
                                  : tr("local_group.stats.empty", fallback: "Ancora nessun dato per questa stagione"))
            )
        } else {
            List {
                if let stats {
                    LocalGroupTotalsSection(
                        stats: stats,
                        membersLabel: tr("local_group.association.members", fallback: "Soci attivi")
                    )
                }
                LocalGroupMonthlyCharts(rows: rows)
                if !offices.isEmpty {
                    Section(tr("local_group.association.by_office", fallback: "Per gruppo locale")) {
                        ForEach(offices) { office in
                            Button {
                                Task { await store.selectOffice(office.id) }
                                dismiss()
                            } label: {
                                officeRow(office)
                            }
                        }
                    }
                }
            }
        }
    }

    /// The office's name, then members, events in season and attendance.
    private func officeRow(_ office: OfficeRow) -> some View {
        let members = tr("local_group.association.members", fallback: "Soci attivi")
        let events = tr("local_group.stats.events_season", fallback: "Eventi in stagione")
        let attendance = tr("local_group.stats.attendance_season", fallback: "Presenze in stagione")
        return HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(office.name)
                    .foregroundStyle(Color.primary)
                HStack(spacing: 14) {
                    Label("\(office.stats.membersActive)", systemImage: "person.3.fill")
                    Label("\(office.stats.eventsSeason)", systemImage: "calendar")
                    Label("\(office.stats.attendanceSeason)", systemImage: "person.crop.circle.badge.checkmark")
                }
                .font(.footnote.monospacedDigit())
                .foregroundStyle(Color.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Color(.tertiaryLabel))
        }
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(office.name))
        .accessibilityValue(Text(
            "\(members) \(office.stats.membersActive), \(events) \(office.stats.eventsSeason), \(attendance) \(office.stats.attendanceSeason)"
        ))
    }

    private func load() async {
        guard !loaded else { return }
        let repo = koin.localGroup
        // Each part on its own: a missing piece leaves the others on screen.
        var anyFailed = false
        do { stats = try await repo.loadAssociationStats() } catch { anyFailed = true }
        do { rows = try await repo.loadAssociationMonthly() } catch { anyFailed = true }
        do {
            let all = try await repo.loadAllOfficeStats()
            var names = ((try? await repo.listOffices()) ?? [])
                .reduce(into: [String: String]()) { $0[$1.id] = $1.name }
            // `listOffices()` leaves out the member's own office (the picker
            // shows it as "Il mio gruppo locale"): the store knows it; any
            // other gap is named by id.
            if let own = store.ownOffice { names[own.id] = own.name }
            for id in Set(all.map(\.localOffice)) where names[id] == nil {
                if let office = try? await koin.localOffices.officeById(id: id) {
                    names[id] = office.name
                }
            }
            offices = all
                .sorted { $0.membersActive > $1.membersActive }
                .map { OfficeRow(id: $0.localOffice, name: names[$0.localOffice] ?? $0.localOffice, stats: $0) }
        } catch { anyFailed = true }
        if Task.isCancelled { return }
        failed = anyFailed
        loaded = true
    }
}
