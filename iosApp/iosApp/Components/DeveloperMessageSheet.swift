import SwiftUI
import MarkdownUI

/// Il messaggio degli sviluppatori (`developer_message` in `configs`), in
/// markdown. Non si chiude e "Avanti" resta spento finché non lo si è
/// scorso fino in fondo: è il modo di essere sicuri che sia stato letto.
/// Quando mostrarlo lo decide `DeveloperMessage` (:shared). Mirrors Android
/// `DeveloperMessageSheet`.
struct DeveloperMessageSheet: View {
    let message: String
    let onDone: () -> Void

    @State private var reachedEnd = false

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: 10) {
                Image(systemName: "megaphone.fill")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                    .frame(width: 44, height: 44)
                    .background(AppTheme.Colors.brandTintAdaptive.opacity(0.12), in: Circle())
                Text(tr("developer_message.title", fallback: "Un messaggio dagli sviluppatori"))
                    .font(.headline)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 24)
            .padding(.horizontal, 24)
            .padding(.bottom, 14)

            Divider()

            GeometryReader { container in
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        Markdown(message)
                            .markdownTheme(.developerMessage)
                            .textSelection(.enabled)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 20)
                        // Il fondo del testo: quando entra nella vista, il
                        // messaggio è stato scorso tutto. Con un testo corto
                        // ci entra subito.
                        GeometryReader { marker in
                            Color.clear.preference(
                                key: EndOfMessageKey.self,
                                value: marker.frame(in: .named(Self.scrollSpace)).minY
                            )
                        }
                        .frame(height: 1)
                    }
                }
                .coordinateSpace(name: Self.scrollSpace)
                .onPreferenceChange(EndOfMessageKey.self) { endY in
                    if endY <= container.size.height + 8 { reachedEnd = true }
                }
            }

            Divider()

            VStack(spacing: 10) {
                if !reachedEnd {
                    Label(tr("developer_message.scroll_hint", fallback: "Scorri fino in fondo per continuare"),
                          systemImage: "arrow.down")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .transition(.opacity)
                }
                Button(action: onDone) {
                    Text(tr("developer_message.continue", fallback: "Avanti"))
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                }
                .compatGlassProminentButtonStyle()
                .tint(AppTheme.Colors.mensaBlue)
                .disabled(!reachedEnd)
            }
            .padding(.horizontal, 24)
            .padding(.top, 14)
            .padding(.bottom, 20)
            .animation(.easeOut(duration: 0.2), value: reachedEnd)
        }
        .interactiveDismissDisabled()
        .presentationDetents([.large])
    }

    private static let scrollSpace = "developer-message-scroll"
}

private struct EndOfMessageKey: PreferenceKey {
    static let defaultValue: CGFloat = .infinity
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

extension Theme {
    /// Il markdown del messaggio: quello delle chat, non quello di un
    /// documento. Titoli poco piu' grandi del testo (in `.basic` un `#`
    /// prendeva mezzo schermo), interlinea larga, link e barra delle citazioni
    /// nel blu del brand, codice su fondo grigio. Mirrors Android
    /// `developerMessageTypography`.
    static let developerMessage = Theme()
        .text {
            ForegroundColor(.primary)
            FontSize(16)
        }
        .code {
            FontFamilyVariant(.monospaced)
            FontSize(.em(0.88))
            BackgroundColor(Color(.tertiarySystemFill))
        }
        .strong {
            FontWeight(.semibold)
        }
        .link {
            ForegroundColor(AppTheme.Colors.brandTintAdaptive)
        }
        .heading1 { configuration in
            configuration.label
                .relativeLineSpacing(.em(0.1))
                .markdownMargin(top: 8, bottom: 12)
                .markdownTextStyle {
                    FontWeight(.bold)
                    FontSize(.em(1.4))
                }
        }
        .heading2 { configuration in
            configuration.label
                .relativeLineSpacing(.em(0.1))
                .markdownMargin(top: 20, bottom: 8)
                .markdownTextStyle {
                    FontWeight(.semibold)
                    FontSize(.em(1.2))
                }
        }
        .heading3 { configuration in
            configuration.label
                .markdownMargin(top: 16, bottom: 6)
                .markdownTextStyle {
                    FontWeight(.semibold)
                    FontSize(.em(1.05))
                }
        }
        .paragraph { configuration in
            configuration.label
                .fixedSize(horizontal: false, vertical: true)
                .relativeLineSpacing(.em(0.3))
                .markdownMargin(top: 0, bottom: 12)
        }
        .listItem { configuration in
            configuration.label
                .markdownMargin(top: .em(0.3))
        }
        // I marcatori di serie stanno in 1,5 em allineati a destra: il testo
        // degli elenchi partiva molto rientrato. Qui pallino blu a sinistra.
        .bulletedListMarker { configuration in
            Text(configuration.listLevel == 1 ? "•" : "◦")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                .relativeFrame(minWidth: .em(1), alignment: .leading)
        }
        .numberedListMarker { configuration in
            Text("\(configuration.itemNumber).")
                .monospacedDigit()
                .foregroundStyle(.secondary)
                .relativeFrame(minWidth: .em(1.4), alignment: .leading)
        }
        .blockquote { configuration in
            HStack(spacing: 0) {
                RoundedRectangle(cornerRadius: 2)
                    .fill(AppTheme.Colors.brandTintAdaptive)
                    .frame(width: 3)
                configuration.label
                    .markdownTextStyle { ForegroundColor(.secondary) }
                    .padding(.leading, 14)
            }
            .fixedSize(horizontal: false, vertical: true)
            .markdownMargin(top: 4, bottom: 12)
        }
        .codeBlock { configuration in
            ScrollView(.horizontal, showsIndicators: false) {
                configuration.label
                    .fixedSize(horizontal: false, vertical: true)
                    .relativeLineSpacing(.em(0.25))
                    .markdownTextStyle {
                        FontFamilyVariant(.monospaced)
                        FontSize(.em(0.85))
                    }
                    .padding(14)
            }
            .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .markdownMargin(top: 4, bottom: 12)
        }
        .thematicBreak {
            Divider()
                .markdownMargin(top: 12, bottom: 20)
        }
}
