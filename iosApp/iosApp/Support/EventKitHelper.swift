import EventKit
import Foundation

enum EventKitHelper {
    /// Adds an event to the user's default calendar.
    /// Throws if calendar access is denied or if saving fails.
    static func addEvent(
        title: String,
        notes: String?,
        location: String?,
        start: Date,
        end: Date
    ) async throws {
        let store = EKEventStore()
        let granted: Bool
        if #available(iOS 17.0, *) {
            granted = try await store.requestFullAccessToEvents()
        } else {
            granted = try await withCheckedThrowingContinuation { cont in
                store.requestAccess(to: .event) { ok, err in
                    if let err = err { cont.resume(throwing: err) }
                    else { cont.resume(returning: ok) }
                }
            }
        }
        guard granted else {
            throw NSError(
                domain: "EventKit",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: tr("calendar.error.access_denied", fallback: "Accesso al calendario negato")]
            )
        }
        let ev = EKEvent(eventStore: store)
        ev.title = title
        ev.notes = notes
        ev.location = location
        ev.startDate = start
        ev.endDate = end
        ev.calendar = store.defaultCalendarForNewEvents
        try store.save(ev, span: .thisEvent, commit: true)
    }
}
