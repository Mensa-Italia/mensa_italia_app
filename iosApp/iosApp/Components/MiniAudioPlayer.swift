import SwiftUI
import Combine

/// Slim Combine-driven mirror of `AudioPlayerService` exposing only the
/// coarse-grained state the button row reads: current track, isPlaying,
/// queue size, hasNext, hasPrevious and playback speed. We deliberately do
/// NOT republish `progress` or `currentTime` — those tick at 10 Hz from the
/// time observer and would otherwise force the button row to re-render
/// constantly, killing tap responsiveness (the full-screen player reads it
/// for the same reason). The thin progress hairline observes the service directly
/// in its own sibling view so its high-frequency re-renders don't ripple
/// up to the buttons.
@MainActor
final class MiniPlayerObserver: ObservableObject {
    @Published private(set) var currentTrack: AudioTrack?
    @Published private(set) var isPlaying: Bool = false
    @Published private(set) var queueCount: Int = 0
    @Published private(set) var hasNext: Bool = false
    @Published private(set) var hasPrevious: Bool = false
    @Published private(set) var playbackRate: Float = 1

    init() {
        let svc = AudioPlayerService.shared
        svc.$currentTrack.removeDuplicates().assign(to: &$currentTrack)
        svc.$isPlaying.removeDuplicates().assign(to: &$isPlaying)
        svc.$queue.map { $0.count }.removeDuplicates().assign(to: &$queueCount)
        Publishers.CombineLatest(svc.$queue, svc.$currentIndex)
            .map { queue, idx in idx < queue.count - 1 }
            .removeDuplicates()
            .assign(to: &$hasNext)
        svc.$currentIndex.map { $0 > 0 }.removeDuplicates().assign(to: &$hasPrevious)
        svc.$playbackRate.removeDuplicates().assign(to: &$playbackRate)
    }
}

/// Apple Music–style mini-player.
///
/// Inside the tab bar's bottom accessory (`inAccessory`, iOS 26.1+) it's only
/// foreground content: the system draws the Liquid Glass and places it the
/// way Apple Music's is — under the tab bar on a phone, beside the tab bar
/// when it collapses on scroll, and along iPhone Duo's vertical bar. There
/// it follows `tabViewBottomAccessoryPlacement`: `.inline` (sharing the row
/// with the collapsed tab bar) shows only what fits. Elsewhere (sign-in and
/// public area, older iOS) it floats on its own glass capsule.
///
/// Tap on the artwork or text → expands to the full now-playing view.
struct MiniAudioPlayer: View {
    var inAccessory = false

