import WidgetKit
import SwiftUI

// MARK: - Timeline entry

struct NextEventEntry: TimelineEntry {
    let date: Date
    let event: WatchPayload.EventSnapshot?

    static let placeholder = NextEventEntry(
        date: Date(),
        event: WatchPayload.EventSnapshot(
            id: "preview",
            name: "Raduno Mensa",
            startDate: Date().addingTimeInterval(3600),
            endDate: nil,
            locationName: "Roma",
            isNational: false
        )
    )
}

// MARK: - Provider

struct NextEventProvider: TimelineProvider {
    func placeholder(in context: Context) -> NextEventEntry {
        .placeholder
    }

    func getSnapshot(in context: Context, completion: @escaping (NextEventEntry) -> Void) {
        completion(currentEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<NextEventEntry>) -> Void) {
        let entry = currentEntry()
        // Refresh ogni 15 minuti — il payload viene riscritto dall'iOS app
        // quando Today aggiorna; WidgetCenter.reloadAllTimelines() lo forza
        // più rapidamente quando necessario.
        let next = Date().addingTimeInterval(15 * 60)
        completion(Timeline(entries: [entry], policy: .after(next)))
    }

    private func currentEntry() -> NextEventEntry {
        // Il widget gira nel target watchOS Widget Extension: legge dal
        // medesimo `UserDefaults.standard` del Watch app, dove
        // `WatchSessionMirror` deposita il payload ricevuto via WCSession.
        let payload: WatchPayload? = {
            guard let data = UserDefaults.standard.data(forKey: "watch_payload_v1") else { return nil }
            let d = JSONDecoder()
            d.dateDecodingStrategy = .iso8601
            return try? d.decode(WatchPayload.self, from: data)
        }()
        return NextEventEntry(date: Date(), event: payload?.nextEvent)
    }
}

// MARK: - Views

struct NextEventEntryView: View {
    var entry: NextEventEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        switch family {
        case .accessoryCircular:
            CircularView(event: entry.event)
        case .accessoryInline:
            InlineView(event: entry.event)
        case .accessoryRectangular:
            RectangularView(event: entry.event)
        default:
            RectangularView(event: entry.event)
        }
    }
}

private struct RectangularView: View {
    let event: WatchPayload.EventSnapshot?

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            if let event {
                Text("MENSA")
                    .font(.caption2).foregroundStyle(.secondary)
                Text(event.name)
                    .font(.headline)
                    .lineLimit(2)
                Text(event.startDate, format: .dateTime.day().month().hour().minute())
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            } else {
                Text("Mensa")
                    .font(.caption2).foregroundStyle(.secondary)
                Text("Nessun evento")
                    .font(.headline)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct CircularView: View {
    let event: WatchPayload.EventSnapshot?

    var body: some View {
        VStack(spacing: 0) {
            if let event {
                Text(event.startDate, format: .dateTime.day())
                    .font(.system(size: 22, weight: .semibold))
                Text(event.startDate, format: .dateTime.month(.abbreviated))
                    .font(.system(size: 10))
                    .foregroundStyle(.secondary)
            } else {
                Image(systemName: "calendar.badge.exclamationmark")
                    .font(.title3)
            }
        }
    }
}

private struct InlineView: View {
    let event: WatchPayload.EventSnapshot?

    var body: some View {
        if let event {
            Text("Mensa · \(event.name)")
        } else {
            Text("Mensa: nessun evento")
        }
    }
}

// MARK: - Widget

struct NextEventComplication: Widget {
    let kind: String = "NextEventComplication"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: NextEventProvider()) { entry in
            NextEventEntryView(entry: entry)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Prossimo evento")
        .description("Il prossimo evento Mensa direttamente sul quadrante.")
        .supportedFamilies([.accessoryRectangular, .accessoryCircular, .accessoryInline])
    }
}

#Preview(as: .accessoryRectangular) {
    NextEventComplication()
} timeline: {
    NextEventEntry.placeholder
    NextEventEntry(date: Date(), event: nil)
}
