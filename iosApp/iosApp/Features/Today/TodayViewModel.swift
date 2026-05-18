import SwiftUI
import CoreLocation
import Shared

@MainActor
@Observable
final class TodayViewModel {
    /// `loading` is intentionally `false` at construction. RootViewModel
    /// pre-warms the SQLDelight cache during the splash, so by the time
    /// TodayView appears the first flow emission is already in-flight (or
    /// about to land within a frame). Showing a spinner for that gap
    /// produced an ugly flash; rendering the empty layout for one frame and
    /// then snapping to populated content is far less jarring. The flag
    /// remains in case we want to re-introduce a spinner for a slow path.
    var loading = false
    var nextEvent: EventModel? = nil
    var notifications: [NotificationModel] = []
    var user: UserModel? = nil

    /// Snapshot eventi (chronological, ascending start) — cached così
    /// quando la posizione utente arriva possiamo ricalcolare `nextEvent`
    /// senza rifare la network call.
    private var upcoming: [EventModel] = []

    var userName: String {
        guard let u = user else { return "Socio" }
        return u.name.isEmpty == false ? u.name : u.username
    }

    var userAvatar: URL? {
        guard let u = user, !u.avatar.isEmpty else { return nil }
        return Files.url(collection: "users", recordId: u.id, filename: u.avatar)
    }

    var membershipExpiry: String {
        guard let u = user else { return "-" }
        return formatItalianDate(u.expireMembership)
    }

    var memberSince: String { "-" }

    private var eventsSub: Closeable?
    private var notifSub: Closeable?
    private var userSub: Closeable?
    /// Last user location captured. Re-applied to upcoming events whenever the
    /// cache flow re-emits, so a fresh refresh respects geo-proximity without
    /// a second location request.
    private var lastUserLocation: CLLocation?

    /// Cache-first bootstrap.
    ///
    /// 1. Subscribe to the local SQLDelight Flows (`observeAll()`) so the UI
    ///    paints with the last-known data **instantly** — zero network wait.
    ///    Works fully offline: if you've ever opened Today before, you'll see
    ///    those events again with no spinner.
    /// 2. Kick off remote refresh in the background. When the network round-trip
    ///    completes, the repository upserts into the DB and the Flow re-emits,
    ///    so the UI updates seamlessly. If we're offline the refresh fails
    ///    silently and the user keeps the cached data.
    /// 3. `loading` flips to `false` as soon as the first cache emission lands
    ///    (typical: <50ms) OR when the background refresh terminates — whichever
    ///    comes first. So "first launch ever, empty DB, online" still works:
    ///    the empty Flow emits immediately, refresh fills the DB, the Flow
    ///    re-emits with real events, no infinite spinner.
    func load() async {
        // 1. Cache subscriptions — emit immediately with whatever the DB holds,
        //    so the UI is interactive from frame one.
        subscribeEventsFromCache()
        subscribeNotificationsFromCache()
        startUserSubscription()

        // 2. Best-effort location lookup (non-blocking).
        Task { [weak self] in
            guard let loc = await LocationProvider.shared.requestOnce() else { return }
            await MainActor.run { [weak self] in
                self?.lastUserLocation = loc
                self?.recomputeNextEvent(userLocation: loc)
            }
        }

        // 3. Background refresh — fire-and-forget. UI doesn't wait. Offline →
        //    silent failure, cached data stays on screen.
        Task.detached(priority: .userInitiated) { [weak self] in
            async let _ev: () = { try? await koin.events.refresh(filter: nil, sort: "when_end") }()
            async let _no: () = { try? await koin.notifications.refresh() }()
            _ = await (_ev, _no)
            // Safety net: clear the spinner once the round-trip is done even
            // if the Flow somehow never emitted (shouldn't happen, but).
            await MainActor.run { self?.loading = false }
        }
    }

    /// Manual pull-to-refresh hook. Re-fetches the network and lets the Flow
    /// propagate the new data back. Awaitable so SwiftUI's `.refreshable`
    /// shows the spinner correctly.
    func refresh() async {
        async let _ev: () = { try? await koin.events.refresh(filter: nil, sort: "when_end") }()
        async let _no: () = { try? await koin.notifications.refresh() }()
        _ = await (_ev, _no)
    }

