import SwiftUI
import Combine

/// Modifier che sovrappone il `MiniAudioPlayer` a una view qualsiasi quando
/// `AudioPlayerService.shared` ha una traccia attiva, e gestisce anche la
/// presentazione del full-screen player via sheet.
///
/// Usato in due posti:
///  - `MainTabView`: shell autenticata. Il player flotta sopra la floating
///    tab bar di iOS 26 (gap calcolato dinamicamente).
///  - `LoginView` / flusso pubblico: niente tab bar, il player flotta sopra
///    il bottom safe area direttamente. Permette di riprodurre un podcast
///    dall'area pubblica pre-login senza perderne il controllo navigando.
///
/// Lo state viene aggiornato via `onReceive` su `AudioPlayerService.shared.$currentTrack`
/// e `$isPresentingFullPlayer` invece che osservando direttamente il service:
/// il time observer di AVPlayer pubblica a 10 Hz e rerendererebbe l'intera
/// gerarchia troppo spesso, resettando lo stato dei NavigationStack figli.
struct MiniPlayerOverlay: ViewModifier {
    /// Distanza extra dal bottom oltre al safe area (es. per stare sopra la
    /// tab bar di iOS 26). 0 = appoggiato al bottom safe area.
    var extraBottomPadding: CGFloat = 0

    @State private var hasAudioTrack: Bool = false
    @State private var isPresentingFullPlayer: Bool = false

    func body(content: Content) -> some View {
        ZStack(alignment: .bottom) {
            content

            if hasAudioTrack {
                MiniAudioPlayer()
                    .padding(.horizontal, 12)
                    .padding(.bottom, extraBottomPadding)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.4, dampingFraction: 0.86), value: hasAudioTrack)
        .sheet(isPresented: $isPresentingFullPlayer) {
            NowPlayingFullScreenView()
                .presentationDetents([.large])
                .presentationDragIndicator(.hidden)
                .presentationBackground(Color.black)
                .presentationCornerRadius(28)
        }
        .onReceive(
            AudioPlayerService.shared.$currentTrack
                .map { $0 != nil }
                .removeDuplicates()
        ) { active in
            hasAudioTrack = active
        }
        .onReceive(
            AudioPlayerService.shared.$isPresentingFullPlayer.removeDuplicates()
        ) { presenting in
            if isPresentingFullPlayer != presenting {
                isPresentingFullPlayer = presenting
            }
        }
        .onChange(of: isPresentingFullPlayer) { _, newValue in
            if AudioPlayerService.shared.isPresentingFullPlayer != newValue {
                AudioPlayerService.shared.isPresentingFullPlayer = newValue
            }
        }
    }
}

extension View {
    /// Applica l'overlay del mini-player. Pass `extraBottomPadding` per
    /// alzarlo sopra una tab bar fluttuante (es. 60-90pt su iOS 26).
    func miniPlayerOverlay(extraBottomPadding: CGFloat = 0) -> some View {
        modifier(MiniPlayerOverlay(extraBottomPadding: extraBottomPadding))
    }
}
