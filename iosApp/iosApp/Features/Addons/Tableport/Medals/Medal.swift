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
    /// Every metal of the medal, ascending, with the value that earns it.
    let levels: [(tier: Tier, threshold: Int)]
    /// Counts stamps, so only the ones `view_medals` lets through (see `MedalCatalog.stampBased`).
    let countsStamps: Bool
    /// Set for a medal of the member's region, which has more to say than a personal one.
    let region: Region?
    /// Won again every season (from 1 October) rather than for good.
    let isSeasonal: Bool
    /// Set for a seasonal medal as it ended in a closed season (the season's starting year): the value is final.
    let pastSeason: Int?
    /// How a seasonal medal ended in the closed seasons, latest first; empty for the others.
    var history: [PastResult] = []

    /// One closed season's result of a seasonal medal.
    struct PastResult: Identifiable {
        let season: Int
        /// "2025/26".
        let label: String
        /// The metal won; nil when the season closed without one.
        let tier: Tier?
        let value: Int
        var id: Int { season }
    }

    var isPast: Bool { pastSeason != nil }

    /// What a region medal adds: its season, how it's earned, the figure in its
    /// unit and the ladder with thresholds written in that unit (a threshold
    /// can be 8,5%, so `levels`' whole numbers won't do). Mirrors Android
    /// `RegionMedalDetail`.
    struct Region {
        let season: Int
        /// "Come si ottiene".
        let rule: String
        /// The figure in its unit: "11,2%", "6", "3°".
        let valueLabel: String
        /// An earlier season: the figure is final, with no next goal.
        let isPast: Bool
        let isPodium: Bool
        /// Every metal with the figure that earns it, and whether the season reached it.
        let ladder: [(tier: Tier, label: String, reached: Bool)]
        /// The next metal's threshold, in the unit.
        let nextLabel: String?
        /// The secondary figures, labelled, in words the member reads.
        let figures: [(label: String, value: String)]
        /// Set while too few people stand behind the figure for it to count.
        let minimumNote: String?
        /// The member was in the region when the metal shown was reached.
        let witnessed: Bool
    }

    static func == (a: Medal, b: Medal) -> Bool {
        a.id == b.id && a.tier == b.tier && a.isEarned == b.isEarned && a.value == b.value && a.history.count == b.history.count
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
            : tr("medals.\(status.id).detail\(MedalCatalog.detailSuffix[status.id] ?? "")", fallback: info.detail)
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
        levels = status.levels.map { (Tier($0.tier), Int($0.threshold)) }
        countsStamps = MedalCatalog.stampBased.contains(status.id)
        region = nil
        isSeasonal = status.isSeasonal
        pastSeason = nil
    }

    /// [base] (the same medal now) as it ended in a closed season: the metal
    /// and the value of that season, no next goal.
    init(past base: Medal, result: MedalSeasonResult) {
        let info = MedalCatalog.info[base.id] ?? (base.id, "", "rosette")
        id = "\(base.id)-\(result.season)"
        // A secret medal earned in a closed season is shown by name, even if this season's is still hidden.
        title = tr("medals.\(base.id).title", fallback: info.title)
        detail = tr("medals.\(base.id).detail\(MedalCatalog.detailSuffix[base.id] ?? "")", fallback: info.detail)
        symbol = info.symbol
        let won = result.tier.map(Tier.init)
        tier = won ?? base.levels.first?.tier ?? .bronze
        isEarned = won != nil
        isHidden = false
        value = Int(result.value)
        next = nil
        progress = 1
        levels = base.levels
        countsStamps = base.countsStamps
        region = nil
        isSeasonal = true
        pastSeason = Int(result.season)
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
    /// The medals whose counter is made of stamps: `view_medals` keeps, for
    /// all of them, only the stamps worth at least a point in the ranking
    /// (enough stampers for the region's size), not the member's own events'
    /// and none of their region's while on its staff. Their showcase says so.
    static let stampBased: Set<String> = [
        "collector", "steady", "unstoppable", "marathon", "four_seasons", "pioneer",
        "globetrotter", "traveller", "national_rally",
        "night_owl", "early_bird", "halloween", "new_year", "valentine", "pi_day", "birthday", "front_row", "better_late",
    ]

    /// Key suffix of the descriptions whose text changed meaning, so an older translation doesn't win.
    static let detailSuffix: [String: String] = ["four_seasons": "_v2"]

    static let info: [String: (title: String, detail: String, symbol: String)] = [
        "collector": ("Collezionista", "Francobolli raccolti nel passaporto, tra quelli che in classifica valgono almeno un punto. Quelli dei tuoi eventi contano nella medaglia Organizzatore.", "seal.fill"),
        "steady": ("Costante", "Mesi diversi con almeno un francobollo.", "calendar"),
        "unstoppable": ("Inarrestabile", "Mesi consecutivi con almeno un francobollo.", "flame.fill"),
        "marathon": ("Maratoneta", "Francobolli raccolti in un solo giorno.", "figure.run"),
        "four_seasons": ("Quattro stagioni", "Un francobollo in ciascuna delle quattro stagioni dell'anno: primavera, estate, autunno e inverno.", "leaf.fill"),
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
        "organizer": ("Organizzatore", "Eventi che hai organizzato: i francobolli dei tuoi eventi contano qui, non nelle altre medaglie.", "megaphone.fill"),
        "national_director": ("Regista nazionale", "Hai organizzato un evento nazionale.", "star.circle.fill"),
        "active_member": ("Socio attivo", "Iscrizione in corso di validità.", "checkmark.seal.fill"),
        "early_adopter": ("Della prima ora", "Anni passati con l'app.", "iphone"),
        "up_to_date": ("Sempre aggiornato", "Calendario degli eventi collegato.", "calendar.badge.checkmark"),
        "cartographer": ("Cartografo", "Regioni seguite nel calendario.", "map.fill"),
        "multiscreen": ("Multischermo", "L'app su almeno due dispositivi.", "laptopcomputer.and.iphone"),
    ]
}

