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

/// Apre il changelog sopra la Home quando `Changelog.shouldShow` lo chiede,
/// e lo segna come visto alla chiusura.
private struct ChangelogOnLaunchModifier: ViewModifier {
    @State private var presented = false

    func body(content: Content) -> some View {
        content
            .task {
                // Un attimo dopo la Home, così la scheda sale sopra
                // qualcosa di già disegnato invece che durante la transizione.
                try? await Task.sleep(nanoseconds: 600_000_000)
                if (try? await koin.changelog.shouldShow())?.boolValue == true {
                    presented = true
                }
            }
            .sheet(isPresented: $presented, onDismiss: { koin.changelog.markSeen() }) {
                ChangelogSheet(items: koin.changelog.items) {
                    presented = false
                }
            }
    }
}

extension View {
    func changelogOnLaunch() -> some View { modifier(ChangelogOnLaunchModifier()) }
}