    @StateObject private var observer = MiniPlayerObserver()
    @Environment(\.miniPlayerIsInline) private var isInline
    @Environment(\.nowPlayingNamespace) private var nowPlayingNamespace
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.miniPlayerShellWidth) private var shellWidth

    var body: some View {
        player.nowPlayingTransitionSource(nowPlayingNamespace)
    }

    @ViewBuilder
    private var player: some View {
        if let track = observer.currentTrack {
            if inAccessory {
                // Along iPhone Duo's bottom edge (unfolded: regular width)
                // the accessory has the whole device: like Music, the
                // controls spread out instead of leaving a long empty bar
                // between title and buttons.
                //
                // The choice can't come from our own measured width: there
                // the system sizes the accessory to fit its content, so a
                // compact player left over from the folded phone kept the
                // bar narrow after unfolding. The size class changes with the
                // hinge. The system also proposes the accessory's current
                // width rather than the room it has, so the wide layout asks
                // for the whole shell and the system trims it to what's left
                // beside the vertical bar.
                if !isInline && MiniPlayerTabAccessory.isWide(horizontalSizeClass, shellWidth: shellWidth) {
                    GeometryReader { geo in
                        wideContent(track: track, width: geo.size.width)
                            .frame(width: geo.size.width, height: geo.size.height)
                    }
                    .frame(
                        idealWidth: max(Self.wideMinWidth, shellWidth),
                        maxWidth: .infinity,
                        maxHeight: .infinity
                    )
                } else {
                    content(track: track)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            } else {
                content(track: track)
                    .frame(height: 56)
                    .frame(maxWidth: .infinity)
                    .compatGlassCapsule()
            }
        }
    }

    @ViewBuilder
    private func content(track: AudioTrack) -> some View {
        ZStack {
            // Full-bleed transparent button — receives every tap that isn't on
            // a transport button above. Apple Music–style: the whole bar is
            // tappable to expand.
            Button {
                AudioPlayerService.shared.presentFullPlayer()
            } label: {
                Color.clear
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(tr("audio.player.open", fallback: "Apri player")))

            HStack(spacing: 12) {
                // Artwork
                artwork(track: track)
                    .frame(width: 40, height: 40)
                    .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
                    .allowsHitTesting(false)

                // Title + subtitle
                VStack(alignment: .leading, spacing: 1) {
                    Text(track.title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.primary)
                        .lineLimit(1)
                        .truncationMode(.tail)
                    Text(track.subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .truncationMode(.tail)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .allowsHitTesting(false)

                // Play / pause — sits on top of the transparent expand button.
                Button {
                    AudioPlayerService.shared.toggle()
                } label: {
                    Image(systemName: observer.isPlaying ? "pause.fill" : "play.fill")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundStyle(.primary)
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text(
                    observer.isPlaying
                        ? tr("audio.player.pause", fallback: "Pausa")
                        : tr("audio.player.play", fallback: "Riproduci")
                ))

                // Right-hand button: morphs based on state to give the user
                // a clear dismissal affordance when they pause.
                //   • Playing + multi-track queue → forward (next track)
                //   • Playing + single track     → skip-forward 15s
                //   • Paused                     → X (stop, dismisses player)
                // Inline there's room for play/pause only, as in Music.
                if isInline {
                    EmptyView()
                } else if !observer.isPlaying {
                    // Paused → tap to fully tear down playback and hide
                    // the mini-player. Mirrors Mail/Music's swipe-to-dismiss
                    // affordance but as a single discoverable tap.
                    Button {
                        AudioPlayerService.shared.stop()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(.secondary)
                            .frame(width: 44, height: 44)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text(tr("audio.player.close", fallback: "Chiudi riproduzione")))
                    .transition(.opacity.combined(with: .scale(scale: 0.85)))
                } else if observer.queueCount > 1 {
                    Button {
                        AudioPlayerService.shared.skipToNext()
                    } label: {
                        Image(systemName: "forward.fill")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(observer.hasNext ? .primary : .tertiary)
                            .frame(width: 44, height: 44)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .disabled(!observer.hasNext)
                    .accessibilityLabel(Text(tr("audio.player.next_track", fallback: "Traccia successiva")))
                    .transition(.opacity.combined(with: .scale(scale: 0.85)))
                } else {
                    Button {
                        AudioPlayerService.shared.skipForward()
                    } label: {
                        Image(systemName: "goforward.15")
                            .font(.system(size: 22, weight: .regular))
                            .foregroundStyle(.primary)
                            .frame(width: 44, height: 44)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text(tr("audio.player.skip_forward", fallback: "Avanti 15 secondi")))
                    .transition(.opacity.combined(with: .scale(scale: 0.85)))
                }
            }
            .padding(.horizontal, 12)
            .frame(maxWidth: .infinity)
            // Smooth the right-button morph (X ↔ next/skip) when the user
            // taps pause/play.
            .animation(.spring(response: 0.3, dampingFraction: 0.78), value: observer.isPlaying)
            .animation(.spring(response: 0.3, dampingFraction: 0.78), value: observer.queueCount)
        }
        .overlay(alignment: .bottom) {
            // La capsula del glass ha il lato dritto solo tra i due raggi
            // d'angolo (height/2): senza questo inset la hairline sborda
            // oltre i bordi curvi della chip.
            if !isInline {
                MiniProgressHairline()
                    .padding(.horizontal, 28)
            }
        }
    }

    // MARK: - Wide layout

    /// Room for the transport on the left, the track in the middle and the
    /// actions on the right without the middle running into either side.
    private static let wideMinWidth: CGFloat = 680
    /// Each side group's width (five 44-pt buttons plus padding), reserved
    /// on both sides so the track stays centered on the bar.
    private static let wideSideWidth: CGFloat = 248

    /// Music's layout for a wide accessory: transport on the left, the
    /// track centered, AirPlay and close on the right.
    private func wideContent(track: AudioTrack, width: CGFloat) -> some View {
        let hasQueue = observer.queueCount > 1
        return ZStack {
            Button {
                AudioPlayerService.shared.presentFullPlayer()
            } label: {
                Color.clear
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(tr("audio.player.open", fallback: "Apri player")))

            HStack(spacing: 0) {
                transportButton(
                    "gobackward.15",
                    label: tr("audio.player.skip_backward", fallback: "Indietro 15 secondi")
                ) { AudioPlayerService.shared.skipBackward() }
                if hasQueue {
                    transportButton(
                        "backward.fill",
                        label: tr("audio.player.previous_track", fallback: "Traccia precedente"),
                        enabled: observer.hasPrevious
                    ) { AudioPlayerService.shared.skipToPrevious() }
                }
                transportButton(
                    observer.isPlaying ? "pause.fill" : "play.fill",
                    size: 22,
                    label: observer.isPlaying
                        ? tr("audio.player.pause", fallback: "Pausa")
                        : tr("audio.player.play", fallback: "Riproduci")
                ) { AudioPlayerService.shared.toggle() }
                if hasQueue {
                    transportButton(
                        "forward.fill",
                        label: tr("audio.player.next_track", fallback: "Traccia successiva"),
                        enabled: observer.hasNext
                    ) { AudioPlayerService.shared.skipToNext() }
                }
                transportButton(
                    "goforward.15",
                    label: tr("audio.player.skip_forward", fallback: "Avanti 15 secondi")
                ) { AudioPlayerService.shared.skipForward() }

                Spacer(minLength: 0)

                AirPlayPickerView(tint: .label)
                    .frame(width: 44, height: 44)
                    .accessibilityLabel(Text(tr("audio.player.output", fallback: "Output audio")))
                transportButton(
                    "xmark",
                    size: 16,
                    label: tr("audio.player.close", fallback: "Chiudi riproduzione")
                ) { AudioPlayerService.shared.stop() }
            }
            .padding(.horizontal, 12)

            HStack(spacing: 10) {
                artwork(track: track)
                    .frame(width: 34, height: 34)
                    .clipShape(RoundedRectangle(cornerRadius: 5, style: .continuous))
                VStack(alignment: .leading, spacing: 1) {
                    Text(track.title)
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(1)
                    Text(track.subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: min(380, width - 2 * Self.wideSideWidth))
            .fixedSize(horizontal: false, vertical: true)
            .allowsHitTesting(false)
        }
        .overlay(alignment: .bottom) {
            MiniProgressHairline()
                .padding(.horizontal, 28)
        }
    }

    private func transportButton(
        _ systemName: String,
        size: CGFloat = 17,
        label: String,
        enabled: Bool = true,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: size, weight: .semibold))
                .foregroundStyle(enabled ? .primary : .tertiary)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityLabel(Text(label))
    }

    @ViewBuilder
    private func artwork(track: AudioTrack) -> some View {
        if let url = track.artworkURL {
            CachedAsyncImage(url: url) { img in
                img.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                placeholderArtwork
            }
        } else {
            placeholderArtwork
        }
    }

    private var placeholderArtwork: some View {
        ZStack {
            Rectangle().fill(Color.secondary.opacity(0.2))
            Image(systemName: "waveform")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.secondary)
        }
    }
}

/// Renders only the thin progress hairline at the bottom of the mini-player.
/// Isolated into its own view so the 10 Hz `progress` updates from the
/// `AudioPlayerService` only re-render this 1.5-pt strip, leaving the
/// transport buttons in the parent view untouched and tappable.
private struct MiniProgressHairline: View {
    @ObservedObject private var service: AudioPlayerService = .shared

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(Color.primary.opacity(0.10))
                    .frame(height: 1.5)
                Capsule()
                    .fill(AppTheme.Colors.brandTintAdaptive)
                    .frame(width: max(0, geo.size.width * service.progress), height: 1.5)
            }
        }
        .frame(height: 1.5)
    }
}

