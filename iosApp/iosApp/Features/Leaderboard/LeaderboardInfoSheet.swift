import Charts
import Shared
import SwiftUI

/// How the points are made, twice: in words for everyone, and as the
/// formulas `view_leaderboard` computes for whoever wants to check them.
/// The formulas mirror the SQL; change both together. A system sheet with a
/// segmented picker and grouped list, so it takes the platform's glass.
/// Mirrors Android `LeaderboardInfoSheet`.
struct LeaderboardInfoSheet: View {
    private enum Mode: Hashable { case basic, math }
    /// The rankings switched on: only their sections and theorems show.
    var kinds: [LeaderboardKind] = [.members, .regions, .secretaries]
    @State private var mode = Mode.basic

    private var members: Bool { kinds.contains(.members) }
    private var regions: Bool { kinds.contains(.regions) || kinds.contains(.secretaries) }
    private var secretaries: Bool { kinds.contains(.secretaries) }
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                switch mode {
                case .basic: basic
                case .math: math
                }
            }
            .navigationTitle(tr("leaderboard.how_it_works", fallback: "Come funziona"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    Picker(tr("leaderboard.how_it_works", fallback: "Come funziona"), selection: $mode) {
                        Text(tr("leaderboard.info.mode.basic", fallback: "Base")).tag(Mode.basic)
                        Text(tr("leaderboard.info.mode.math", fallback: "Matematica")).tag(Mode.math)
                    }
                    .pickerStyle(.segmented)
                    .frame(width: 240)
                }
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

    // MARK: - In words

    @ViewBuilder
    private var basic: some View {
        if members { Section(tr("leaderboard.tab.members", fallback: "Soci")) {
            point("seal", .blue,
                  tr("leaderboard.info.stamps.title", fallback: "Timbri"),
                  tr("leaderboard.info.stamps.detail_v6", fallback: "Ogni timbro vale fino a 20 punti. Vale tanto se all'evento hanno timbrato in tanti rispetto ai soci della regione, poco se in pochi. Ed è pesato per quanto è ampia, rispetto ai soci della regione, la comunità che timbra ai suoi eventi."))
            point("hourglass", .gray,
                  tr("leaderboard.info.time.title_v2", fallback: "Scadenza"),
                  tr("leaderboard.info.time.detail_v3", fallback: "I punti durano un anno e calano piano piano; due anni per gli eventi nazionali."))
            point("medal", .orange,
                  tr("leaderboard.info.medals.title", fallback: "Medaglie"),
                  tr("leaderboard.info.medals.detail_v5", fallback: "Ogni medaglia del Tableport aggiunge punti: più il metallo è prezioso, più punti. Contano solo i timbri che in classifica valgono almeno un punto."))
            point("trophy", .teal,
                  tr("leaderboard.info.events.title", fallback: "Eventi organizzati"),
                  tr("leaderboard.info.events.detail_v4", fallback: "Chi organizza prende fino a 10 punti a evento, con lo stesso criterio dei timbri. Chi fa parte dello staff di una regione non prende i timbri degli eventi di casa."))
            point("list.number", .indigo,
                  tr("leaderboard.info.top.title", fallback: "I primi 10"),
                  tr("leaderboard.info.top.detail_v3", fallback: "Si vedono i primi dieci. A pari punti, pari posizione."))
        } }
        if regions { Section(tr("leaderboard.tab.regions", fallback: "Regioni")) {
            point("map", .green,
                  tr("leaderboard.info.regions.score.title", fallback: "Punteggio"),
                  tr("leaderboard.info.regions.score.detail_v2", fallback: "Si sommano gli eventi fatti in regione nell'ultimo anno, da chiunque organizzati, fino a 10 punti l'uno. Il totale è poi pesato per quanto è ampia, rispetto ai soci della regione, la comunità che timbra ai suoi eventi. I nazionali ospitati si aggiungono a parte."))
            point("person.3", .purple,
                  tr("leaderboard.info.regions.who.title", fallback: "Chi c'è"),
                  tr("leaderboard.info.regions.who.detail_v2", fallback: "In classifica le regioni con almeno 3 punti. Chi fa parte dello staff di una regione vede anche la sua, sotto la lista, anche se è sotto i 3. A pari punti, pari posizione. Tocca una riga per vedere il conto, passo per passo."))
        } }
        if secretaries { Section(tr("leaderboard.tab.secretaries", fallback: "Segretari")) {
            point("person.text.rectangle", .pink,
                  tr("leaderboard.info.secretaries.title", fallback: "Come la regione"),
                  tr("leaderboard.info.secretaries.detail_v2", fallback: "Il segretario vale quanto la sua regione: stessi punti, stessa posizione. Dove i segretari sono due, pari merito."))
        } }
    }

    private func point(_ symbol: String, _ tint: Color, _ title: String, _ detail: String) -> some View {
        Label {
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.headline)
                Text(detail).font(.subheadline).foregroundStyle(.secondary)
            }
        } icon: {
            Image(systemName: symbol).foregroundStyle(tint)
        }
    }

    // MARK: - In formulas

    @ViewBuilder
    private var math: some View {
        // Laid out like a proof: definitions, lemmas, theorems, corollaries.
        formula(tr("leaderboard.math.definitions", fallback: "Definizioni"),
                tr("leaderboard.math.definitions.caption", fallback: "Sia $p$ il numero di soci che hanno preso il timbro di un evento, organizzatore escluso; $N_{r}$ i soci attivi della regione dell'evento, $N̄$ quelli di una regione media; $Δt$ i giorni trascorsi. $s$ è la quota di timbratori: l'esponente 0,7 perché l'affluenza cresce meno che linearmente con i soci. Le sedi che coprono due regioni contano come una; per gli eventi nazionali o senza regione $N_{r} = N̄$.")) {
            MathText("s = ")
            Fraction(numerator: "p", denominator: "N_{r}^{0,7} · N̄^{0,3}")
        }
        formula(tr("leaderboard.math.lemma1", fallback: "Lemma 1 · la curva"),
                tr("leaderboard.math.lemma1.caption", fallback: "$σ$ è nulla sotto il 2%, vale ½ all'8,5%, 1 dal 15%; continua e con derivata nulla alle soglie, quindi senza salti. Le soglie sono quote, non numeri di timbri: lo stesso evento pesa diversamente in regioni di dimensione diversa."),
                plot: true) {
            // Two lines: the curve, then its argument.
            VStack(spacing: 6) {
                MathText("σ(s) = t^{2}(3 − 2t)")
                HStack(spacing: 0) {
                    MathText("t = clamp(")
                    Fraction(numerator: "s − 2%", denominator: "13%")
                    MathText(", 0, 1)")
                }
            }
        }
        formula(tr("leaderboard.math.lemma2", fallback: "Lemma 2 · la comunità"),
                tr("leaderboard.math.lemma2.caption", fallback: "$n_{i}$ i timbri dell'$i$-esimo socio agli eventi regionali degli ultimi 12 mesi. $e$ è il numero di soci equivalenti: uguale ai timbratori se ognuno timbra una volta sola, tende a 1 se timbra sempre uno solo. La sua quota sulla regione passa nella stessa $σ$, quindi $c = 1$ dal 15% in su. Per i nazionali $c = 1$.")) {
            MathText("e = ")
            Fraction(numerator: "(Σ n_{i})^{2}", denominator: "Σ n_{i}^{2}")
            MathText(",  c = σ(")
            Fraction(numerator: "e", denominator: "N_{r}^{0,7} · N̄^{0,3}")
            MathText(")")
        }
        formula(tr("leaderboard.math.lemma3", fallback: "Lemma 3 · la scadenza"),
                tr("leaderboard.math.lemma3.caption", fallback: "Lineare; 730 al posto di 365 per gli eventi nazionali.")) {
            MathText("d = max(0, 1 − ")
            Fraction(numerator: "Δt", denominator: "365")
            MathText(")")
        }
        if members {
            formula(tr("leaderboard.math.thm1", fallback: "Teorema 1 · il timbro"),
                    tr("leaderboard.math.thm1.caption", fallback: "Con $s$, $d$ e $c$ dell'evento e della sua regione. $T = 0$ per chi fa parte dello staff della regione dell'evento.")) {
                MathText("T = 20 · d · σ(s) · c")
            }
        }
        formula(tr("leaderboard.math.thm2", fallback: "Teorema 2 · l'evento organizzato"),
                tr("leaderboard.math.thm2.caption", fallback: "Per chi lo organizza; $d$ dalla data dell'evento.")) {
            MathText("E = 10 · d · σ(s) · c")
        }
        if members {
            formula(tr("leaderboard.math.thm3", fallback: "Teorema 3 · le medaglie"),
                    tr("leaderboard.math.thm3.caption_v2", fallback: "Bronzo 3, argento 5, oro 10, platino 20; 8 per le medaglie a livello unico. Contano solo i timbri con $20 · σ(s) ≥ 1$, cioè $s$ oltre il 3,8%, a prescindere da $d$ e $c$.")) {
                MathText("M = Σ m,  m ∈ {3, 5, 8, 10, 20}")
            }
            formula(tr("leaderboard.math.cor1", fallback: "Corollario 1 · il socio"),
                    tr("leaderboard.math.cor1.caption", fallback: "Sui timbri e sugli eventi degli ultimi 12 mesi (24 i nazionali); ordinamento per $S$ arrotondato.")) {
                MathText("S = ΣT + ΣE + M")
            }
        }
        if regions {
            formula(tr("leaderboard.math.thm4", fallback: "Teorema 4 · la regione"),
                    tr("leaderboard.math.thm4.caption", fallback: "$E_{r}$ come nel Teorema 2 ma con $c = 1$, sugli eventi non nazionali degli ultimi 12 mesi nella regione, da chiunque organizzati; il fattore si applica una volta sola alla somma, ed è lo stesso: $Σ c·E = c·ΣE$. $E_{n}$ sui nazionali ospitati, $d$ a 24 mesi, senza $c$.")) {
                MathText("R = c · ΣE_{r} + ΣE_{n}")
            }
        }
        if secretaries {
            formula(tr("leaderboard.math.cor2", fallback: "Corollario 2 · il segretario"),
                    tr("leaderboard.math.cor2.caption_v2", fallback: "Della regione di cui è segretario: stessi punti, stessa posizione.")) {
                MathText("S_{seg} = R")
            }
        }
    }

    private func formula<F: View>(_ title: String, _ caption: String, plot: Bool = false,
                                  @ViewBuilder _ content: () -> F) -> some View {
        Section {
            HStack(spacing: 0) { content() }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 6)
            if plot { CurvePlot() }
        } header: {
            Text(title)
        } footer: {
            mathCaption(caption)
        }
    }
}

