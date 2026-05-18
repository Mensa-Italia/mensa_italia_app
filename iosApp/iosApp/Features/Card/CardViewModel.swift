import SwiftUI
import Shared

/// VM "leggera" per la card socio. Il `UserModel` viene letto sincrono da
/// `koin.auth.currentUser.value` ad ogni accesso: i dati tessera (id, nome,
/// scadenza, avatar) sono session-stable, cambiano solo a login/logout e in
/// quei momenti la view viene smontata da `RootView`. Nessuna subscription,
/// nessun closeable da gestire.
@MainActor
@Observable
final class CardViewModel {
    var loading = false

    var user: UserModel? {
        koin.auth.currentUser.value as? UserModel
    }

    var fullName: String {
        guard let u = user else { return "Socio Mensa" }
        return u.name.isEmpty == false ? u.name : u.username
    }

    var memberId: String { user?.id ?? "-" }
    var memberSince: String { "-" }

    var expiry: String {
        guard let u = user else { return "-" }
        return formatItalianDate(u.expireMembership)
    }

    var avatarURL: URL? {
        guard let u = user, !u.avatar.isEmpty else { return nil }
        return Files.url(collection: "users", recordId: u.id, filename: u.avatar)
    }

    var qrPayload: String {
        guard let u = user else { return "" }
        return "MENSA-IT|id:\(u.id)|user:\(u.username)|exp:\(expiry)"
    }

    /// Mantenuto per back-compat col chiamante `.task { await vm.load() }` —
    /// no-op ora che il `user` è derivato sincrono. Resta come hook per un
    /// futuro refresh esplicito (es. /api/cs/me) se servirà.
    func load() async {}
}
