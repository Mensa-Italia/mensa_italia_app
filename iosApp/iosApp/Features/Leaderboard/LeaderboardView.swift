import Charts
import Shared
import SwiftUI

/// The members' ranking: where the member stands and how far the next place
/// is, then the first ten. System list and chart throughout, so it takes the
/// platform's look (Liquid Glass bars on iOS 26). Mirrors Android
/// `LeaderboardScreen`.
struct LeaderboardView: View {
    private static let podiumSize = 3
    /// Soci, regioni, segretari: one page, one switch at the top.
    private enum Board: Hashable { case members, regions, secretaries }

    @State private var store = LeaderboardStore.shared
    @State private var showInfo = false
    @State private var showSettings = false
    @State private var openMember: String?
    @State private var board = Board.members
    /// A region's or secretary's row, tapped: its calculation in a sheet.
    @State private var detail: ScoreDetail?
    /// The app's own appearance, read outside the stage: a member's profile
    /// opened from here goes back to it.
    @Environment(\.colorScheme) private var appScheme

    /// The rankings switched on in `configs`, in the switch's order.
    private var kinds: [LeaderboardKind] { koin.featureFlags.enabledLeaderboards() }
    private var boards: [Board] {
        kinds.compactMap { kind in
            if kind == .members { return .members }
            if kind == .regions { return .regions }
            if kind == .secretaries { return .secretaries }
            return nil
        }
    }
    /// The selected ranking, or the first one on when the selection is off.
    private var current: Board? { boards.contains(board) ? board : boards.first }

    private func label(_ b: Board) -> String {
        switch b {
        case .members: tr("leaderboard.tab.members", fallback: "Soci")
        case .regions: tr("leaderboard.tab.regions", fallback: "Regioni")
        case .secretaries: tr("leaderboard.tab.secretaries", fallback: "Segretari")
        }
    }