// MARK: - Formula pieces

let mathSize: CGFloat = 21

/// A formula set the way a book would.
struct MathText: View {
    let source: String
    var size: CGFloat = mathSize
    init(_ source: String, size: CGFloat = mathSize) {
        self.source = source
        self.size = size
    }

    var body: some View {
        typeset(source, size: size)
            .fixedSize()
    }
}

/// Numerator over denominator, with the bar as wide as the wider of the two.
struct Fraction: View {
    let numerator: String
    let denominator: String

    var body: some View {
        VStack(spacing: 2) {
            MathText(numerator, size: 18)
            Rectangle().fill(.primary).frame(height: 1.5)
            MathText(denominator, size: 18)
        }
        .fixedSize()
        .padding(.horizontal, 2)
    }
}

/// Single letters (variables) in italic, words (max, clamp), numbers and
/// the operators Σ and Δ upright, `^{…}` raised and `_{…}` lowered.
func typeset(_ source: String, size: CGFloat) -> Text {
    let chars = Array(source)
    var out = Text("")
    var i = 0
    func add(_ piece: Text) { out = Text("\(out)\(piece)") }
    while i < chars.count {
        let c = chars[i]
        if (c == "^" || c == "_"), i + 1 < chars.count, chars[i + 1] == "{",
           let close = chars[(i + 2)...].firstIndex(of: "}") {
            let inner = String(chars[(i + 2)..<close])
            add(typeset(inner, size: size * 0.62).baselineOffset(c == "^" ? size * 0.42 : -size * 0.18))
            i = close + 1
        } else if c == "Σ" || c == "Δ" {
            add(Text(String(c)).font(.system(size: size, design: .serif)))
            i += 1
        } else if c.isLetter {
            var j = i
            // A letter and its combining marks (N̄) are one character in Swift.
            while j < chars.count, chars[j].isLetter, chars[j] != "Σ", chars[j] != "Δ" { j += 1 }
            let word = String(chars[i..<j])
            let piece = Text(word).font(.system(size: size, design: .serif))
            add(word.count == 1 ? piece.italic() : piece)
            i = j
        } else {
            add(Text(String(c)).font(.system(size: size, design: .serif)))
            i += 1
        }
    }
    return out
}

