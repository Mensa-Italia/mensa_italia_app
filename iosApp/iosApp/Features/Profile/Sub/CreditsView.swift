import SwiftUI

/// "About / Crediti" page — Apple "About" pattern (cf. Settings → Generali →
/// Info, or Music → Crediti). Hero with app mark + name + version, then
/// inset-grouped sections for the developer credit and a short colophon.
struct CreditsView: View {
    private var appVersion: String {
        let v = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let b = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(v) (\(b))"
    }

    private var copyrightYear: String {
        let f = DateFormatter()
        f.dateFormat = "yyyy"
        return f.string(from: Date())
    }

    var body: some View {
        List {
            // MARK: Hero
            Section {
                heroBlock
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
            }

            // MARK: Sviluppato da
            Section {
                LabeledContent {
                    Text("Matteo Sipione")
                        .foregroundStyle(.secondary)
                } label: {
                    Text(tr("app.credits.developed_by", fallback: "Sviluppato da")) // i18n
                }

                Link(destination: URL(string: "mailto:matteo@sipio.it")!) {
                    HStack {
                        Image(systemName: "envelope")
                            .frame(width: 28, height: 28)
                            .foregroundStyle(AppTheme.Colors.brandPrimary)
                        Text("matteo@sipio.it")
                            .foregroundStyle(.primary)
                        Spacer()
                        Image(systemName: "arrow.up.forward.app")
                            .font(.footnote)
                            .foregroundStyle(.tertiary)
                    }
                }
            } header: {
                Text(tr("app.credits.section.author", fallback: "Autore")) // i18n
            } footer: {
                Text(tr(
                    "app.credits.author_footer",
                    fallback: "App nativa iOS in SwiftUI sopra un core Kotlin Multiplatform condiviso con Android."
                )) // i18n
            }

            // MARK: Tech / colophon
            Section {
                creditRow(symbol: "swift",
                          label: tr("app.credits.tech.swiftui", fallback: "SwiftUI · Liquid Glass"))
                creditRow(symbol: "shippingbox",
                          label: tr("app.credits.tech.kmp", fallback: "Kotlin Multiplatform · Ktor · SQLDelight"))
                creditRow(symbol: "server.rack",
                          label: tr("app.credits.tech.pocketbase", fallback: "PocketBase backend"))
            } header: {
                Text(tr("app.credits.section.tech", fallback: "Tecnologia")) // i18n
            }

            // MARK: Copyright
            Section {
                EmptyView()
            } footer: {
                VStack(spacing: 4) {
                    Text("Mensa Italia")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(.secondary)
                    Text("© \(copyrightYear) · v\(appVersion)")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                        .monospacedDigit()
                    Text(tr("app.credits.made_with_love", fallback: "Fatta con cura in Italia")) // i18n
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 8)
            }
        }
        .navigationTitle(tr("app.credits.title", fallback: "Crediti")) // i18n
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Hero

    private var heroBlock: some View {
        VStack(spacing: 14) {
            // App mark — uses the bundled Mensa logo asset.
            ZStack {
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                AppTheme.Colors.brandPrimary,
                                AppTheme.Colors.brandSecondary,
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 120, height: 120)
                    .shadow(color: .black.opacity(0.18), radius: 14, y: 8)

                Image("MensaLogo")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .padding(22)
                    .frame(width: 120, height: 120)
                    .foregroundStyle(.white)
            }

            VStack(spacing: 4) {
                Text("Mensa Italia")
                    .font(.title2.weight(.bold))
                Text(tr("app.credits.tagline", fallback: "L'app ufficiale dei soci")) // i18n
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
    }

    // MARK: - Tech row helper

    private func creditRow(symbol: String, label: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .frame(width: 28, height: 28)
                .foregroundStyle(AppTheme.Colors.brandPrimary)
            Text(label)
                .foregroundStyle(.primary)
            Spacer()
        }
    }
}

#Preview {
    NavigationStack {
        CreditsView()
    }
}