/// A closed season of the member's seasonal medals.
struct MedalPastSeason: Identifiable {
    let season: Int
    let label: String
    let medals: [Medal]
    var id: Int { season }
    var earnedCount: Int { medals.filter(\.isEarned).count }
}

/// The member's medals, read once and shared by Today's count and the
/// cabinet. Follows the shared repository, which keeps them per user. The
/// seasonal ones are counted in the current season; the closed ones live in
/// `past` ("Annate"), from `medals_history`.
@MainActor
@Observable
final class MedalStore {
    static let shared = MedalStore()

    private(set) var medals: [Medal] = []
    /// The season the seasonal medals are counted in (its starting year); 0 until known.
    private(set) var season = 0
    /// Closed seasons, latest first.
    private(set) var past: [MedalPastSeason] = []
    /// Set when the last read failed and nothing was read before.
    private(set) var failed = false
    private var statuses: [MedalStatus] = []
    private var historyRead: MedalHistory?
    private var subscriptions: [Closeable] = []

    var earnedCount: Int { medals.filter(\.isEarned).count }

    /// "2026/27".
    var seasonLabel: String { season > 0 ? Season.shared.label(season: Int32(season)) : "" }

    private init() {
        subscriptions = [
            subscribeFlow(koin.medals.observe() as Kotlinx_coroutines_coreFlow) { [weak self] (list: NSArray) in
                let statuses = (list as? [MedalStatus]) ?? []
                Task { @MainActor [weak self] in
                    self?.statuses = statuses
                    self?.rebuild()
                }
            },
            subscribeFlow(koin.medals.observeSeason() as Kotlinx_coroutines_coreFlow) { [weak self] (value: KotlinInt) in
                Task { @MainActor [weak self] in self?.season = Int(value.int32Value) }
            },
            subscribeFlow(koin.medalsHistory.observe() as Kotlinx_coroutines_coreFlow) { [weak self] (history: MedalHistory) in
                Task { @MainActor [weak self] in
                    self?.historyRead = history
                    self?.rebuild()
                }
            },
        ]
    }

    /// The medals best first, each seasonal one with its past seasons, and the
    /// closed seasons as lists of medals.
    private func rebuild() {
        let base = Self.bestFirst(statuses.map(Medal.init))
        guard let history = historyRead, !history.isEmpty else {
            medals = base
            past = []
            return
        }
        let byId = Dictionary(uniqueKeysWithValues: base.map { ($0.id, $0) })
        medals = base.map { medal in
            guard medal.isSeasonal else { return medal }
            var withHistory = medal
            withHistory.history = history.results(medalId: medal.id).map {
                Medal.PastResult(
                    season: Int($0.season), label: $0.seasonLabel,
                    tier: $0.tier.map(Medal.Tier.init), value: Int($0.value)
                )
            }
            return withHistory
        }
        past = history.seasons.map { entry in
            MedalPastSeason(
                season: Int(entry.season),
                label: entry.label,
                medals: entry.results.compactMap { result in byId[result.medalId].map { Medal(past: $0, result: result) } }
            )
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
        // The annate are a bonus: a failed read leaves the saved copy.
        try? await koin.medalsHistory.refresh()
    }
}