    var body: some View {
        List {
            if boards.count > 1 {
                Section {
                    Picker(tr("leaderboard.home.section", fallback: "Classifica"), selection: $board) {
                        ForEach(boards, id: \.self) { b in Text(label(b)).tag(b) }
                    }
                    .pickerStyle(.segmented)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
                }
            }
            switch current {
            case .members: members
            case .regions: regions
            case .secretaries: secretaries
            case nil:
                Text(tr("leaderboard.disabled", fallback: "La classifica non è attiva."))
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
            }
        }
        .navigationTitle(tr("leaderboard.home.section", fallback: "Classifica"))
        .navigationDestination(item: $openMember) { id in
            MemberDetailView(memberId: id)
                .environment(\.colorScheme, appScheme)
        }
        .darkStage()
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showInfo = true } label: {
                    Label(tr("leaderboard.how_it_works", fallback: "Come funziona"), systemImage: "info.circle")
                }
            }
            // Leaving the ranking only concerns the members' one.
            if kinds.contains(.members) {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showSettings = true } label: {
                        Label(tr("leaderboard.settings", fallback: "Impostazioni"), systemImage: "gearshape")
                    }
                }
            }
        }
        .sheet(isPresented: $showInfo) { LeaderboardInfoSheet(kinds: kinds) }
        .sheet(isPresented: $showSettings) { LeaderboardSettingsSheet() }
        .sheet(item: $detail) { item in
            RegionScoreSheet(detail: item) { id in
                detail = nil
                openMember = id
            }
        }
        .overlay {
            if current == .members, store.board == nil {
                if store.failed {
                    ContentUnavailableView(
                        tr("leaderboard.home.section", fallback: "Classifica"),
                        systemImage: "trophy",
                        description: Text(tr("leaderboard.load_error", fallback: "Non riesco a leggere la classifica. Riprova più tardi."))
                    )
                } else {
                    ProgressView()
                }
            } else if current != nil, current != .members, store.regions == nil {
                if store.regionsFailed {
                    ContentUnavailableView(
                        tr("leaderboard.home.section", fallback: "Classifica"),
                        systemImage: "trophy",
                        description: Text(tr("leaderboard.load_error", fallback: "Non riesco a leggere la classifica. Riprova più tardi."))
                    )
                } else {
                    ProgressView()
                }
            }
        }
        .refreshable { await store.refresh() }
        .task { await store.refresh() }
    }

    // MARK: - Regions and secretaries

    @ViewBuilder
    private var regions: some View {
        if let rows = store.regions {
            // Rows under the public threshold come only to their own staff
            // (see `LeaderboardRepository.VISIBLE_MIN_SCORE`): after a gap.
            let minScore = LeaderboardRepository.companion.VISIBLE_MIN_SCORE
            let visible = rows.filter { $0.score >= minScore }
            let mine = rows.filter { $0.score < minScore }
            Section {
                ForEach(visible, id: \.id) { row in
                    RegionRankRow(row: row) {
                        detail = ScoreDetail(id: row.id, score: row, title: row.displayName, subtitle: nil, memberId: nil)
                    }
                }
                if !mine.isEmpty {
                    Image(systemName: "ellipsis")
                        .foregroundStyle(.tertiary)
                        .frame(maxWidth: .infinity)
                    ForEach(mine, id: \.id) { row in
                        RegionRankRow(row: row) {
                            detail = ScoreDetail(id: row.id, score: row, title: row.displayName, subtitle: nil, memberId: nil)
                        }
                    }
                }
            } header: {
                Text(tr("leaderboard.last_year", fallback: "Ultimi 12 mesi"))
            } footer: {
                Text(regionsFooter)
            }
        }
    }

    @ViewBuilder
    private var secretaries: some View {
        if let rows = store.secretaries {
            let minScore = LeaderboardRepository.companion.VISIBLE_MIN_SCORE
            let visible = rows.filter { $0.score >= minScore }
            let mine = rows.filter { $0.score < minScore }
            Section {
                ForEach(visible, id: \.id) { row in
                    SecretaryRankRow(row: row) { detail = secretaryDetail(row) }
                }
                if !mine.isEmpty {
                    Image(systemName: "ellipsis")
                        .foregroundStyle(.tertiary)
                        .frame(maxWidth: .infinity)
                    ForEach(mine, id: \.id) { row in
                        SecretaryRankRow(row: row) { detail = secretaryDetail(row) }
                    }
                }
            } header: {
                Text(tr("leaderboard.last_year", fallback: "Ultimi 12 mesi"))
            } footer: {
                Text(regionsFooter)
            }
        }
    }

    private var regionsFooter: String {
        tr("leaderboard.footer_regions_v4",
           fallback: "In classifica le regioni con almeno {min} punti; chi fa parte dello staff di una regione vede anche la sua, qui sotto. A pari punti, pari posizione. Tocca una riga per il conto.",
           ["min": "\(LeaderboardRepository.companion.VISIBLE_MIN_SCORE)"])
    }

    private func secretaryDetail(_ row: SecretaryRow) -> ScoreDetail {
        ScoreDetail(
            id: row.id, score: row, title: row.regionName, subtitle: row.displayName,
            memberId: row.member != nil && !row.isMe ? row.id : nil
        )
    }

    // MARK: - Members

    @ViewBuilder
    private var members: some View {
            if let board = store.board {
                Section(tr("leaderboard.my_place", fallback: "La tua posizione")) {
                    if let me = board.me {
                        MyPlace(board: board, me: me)
                    } else {
                        Label {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(tr("leaderboard.unranked.title", fallback: "Non sei ancora in classifica"))
                                Text(tr("leaderboard.unranked.detail", fallback: "Raccogli il primo timbro a un evento: da lì inizi a fare punti."))
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            }
                        } icon: {
                            Image(systemName: "trophy")
                        }
                    }
                }

                // The first three on the podium, the rest of the ten below.
                Section {
                    LeaderboardPodium(rows: Array(board.top.prefix(Self.podiumSize))) { openMember = $0 }
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 8, trailing: 16))
                } header: {
                    Text(tr("leaderboard.top_label", fallback: "I primi {count}",
                            ["count": "\(LeaderboardBoard.companion.TOP_SIZE)"]))
                }

                Section {
                    ForEach(board.top.dropFirst(Self.podiumSize), id: \.id) { RankRow(row: $0) { openMember = $0 } }
                    // Below the first ten only the member's own row, after a
                    // gap that says places are skipped.
                    if let me = board.meBelowTop {
                        Image(systemName: "ellipsis")
                            .foregroundStyle(.tertiary)
                            .frame(maxWidth: .infinity)
                        RankRow(row: me) { openMember = $0 }
                    }
                } footer: {
                    Text(tr("leaderboard.footer", fallback: "Ultimi 12 mesi. A pari punti, pari posizione."))
                }
            }
    }
}

