import Shared
import SwiftUI

/// A medal of the member's collection, as shown: the rule's state from the
/// shared module (`MedalRules`, read from `view_medals`) with its name,
/// description and symbol.
struct Medal: Identifiable, Hashable {
    enum Tier: Int, CaseIterable, Hashable {
        case bronze, silver, gold, platinum
    }

    let id: String
    let title: String
    let detail: String
    /// SF Symbol struck on the face.
    let symbol: String
    /// The metal shown: the one earned, or the first to aim for.
    let tier: Tier
    let isEarned: Bool
    /// A secret medal not earned yet: shown as a mystery.
    let isHidden: Bool
    /// The counter's current value.
    let value: Int
    /// The next metal and the value that earns it; nil once at the top.
    let next: (tier: Tier, threshold: Int)?
    /// 0…1 toward `next`.
    let progress: Double

    static func == (a: Medal, b: Medal) -> Bool {
        a.id == b.id && a.tier == b.tier && a.isEarned == b.isEarned && a.value == b.value
    }

    func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

extension Medal {
    init(_ status: MedalStatus) {
        let info = MedalCatalog.info[status.id] ?? (status.id, "", "rosette")
        let hidden = status.secret && !status.isEarned
        id = status.id
        title = hidden
            ? tr("medals.secret.title", fallback: "Medaglia segreta")
            : tr("medals.\(status.id).title", fallback: info.title)
        detail = hidden
            ? tr("medals.secret.detail", fallback: "Si svela quando la ottieni.")
            : tr("medals.\(status.id).detail", fallback: info.detail)
        symbol = hidden ? "questionmark" : info.symbol
        tier = Tier(status.displayTier)
        isEarned = status.isEarned
        isHidden = hidden
        value = Int(status.value)
        if let nextTier = status.nextTier, let threshold = status.nextThreshold {
            next = (Tier(nextTier), Int(threshold.int32Value))
        } else {
            next = nil
        }
        progress = status.progress
    }
}

extension Medal.Tier {
    init(_ tier: MedalTier) {
        if tier == .platinum { self = .platinum }
        else if tier == .gold { self = .gold }
        else if tier == .silver { self = .silver }
        else { self = .bronze }
    }

    var name: String {
        switch self {
        case .bronze: tr("medals.tier.bronze", fallback: "Bronzo")
        case .silver: tr("medals.tier.silver", fallback: "Argento")
        case .gold: tr("medals.tier.gold", fallback: "Oro")
        case .platinum: tr("medals.tier.platinum", fallback: "Platino")
        }
    }

