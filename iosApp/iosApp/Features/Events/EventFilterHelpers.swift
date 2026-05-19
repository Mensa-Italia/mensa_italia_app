import Foundation
import CoreLocation
import Shared

/// Type of event for filter purposes.
///
/// `online` is inferred from `event.position == nil` — there is no explicit
/// "isOnline" field on `EventModel`. `national` and `local` come from
/// `event.isNational`.
enum EventType: String, CaseIterable, Identifiable, Codable, Hashable {
    case national, local, online
    var id: String { rawValue }

    var label: String {
        switch self {
        case .national: return tr("events.type.national", fallback: "Nazionale")
        case .local:    return tr("events.type.local", fallback: "Locale")
        case .online:   return tr("events.type.online", fallback: "Online")
        }
    }

    var systemImage: String {
        switch self {
        case .national: return "globe.europe.africa"
        case .local:    return "mappin.and.ellipse"
        case .online:   return "wifi"
        }
    }
}

/// Canonical list of Italian regions used for region chip matching.
enum ItalianRegions {
    static let all: [String] = [
        "Abruzzo", "Basilicata", "Calabria", "Campania", "Emilia-Romagna",
        "Friuli-Venezia Giulia", "Lazio", "Liguria", "Lombardia", "Marche",
        "Molise", "Piemonte", "Puglia", "Sardegna", "Sicilia", "Toscana",
        "Trentino-Alto Adige", "Umbria", "Valle d'Aosta", "Veneto"
    ]
}

/// Discrete distance steps in km. `nil` means "illimitato".
enum DistanceSteps {
    static let kmValues: [Int] = [5, 25, 50, 100, 200, 500]

    static func label(for km: Int?) -> String {
        guard let km else { return tr("events.filter.distance.unlimited", fallback: "Illimitato") }
        return "\(km) km"
    }
}

/// Persisted filter state for the Events list.
///
/// Stored as JSON in `@AppStorage` so it survives app launches. `userLocation`
/// is transient (not encoded) — it is reacquired from `CLLocationManager` on
/// each session.
struct EventFilterState: Equatable, Codable {
    var types: Set<EventType> = []
    var regions: Set<String> = []
    /// `nil` => unlimited.
    var maxDistanceKm: Int?
    var useMyLocation: Bool = false

    // Transient (not encoded): live location updates from CLLocationManager.
    var userLatitude: Double?
    var userLongitude: Double?

    enum CodingKeys: String, CodingKey {
        case types, regions, maxDistanceKm, useMyLocation
    }

    var userLocation: CLLocation? {
        guard let lat = userLatitude, let lon = userLongitude else { return nil }
        return CLLocation(latitude: lat, longitude: lon)
    }

    /// Count of active filter facets (used for badge + reset visibility).
    var activeCount: Int {
        var n = 0
        if !types.isEmpty { n += 1 }
        if !regions.isEmpty { n += 1 }
        if useMyLocation, maxDistanceKm != nil { n += 1 }
        return n
    }

    var isEmpty: Bool { activeCount == 0 }

    mutating func reset() {
        types.removeAll()
        regions.removeAll()
        maxDistanceKm = nil
        useMyLocation = false
        // keep cached coordinates so re-enabling is instant
    }
}

/// Pure filter predicate. Keep deterministic and side-effect free.
enum EventFilterHelpers {
    static func type(of event: EventModel) -> EventType {
        if event.position == nil { return .online }
        return event.isNational ? .national : .local
    }

    static func matchesType(_ event: EventModel, types: Set<EventType>) -> Bool {
        guard !types.isEmpty else { return true }
        return types.contains(type(of: event))
    }

    static func matchesRegion(_ event: EventModel, regions: Set<String>) -> Bool {
        guard !regions.isEmpty else { return true }
        guard let address = event.position?.address, !address.isEmpty else { return false }
        let lower = address.lowercased()
        return regions.contains { lower.contains($0.lowercased()) }
    }

    static func matchesDistance(_ event: EventModel,
                                maxDistanceKm: Int?,
                                useMyLocation: Bool,
                                userLocation: CLLocation?) -> Bool {
        // If user disabled location-based filter or no cap, match everything.
        guard useMyLocation, let maxKm = maxDistanceKm, let me = userLocation else { return true }
        // Online events have no position — exclude them from a distance filter.
        guard let pos = event.position else { return false }
        let event = CLLocation(latitude: pos.lat, longitude: pos.lon)
        let km = me.distance(from: event) / 1000.0
        return km <= Double(maxKm)
    }

    static func matches(event: EventModel, state: EventFilterState) -> Bool {
        matchesType(event, types: state.types)
            && matchesRegion(event, regions: state.regions)
            && matchesDistance(event,
                               maxDistanceKm: state.maxDistanceKm,
                               useMyLocation: state.useMyLocation,
                               userLocation: state.userLocation)
    }
}
