import SwiftUI
import Network
import Shared

/// Lo stato della connessione col server, per SwiftUI. Specchio di
/// `Connectivity` (:shared): una risposta da `svc.mensa.it` vuol dire online,
/// un errore di rete offline; `lastContact` è l'ultima volta che ha risposto.
@MainActor @Observable
final class ConnectivityStore {
    static let shared = ConnectivityStore()

    /// nil finché nessuna richiesta è finita (i primi istanti dopo l'avvio).
    private(set) var reachable: Bool?
    private(set) var lastContact: Date?

    @ObservationIgnored private var subs: [Closeable] = []
    @ObservationIgnored private let pathMonitor = NWPathMonitor()

    private init() {
        let connectivity = koin.connectivity
        subs.append(subscribeFlow(connectivity.reconnects as Kotlinx_coroutines_coreFlow) { (value: KotlinInt) in
            Task { @MainActor in ReconnectSignal.shared.count = value.intValue }
        })
        // La rete del telefono, dal sistema: tornata, si chiede subito al
        // server invece di aspettare il controllo periodico di `Connectivity`.
        pathMonitor.pathUpdateHandler = { path in
            connectivity.onNetworkChange(available: path.status == .satisfied)
        }
        pathMonitor.start(queue: DispatchQueue(label: "it.mensa.app.network-path"))
        subs.append(subscribeOptionalFlow(connectivity.reachable as Kotlinx_coroutines_coreFlow) { [weak self] (value: KotlinBoolean?) in
            Task { @MainActor in self?.reachable = value?.boolValue }
        })
        subs.append(subscribeOptionalFlow(connectivity.lastContact as Kotlinx_coroutines_coreFlow) { [weak self] (value: Kotlinx_datetimeInstant?) in
            Task { @MainActor in
                self?.lastContact = value.map { Date(timeIntervalSince1970: Double($0.toEpochMilliseconds()) / 1000) }
            }
        })
    }

    var isOffline: Bool { reachable == false }

    /// Data e ora dell'ultimo contatto, nella lingua dell'app.
    var lastContactText: String? {
        guard let lastContact else { return nil }
        let f = DateFormatter()
        f.locale = Locale(identifier: LocaleManager.shared.activeTag)
        f.setLocalizedDateFormatFromTemplate("ddMMMyyyyHHmm")
        return f.string(from: lastContact)
    }
}

/// Quante volte il server e' tornato a rispondere dopo un periodo offline
/// (`Connectivity.reconnects` in :shared). Tenuto fuori da `ConnectivityStore`
/// perche' lo leggono anche le immagini, che possono comparire prima che Koin
/// abbia il database: questo non tocca Koin, lo aggiorna `ConnectivityStore`.
@MainActor @Observable
final class ReconnectSignal {
    static let shared = ReconnectSignal()
    var count = 0
    private init() {}
}

extension View {
    /// `.refreshable`, e la stessa azione quando la connessione torna: la
    /// pagina aperta offline si aggiorna da sola, senza uscire e rientrare.
    func refreshableOnReconnect(_ action: @escaping @Sendable () async -> Void) -> some View {
        refreshable(action: action)
            .onChange(of: ReconnectSignal.shared.count) { _, _ in
                Task { await action() }
            }
    }
}

/// "Sei offline": i dati a schermo sono quelli dell'ultimo contatto col
/// server. Compare solo quando la rete ha davvero fallito e sparisce da sola
/// appena il server risponde. Mirrors Android `OfflineBanner`.
struct OfflineBanner: View {
    @State private var store = ConnectivityStore.shared

    var body: some View {
        if store.isOffline {
            HStack(spacing: 12) {
                Image(systemName: "icloud.slash")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.secondary)
                VStack(alignment: .leading, spacing: 2) {
                    Text(tr("app.offline.title", fallback: "Sei offline"))
                        .font(.subheadline.weight(.semibold))
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .transition(.move(edge: .top).combined(with: .opacity))
        }
    }

    private var subtitle: String {
        if let date = store.lastContactText {
            return tr("app.offline.updated_at", fallback: "Dati aggiornati al {date}", ["date": date])
        }
        return tr("app.offline.not_updated", fallback: "I dati potrebbero non essere aggiornati")
    }
}