    private func subscribeEventsFromCache() {
        eventsSub?.close()
        let flow = koin.events.observeAll() as Kotlinx_coroutines_coreFlow
        eventsSub = subscribeFlow(flow) { [weak self] (list: NSArray) in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let events = (list as? [EventModel]) ?? []
                let nowSeconds = Int64(Date().timeIntervalSince1970)
                self.upcoming = events
                    .filter { $0.whenStart.epochSeconds >= nowSeconds }
                    .sorted { $0.whenStart.epochSeconds < $1.whenStart.epochSeconds }
                // Default chronological pick — replaced by geo-nearest below
                // if we already have a user location.
                self.nextEvent = self.upcoming.first ?? events.first
                if let loc = self.lastUserLocation {
                    self.recomputeNextEvent(userLocation: loc)
                }
                // First cache emission unblocks the UI even if the network
                // refresh is still in flight.
                self.loading = false
            }
        }
    }

    private func subscribeNotificationsFromCache() {
        notifSub?.close()
        let notifFlow = koin.notifications.observeAll() as Kotlinx_coroutines_coreFlow
        notifSub = subscribeFlow(notifFlow) { [weak self] (list: NSArray) in
            Task { @MainActor [weak self] in
                self?.notifications = (list as? [NotificationModel]) ?? []
            }
        }
    }

    func startUserSubscription() {
        userSub?.close()
        let flow = koin.auth.currentUser as Kotlinx_coroutines_coreFlow
        userSub = subscribeOptionalFlow(flow) { [weak self] (u: UserModel?) in
            Task { @MainActor in self?.user = u }
        } onError: { _ in }
    }

    /// Sceglie tra gli eventi upcoming quello geograficamente più vicino
    /// all'utente. Limita la ricerca a un orizzonte temporale ragionevole
    /// (90 giorni) — altrimenti un evento a 6 mesi vicino "vince" su uno
    /// domani in un'altra regione, e non è quello che vogliamo. Se nessun
    /// evento upcoming ha una posizione, lascia il fallback chronologico.
    private func recomputeNextEvent(userLocation me: CLLocation) {
        let horizonSeconds = Int64(Date().timeIntervalSince1970) + 90 * 86_400
        let candidates = upcoming
            .filter { $0.whenStart.epochSeconds <= horizonSeconds }
            .compactMap { event -> (EventModel, Double)? in
                guard let pos = event.position else { return nil }
                let loc = CLLocation(latitude: pos.lat, longitude: pos.lon)
                return (event, me.distance(from: loc))
            }
        guard let closest = candidates.min(by: { $0.1 < $1.1 })?.0 else { return }
        self.nextEvent = closest
    }

    /// Time-of-day greeting. Localised via Tolgee with Italian fallbacks.
    var greeting: String {
        let h = Calendar.current.component(.hour, from: Date())
        switch h {
        case 5..<12:  return tr("today.greeting.morning",   fallback: "Buongiorno")
        case 12..<18: return tr("today.greeting.afternoon", fallback: "Buon pomeriggio")
        default:      return tr("today.greeting.evening",   fallback: "Buonasera")
        }
    }

    nonisolated func cancelSubscription() {
        // notifSub is stored but closing must happen from the call site.
        // deinit on @Observable @MainActor class runs off main actor — skip.
    }
}

// MARK: - Date formatting helper

private let italianDateFormatter: DateFormatter = {
    let f = DateFormatter()
    f.locale = Locale(identifier: "it_IT")
    f.dateFormat = "dd MMM yyyy"
    return f
}()

func formatItalianDate(_ instant: Kotlinx_datetimeInstant?) -> String {
    guard let instant = instant, instant.epochSeconds > 0 else { return "-" }
    let date = Date(timeIntervalSince1970: Double(instant.epochSeconds))
    return italianDateFormatter.string(from: date)
}
