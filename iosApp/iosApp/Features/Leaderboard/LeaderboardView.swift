import Charts
import Shared
import SwiftUI

/// The members' ranking: where the member stands and how far the next place
/// is, then the first ten. System list and chart throughout, so it takes the
/// platform's look (Liquid Glass bars on iOS 26). Mirrors Android
/// `LeaderboardScreen`.
struct LeaderboardView: View {
    private static let podiumSize = 3
    /// Without a hinge, narrower than this the page is one column, as on a phone.
    private static let twoColumnMinWidth: CGFloat = 600
    /// Soci, regioni, segretari: one page, one switch at the top.
    private enum Board: Hashable { case members, regions, secretaries }

    @State private var store = LeaderboardStore.shared
    @State private var showInfo = false
    @State private var showSettings = false
    @State private var showHall = false
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
        GeometryReader { geo in
            if boards.contains(.members), let split = columnSplit(in: geo) {
                // Unfolded iPhone Duo, or a screen this wide: on the left the
                // podium and the member's place, sized to the column so it
                // never scrolls; on the right the switch and the rankings.
                // On a foldable the columns meet at the hinge, else halfway.
                HStack(alignment: .top, spacing: 0) {
                    podiumColumn
                        .frame(width: split.minX)
                        // The spotlights hang from the top of the screen.
                        .ignoresSafeArea(.container, edges: .top)
                    Color.clear.frame(width: split.width)
                    List {
                        picker
                        rankings
                    }
                }
                .modifier(SplitBar())
            } else {
                List {
                    picker
                    if current == .members { memberSummary }
                    rankings
                }
            }
        }
        .navigationTitle(tr("leaderboard.home.section", fallback: "Classifica"))
        .navigationDestination(item: $openMember) { id in
            MemberDetailView(memberId: id)
                .environment(\.colorScheme, appScheme)
        }
        .darkStage()
        .toolbar {
            // The closed seasons: next to the ⓘ, same style.
            ToolbarItem(placement: .topBarTrailing) {
                Button { showHall = true } label: {
                    Label(tr("leaderboard.hall.title", fallback: "Albo d'oro"), systemImage: "trophy")
                }
            }
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
        .sheet(isPresented: $showHall) { LeaderboardHallSheet(kinds: kinds) }
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

    /// Where the two columns part: the hinge of an unfolded foldable, a
    /// gutter down the middle of a wide screen, nil for one column.
    private func columnSplit(in geo: GeometryProxy) -> CGRect? {
        if let hinge = geo.verticalHinge { return hinge }
        guard geo.size.width >= Self.twoColumnMinWidth else { return nil }
        let gutter: CGFloat = 16
        return CGRect(x: (geo.size.width - gutter) / 2, y: 0, width: gutter, height: geo.size.height)
    }

    @ViewBuilder
    private var picker: some View {
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
    }

    @ViewBuilder
    private var rankings: some View {
        switch current {
        case .members: memberRanks
        case .regions: regions
        case .secretaries: secretaries
        case nil:
            Text(tr("leaderboard.disabled", fallback: "La classifica non è attiva."))
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
        }
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
                Text(seasonTitle)
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
                Text(seasonTitle)
            } footer: {
                Text(regionsFooter)
            }
        }
    }

    /// "Stagione 2026/27": the window the rankings count.
    private var seasonTitle: String {
        tr("leaderboard.season", fallback: "Stagione {season}", ["season": store.seasonLabel])
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

    /// The member's place and the podium: on top of the members' ranking on a
    /// phone, the whole left column on a wide screen.
    @ViewBuilder
    private var memberSummary: some View {
            if let board = store.board {
                Section(tr("leaderboard.my_place", fallback: "La tua posizione")) {
                    if let me = board.me {
                        MyPlace(board: board, me: me)
                    } else {
                        unranked
                    }
                }

                // The first three on the podium, the rest of the ten below.
                Section {
                    LeaderboardPodium(rows: Array(board.top.prefix(Self.podiumSize))) { openMember = $0 }
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 8, trailing: 16))
                } header: {
                    Text(tr("leaderboard.subtitle_top_v2", fallback: "Stagione {season} · i primi {count}",
                            ["season": store.seasonLabel, "count": "\(LeaderboardBoard.companion.TOP_SIZE)"]))
                }
            }
    }

    /// The left column on a wide screen: the podium on top, scaled to the
    /// height the member's place leaves it, and the member's place under it.
    @ViewBuilder
    private var podiumColumn: some View {
        if let board = store.board {
            VStack(spacing: 16) {
                FitHeight {
                    LeaderboardPodium(rows: Array(board.top.prefix(Self.podiumSize))) { openMember = $0 }
                }
                VStack(alignment: .leading, spacing: 12) {
                    if let me = board.me {
                        MyPlace(board: board, me: me, stacked: true)
                    } else {
                        unranked
                    }
                }
                .padding(16)
                .background(Color(.secondarySystemGroupedBackground),
                            in: .rect(cornerRadius: 26, style: .continuous))
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
        }
    }

    private var unranked: some View {
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

    /// The rest of the ten after the podium, and the member's own row.
    @ViewBuilder
    private var memberRanks: some View {
            if let board = store.board {
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
                    Text(tr("leaderboard.footer_v2", fallback: "Dal 1° ottobre al 30 settembre. A pari punti, pari posizione."))
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
    /// Places gained since yesterday's snapshot (negative = lost); nil when unknown.
    var trend: KotlinInt? = nil

    var body: some View {
        VStack(spacing: 1) {
            if let tier = podiumMetal(position: Int(position)) {
                Image(systemName: "medal.fill")
                    .font(.title3)
                    .foregroundStyle(tier.metal.mid)
            } else {
                Text("\(position)")
                    .font(.body.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
            Trend(trend: trend)
        }
        .frame(width: 28)
    }
}

/// "▲2" in green or "▼1" in red under the position; nothing when unchanged
/// or unknown. The server writes `previous_position` from its daily history.
private struct Trend: View {
    let trend: KotlinInt?

    var body: some View {
        if let t = trend?.int32Value, t != 0 {
            Text(t > 0 ? "▲\(t)" : "▼\(-t)")
                .font(.caption2.weight(.semibold).monospacedDigit())
                .foregroundStyle(t > 0 ? Color.green : Color.red)
        }
    }
}

private struct RegionRankRow: View {
    let row: RegionRow
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Place(position: row.position, trend: row.trend)
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
                Place(position: row.position, trend: row.trend)
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
            ? tr("leaderboard.calc.step1.none_v2", fallback: "Nessun evento in regione in questa stagione, quindi nessun punto da questa voce.")
            : tr("leaderboard.calc.step1.detail_v2", fallback: "In questa stagione in regione si sono fatti {events} eventi, {attendance} timbri in tutto. Ogni evento vale fino a 10 punti secondo quanti soci della regione hanno timbrato: niente se erano meno del 2%, il massimo dal 15% in su. Ne hanno superato la soglia {scoring}; i punti non calano, valgono per intero fino alla chiusura della stagione. Sommati fanno {points} punti.",
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
            ? tr("leaderboard.calc.step3.none_v2", fallback: "Nessun evento nazionale ospitato in questa stagione: niente da aggiungere.")
            : tr("leaderboard.calc.step3.detail_v2", fallback: "La regione ha ospitato {events} eventi nazionali in questa stagione, che valgono {points} punti con la stessa curva sui timbri. Si sommano a parte, senza il fattore del passo 2: un convegno porta gente da tutta Italia, non misura la comunità locale.",
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
    /// In a card of its own rather than as list rows: dividers between parts.
    var stacked = false

    var body: some View {
        if stacked {
            header
            Divider()
            toNext
            Divider()
            Breakdown(row: me)
        } else {
            header
            toNext
            Breakdown(row: me)
        }
    }

    private var header: some View {
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
    }

    @ViewBuilder
    private var toNext: some View {
        if let gap = board.pointsToNext?.int32Value, let next = board.nextPosition?.int32Value {
            Label(tr("leaderboard.to_next", fallback: "Ti mancano {points} punti per salire al {position}° posto",
                     ["points": "\(gap)", "position": "\(next)"]),
                  systemImage: "chart.line.uptrend.xyaxis")
        } else {
            Label(tr("leaderboard.leading", fallback: "Sei in testa alla classifica"), systemImage: "trophy")
        }
    }
}

/// Lays its content out at the given width and its own height, then scales
/// it to the height it is offered (up to `maxScale`), keeping it on top.
private struct FitHeight<Content: View>: View {
    var maxScale: CGFloat = 1.3
    @ViewBuilder let content: Content
    @State private var natural: CGFloat = 0

    var body: some View {
        GeometryReader { geo in
            let scale = natural > 0 ? min(geo.size.height / natural, maxScale) : 1
            content
                .frame(width: geo.size.width / scale)
                .fixedSize(horizontal: false, vertical: true)
                .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { natural = $0 }
                .scaleEffect(scale, anchor: .top)
                .frame(width: geo.size.width, height: geo.size.height, alignment: .top)
        }
    }
}

/// The bar over two columns: no title, which belongs to neither, and no
/// minimizing on scroll, which moved the column that wasn't scrolling.
private struct SplitBar: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 27.0, *) {
            content
                .toolbar(removing: .title)
                .toolbarMinimizationBehavior(.never, for: .navigationBar)
                .navigationBarTitleDisplayMode(.inline)
        } else if #available(iOS 18.0, *) {
            content
                .toolbar(removing: .title)
                .navigationBarTitleDisplayMode(.inline)
        } else {
            content.navigationBarTitleDisplayMode(.inline)
        }
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
            Place(position: row.position, trend: row.trend)
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
