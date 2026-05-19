import SwiftUI
import Shared

/// User preference: app color scheme.
enum ThemeChoice: String, CaseIterable, Identifiable {
    case system = "Sistema"
    case light = "Chiaro"
    case dark = "Scuro"

    var id: String { rawValue }

    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}

@MainActor
@Observable
final class ProfileViewModel {
    var loggingOut = false
    var errorMessage: String?
    var selectedTheme: ThemeChoice = .system
    var notificationsEnabled = true

    /// Session-stable: cambia solo a login/logout (entrambi smontano la view).
    var user: UserModel? {
        koin.auth.currentUser.value as? UserModel
    }

    var fullName: String {
        guard let u = user else { return "Socio Mensa" }
        return u.name.isEmpty == false ? u.name : u.username
    }

    var email: String { user?.email ?? "" }

    var avatarURL: URL? {
        guard let u = user, !u.avatar.isEmpty else { return nil }
        return Files.url(collection: "users", recordId: u.id, filename: u.avatar)
    }

    /// No-op kept for back-compat con `.task { await vm.load() }`.
    func load() async {}

    func logout() async {
        loggingOut = true
        defer { loggingOut = false }
        do {
            try await koin.auth.logout()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