/// Captions name the symbols between `$…$`, set like the formulas above.
func mathCaption(_ caption: String) -> Text {
    var out = Text("")
    for (index, part) in caption.split(separator: "$", omittingEmptySubsequences: false).enumerated() {
        let piece = index.isMultiple(of: 2)
            ? Text(String(part))
            : typeset(String(part), size: 14)
        out = Text("\(out)\(piece)")
    }
    return out
}

/// The stamp curve over s, the share of the region's members who stamped, from
/// 0 to 20%: nothing under 2%, everything from 15%, a smoothstep in between,
/// with the half-way point at 8.5% marked.
private struct CurvePlot: View {
    private let samples: [(n: Double, points: Double)] = stride(from: 0.0, through: 20.0, by: 0.25).map { s in
        let t = min(1, max(0, (s - 2) / 13))
        return (s, 20 * t * t * (3 - 2 * t))
    }

    var body: some View {
        Chart {
            ForEach(samples, id: \.n) { sample in
                AreaMark(x: .value("n", sample.n), y: .value("pt", sample.points))
                    .foregroundStyle(.tint.opacity(0.2))
                LineMark(x: .value("n", sample.n), y: .value("pt", sample.points))
                    .foregroundStyle(.tint)
            }
            RuleMark(x: .value("n", 8.5))
                .lineStyle(StrokeStyle(lineWidth: 1, dash: [4, 4]))
                .foregroundStyle(.secondary)
            PointMark(x: .value("n", 8.5), y: .value("pt", 10))
                .foregroundStyle(.tint)
        }
        .chartXAxis {
            AxisMarks(values: [0, 5, 10, 15, 20]) { value in
                AxisGridLine()
                AxisValueLabel { if let s = value.as(Int.self) { Text("\(s)%") } }
            }
        }
        .chartYAxis { AxisMarks(values: [0, 10, 20]) }
        .chartXAxisLabel(tr("leaderboard.math.plot.x_v4", fallback: "s, quota di soci della regione che hanno timbrato"))
        .chartYAxisLabel(tr("leaderboard.math.plot.y", fallback: "punti del timbro"))
        .frame(height: 150)
        .padding(.vertical, 6)
    }
}
