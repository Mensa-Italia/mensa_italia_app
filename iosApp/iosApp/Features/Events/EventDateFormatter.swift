import Foundation
import Shared

/// Shared date helpers used across the Events module.
enum EventDateUtil {
    static let fullFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.dateStyle = .full
        f.timeStyle = .short
        return f
    }()

    static let mediumFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.dateStyle = .medium
        f.timeStyle = .short
        return f
    }()

    static let dayMonthFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.dateFormat = "d MMM"
        return f
    }()

    static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.timeStyle = .short
        f.dateStyle = .none
        return f
    }()

    static func date(_ instant: Kotlinx_datetimeInstant) -> Date {
        Date(timeIntervalSince1970: Double(instant.epochSeconds))
    }

    /// True quando l'evento è concluso. Preferiamo `whenEnd` (la fine reale),
    /// ma se non è popolato (alcuni record legacy hanno solo `whenStart`)
    /// fallback su `whenStart`. Una giornata di tolleranza non serve: PB
    /// mantiene whenEnd sull'orario di chiusura e il confronto con `now`
    /// è preciso al secondo.
    static func isPast(_ event: EventModel) -> Bool {
        let now = Int64(Date().timeIntervalSince1970)
        let end = event.whenEnd.epochSeconds
        if end > 0 { return end < now }
        return event.whenStart.epochSeconds < now
    }
}
