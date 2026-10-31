import Shared
import SwiftUI

/// Each region medal's texts and symbol, by kind key. The rules themselves
/// (thresholds, figures) come from the server through the shared module's
/// `RegionMedalStatus`; Android keeps the same table in `RegionMedals.kt`.
/// Plain adult Italian, no formulas: those are in the info sheet.
enum RegionMedalCatalog {
    struct Info {
        let title: String
        let detail: String
        let rule: String
        let symbol: String
        /// One label per secondary figure, in the order the server sends them.
        let figures: [String]
        /// Appended to the detail's and rule's keys, bumped when their text changes meaning.
        var keySuffix = ""
    }

    static let info: [String: Info] = [
        "present": Info(
            title: "Tutti presenti",
            detail: "I soci che tornano agli eventi, in rapporto a quanti sono.",
            rule: "Soci della regione che nella stagione si sono visti ad almeno due eventi diversi, in rapporto a quanti sono. Chi compare una volta sola non conta.",
            symbol: "person.3.fill",
            figures: ["Soci tornati", "Di passaggio"], keySuffix: "_v2"),
        "host": Info(
            title: "Casa aperta",
            detail: "Le regioni da cui arrivano ospiti agli eventi di casa.",
            rule: "Da quante regioni diverse arrivano, agli eventi della regione, soci che nella stagione si sono visti almeno due volte.",
            symbol: "house.fill",
            figures: ["Ospiti da fuori"], keySuffix: "_v2"),
        "away": Info(
            title: "In trasferta",
            detail: "Le regioni in cui i soci vanno a timbrare fuori casa.",
            rule: "In quante altre regioni i soci della regione, visti almeno due volte nella stagione, sono andati a timbrare.",
            symbol: "suitcase.fill",
            figures: ["Soci in trasferta"], keySuffix: "_v2"),
        "months": Info(
            title: "Senza pause",
            detail: "I mesi della stagione con un evento timbrato in regione.",
            rule: "Mesi della stagione con almeno un evento nella regione a cui qualcuno ha timbrato.",
            symbol: "calendar.badge.clock",
            figures: ["Eventi in regione"], keySuffix: "_v2"),
        "welcome": Info(
            title: "Benvenuto",
            detail: "Chi si è iscritto in questa stagione e ha timbrato presto.",
            rule: "Tra chi si è iscritto all'app nella stagione, quanti hanno preso il primo timbro entro due mesi.",
            symbol: "hand.wave.fill",
            figures: ["Nuovi con un timbro", "Nuovi iscritti"], keySuffix: "_v2"),
        "fullhouse": Info(
            title: "Sala piena",
            detail: "L'evento più partecipato della stagione.",
            rule: "L'evento della stagione con più timbratori, in rapporto ai soci della regione.",
            symbol: "person.3.sequence.fill",
            figures: ["Timbratori"], keySuffix: "_v2"),
        "core": Info(
            title: "Zoccolo duro",
            detail: "Chi torna davvero agli eventi della regione.",
            rule: "Quanti soci, rispetto a quanti sono, tornano davvero agli eventi della regione: conta chi c'è spesso, poco chi passa una volta.",
            symbol: "shield.fill",
            figures: ["Persone", "Effettive"]),
        "national": Info(
            title: "Padroni di casa",
            detail: "Gli eventi nazionali ospitati in regione.",
            rule: "Eventi nazionali ospitati nella regione.",
            symbol: "flag.fill",
            figures: []),
        "relay": Info(
            title: "Staffetta",
            detail: "Le persone che organizzano eventi in regione.",
            rule: "Quante persone diverse hanno organizzato eventi nella regione.",
            symbol: "arrow.triangle.swap",
            figures: ["Eventi in regione"]),
        "podium": Info(
            title: "Podio",
            detail: "Il posto della regione in classifica a fine stagione.",
            rule: "Posizione della regione nella classifica regioni a fine stagione. Si assegna il 30 settembre.",
            symbol: "trophy.fill",
            figures: ["Punteggio della regione"], keySuffix: "_v2"),
    ]

    /// The kinds in the order the sheets list them.
    static let order = ["present", "host", "away", "months", "welcome", "fullhouse", "core", "national", "relay", "podium"]

