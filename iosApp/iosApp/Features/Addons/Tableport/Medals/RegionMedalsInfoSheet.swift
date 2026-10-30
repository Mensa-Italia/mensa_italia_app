import SwiftUI

/// How the region's medals are earned, twice: in words, and as the formulas
/// behind the shares for whoever wants to check them. The formulas mirror
/// the server's calculation; change both together. Same sheet as the
/// leaderboard's (a segmented picker and grouped list, so it takes the
/// platform's glass). Mirrors Android `RegionMedalsInfoSheet`.
struct RegionMedalsInfoSheet: View {
    private enum Mode: Hashable { case basic, math }
    @State private var mode = Mode.basic
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
        Section(tr("medals.region.info.general", fallback: "In breve")) {
            point("person.3", .blue,
                  tr("medals.region.info.team.title", fallback: "La squadra"),
                  tr("medals.region.rules.team", fallback: "Le medaglie di regione sono della squadra: le conquista la regione tutta insieme e le vede chi ne fa parte oggi."))
            point("calendar", .orange,
                  tr("medals.region.info.yearly.title", fallback: "Ogni anno"),
                  tr("medals.region.rules.yearly", fallback: "Ogni anno si ricomincia; le annate passate restano in bacheca."))
            point("person", .teal,
                  tr("medals.region.info.people.title", fallback: "Le persone"),
                  tr("medals.region.rules.people", fallback: "Contano le persone, non i timbri: chi si vede una volta sola nell'anno non fa numero."))
        }
        Section(tr("medals.region.info.medals", fallback: "Le dieci medaglie")) {
            ForEach(RegionMedalCatalog.order, id: \.self) { key in
                point(RegionMedalCatalog.info[key]?.symbol ?? "rosette", .gray,
                      RegionMedalCatalog.title(key), RegionMedalCatalog.rule(key))
            }
        }
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
        formula(tr("medals.region.math.definitions", fallback: "Definizioni"),
                tr("medals.region.math.definitions.caption", fallback: "Sia $v$ il numero di persone che la medaglia conta; $N_{r}$ i soci attivi del registro nella regione, $N̄$ quelli di una regione media. $q$ è la quota efficace: l'esponente 0,7 perché l'affluenza cresce meno che linearmente con i soci, quindi una regione più grande chiede più persone, ma meno che in proporzione. Le sedi che coprono due regioni contano come una.")) {
            MathText("q = ")
            Fraction(numerator: "v", denominator: "N_{r}^{0,7} · N̄^{0,3}")
        }
        formula(tr("medals.region.math.lemma", fallback: "Lemma · i soci effettivi"),
                tr("medals.region.math.lemma.caption", fallback: "$n_{i}$ i timbri dell'$i$-esimo socio agli eventi della regione nell'anno. $e$ è uguale ai soci presenti se ognuno timbra una volta sola, e tende a 1 se timbra sempre uno solo: conta chi c'è spesso, poco chi passa una volta.")) {
            MathText("e = ")
            Fraction(numerator: "(Σ n_{i})^{2}", denominator: "Σ n_{i}^{2}")
        }
        formula(tr("medals.region.math.thm1", fallback: "Teorema 1 · Sala piena e Zoccolo duro"),
                tr("medals.region.math.thm1.caption", fallback: "Bronzo, argento, oro. Per Sala piena $v$ sono i timbratori dell'evento migliore dell'anno; per Zoccolo duro $v = e$, sugli eventi della regione.")) {
            MathText("q ≥ 8,5% · 15% · 25%")
        }
        formula(tr("medals.region.math.thm2", fallback: "Teorema 2 · Tutti presenti"),
                tr("medals.region.math.thm2.caption", fallback: "Bronzo, argento, oro. $v$ sono i soci visti ad almeno due eventi diversi nell'anno, in tutta Italia: chi compare una volta sola non entra.")) {
            MathText("q ≥ 5% · 10% · 15%")
        }
        formula(tr("medals.region.math.thm3", fallback: "Teorema 3 · Benvenuto"),
                tr("medals.region.math.thm3.caption", fallback: "$n_{i}$ i soci della regione iscritti all'app nell'anno, $n_{t}$ quelli con il primo timbro entro due mesi. Qui la quota è semplice, senza l'esponente.")) {
            MathText("b = ")
            Fraction(numerator: "n_{t}", denominator: "n_{i}")
            MathText(" ≥ 25% · 40% · 60%")
        }
        formula(tr("medals.region.math.thm4", fallback: "Teorema 4 · i conteggi"),
                tr("medals.region.math.thm4.caption", fallback: "Casa aperta 2 · 4 · 6 regioni. In trasferta 2 · 4 · 6 regioni. Senza pause 6 · 9 · 12 mesi. Padroni di casa 1 · 2 · 3 eventi. Staffetta 2 · 3 · 4 persone. Podio: 3°, 2°, 1° posto al 31 dicembre.")) {
            MathText("c ≥ c_{bronzo}, c_{argento}, c_{oro}")
        }
    }

    private func formula<F: View>(_ title: String, _ caption: String,
                                  @ViewBuilder _ content: () -> F) -> some View {
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
