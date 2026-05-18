import SwiftUI
import Combine
import Shared

// MARK: - Swift sugar over the KMP-bridged `MensaAgeGroup` Kotlin enum.
// K/N bridges Kotlin enums to Swift as a *class* (not a Swift enum) — pattern
// matching with `case .y1617:` doesn't work. We switch on the `rawValue` Int
// instead. Also add `Identifiable` + a stable `allCases` since the bridge
// doesn't expose `CaseIterable`.
extension MensaAgeGroup: @retroactive Identifiable {
    public var id: Int32 { rawValue }

    var label: String {
        switch rawValue {
        case 1617: return "16–17 anni"
        case 1850: return "18–50 anni"
        case 5160: return "51–60 anni"
        case 6199: return "61–99 anni"
        default:   return ""
        }
    }

    static var allCases: [MensaAgeGroup] { [.y1617, .y1850, .y5160, .y6199] }
}

private enum TestPhase {
    case loading
    case ageGate
    case instructions
    case taking
    case submitting
    case result(MensaTestResult)
    case failed(String)
}

struct IQTestView: View {
    @State private var phase: TestPhase = .loading
    @State private var payload: MensaTestPayload?
    @State private var selectedAge: MensaAgeGroup = .y1850
    @State private var answers: [Int: Int] = [:]
    @State private var currentQuestion: Int = 0
    @State private var startedAt: Date = Date()
    @State private var secondsRemaining: Int = 1500
    @State private var timerSub: AnyCancellable?


    private var client: MensaTestClient { koin.mensaTest }

    var body: some View {
        Group {
            switch phase {
            case .loading:               loadingView
            case .ageGate:               ageGateView
            case .instructions:          instructionsView
            case .taking:                takingView
            case .submitting:            submittingView
            case .result(let result):    resultView(result)
            case .failed(let message):   failedView(message)
            }
        }
        .navigationTitle(tr("iqtest.title", fallback: "Test di prova"))
        .navigationBarTitleDisplayMode(.inline)
        .task { await startLoading() }
    }

    // MARK: - Loading

