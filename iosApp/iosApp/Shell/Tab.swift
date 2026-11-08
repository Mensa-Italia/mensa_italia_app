import Foundation

enum AppTab: Hashable {
    case today
    case discover
    /// "Gruppo locale": shown only while `LocalGroupStore.shared.available`.
    case localGroup
    case search
    case profile
}