private struct MiniPlayerIsInlineKey: EnvironmentKey {
    static let defaultValue = false
}

private struct MiniPlayerShellWidthKey: EnvironmentKey {
    static let defaultValue: CGFloat = 0
}

extension EnvironmentValues {
    /// The tab accessory shares its row with the collapsed tab bar.
    var miniPlayerIsInline: Bool {
        get { self[MiniPlayerIsInlineKey.self] }
        set { self[MiniPlayerIsInlineKey.self] = newValue }
    }

    /// Width of the tab shell hosting the accessory.
    var miniPlayerShellWidth: CGFloat {
        get { self[MiniPlayerShellWidthKey.self] }
        set { self[MiniPlayerShellWidthKey.self] = newValue }
    }
}

/// The mini-player as the tab bar's bottom accessory, Apple Music's way, when
/// the system has it (iOS 26.1+: `isEnabled` keeps the TabView's structure
/// fixed as playback starts and stops, so its NavigationStacks survive).
/// `usesSystemAccessory` tells the shell whether to float its own instead.
///
/// Like Music, while something plays the tab bar collapses on scrolling down:
/// the accessory then sits inline beside it (`miniPlayerIsInline`) instead of
/// above it. With
/// iPhone Duo's vertical tab bar the system lays it along the bottom edge.
struct MiniPlayerTabAccessory: ViewModifier {
    let isEnabled: Bool
    @State private var shellWidth: CGFloat = 0
    /// Briefly takes the accessory away when iPhone Duo folds, unfolds or
    /// turns between the wide and the phone-wide layout.
    /// Beside the vertical bar the system sizes the accessory once, when it
    /// appears, and keeps that width afterwards: unfolding left the phone's
    /// narrow bar in the middle of the wide screen. Shown again, it's
    /// measured for the screen it's on.
    @State private var isRemounting = false
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    /// The accessory runs along the bottom of a wide screen beside the
    /// vertical bar (iPhone Duo unfolded on its side). Upright the same
    /// phone is regular too, but phone-wide, with the bar under the tabs.
    static func isWide(_ sizeClass: UserInterfaceSizeClass?, shellWidth: CGFloat) -> Bool {
        sizeClass == .regular && shellWidth >= 760
    }