    /// The metal's ramp: where light hits, the body, the shade.
    var metal: (hi: Color, mid: Color, lo: Color) {
        switch self {
        case .bronze:
            (Color(red: 0.97, green: 0.78, blue: 0.58),
             Color(red: 0.78, green: 0.49, blue: 0.26),
             Color(red: 0.42, green: 0.23, blue: 0.10))
        case .silver:
            (Color(red: 0.99, green: 0.99, blue: 1.00),
             Color(red: 0.74, green: 0.76, blue: 0.80),
             Color(red: 0.40, green: 0.42, blue: 0.47))
        case .gold:
            (Color(red: 1.00, green: 0.94, blue: 0.64),
             Color(red: 0.86, green: 0.66, blue: 0.22),
             Color(red: 0.50, green: 0.34, blue: 0.07))
        case .platinum:
            (Color(red: 0.95, green: 0.98, blue: 1.00),
             Color(red: 0.70, green: 0.79, blue: 0.88),
             Color(red: 0.33, green: 0.41, blue: 0.52))
        }
    }
}

/// Each medal's name, description and symbol, by rule id. The rules
/// themselves (counters, thresholds) are the shared module's `MedalRules`;
/// Android keeps the same table in `MedalCatalog.kt`.
enum MedalCatalog {
    static let info: [String: (title: String, detail: String, symbol: String)] = [
        "collector": ("Collezionista", "Francobolli raccolti nel passaporto.", "seal.fill"),
        "steady": ("Costante", "Mesi diversi con almeno un francobollo.", "calendar"),
        "unstoppable": ("Inarrestabile", "Mesi consecutivi con almeno un francobollo.", "flame.fill"),
        "marathon": ("Maratoneta", "Francobolli raccolti in un solo giorno.", "figure.run"),
        "four_seasons": ("Quattro stagioni", "Un francobollo in ogni stagione dello stesso anno.", "leaf.fill"),
        "pioneer": ("Pioniere", "Tra i primi dieci a raccogliere un francobollo.", "sparkles"),
        "globetrotter": ("Giramondo", "Regioni diverse in cui hai raccolto francobolli.", "globe.europe.africa.fill"),
        "traveller": ("Trasfertista", "Francobolli raccolti fuori dalla tua regione.", "car.fill"),
        "national_rally": ("Raduno nazionale", "Francobolli di eventi nazionali.", "flag.fill"),
        "night_owl": ("Nottambulo", "Un francobollo dopo mezzanotte.", "moon.stars.fill"),
        "early_bird": ("Mattiniero", "Un francobollo tra le 5 e le 9 del mattino.", "sunrise.fill"),
        "halloween": ("Dolcetto o scherzetto", "Un francobollo la notte di Halloween.", "theatermasks.fill"),
        "new_year": ("Brindisi di mezzanotte", "Un francobollo a Capodanno.", "party.popper.fill"),
        "valentine": ("Cuore razionale", "Un francobollo a San Valentino.", "heart.fill"),
        "pi_day": ("3,14159…", "Un francobollo il 14 marzo, il giorno del pi greco.", "function"),
        "birthday": ("Festeggiato", "Un francobollo il giorno del tuo compleanno.", "birthday.cake.fill"),
        "front_row": ("Prima fila", "Un francobollo raccolto il giorno stesso in cui è uscito.", "bolt.fill"),
        "better_late": ("Meglio tardi", "Un francobollo raccolto sei mesi dopo la sua uscita.", "hourglass"),
        "always_there": ("Sempre presente", "Iscrizioni agli appuntamenti degli eventi.", "person.3.fill"),
        "organizer": ("Organizzatore", "Eventi che hai organizzato.", "megaphone.fill"),
        "national_director": ("Regista nazionale", "Hai organizzato un evento nazionale.", "star.circle.fill"),
        "active_member": ("Socio attivo", "Iscrizione in corso di validità.", "checkmark.seal.fill"),
        "early_adopter": ("Della prima ora", "Anni passati con l'app.", "iphone"),
        "up_to_date": ("Sempre aggiornato", "Calendario degli eventi collegato.", "calendar.badge.checkmark"),
        "cartographer": ("Cartografo", "Regioni seguite nel calendario.", "map.fill"),
        "multiscreen": ("Multischermo", "L'app su almeno due dispositivi.", "laptopcomputer.and.iphone"),
    ]
}

/// The member's medals, read once and shared by Today's count and the
/// cabinet. Follows the shared repository, which keeps them per user.
@MainActor
@Observable
final class MedalStore {
    static let shared = MedalStore()

    private(set) var medals: [Medal] = []
    /// Set when the last read failed and nothing was read before.
    private(set) var failed = false
    private var subscription: Closeable?

    var earnedCount: Int { medals.filter(\.isEarned).count }

    private init() {
        subscription = subscribeFlow(koin.medals.observe() as Kotlinx_coroutines_coreFlow) { [weak self] (list: NSArray) in
            let statuses = (list as? [MedalStatus]) ?? []
            Task { @MainActor [weak self] in
                self?.medals = Self.bestFirst(statuses.map(Medal.init))
            }
        }
    }

    /// Best to worst: earned by metal, platinum first; then the ones on
    /// their way, closest first; the secret ones last. Ties keep the table's
    /// order.
    static func bestFirst(_ medals: [Medal]) -> [Medal] {
        func rank(_ medal: Medal) -> (Int, Double) {
            if medal.isEarned { return (0, -Double(medal.tier.rawValue)) }
            if medal.isHidden { return (2, 0) }
            return (1, -medal.progress)
        }
        return medals.enumerated()
            .sorted { lhs, rhs in
                let (a, b) = (rank(lhs.element), rank(rhs.element))
                return a != b ? a < b : lhs.offset < rhs.offset
            }
            .map(\.element)
    }

    func refresh() async {
        do {
            _ = try await koin.medals.refresh()
            failed = false
        } catch {
            failed = medals.isEmpty
        }
    }
}
