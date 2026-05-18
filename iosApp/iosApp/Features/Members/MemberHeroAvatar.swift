import SwiftUI
import Shared

/// Hero avatar usato nel dettaglio socio. Differisce da `MemberAvatar`
/// (la cella lista) per il caricamento **progressivo**:
///
/// 1. mostra subito `?thumb=0x100` — la versione già scaricata da Spotlight
///    e dalla cella lista, quindi tipicamente già in cache → render istantaneo;
/// 2. in parallelo richiede `?thumb=0x500` — la versione "retina-grade" per il
///    hero a 120pt × 3× (≈360px);
/// 3. quando la 0x500 arriva, cross-fade in 350 ms — l'utente percepisce solo
///    una nitidezza che migliora, niente flash di placeholder.
///
/// Se PocketBase non ha configurato il thumb 0x500 (response 400), il layer
/// alto-risoluzione semplicemente non compare e resta visibile la 0x100 —
/// degradazione silente, niente bug visibile.
struct MemberHeroAvatar: View {
    let member: RegSociModel
    let size: CGFloat

    @State private var highResReady = false

    private static let lowResThumb = "0x100"
    private static let highResThumb = "0x500"
    private static let legacyMaleAvatar = "cloud32.it/Associazioni/img/Uomo-1.png"

    private func url(thumb: String) -> URL? {
        let raw = member.image
        guard !raw.isEmpty, !raw.contains(Self.legacyMaleAvatar) else { return nil }
        if raw.hasPrefix("http://") || raw.hasPrefix("https://") {
            return URL(string: raw)
        }
        return Files.url(
            collection: "members_registry",
            recordId: member.id,
            filename: raw,
            thumb: thumb
        )
    }

    var body: some View {
        Group {
            if let lowURL = url(thumb: Self.lowResThumb) {
                ZStack {
                    CachedAsyncImage(url: lowURL) { img in
                        img.resizable().scaledToFill()
                    } placeholder: {
                        initialBubble
                    }

                    if let highURL = url(thumb: Self.highResThumb) {
                        CachedAsyncImage(url: highURL) { img in
                            img.resizable().scaledToFill()
                                .onAppear {
                                    // L'onAppear scatta solo quando l'Image
                                    // viene effettivamente renderizzata
                                    // (cioè quando i bytes 0x500 sono pronti).
                                    // Se la richiesta fallisce, il content
                                    // closure non viene chiamato → niente
                                    // cross-fade, niente artefatti.
                                    withAnimation(.easeInOut(duration: 0.35)) {
                                        highResReady = true
                                    }
                                }
                        } placeholder: {
                            Color.clear
                        }
                        .opacity(highResReady ? 1 : 0)
                        .allowsHitTesting(false)
                    }
                }
            } else {
                initialBubble
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(Circle().stroke(.white.opacity(0.6), lineWidth: 1))
    }

    private var initials: String {
        let parts = member.name.split(separator: " ").prefix(2)
        let chars = parts.compactMap { $0.first }
        let result = String(chars).uppercased()
        return result.isEmpty ? "?" : result
    }

    private var initialBubble: some View {
        ZStack {
            AppTheme.brandGradient
            Text(initials)
                .font(.system(size: size * 0.4, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
        }
    }
}