    static var usesSystemAccessory: Bool {
        if #available(iOS 26.1, *) { return true }
        return false
    }

    func body(content: Content) -> some View {
        if #available(iOS 26.1, *) {
            content
                .tabViewBottomAccessory(isEnabled: isEnabled && !isRemounting) {
                    AccessoryMiniPlayer()
                        .environment(\.miniPlayerShellWidth, shellWidth)
                }
                // Collapsing on scroll only makes room for the player; with
                // nothing playing it just hid the tabs this app lives in.
                .tabBarMinimizeBehavior(isEnabled ? .onScrollDown : .never)
                .onGeometryChange(for: CGFloat.self) { $0.size.width } action: { shellWidth = $0 }
                .onChange(of: Self.isWide(horizontalSizeClass, shellWidth: shellWidth)) {
                    guard isEnabled else { return }
                    var noAnimation = Transaction()
                    noAnimation.disablesAnimations = true
                    withTransaction(noAnimation) { isRemounting = true }
                    Task { @MainActor in
                        // A frame apart, or the two changes collapse into
                        // one update and the accessory never leaves.
                        try? await Task.sleep(for: .milliseconds(50))
                        withTransaction(noAnimation) { isRemounting = false }
                    }
                }
        } else {
            content
        }
    }
}

@available(iOS 26.1, *)
private struct AccessoryMiniPlayer: View {
    @Environment(\.tabViewBottomAccessoryPlacement) private var placement

    var body: some View {
        MiniAudioPlayer(inAccessory: true)
            .environment(\.miniPlayerIsInline, placement == .inline)
    }
}