// MARK: - Regions and secretaries

/// What the calculation sheet shows: a region's numbers, and whose they are.
private struct ScoreDetail: Identifiable {
    let id: String
    let score: RegionScore
    let title: String
    /// The secretary's name, when the row is theirs.
    let subtitle: String?
    /// The secretary's member id, when their profile can be opened.
    let memberId: String?
}

private struct Place: View {
    let position: Int32

    var body: some View {
        Group {
            if let tier = podiumMetal(position: Int(position)) {
                Image(systemName: "medal.fill")
                    .font(.title3)
                    .foregroundStyle(tier.metal.mid)
            } else {
                Text("\(position)")
                    .font(.body.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: 28)
    }
}

private struct RegionRankRow: View {
    let row: RegionRow
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Place(position: row.position)
                Text(row.displayName).lineLimit(1)
                Spacer()
                Text(tr("leaderboard.points", fallback: "{points} pt", ["points": "\(row.score)"]))
                    .font(.subheadline.weight(.semibold).monospacedDigit())
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
            .accessibilityElement(children: .combine)
        }
        .foregroundStyle(.primary)
    }
}

private struct SecretaryRankRow: View {
    let row: SecretaryRow
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Place(position: row.position)
                SecretaryAvatar(row: row, size: 40)
                VStack(alignment: .leading, spacing: 1) {
                    HStack(spacing: 6) {
                        Text(row.displayName).lineLimit(1)
                        if row.isMe {
                            Text(tr("leaderboard.you", fallback: "Tu"))
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(.tint)
                        }
                    }
                    Text(row.region)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Text(tr("leaderboard.points", fallback: "{points} pt", ["points": "\(row.score)"]))
                    .font(.subheadline.weight(.semibold).monospacedDigit())
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
            .accessibilityElement(children: .combine)
        }
        .foregroundStyle(.primary)
    }
}

/// How a region's (or its secretary's) points were made, as
/// `view_leaderboard_regioni` computes them: participation × community +
/// national events hosted. Mirrors Android `RegionScoreSheet`.
private struct RegionScoreSheet: View {
    let detail: ScoreDetail
    let onOpenMember: (String) -> Void
    @Environment(\.dismiss) private var dismiss

    private var score: RegionScore { detail.score }

