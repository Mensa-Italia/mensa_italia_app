import os

private let subsystem = "it.mensa.app"

enum Log {
    static let app   = Logger(subsystem: subsystem, category: "app")
    static let auth  = Logger(subsystem: subsystem, category: "auth")
    static let ui    = Logger(subsystem: subsystem, category: "ui")
    static let net   = Logger(subsystem: subsystem, category: "network")
}
