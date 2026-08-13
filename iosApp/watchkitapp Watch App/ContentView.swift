import SwiftUI

// MARK: - Root

struct RootWatchView: View {
    @State private var payload: WatchPayload? = WatchSessionMirror.readPayload()

    var body: some View {
        Group {
            if let payload, payload.card != nil || payload.nextEvent != nil {
                TabView {
                    if let card = payload.card {
                        CardWatchView(card: card)
                    }
                    if let event = payload.nextEvent {
                        NextEventWatchView(event: event)
                    }
                }
                .tabViewStyle(.verticalPage)
            } else {
                EmptyStateView()
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: UserDefaults.didChangeNotification)) { _ in
            payload = WatchSessionMirror.readPayload()
        }
    }
}

// MARK: - Card

struct CardWatchView: View {
    let card: WatchPayload.CardSnapshot

    var body: some View {
        VStack(spacing: 6) {
            Text(card.memberId)
                .font(.title3.weight(.bold).monospacedDigit())
                .minimumScaleFactor(0.6)
                .lineLimit(1)

            Text(card.fullName)
                .font(.caption2.weight(.semibold))
                .lineLimit(1)
                .minimumScaleFactor(0.7)

            if card.isActive {
                Label("Attiva", systemImage: "checkmark.seal.fill")
                    .font(.caption2)
                    .foregroundStyle(.green)
            } else {
                Label("Scaduta", systemImage: "exclamationmark.triangle.fill")
                    .font(.caption2)
                    .foregroundStyle(.orange)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Next event

struct NextEventWatchView: View {
    let event: WatchPayload.EventSnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Prossimo evento")
                .font(.caption2)
                .foregroundStyle(.secondary)

            Text(event.name)
                .font(.headline)
                .lineLimit(2)

            Text(event.startDate, format: .dateTime.day().month().hour().minute())
                .font(.caption)
                .foregroundStyle(.secondary)

            if let location = event.locationName, !location.isEmpty {
                Label(location, systemImage: "mappin.and.ellipse")
                    .font(.caption2)
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 6)
    }
}

// MARK: - Empty state

struct EmptyStateView: View {
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: "iphone")
                .font(.title2)
                .foregroundStyle(.secondary)
            Text("Accedi sull'iPhone")
                .font(.caption2)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
        }
        .padding()
    }
}