    var body: some View {
        NavigationStack {
            List {
                if let subtitle = detail.subtitle {
                    Section { Text(subtitle).foregroundStyle(.secondary) }
                }
                // Laid out like the proof in the info sheet: the theorem,
                // three steps with the formula and the numbers in it, the result.
                let steps = scoreSteps(score)
                formula(tr("leaderboard.calc.theorem", fallback: "Teorema · la regione"),
                        tr("leaderboard.calc.theorem.caption", fallback: "$ΣE_{r}$ gli eventi in regione, $c$ chi timbra, $ΣE_{n}$ i nazionali ospitati: i tre passi qui sotto.")) {
                    MathText("R = c · ΣE_{r} + ΣE_{n}")
                }
                formula(steps[0].title, steps[0].detail) {
                    VStack(spacing: 6) {
                        MathText("ΣE_{r} = Σ 10 · d · σ(s)")
                        MathText("= \(score.participation) pt")
                    }
                }
                formula(steps[1].title, steps[1].detail) {
                    VStack(spacing: 6) {
                        HStack(spacing: 0) {
                            MathText("c = σ(")
                            Fraction(numerator: "e", denominator: "N_{r}^{0,7} · N̄^{0,3}")
                            MathText(")")
                        }
                        MathText("e = \(score.effectiveAttendees), N_{r} = \(score.membersActive) ⇒ c = \(score.communityPct)%")
                    }
                }
                formula(steps[2].title, steps[2].detail) {
                    VStack(spacing: 6) {
                        MathText("ΣE_{n} = Σ 10 · d · σ(s)")
                        MathText("= \(score.nationalPoints) pt")
                    }
                }
                formula(tr("leaderboard.calc.result", fallback: "Risultato"), "") {
                    VStack(spacing: 6) {
                        MathText("R = \(score.communityPct)% · \(score.participation) + \(score.nationalPoints)")
                        MathText("= \(score.score) pt")
                    }
                }
                if let memberId = detail.memberId {
                    Section {
                        Button(tr("leaderboard.calc.open_profile", fallback: "Apri il profilo")) { onOpenMember(memberId) }
                    }
                }
            }
            .navigationTitle(detail.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { dismiss() } label: {
                        Label(tr("app.close", fallback: "Chiudi"), systemImage: "xmark")
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .preferredColorScheme(.dark)
    }

    /// A card like the info sheet's: title, the formula centred, the note under it.
    private func formula<F: View>(_ title: String, _ caption: String, @ViewBuilder _ content: () -> F) -> some View {
        Section {
            HStack(spacing: 0) { content() }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 6)
        } header: {
            Text(title)
        } footer: {
            mathCaption(caption)
        }
    }
}

/// The calculation as three steps, each a title, the number it produces and
/// the explanation: the region's events, the community that attends them,
/// the national events hosted. Written for an adult who has never seen the
/// formula. Mirrors Android `scoreSteps`.
private func scoreSteps(_ score: RegionScore) -> [(title: String, value: String, detail: String)] {
    let pts = { (n: Int32) in tr("leaderboard.points", fallback: "{points} pt", ["points": "\(n)"]) }
    return [
        (tr("leaderboard.calc.step1.title", fallback: "1 · Gli eventi in regione"),
         pts(score.participation),
         score.eventsTotal == 0
            ? tr("leaderboard.calc.step1.none", fallback: "Nessun evento in regione negli ultimi 12 mesi, quindi nessun punto da questa voce.")
            : tr("leaderboard.calc.step1.detail",
                 fallback: "Negli ultimi 12 mesi in regione si sono fatti {events} eventi, {attendance} timbri in tutto. Ogni evento vale fino a 10 punti secondo quanti soci della regione hanno timbrato: niente se erano meno del 2%, il massimo dal 15% in su. Ne hanno superato la soglia {scoring}; i punti calano col passare dei mesi e spariscono dopo un anno. Sommati fanno {points} punti.",
                 ["events": "\(score.eventsTotal)", "attendance": "\(score.attendanceTotal)",
                  "scoring": "\(score.eventsScoring)", "points": "\(score.participation)"])),
        (tr("leaderboard.calc.step2.title", fallback: "2 · Chi timbra"),
         "× \(score.communityPct)%",
         score.distinctAttendees == 0
            ? tr("leaderboard.calc.step2.none", fallback: "Senza timbri non c'è una comunità da misurare: il fattore è 0%.")
            : tr("leaderboard.calc.step2.detail",
                 fallback: "A quegli eventi hanno timbrato {distinct} soci diversi. Contando quanti timbri ha ognuno, equivalgono a {effective} soci fissi: il {core}% dei {members} soci della regione. Questa quota passa nella stessa curva degli eventi, 0% sotto il 2% dei soci e 100% dal 15%, e dà il fattore comunità: {community}%. I punti del passo 1 vengono moltiplicati per questo fattore, perché il punteggio premia l'ampiezza della comunità, non il numero di eventi.",
                 ["distinct": "\(score.distinctAttendees)", "effective": "\(score.effectiveAttendees)",
                  "core": "\(score.corePct)", "members": "\(score.membersActive)", "community": "\(score.communityPct)"])),
        (tr("leaderboard.calc.step3.title", fallback: "3 · I nazionali ospitati"),
         "+ " + pts(score.nationalPoints),
         score.nationalEvents == 0
            ? tr("leaderboard.calc.step3.none", fallback: "Nessun evento nazionale ospitato negli ultimi 24 mesi: niente da aggiungere.")
            : tr("leaderboard.calc.step3.detail",
                 fallback: "La regione ha ospitato {events} eventi nazionali negli ultimi 24 mesi, che valgono {points} punti con la stessa curva sui timbri. Si sommano a parte, senza il fattore del passo 2: un convegno porta gente da tutta Italia, non misura la comunità locale.",
                 ["events": "\(score.nationalEvents)", "points": "\(score.nationalPoints)"])),
    ]
}

private struct SecretaryAvatar: View {
    let row: SecretaryRow
    let size: CGFloat

    var body: some View {
        if let member = row.member {
            MemberAvatar(member: member, size: size)
        } else {
            Text(row.initials)
                .font(.system(size: size * 0.4, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
                .frame(width: size, height: size)
                .background(AppTheme.brandGradient, in: Circle())
        }
    }
}

// MARK: - The member's place

private struct MyPlace: View {
    let board: LeaderboardBoard
    let me: LeaderboardRow

    var body: some View {
        HStack(spacing: 14) {
            RankAvatar(row: me, size: 48)
            VStack(alignment: .leading, spacing: 0) {
                Text("\(me.position)°")
                    .font(.largeTitle.weight(.bold).monospacedDigit())
                Text(me.displayName)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 0) {
                Text("\(me.score)")
                    .font(.title2.weight(.semibold).monospacedDigit())
                Text(tr("leaderboard.points_label", fallback: "punti"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }

        if let gap = board.pointsToNext?.int32Value, let next = board.nextPosition?.int32Value {
            Label(tr("leaderboard.to_next", fallback: "Ti mancano {points} punti per salire al {position}° posto",
                     ["points": "\(gap)", "position": "\(next)"]),
                  systemImage: "chart.line.uptrend.xyaxis")
        } else {
            Label(tr("leaderboard.leading", fallback: "Sei in testa alla classifica"), systemImage: "trophy")
        }

        Breakdown(row: me)
    }
}

/// Where the points come from, as one stacked bar with the chart's legend.
private struct Breakdown: View {
    let row: LeaderboardRow

    private var parts: [(label: String, points: Int)] {
        [
            (tr("leaderboard.source.stamps", fallback: "Timbri"), Int(row.stampsPoints)),
            (tr("leaderboard.source.medals", fallback: "Medaglie"), Int(row.medalPoints)),
            (tr("leaderboard.source.events", fallback: "Eventi organizzati"), Int(row.eventsPoints)),
        ].filter { $0.points > 0 }
    }

    var body: some View {
        if !parts.isEmpty {
            Chart(parts, id: \.label) { part in
                BarMark(x: .value(tr("leaderboard.points_label", fallback: "punti"), part.points))
                    .foregroundStyle(by: .value("", "\(part.label) \(part.points)"))
            }
            .chartXAxis(.hidden)
            .chartLegend(position: .bottom, alignment: .leading)
            .frame(height: 56)
        }
    }
}

// MARK: - A row

private struct RankRow: View {
    let row: LeaderboardRow
    let onMember: (String) -> Void

    var body: some View {
        if row.member != nil, !row.isMe {
            Button { onMember(row.id) } label: {
                HStack {
                    content
                    Image(systemName: "chevron.right")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(.tertiary)
                }
            }
            .foregroundStyle(.primary)
        } else {
            content
        }
    }

    private var content: some View {
        HStack(spacing: 12) {
            Group {
                if let tier = row.podiumTier {
                    Image(systemName: "medal.fill")
                        .font(.title3)
                        .foregroundStyle(tier.metal.mid)
                } else {
                    Text("\(row.position)")
                        .font(.body.monospacedDigit())
                        .foregroundStyle(.secondary)
                }
            }
            .frame(width: 28)
            RankAvatar(row: row, size: 40)
            HStack(spacing: 6) {
                Text(row.displayName).lineLimit(1)
                if row.isMe {
                    Text(tr("leaderboard.you", fallback: "Tu"))
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.tint)
                }
            }
            Spacer()
            Text(tr("leaderboard.points", fallback: "{points} pt", ["points": "\(row.score)"]))
                .font(.subheadline.weight(.semibold).monospacedDigit())
        }
        .accessibilityElement(children: .combine)
    }
}