    static func title(_ key: String) -> String {
        tr("medals.region.\(key).title", fallback: info[key]?.title ?? key)
    }

    static func rule(_ key: String) -> String {
        tr("medals.region.\(key).rule\(info[key]?.keySuffix ?? "")", fallback: info[key]?.rule ?? "")
    }
}

extension Medal.Tier {
    /// The metal reached, or bronze: what an unearned region medal is shown in.
    init(region tier: RegionMedalTier) {
        if tier == .gold { self = .gold }
        else if tier == .silver { self = .silver }
        else { self = .bronze }
    }
}

extension Medal {
    /// A medal of the member's region, in `past` season or the current one.
    init(_ status: RegionMedalStatus, state: RegionMedalsState, past: Bool) {
        let key = status.kind.key
        let info = RegionMedalCatalog.info[key] ?? RegionMedalCatalog.Info(title: key, detail: "", rule: "", symbol: "rosette", figures: [])
        let levelList = status.levels
        let next = past ? nil : status.nextTier
        let nextLevel = levelList.first { next != nil && $0.tier == next }
        let figureRows: [(label: String, value: String)] = info.figures.enumerated().compactMap { index, label in
            guard index < status.extra.count else { return nil }
            return (tr("medals.region.\(key).fig\(index)", fallback: label), RegionMedalsKt.formatFigure(figure: status.extra[index].doubleValue))
        }
        let earnedTier = Tier(region: status.tier)
        id = "region-\(key)-\(status.season)"
        title = tr("medals.region.\(key).title", fallback: info.title)
        detail = tr("medals.region.\(key).detail\(info.keySuffix)", fallback: info.detail)
        symbol = info.symbol
        tier = status.isEarned ? earnedTier : .bronze
        isEarned = status.isEarned
        isHidden = false
        value = Int(status.value.rounded())
        if let nextLevel {
            self.next = (Tier(region: nextLevel.tier), Int(nextLevel.threshold.rounded()))
        } else {
            self.next = nil
        }
        progress = past ? 1 : status.progress
        levels = levelList.map { (Tier(region: $0.tier), Int($0.threshold.rounded())) }
        countsStamps = false
        isSeasonal = false
        pastSeason = nil
        region = Region(
            season: Int(status.season),
            rule: tr("medals.region.\(key).rule\(info.keySuffix)", fallback: info.rule),
            valueLabel: status.valueLabel,
            isPast: past,
            isPodium: key == "podium",
            ladder: levelList.map { level in
                (Tier(region: level.tier), status.thresholdLabel(level: level), level.tier.ordinal <= status.tier.ordinal)
            },
            nextLabel: nextLevel.map { status.thresholdLabel(level: $0) },
            figures: figureRows,
            minimumNote: (!status.eligible && status.minimum > 0 && !past)
                ? tr("medals.region.minimum", fallback: "Conta dal minimo di {minimum} persone.",
                     ["minimum": RegionMedalsKt.formatFigure(figure: status.minimum)])
                : nil,
            witnessed: state.witnessed(status: status)
        )
    }
}

/// An earlier season of the region's medals.
struct RegionPastSeason: Identifiable {
    let season: Int
    let medals: [Medal]
    var id: Int { season }
    var earnedCount: Int { medals.filter(\.isEarned).count }
}

/// The member's region medals, read once and shared by the cabinet's two
/// pages. Follows the shared repository, which keeps them per user and does
/// nothing while the `region_medals_enabled` flag is off.
@MainActor
@Observable
final class RegionMedalStore {
    static let shared = RegionMedalStore()

    /// The flag is on and the member's region is known: the cabinet's switch exists.
    private(set) var available = false
    /// Read at least once (or from the saved copy).
    private(set) var loaded = false
    /// Set when the last read failed and nothing was read before.
    private(set) var failed = false
    private(set) var regionName = ""
    private(set) var season = 0
    /// This season's medals, best first.
    private(set) var current: [Medal] = []
    /// Earlier seasons, latest first, each in the kinds' order.
    private(set) var past: [RegionPastSeason] = []
    private var subscriptions: [Closeable] = []

