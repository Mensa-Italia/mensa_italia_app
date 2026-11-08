import Charts
import Shared
import SwiftUI

/// The office's figures: the season's totals from the snapshot, then one
/// Swift Charts chart per measure, month by month from the start of the
/// previous season (13 to 24 months). Mirrors Android
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
    /// "Da ottobre 2025": the first month the charts cover (the start of the
    /// previous season), empty when the rows have no readable month.
    @State private var since = ""
    /// The rows cover two calendar years: labels carry the year.
    @State private var spansYears = false
    @State private var loaded = false
    @State private var failed = false

    /// "ott": the axis label when every month is in the same year.
    private static let monthFormatter = italianFormatter("MMM")
    /// "ott 25": the axis label when the rows span two years (the server sends
    /// the previous season too), so October 2025 and October 2026 stay apart.
    private static let monthYearFormatter = italianFormatter("MMM yy")
    /// "ottobre 2025": the first month, for the charts' range.
    private static let sinceFormatter = italianFormatter("LLLL yyyy")

    private static func italianFormatter(_ format: String) -> DateFormatter {
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.dateFormat = format
        return f
    }

    /// Up to 24 bars: label one month in two, three or four ("ott 25" needs
    /// about twice the room of "ott"), so the labels are never truncated.
    private var axisLabels: Set<String> {
        let maxLabels = spansYears ? 6 : 12
        let step = max(1, Int((Double(months.count) / Double(maxLabels)).rounded(.up)))
        return Set(months.enumerated().compactMap { $0.offset % step == 0 ? $0.element.label : nil })
    }

    /// The month axis: a grid line and a label only on the labelled months.
    private var monthAxis: some AxisContent {
        let shown = axisLabels
        return AxisMarks { value in
            if let label = value.as(String.self), shown.contains(label) {
                AxisGridLine()
                AxisTick()
                AxisValueLabel(collisionResolution: .disabled)
            }
        }
    }

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
        chartSection(tr("local_group.stats.events", fallback: "Eventi per mese")) {
            Chart(months) { m in
                BarMark(x: .value("Mese", m.label), y: .value("Eventi", m.events))
            }
            .chartXAxis { monthAxis }
            .frame(height: 180)
            .padding(.vertical, 8)
        }
        chartSection(tr("local_group.stats.attendance", fallback: "Presenze per mese")) {
            Chart(months) { m in
                BarMark(x: .value("Mese", m.label), y: .value("Presenze", m.attendance))
                    .foregroundStyle(.orange)
            }
            .chartXAxis { monthAxis }
            .frame(height: 180)
            .padding(.vertical, 8)
        }
        chartSection(tr("local_group.stats.members", fallback: "Soci attivi")) {
            Chart(months) { m in
                LineMark(x: .value("Mese", m.label), y: .value("Soci", m.active))
                PointMark(x: .value("Mese", m.label), y: .value("Soci", m.active))
            }
            .chartYScale(domain: .automatic(includesZero: false))
            .chartXAxis { monthAxis }
            .foregroundStyle(.green)
            .frame(height: 180)
            .padding(.vertical, 8)
        }
        chartSection(tr("local_group.stats.flows", fallback: "Entrati e usciti")) {
            Chart(flows) { f in
                BarMark(x: .value("Mese", f.month), y: .value("Soci", f.value))
                    .foregroundStyle(by: .value("Movimento", f.series))
                    .position(by: .value("Movimento", f.series))
            }
            .chartForegroundStyleScale([
                tr("local_group.members.arrived", fallback: "Entrati"): Color.blue,
                tr("local_group.members.left", fallback: "Usciti"): Color.red,
            ])
            .chartXAxis { monthAxis }
            .chartLegend(position: .bottom)
            .frame(height: 200)
            .padding(.vertical, 8)
        }
    }

    /// A chart under its title, with the range it covers on top of the row
    /// (as Health does), so the two seasons read at a glance.
    private func chartSection<Content: View>(_ title: String, @ViewBuilder chart: () -> Content) -> some View {
        Section(title) {
            VStack(alignment: .leading, spacing: 4) {
                if !since.isEmpty {
                    Text(since)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                chart()
            }
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
            let years = Set(rows.map { Int($0.year) }.filter { $0 > 0 })
            spansYears = years.count > 1
            let formatter = spansYears ? Self.monthYearFormatter : Self.monthFormatter
            months = rows.map { row in
                Month(id: row.month,
                      label: Self.date(year: Int(row.year), month: Int(row.monthNumber)).map(formatter.string(from:)) ?? row.month,
                      events: Int(row.events), attendance: Int(row.attendance), active: Int(row.membersActive),
                      arrived: Int(row.membersNew), left: Int(row.membersLeft))
            }
            since = rows.first
                .flatMap { Self.date(year: Int($0.year), month: Int($0.monthNumber)) }
                .map { tr("local_group.stats.since", fallback: "Da {month}", ["month": Self.sinceFormatter.string(from: $0)]) }
                ?? ""
            failed = false
        } catch {
            if error is CancellationError { return }
            failed = true
        }
        loaded = true
    }

    /// The first day of the row's month; nil when the row's month is unreadable.
    private static func date(year: Int, month: Int) -> Date? {
        guard year > 0, (1...12).contains(month) else { return nil }
        return Calendar(identifier: .gregorian).date(from: DateComponents(year: year, month: month, day: 1))
    }
}
