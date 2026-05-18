import Foundation
import Shared

/// Swift-facing wrapper that delegates to the shared KMP `QuidDateParser`.
/// Bridges the returned Kotlin `Instant` (epoch millis) to a Swift `Date`.
enum QuidDateParser {
    static func parse(_ raw: String) -> Date? {
        guard let instant = Shared.QuidDateParser.shared.parse(raw: raw) else { return nil }
        return Date(timeIntervalSince1970: Double(instant.toEpochMilliseconds()) / 1000.0)
    }
}
