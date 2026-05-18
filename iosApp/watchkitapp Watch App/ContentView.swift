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
            QRCodeView(pngData: card.qrPng)
                .frame(width: 110, height: 110)
                .padding(6)
                .background(Color.white)
                .cornerRadius(8)

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

// MARK: - QR

/// Renderizza il PNG del QR generato lato iOS. CoreImage non risolve come
/// modulo sul Watch SDK in questa toolchain, quindi il QR arriva
/// pre-renderizzato nel payload (vedi `WatchPayloadWriter` lato iosApp).
struct QRCodeView: View {
    let pngData: Data?

    var body: some View {
        if let pngData,
           let provider = CGDataProvider(data: pngData as CFData),
           let cg = CGImage(
                pngDataProviderSource: provider,
                decode: nil,
                shouldInterpolate: false,
                intent: .defaultIntent
           ) {
            Image(decorative: cg, scale: 1, orientation: .up)
                .interpolation(.none)
                .resizable()
                .scaledToFit()
        } else {
            ZStack {
                Color.white
                Image(systemName: "qrcode")
                    .font(.system(size: 48))
                    .foregroundStyle(.black)
            }
        }
    }
}