    var earnedCount: Int { current.filter(\.isEarned).count }

    private init() {
        subscriptions = [
            subscribeFlow(koin.regionMedals.observeAvailable() as Kotlinx_coroutines_coreFlow) { [weak self] (value: KotlinBoolean) in
                Task { @MainActor [weak self] in self?.available = value.boolValue }
            },
            subscribeOptionalFlow(koin.regionMedals.observe() as Kotlinx_coroutines_coreFlow) { [weak self] (state: RegionMedalsState?) in
                Task { @MainActor [weak self] in self?.apply(state) }
            },
        ]
    }

    private func apply(_ state: RegionMedalsState?) {
        guard let state else {
            loaded = false
            return
        }
        loaded = true
        regionName = state.regionName
        season = Int(state.season)
        current = state.currentSeason.map { Medal($0, state: state, past: false) }
        past = state.pastSeasonList.map { entry in
            RegionPastSeason(
                season: entry.intValue,
                medals: state.past(season: entry.int32Value).map { Medal($0, state: state, past: true) }
            )
        }
    }

    func refresh() async {
        do {
            try await koin.regionMedals.refresh()
            failed = false
        } catch {
            failed = !loaded
        }
    }
}

// MARK: - The region page

/// What the region's medals are, said once at the top of their page: of the
/// team, by season, counting people. Mirrors Android `RegionRules`.
struct RegionRules: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(tr("medals.region.rules.title", fallback: "Come funzionano"))
                .font(.subheadline.weight(.semibold))
            rule("person.3.fill",
                 tr("medals.region.rules.team", fallback: "Le medaglie di regione sono della squadra: le conquista la regione tutta insieme e le vede chi ne fa parte oggi."))
            rule("calendar",
                 tr("medals.region.rules.yearly_v2", fallback: "Ogni stagione, dal 1° ottobre al 30 settembre, si ricomincia; le annate passate restano in bacheca."))
            rule("person.fill",
                 tr("medals.region.rules.people_v2", fallback: "Contano le persone, non i timbri: chi si vede una volta sola nella stagione non fa numero."))
            Text(tr("medals.region.rules.more", fallback: "Il criterio esatto, formula compresa, è nella scheda ⓘ, sezione Matematica."))
                .font(.footnote)
                .foregroundStyle(.white.opacity(0.45))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .background(.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private func rule(_ symbol: String, _ text: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 15))
                .foregroundStyle(.white.opacity(0.6))
                .frame(width: 18)
                .padding(.top, 2)
            Text(text)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }
}

/// An earlier season's header: tap to open or close its medals.
struct SeasonHeader: View {
    /// "2025/26".
    let label: String
    let summary: String
    let expanded: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Text(label)
                    .font(.headline.monospacedDigit())
                    .foregroundStyle(.white)
                Spacer()
                Text(summary)
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.6))
                Image(systemName: expanded ? "chevron.up" : "chevron.down")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.6))
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 14)
            .background(.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("\(label), \(summary)"))
        .accessibilityAddTraits(.isButton)
    }
}

// MARK: - The region medal's detail

/// "C'eri anche tu": the member was in the region when this metal was reached.
struct WitnessBadge: View {
    var body: some View {
        Label(tr("medals.region.witness", fallback: "C'eri anche tu"), systemImage: "checkmark.seal.fill")
            .font(.footnote.weight(.medium))
            .foregroundStyle(.white.opacity(0.8))
            .padding(.horizontal, 12)
            .padding(.vertical, 5)
            .background(.white.opacity(0.06), in: Capsule())
            .overlay(Capsule().strokeBorder(.white.opacity(0.25), lineWidth: 1))
    }
}

