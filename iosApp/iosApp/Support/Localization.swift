import Foundation
import Shared

/// Global translation function. Read by SwiftUI Views.
/// Thread-safe: I18n.t is a pure lookup on a snapshot.
///
/// Usage:
///   tr("app.login.title")                          → "Bentornato in Mensa" (or key if missing)
///   tr("app.login.title", fallback: "Bentornato")  → fallback when key missing
///   tr("greeting", fallback: "Ciao {name}", ["name": "Matteo"]) → "Ciao Matteo"
func tr(_ key: String, fallback: String? = nil, _ args: [String: String] = [:]) -> String {
    koin.i18n.t(key: key, fallback: fallback, args: args)
}
