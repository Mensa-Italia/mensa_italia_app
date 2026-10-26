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
            Text(tr("developer_message.title", fallback: "Un messaggio dagli sviluppatori"))
                .font(.title3.bold())
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, 28)
                .padding(.horizontal, 24)
                .padding(.bottom, 12)

            Divider()

            GeometryReader { container in
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        Markdown(message)
                            .markdownTheme(.basic)
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