/// The ladder of a region medal: each metal with the figure that earns it,
/// the ones the season reached lit.
struct RegionTierLadder: View {
    let medal: Medal
    let region: Medal.Region

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            ForEach(Array(region.ladder.enumerated()), id: \.offset) { index, step in
                let isNext = medal.next?.tier == step.tier
                if index > 0 {
                    Capsule()
                        .fill(step.reached ? step.tier.metal.mid : .white.opacity(0.14))
                        .frame(height: 2)
                        .padding(.top, 27)
                        .frame(maxWidth: .infinity)
                }
                VStack(spacing: 2) {
                    ZStack {
                        if isNext {
                            Circle()
                                .stroke(.white.opacity(0.12), lineWidth: 4)
                            Circle()
                                .trim(from: 0, to: medal.progress)
                                .stroke(step.tier.metal.mid, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                                .rotationEffect(.degrees(-90))
                        }
                        MedalArtwork(tier: step.tier, symbol: medal.symbol, size: 38, locked: !step.reached)
                    }
                    .frame(width: 56, height: 56)
                    Text(step.tier.name)
                        .font(.caption2)
                        .foregroundStyle(step.reached ? step.tier.metal.hi : .white.opacity(0.45))
                        .padding(.top, 4)
                    Text(step.label)
                        .font(.subheadline.weight(.bold).monospacedDigit())
                        .foregroundStyle(step.reached || isNext ? .white : .white.opacity(0.45))
                }
                .accessibilityElement(children: .combine)
            }
        }
    }
}

/// Where the region stands with a medal: the figure, the secondary figures
/// in words, and how far the next metal is. The podium says when it's awarded.
struct RegionStatus: View {
    let medal: Medal
    let region: Medal.Region

    var body: some View {
        VStack(spacing: 0) {
            Text(caption.uppercased())
                .font(.caption2.weight(.semibold))
                .tracking(2)
                .foregroundStyle(.white.opacity(0.5))
            Text(region.valueLabel)
                .font(.system(size: 40, weight: .heavy).monospacedDigit())
                .foregroundStyle(medal.isEarned ? medal.tier.metal.hi : .white)
            if !region.figures.isEmpty {
                VStack(spacing: 6) {
                    ForEach(Array(region.figures.enumerated()), id: \.offset) { _, figure in
                        HStack {
                            Text(figure.label).foregroundStyle(.secondary)
                            Spacer()
                            Text(figure.value).fontWeight(.bold).monospacedDigit()
                        }
                        .font(.subheadline)
                        .accessibilityElement(children: .combine)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                .padding(.top, 12)
            }
            if let note = region.minimumNote {
                Text(note)
                    .font(.footnote)
                    .foregroundStyle(.white.opacity(0.55))
                    .padding(.top, 10)
            }
            if region.isPodium, !medal.isEarned, !region.isPast {
                Text(tr("medals.region.podium_pending_v2", fallback: "Si assegna il 30 settembre"))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.75))
                    .padding(.top, 10)
            }
            goal
        }
        .frame(maxWidth: 320)
    }

    private var caption: String {
        if region.isPast { return tr("medals.region.value_season", fallback: "Nella stagione") }
        if region.isPodium { return tr("medals.region.value_podium", fallback: "Posizione attuale") }
        return tr("medals.region.value_now", fallback: "Adesso")
    }

    @ViewBuilder private var goal: some View {
        if let next = medal.next, let nextLabel = region.nextLabel {
            VStack(spacing: 8) {
                Text(tr("medals.next", fallback: "Prossimo traguardo: %1$@ a %2$d")
                    .replacingOccurrences(of: "%1$@", with: next.tier.name)
                    .replacingOccurrences(of: "%2$d", with: nextLabel))
                    .font(.subheadline.weight(.semibold))
                if !region.isPodium {
                    ProgressView(value: medal.progress)
                        .tint(next.tier.metal.mid)
                    Text(tr("medals.progress", fallback: "%1$d su %2$d")
                        .replacingOccurrences(of: "%1$d", with: region.valueLabel)
                        .replacingOccurrences(of: "%2$d", with: nextLabel))
                        .font(.footnote.monospacedDigit())
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.top, 20)
        } else if medal.isEarned, !region.isPast {
            Text(tr("medals.max_tier", fallback: "Livello massimo raggiunto"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(medal.tier.metal.hi)
                .padding(.top, 20)
        }
    }
}

/// How the medal is earned, in the card of the stamp rules.
struct RegionRuleCard: View {
    let rule: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(tr("medals.region.rule_title", fallback: "Come si ottiene"))
                .font(.subheadline.weight(.semibold))
            Text(rule)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .background(.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}
