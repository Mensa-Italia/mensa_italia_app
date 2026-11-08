import Charts
import Shared
import SwiftUI

/// The office's figures: the season's totals from the snapshot, then one
/// Swift Charts chart per measure, month by month. Mirrors Android
/// `LocalGroupStatsScreen`.
struct LocalGroupStatsView: View {
    let snapshot: LocalGroupSnapshot

    /// One month as the charts read it.
    private struct Month: Identifiable {
        let id: String
        let label: String
        let events: Int
        let attendance: Int
        let active: Int
        let arrived: Int
        let left: Int
    }

    /// One bar of the arrived/left chart.
    private struct Flow: Identifiable {
        let id: String
        let month: String
        let series: String
        let value: Int
    }

    @State private var months: [Month] = []
    @State private var loaded = false
    @State private var failed = false

    private static let monthFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.dateFormat = "MMM"
        return f
    }()

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
        } else if months.isEmpty && snapshot.stats == nil {
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
                    Section(stats.season) {
                        LabeledContent(tr("local_group.stats.members_active", fallback: "Iscritti in regione"), value: "\(stats.membersActive)")
                        LabeledContent(tr("local_group.stats.new_30", fallback: "Nuovi (30 giorni)"), value: "\(stats.membersNew30d)")
                        LabeledContent(tr("local_group.stats.left_30", fallback: "Usciti (30 giorni)"), value: "\(stats.membersLeft30d)")
                        LabeledContent(tr("local_group.stats.events_season", fallback: "Eventi in stagione"), value: "\(stats.eventsSeason)")
                        LabeledContent(tr("local_group.stats.attendance_season", fallback: "Presenze in stagione"), value: "\(stats.attendanceSeason)")
                        LabeledContent(tr("local_group.stats.deals", fallback: "Convenzioni attive"), value: "\(stats.dealsActive)")
                    }
                }
                if months.isEmpty {
                    Section {
                        Text(tr("local_group.stats.empty", fallback: "Ancora nessun dato per questa stagione"))
                            .foregroundStyle(.secondary)
                    }
                } else {
                    charts
                }
            }
        }
    }

    @ViewBuilder
    private var charts: some View {
        Section(tr("local_group.stats.events", fallback: "Eventi per mese")) {
            Chart(months) { m in
                BarMark(x: .value("Mese", m.label), y: .value("Eventi", m.events))
            }
            .frame(height: 180)
            .padding(.vertical, 8)
        }
        Section(tr("local_group.stats.attendance", fallback: "Presenze per mese")) {
            Chart(months) { m in
                BarMark(x: .value("Mese", m.label), y: .value("Presenze", m.attendance))
                    .foregroundStyle(.orange)
            }
            .frame(height: 180)
            .padding(.vertical, 8)
        }
        Section(tr("local_group.stats.members", fallback: "Soci attivi")) {
            Chart(months) { m in
                LineMark(x: .value("Mese", m.label), y: .value("Soci", m.active))
                PointMark(x: .value("Mese", m.label), y: .value("Soci", m.active))
            }
            .chartYScale(domain: .automatic(includesZero: false))
            .foregroundStyle(.green)
            .frame(height: 180)
            .padding(.vertical, 8)
        }
        Section(tr("local_group.stats.flows", fallback: "Entrati e usciti")) {
            Chart(flows) { f in
                BarMark(x: .value("Mese", f.month), y: .value("Soci", f.value))
                    .foregroundStyle(by: .value("Movimento", f.series))
                    .position(by: .value("Movimento", f.series))
            }
            .chartForegroundStyleScale([
                tr("local_group.members.arrived", fallback: "Entrati"): Color.blue,
                tr("local_group.members.left", fallback: "Usciti"): Color.red,
            ])
            .chartLegend(position: .bottom)
            .frame(height: 200)
            .padding(.vertical, 8)
        }
    }

    private var flows: [Flow] {
        let arrived = tr("local_group.members.arrived", fallback: "Entrati")
        let left = tr("local_group.members.left", fallback: "Usciti")
        return months.flatMap { m in
            [Flow(id: m.id + "-in", month: m.label, series: arrived, value: m.arrived),
             Flow(id: m.id + "-out", month: m.label, series: left, value: m.left)]
        }
    }

    private func load() async {
        guard !loaded else { return }
        do {
            let rows = try await koin.localGroup.loadMonthly(officeId: snapshot.office.id)
            months = rows.map { row in
                Month(id: row.month, label: Self.label(year: Int(row.year), month: Int(row.monthNumber), raw: row.month),
                      events: Int(row.events), attendance: Int(row.attendance), active: Int(row.membersActive),
                      arrived: Int(row.membersNew), left: Int(row.membersLeft))
            }
            failed = false
        } catch {
            if error is CancellationError { return }
            failed = true
        }
        loaded = true
    }

    /// "ott", "nov": the Italian short month; the raw text when unreadable.
    private static func label(year: Int, month: Int, raw: String) -> String {
        guard year > 0, (1...12).contains(month),
              let date = Calendar(identifier: .gregorian).date(from: DateComponents(year: year, month: month, day: 1))
        else { return raw }
        return monthFormatter.string(from: date)
    }
}
