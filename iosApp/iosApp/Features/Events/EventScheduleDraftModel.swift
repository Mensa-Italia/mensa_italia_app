import Foundation

/// Swift-side mutable schedule draft. Mirrors `it.mensa.shared.repository.ScheduleDraft`
/// from KMP (which the integrator will use when calling `koin.events.create/update`).
///
/// `id` semantics — match the Flutter convention:
///   - nil           → new schedule, will be CREATEd on save
///   - "DELETE:xxx"  → existing schedule xxx, marked for DELETE on save
///   - any other     → existing schedule, will be UPDATEd on save
struct EventScheduleDraftSwift: Identifiable, Hashable {
    var id: String? = nil       // PB id (or nil/DELETE: marker)
    var stableId: UUID = UUID() // for SwiftUI ForEach diffing
    var title: String = ""
    var description: String = ""
    var infoLink: String = ""
    var whenStart: Date = Date()
    var whenEnd: Date = Date()
    var maxExternalGuests: Int = 0
    var price: Double = 0
    var isSubscriptable: Bool = false

    var isDeleted: Bool { id?.hasPrefix("DELETE:") == true }
}
