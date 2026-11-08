import Shared
import SwiftUI

/// The office's figures: the season's totals from the snapshot, then one
/// Swift Charts chart per measure, month by month from the start of the
/// previous season (13 to 24 months). Both from `LocalGroupStatsBlocks.swift`,
/// shared with the association's page. Mirrors Android `LocalGroupStatsScreen`.
struct LocalGroupStatsView: View {
    let snapshot: LocalGroupSnapshot

    @State private var rows: [LocalOfficeMonthlyStatsModel] = []
    @State private var loaded = false
    @State private var failed = false

    var body: some View {
        content
            .navigationTitle(tr("local_group.admin.stats", fallback: "Statistiche"))
            .task { await load() }
    }

    @ViewBuilder
    private var content: some View {
        if !loaded {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if rows.isEmpty && snapshot.stats == nil {
            ContentUnavailableView(
                tr("local_group.admin.stats", fallback: "Statistiche"),
                systemImage: "chart.bar",
                description: Text(failed
                                  ? tr("local_group.state.failed", fallback: "Non siamo riusciti a caricare il gruppo locale.")
                                  : tr("local_group.stats.empty", fallback: "Ancora nessun dato per questa stagione"))
            )
        } else {
            List {
                if let stats = snapshot.stats {
                    LocalGroupTotalsSection(stats: stats)
                }
                LocalGroupMonthlyCharts(rows: rows)
            }
        }
    }

    private func load() async {
        guard !loaded else { return }
        do {
            rows = try await koin.localGroup.loadMonthly(officeId: snapshot.office.id)
            failed = false
        } catch {
            if error is CancellationError { return }
            failed = true
        }
        loaded = true
    }
}
