import SwiftUI
import Combine

/// Slim Combine-driven mirror of `AudioPlayerService` exposing only the
/// coarse-grained state the button row reads: current track, isPlaying,
/// queue size, and hasNext. We deliberately do NOT republish `progress` or
/// `currentTime` — those tick at 10 Hz from the time observer and would
/// otherwise force the button row to re-render constantly, killing tap
/// responsiveness. The thin progress hairline observes the service directly
/// in its own sibling view so its high-frequency re-renders don't ripple
/// up to the buttons.
@MainActor
final class MiniPlayerObserver: ObservableObject {
    @Published private(set) var currentTrack: AudioTrack?
    @Published private(set) var isPlaying: Bool = false
    @Published private(set) var queueCount: Int = 0
    @Published private(set) var hasNext: Bool = false

    init() {
        let svc = AudioPlayerService.shared
        svc.$currentTrack.removeDuplicates().assign(to: &$currentTrack)
        svc.$isPlaying.removeDuplicates().assign(to: &$isPlaying)
        svc.$queue.map { $0.count }.removeDuplicates().assign(to: &$queueCount)
        Publishers.CombineLatest(svc.$queue, svc.$currentIndex)
            .map { queue, idx in idx < queue.count - 1 }
            .removeDuplicates()
            .assign(to: &$hasNext)
    }
}

/// Apple Music–style floating mini-player. Hosted by `MainTabView` inside the
/// iOS 26 `tabViewBottomAccessory`, which provides the Liquid Glass background
/// and handles placement above the floating tab bar. This view therefore only
/// renders foreground content — artwork, labels, transport buttons, and a
/// progress hairline along its bottom edge — and lets the accessory chrome do
/// the rest.
///
/// Tap on the artwork or text → expands to the full now-playing view.
struct MiniAudioPlayer: View {

    @StateObject private var observer = MiniPlayerObserver()

    var body: some View {
        if let track = observer.currentTrack {
            content(track: track)
                .frame(height: 56)
                .frame(maxWidth: .infinity)
                .glassEffect(.regular, in: .capsule)
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
                if !observer.isPlaying {
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
            MiniProgressHairline()
        }
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
                Rectangle()
                    .fill(Color.primary.opacity(0.10))
                    .frame(height: 1.5)
                Rectangle()
                    .fill(AppTheme.Colors.brandTintAdaptive)
                    .frame(width: max(0, geo.size.width * service.progress), height: 1.5)
            }
        }
        .frame(height: 1.5)
    }
}