    private var loadingView: some View {
        ProgressView(tr("iqtest.loading", fallback: "Caricamento test in corso…"))
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Age Gate

    private var ageGateView: some View {
        Form {
            Section {
                ForEach(MensaAgeGroup.allCases) { group in
                    Button {
                        selectedAge = group
                    } label: {
                        HStack {
                            Text(group.label)
                                .foregroundStyle(.primary)
                            Spacer()
                            if selectedAge == group {
                                Image(systemName: "checkmark")
                                    .foregroundStyle(Color.accentColor)
                                    .font(.body.weight(.semibold))
                            }
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            } header: {
                Text(tr("iqtest.age.header", fallback: "Quanti anni hai?"))
            } footer: {
                Text(tr(
                    "iqtest.age.footer",
                    fallback: "Seleziona la tua fascia d'età per ottenere un risultato accurato."
                ))
            }

            Section {
                Button {
                    phase = .instructions
                } label: {
                    Text(tr("iqtest.cta.next", fallback: "Avanti"))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
            } footer: {
                Text(tr(
                    "iqtest.disclaimer.context",
                    fallback: "Test ufficiale di esempio realizzato da Mensa Norge. Mensa Italia ospita solo l'interfaccia: domande, calcolo del punteggio e percentile arrivano da test.mensa.no."
                ))
            }
        }
    }

    // MARK: - Instructions

    private var instructionsView: some View {
        Form {
            Section(tr("iqtest.howto.header", fallback: "Come funziona")) {
                Label(tr("iqtest.howto.questions", fallback: "35 domande figurative"),
                      systemImage: "square.grid.3x3")
                Label(tr("iqtest.howto.duration", fallback: "20–25 minuti di tempo"),
                      systemImage: "clock")
                Label(tr("iqtest.howto.no_math", fallback: "Nessuna matematica richiesta"),
                      systemImage: "function")
                Label(tr("iqtest.howto.tap_answer", fallback: "Tocca l'opzione che completa la matrice"),
                      systemImage: "hand.tap")
                Label(tr("iqtest.howto.skip", fallback: "Puoi lasciare domande senza risposta"),
                      systemImage: "checkmark.circle")
            }

            Section {
                Button {
                    startedAt = Date()
                    if let p = payload { secondsRemaining = Int(p.durationSeconds) }
                    answers = [:]
                    currentQuestion = 0
                    startTimer()
                    phase = .taking
                } label: {
                    Text(tr("iqtest.cta.start", fallback: "Inizia il test"))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
            } footer: {
                VStack(alignment: .leading, spacing: 6) {
                    Text(tr(
                        "iqtest.disclaimer.privacy",
                        fallback: "Le tue risposte vengono inviate direttamente a Mensa Norge, che calcola e restituisce il risultato. Mensa Italia non vede, non conserva e non elabora i dati del test."
                    ))
                    if let url = URL(string: "https://test.mensa.no/Home/Test/it") {
                        Link(
                            tr(
                                "iqtest.disclaimer.open_original",
                                fallback: "Preferisci farlo sul sito originale? Apri test.mensa.no"
                            ),
                            destination: url
                        )
                        .font(.footnote.weight(.medium))
                    }
                }
            }
        }
    }

    // MARK: - Taking
    //
    // Questa fase mantiene un layout custom (immagine grande + griglia di
    // risposte) perché è il cuore del test e non si mappa su una `List`.
    // Usa solo `.thinMaterial` / `.ultraThinMaterial`, che SONO API native.

    @ViewBuilder
    private var takingView: some View {
        if let p = payload, currentQuestion < p.questions.count {
            let question = p.questions[currentQuestion]
            VStack(spacing: 12) {
                testTopBar(total: p.questions.count)
                    .padding(.horizontal, 16)

                // Matrice principale — quadrata, prende lo spazio massimo
                // disponibile mantenendo l'aspetto 1:1. `layoutPriority(1)` la
                // fa "vincere" se serve cedere spazio.
                questionImageView(url: URL(string: question.imageUrl))
                    .aspectRatio(1, contentMode: .fit)
                    .frame(maxHeight: .infinity)
                    .padding(.horizontal, 16)
                    .layoutPriority(1)

                // Griglia 3×2 — ogni cella mantiene l'aspetto 1:1 così non si
                // schiaccia su schermi piccoli. La griglia è lasciata libera
                // di scegliere la propria altezza in base alla larghezza.
                answerGrid(question: question)
                    .padding(.horizontal, 16)
            }
            .padding(.top, 8)
            .id(currentQuestion)
            .animation(.easeInOut(duration: 0.25), value: currentQuestion)
            .sensoryFeedback(.impact(flexibility: .soft), trigger: currentQuestion)
            .safeAreaInset(edge: .bottom) {
                navigationBar(payload: p)
            }
        }
    }

    private func testTopBar(total: Int) -> some View {
        let isLowTime = secondsRemaining < 60
        return HStack {
            Label(timeString(secondsRemaining), systemImage: "clock")
                .font(.subheadline.monospacedDigit())
                .foregroundStyle(isLowTime ? .red : .primary)
            Spacer()
            Text("\(currentQuestion + 1) / \(total)")
                .font(.subheadline.monospacedDigit())
                .foregroundStyle(.secondary)
        }
    }

    private func navigationBar(payload p: MensaTestPayload) -> some View {
        let isLast = currentQuestion == p.questions.count - 1
        return HStack {
            Button {
                if currentQuestion > 0 {
                    withAnimation(.easeInOut(duration: 0.2)) { currentQuestion -= 1 }
                }
            } label: {
                Image(systemName: "chevron.left")
            }
            .buttonStyle(.bordered)
            .disabled(currentQuestion == 0)

            Spacer()

            Button {
                if isLast {
                    Task { await doSubmit(payload: p) }
                } else {
                    withAnimation(.easeInOut(duration: 0.2)) { currentQuestion += 1 }
                }
            } label: {
                Label(
                    isLast
                        ? tr("iqtest.cta.submit", fallback: "Concludi")
                        : tr("iqtest.cta.next", fallback: "Avanti"),
                    systemImage: isLast ? "checkmark" : "chevron.right"
                )
                .labelStyle(.titleAndIcon)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(.bar)
    }

    private func questionImageView(url: URL?) -> some View {
        AsyncImage(url: url) { phase in
            switch phase {
            case .empty:
                ProgressView()
                    .frame(maxWidth: .infinity, minHeight: 200)
            case .success(let image):
                adaptiveMonochrome(image)
            case .failure:
                Image(systemName: "photo")
                    .font(.system(size: 48))
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, minHeight: 200)
            @unknown default:
                EmptyView()
            }
        }
        .frame(maxWidth: .infinity)
        .padding(8)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    /// Tratta una PNG monocroma (nero su bianco) come un glifo: il disegno
    /// nero diventa una maschera di luminanza riempita con `Color.primary`,
    /// lo sfondo bianco diventa trasparente.
    ///
    /// Pipeline:
    ///   1. `colorInvert()` — nero↔bianco, ora il disegno e' bianco.
    ///   2. `luminanceToAlpha()` — luminanza alta (disegno) -> alpha 1,
    ///       luminanza zero (sfondo) -> alpha 0. Ora abbiamo una maschera.
    ///   3. `Color.primary.mask(...)` — riempie la maschera col colore
    ///       primary di sistema (nero in light, bianco in dark).
    ///
    /// L'immagine `hidden()` di base serve solo da sizer per dare il
    /// frame corretto all'overlay (l'aspect ratio dell'immagine originale).
    @ViewBuilder
    private func adaptiveMonochrome(_ image: Image) -> some View {
        image
            .resizable()
            .aspectRatio(contentMode: .fit)
            .hidden()
            .overlay {
                Color.primary
                    .mask {
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .colorInvert()
                            .luminanceToAlpha()
                    }
            }
    }

    private func answerGrid(question: MensaTestQuestion) -> some View {
        let cols = [GridItem(.flexible(), spacing: 6),
                    GridItem(.flexible(), spacing: 6),
                    GridItem(.flexible(), spacing: 6)]
        return LazyVGrid(columns: cols, spacing: 6) {
            ForEach(Array(question.options.enumerated()), id: \.offset) { idx, optURLString in
                let isSelected = answers[currentQuestion] == idx
                Button {
                    answers[currentQuestion] = idx
                } label: {
                    AsyncImage(url: URL(string: optURLString)) { imgPhase in
                        switch imgPhase {
                        case .empty:
                            ProgressView()
                        case .success(let image):
                            adaptiveMonochrome(image)
                        case .failure:
                            Image(systemName: "photo")
                                .font(.title2)
                                .foregroundStyle(.secondary)
                        @unknown default:
                            EmptyView()
                        }
                    }
                    .padding(6)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .aspectRatio(1, contentMode: .fit)
                    .background(
                        Color(.secondarySystemGroupedBackground),
                        in: RoundedRectangle(cornerRadius: 10)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .strokeBorder(isSelected ? Color.accentColor : Color.clear, lineWidth: 3)
                    )
                }
                .buttonStyle(.plain)
            }
        }
        .sensoryFeedback(.selection, trigger: answers[currentQuestion])
    }

    // MARK: - Submitting

    private var submittingView: some View {
        ProgressView(tr("iqtest.submitting", fallback: "Calcolo del risultato…"))
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Result

    private func resultView(_ result: MensaTestResult) -> some View {
        Form {
            Section {
                VStack(spacing: 6) {
                    Text(tr("iqtest.result.iq_label", fallback: "Quoziente Intellettivo"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    if let iq = result.iq?.intValue {
                        Text("\(iq)")
                            .font(.system(size: 64, weight: .semibold, design: .rounded))
                            .monospacedDigit()
                            .contentTransition(.numericText())
                    } else {
                        Text("-")
                            .font(.system(size: 64, weight: .semibold, design: .rounded))
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            }

            if let percentile = result.percentile?.intValue, percentile > 0 {
                Section(tr("iqtest.result.percentile_section", fallback: "Percentile")) {
                    let suffix = (result.orMore?.boolValue ?? false) ? "° o superiore" : "°"
                    LabeledContent(tr("iqtest.result.your_percentile", fallback: "Tuo percentile")) {
                        Text("\(percentile)\(suffix)")
                            .monospacedDigit()
                    }
                    ProgressView(value: Double(min(max(percentile, 0), 100)) / 100.0)
                        .tint(Color.accentColor)
                }
            }

            Section {
                Button {
                    Task {
                        phase = .loading
                        await startLoading()
                    }
                } label: {
                    Text(tr("iqtest.cta.retry", fallback: "Ripeti il test"))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
            } footer: {
                Text(tr(
                    "iqtest.result.footer",
                    fallback: "Test ufficiale di esempio fornito da Mensa Norge. L'app fa da contenitore grafico; punteggio e domande arrivano da test.mensa.no."
                ))
            }
        }
    }

    // MARK: - Failed

    private func failedView(_ message: String) -> some View {
        ContentUnavailableView {
            Label(tr("iqtest.failed.title", fallback: "Qualcosa è andato storto"),
                  systemImage: "exclamationmark.triangle")
        } description: {
            Text(message)
        } actions: {
            Button(tr("common.retry", fallback: "Riprova")) {
                Task {
                    phase = .loading
                    await startLoading()
                }
            }
            .buttonStyle(.borderedProminent)
        }
    }

    // MARK: - Business Logic

    private func startLoading() async {
        phase = .loading
        do {
            let p = try await client.loadTest()
            payload = p
            secondsRemaining = Int(p.durationSeconds)
            phase = .ageGate
        } catch {
            phase = .failed(error.localizedDescription)
        }
    }

    private func startTimer() {
        timerSub?.cancel()
        timerSub = Timer.publish(every: 1, on: .main, in: .common)
            .autoconnect()
            .sink { _ in
                if secondsRemaining > 0 {
                    secondsRemaining -= 1
                } else {
                    timerSub?.cancel()
                    Task { await doSubmit(payload: payload!) }
                }
            }
    }

    private func doSubmit(payload p: MensaTestPayload) async {
        timerSub?.cancel()
        let finishedAt = Date()
        phase = .submitting
        do {
            var stringKeyed: [String: KotlinInt] = [:]
            for (k, v) in answers {
                stringKeyed[String(k)] = KotlinInt(int: Int32(v))
            }
            let startInstant = Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
                epochMilliseconds: Int64(startedAt.timeIntervalSince1970 * 1000)
            )
            let endInstant = Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
                epochMilliseconds: Int64(finishedAt.timeIntervalSince1970 * 1000)
            )
            let result = try await client.submit(
                payload: p,
                answers: stringKeyed,
                ageGroup: selectedAge,
                startedAt: startInstant,
                finishedAt: endInstant
            )
            phase = .result(result)
        } catch {
            phase = .failed(error.localizedDescription)
        }
    }

    private func timeString(_ seconds: Int) -> String {
        let m = seconds / 60
        let s = seconds % 60
        return String(format: "%02d:%02d", m, s)
    }
}
