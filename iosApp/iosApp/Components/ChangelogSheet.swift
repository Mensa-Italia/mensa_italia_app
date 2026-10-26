import SwiftUI
import Shared

/// "Novità": cosa c'è di nuovo in questa versione, una volta sola.
///
/// Le regole (quale versione, a chi, quante volte) stanno in `Changelog`
/// (:shared); qui c'è solo la presentazione. Mirrors Android `ChangelogSheet`.
struct ChangelogSheet: View {
    let items: [ChangelogItem]
    let onDone: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 28) {
                    Text(tr("changelog.title", fallback: "Novità"))
                        .font(.largeTitle.bold())
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.top, 48)
                        .padding(.bottom, 8)

                    ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                        row(item)
                    }
                }
                .padding(.horizontal, 32)
                .padding(.bottom, 24)
            }

            Button(action: onDone) {
                Text(tr("changelog.done", fallback: "Continua"))
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
            }
            .compatGlassProminentButtonStyle()
            .tint(AppTheme.Colors.mensaBlue)
            .padding(.horizontal, 32)
            .padding(.bottom, 20)
        }
        .interactiveDismissDisabled()
    }

    private func row(_ item: ChangelogItem) -> some View {
        let style = Self.style(for: item.icon)
        return HStack(alignment: .top, spacing: 18) {
            Image(systemName: style.symbol)
                .font(.system(size: 30, weight: .semibold))
                .foregroundStyle(style.color)
                .frame(width: 44)
            VStack(alignment: .leading, spacing: 3) {
                Text(tr(item.titleKey, fallback: item.titleFallback))
                    .font(.headline)
                Text(tr(item.bodyKey, fallback: item.bodyFallback))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private static func style(for icon: String) -> (symbol: String, color: Color) {
        switch icon {
        case "passport": return ("book.closed.fill", .green)
        case "medal": return ("medal.fill", .orange)
        case "qr": return ("qrcode.viewfinder", .blue)
        case "bolt": return ("bolt.fill", .yellow)
        case "bell": return ("bell.badge.fill", .red)
        case "fixes": return ("wrench.and.screwdriver.fill", .gray)
        default: return ("sparkles", AppTheme.Colors.mensaBlue)
        }
    }
}

/// Le schede dell'apertura sopra la Home, una alla volta: prima il messaggio
/// degli sviluppatori (`DeveloperMessage.pending`), poi il changelog
/// (`Changelog.shouldShow`). Ognuna si segna come vista alla chiusura.
private struct LaunchNoticesModifier: ViewModifier {
    /// Con `sheet(item:)` il testo viaggia con la presentazione: con
    /// `isPresented` la scheda leggeva il valore di prima ed era vuota.
    private struct PendingMessage: Identifiable {
        let text: String
        var id: String { text }
    }

    @State private var developerMessage: PendingMessage?
    @State private var showChangelog = false
    @Environment(\.scenePhase) private var scenePhase

    func body(content: Content) -> some View {
        content
            .task {
                // Un attimo dopo la Home, così la scheda sale sopra
                // qualcosa di già disegnato invece che durante la transizione.
                try? await Task.sleep(nanoseconds: 600_000_000)
                if let message = try? await koin.developerMessage.pending() {
                    developerMessage = PendingMessage(text: message)
                } else {
                    await presentChangelogIfNeeded()
                }
            }
            // Anche al ritorno in primo piano: un messaggio cambiato mentre
            // l'app era aperta si vede subito, senza chiuderla del tutto.
            .onChange(of: scenePhase) { _, phase in
                guard phase == .active, developerMessage == nil, !showChangelog else { return }
                Task {
                    if let message = try? await koin.developerMessage.pending() {
                        developerMessage = PendingMessage(text: message)
                    }
                }
            }
            .sheet(item: $developerMessage, onDismiss: {
                // Il changelog dopo, a scheda chiusa: due insieme non salgono.
                Task { await presentChangelogIfNeeded() }
            }) { pending in
                DeveloperMessageSheet(message: pending.text) {
                    Task { try? await koin.developerMessage.markSeen(message: pending.text) }
                    developerMessage = nil
                }
            }
            .sheet(isPresented: $showChangelog, onDismiss: { koin.changelog.markSeen() }) {
                ChangelogSheet(items: koin.changelog.items) {
                    showChangelog = false
                }
            }
    }

    private func presentChangelogIfNeeded() async {
        if (try? await koin.changelog.shouldShow())?.boolValue == true {
            showChangelog = true
        }
    }
}

extension View {
    func launchNotices() -> some View { modifier(LaunchNoticesModifier()) }
}
