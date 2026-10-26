import SwiftUI
#if canImport(UIKit)
import UIKit
#endif
import AVKit
import MediaPlayer
import Combine

@inline(__always)
private func playLightHaptic() {
    #if canImport(UIKit)
    UIImpactFeedbackGenerator(style: .light).impactOccurred()
    #endif
}

// MARK: - Root View

/// Full-screen now playing, Apple Music's way: the artwork's colours fill the
/// screen, the artwork sits big above (or, with room to spare, beside) the
/// controls, and the transport is bare glyphs on that colour. No close
/// button: the grabber, or pulling the page down, puts it away.
///
/// With width to spare (iPhone Duo unfolded, landscape) artwork and controls
/// take one side each, split at the hinge when there is one so neither
/// crosses it.
struct NowPlayingFullScreenView: View {
    /// Coarse state only. The service republishes the position 10 times a
    /// second: observed here, the whole page — its menus included — was
    /// rebuilt under the finger, and a menu item tapped while its menu was
    /// being replaced did nothing. The position lives in `NowPlayingProgress`.
    @StateObject private var observer = MiniPlayerObserver()
    private let audioService = AudioPlayerService.shared
    @Environment(\.openURL) private var openURL
    @State private var showingQueue = false

    var body: some View {
        ZStack {
            if let track = observer.currentTrack {
                NowPlayingBackdrop(url: track.artworkURL, trackId: track.id)
                GeometryReader { geo in
                    if geo.size.width > geo.size.height && geo.size.width >= 640 {
                        wideLayout(track: track, geo: geo)
                    } else {
                        compactLayout(track: track, geo: geo)
                    }
                }
                .overlay(alignment: .top) { grabber }
            } else {
                Color.black.ignoresSafeArea()
            }
        }
        .foregroundStyle(.white)
        .preferredColorScheme(.dark)
        .sheet(isPresented: $showingQueue) {
            QueueListSheet()
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
        }
    }

    // MARK: Layouts

    private func compactLayout(track: AudioTrack, geo: GeometryProxy) -> some View {
        let side = min(geo.size.width - 56, geo.size.height * 0.48)
        return VStack(spacing: 0) {
            Spacer(minLength: 28)
            artwork(for: track)
                .frame(maxWidth: side, maxHeight: side)
            Spacer(minLength: 20)
            controls(for: track)
        }
        .padding(.horizontal, 28)
        .padding(.bottom, 8)
        .frame(width: geo.size.width, height: geo.size.height)
    }

    private func wideLayout(track: AudioTrack, geo: GeometryProxy) -> some View {
        let hinge = geo.verticalHinge
        let leftWidth = hinge?.minX ?? geo.size.width / 2
        let rightWidth = geo.size.width - (hinge?.maxX ?? geo.size.width / 2)
        let side = min(leftWidth - 64, geo.size.height - 64)
        return HStack(spacing: 0) {
            artwork(for: track)
                .frame(maxWidth: side, maxHeight: side)
                .frame(width: leftWidth, height: geo.size.height)
            Spacer(minLength: 0)
            controls(for: track)
                .frame(maxWidth: 440)
                .padding(.horizontal, 32)
                .frame(width: rightWidth, height: geo.size.height)
        }
    }

    // MARK: Pieces

    /// Tapping it puts the page away too, so dismissing never depends on the
    /// drag alone.
    private var grabber: some View {
        Button {
            audioService.dismissFullPlayer()
        } label: {
            Capsule()
                .fill(.white.opacity(0.4))
                .frame(width: 36, height: 5)
                .padding(.vertical, 8)
                .padding(.horizontal, 20)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(tr("audio.player.dismiss", fallback: "Chiudi player")))
    }

    private func controls(for track: AudioTrack) -> some View {
        VStack(spacing: 0) {
            titleRow(for: track)
                .padding(.bottom, 22)
            NowPlayingProgress(duration: track.duration)
            transportControls()
                .padding(.vertical, 26)
            volumeRow()
            bottomRow()
                .padding(.top, 22)
        }
    }

    @ViewBuilder
    private func artwork(for track: AudioTrack) -> some View {
        // Placeholder `Color.clear`: `CachedAsyncImage` draws it behind the
        // image, and a coloured one would show around a landscape cover.
        Group {
            if let url = track.artworkURL {
                CachedAsyncImage(url: url) { image in
                    image
                        .resizable()
                        .scaledToFit()
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                } placeholder: {
                    Color.clear
                }
            } else {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(AppTheme.brandGradient)
                    .aspectRatio(1, contentMode: .fit)
            }
        }
        .shadow(color: .black.opacity(0.35), radius: 28, x: 0, y: 14)
        // Music shrinks the artwork while paused.
        .scaleEffect(observer.isPlaying ? 1.0 : 0.86)
        .animation(.spring(response: 0.5, dampingFraction: 0.72), value: observer.isPlaying)
    }

    private func titleRow(for track: AudioTrack) -> some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(track.title)
                    .font(.title3.weight(.semibold))
                    .lineLimit(2)
                Text(track.subtitle)
                    .font(.body)
                    .foregroundStyle(.white.opacity(0.6))
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Menu {
                if let link = track.originDeepLink, let url = URL(string: link) {
                    Button {
                        audioService.dismissFullPlayer()
                        openURL(url)
                    } label: {
                        Label(tr("audio.player.open_source", fallback: "Vai all'episodio"), systemImage: "arrow.up.right.square")
                    }
                }
                Button {
                    showingQueue = true
                } label: {
                    Label(tr("audio.player.queue", fallback: "Coda di riproduzione"), systemImage: "list.bullet")
                }
                Button(role: .destructive) {
                    audioService.stop()
                } label: {
                    Label(tr("audio.player.stop", fallback: "Interrompi riproduzione"), systemImage: "stop.fill")
                }
            } label: {
                Image(systemName: "ellipsis")
                    .font(.system(size: 16, weight: .bold))
                    .frame(width: 32, height: 32)
                    .background(Circle().fill(.white.opacity(0.15)))
                    .contentShape(Circle())
            }
            .accessibilityLabel(Text(tr("app.more_actions", fallback: "Altre azioni")))
        }
    }

    private func transportControls() -> some View {
        let hasQueue = observer.queueCount > 1
        return HStack(spacing: 0) {
            Spacer(minLength: 0)
            if hasQueue {
                transportButton("backward.fill", size: 30, label: tr("audio.player.previous_track", fallback: "Traccia precedente")) {
                    audioService.skipToPrevious()
                }
            } else {
                transportButton("gobackward.15", size: 30, label: tr("audio.player.skip_backward", fallback: "Indietro 15 secondi")) {
                    audioService.skipBackward(15)
                }
            }
            Spacer(minLength: 0)
            transportButton(
                observer.isPlaying ? "pause.fill" : "play.fill",
                size: 46,
                label: observer.isPlaying
                    ? tr("audio.player.pause", fallback: "Pausa")
                    : tr("audio.player.play", fallback: "Riproduci")
            ) {
                audioService.toggle()
            }
            .contentTransition(.symbolEffect(.replace))
            Spacer(minLength: 0)
            if hasQueue {
                transportButton("forward.fill", size: 30, label: tr("audio.player.next_track", fallback: "Traccia successiva"), enabled: observer.hasNext) {
                    audioService.skipToNext()
                }
            } else {
                transportButton("goforward.15", size: 30, label: tr("audio.player.skip_forward", fallback: "Avanti 15 secondi")) {
                    audioService.skipForward(15)
                }
            }
            Spacer(minLength: 0)
        }
    }

    private func transportButton(
        _ systemName: String,
        size: CGFloat,
        label: String,
        enabled: Bool = true,
        action: @escaping () -> Void
    ) -> some View {
        Button {
            playLightHaptic()
            action()
        } label: {
            Image(systemName: systemName)
                .font(.system(size: size, weight: .regular))
                .frame(width: 72, height: 72)
                .contentShape(Circle())
        }
        .buttonStyle(NowPlayingButtonStyle())
        .opacity(enabled ? 1 : 0.35)
        .disabled(!enabled)
        .accessibilityLabel(Text(label))
    }

    private func volumeRow() -> some View {
        // `MPVolumeView`: the only way to drive the system volume from an
        // app (`AVAudioSession.outputVolume` is read-only).
        HStack(spacing: 12) {
            Image(systemName: "speaker.fill")
            SystemVolumeSlider()
                .frame(height: 30)
            Image(systemName: "speaker.wave.3.fill")
        }
        .font(.caption)
        .foregroundStyle(.white.opacity(0.6))
    }

    /// Music's bottom row: an action left, AirPlay in the middle, the queue
    /// right. For spoken audio the left one is the playback speed.
    private func bottomRow() -> some View {
        HStack {
            Menu {
                Picker(selection: Binding(
                    get: { observer.playbackRate },
                    set: { audioService.setPlaybackRate($0) }
                )) {
                    ForEach(AudioPlayerService.playbackRates, id: \.self) { rate in
                        Text(Self.rateLabel(rate)).tag(rate)
                    }
                } label: {
                    Text(tr("audio.player.speed", fallback: "Velocità"))
                }
            } label: {
                Text(Self.rateLabel(observer.playbackRate))
                    .font(.subheadline.weight(.semibold).monospacedDigit())
                    .frame(minWidth: 44, minHeight: 44)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel(Text(tr("audio.player.speed", fallback: "Velocità")))

            Spacer()

            AirPlayPickerView(tint: UIColor.white.withAlphaComponent(0.7))
                .frame(width: 44, height: 44)
                .accessibilityLabel(Text(tr("audio.player.output", fallback: "Output audio")))

            Spacer()

            Button {
                showingQueue = true
            } label: {
                Image(systemName: "list.bullet")
                    .font(.system(size: 20, weight: .regular))
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(tr("audio.player.queue", fallback: "Coda di riproduzione")))
        }
        .foregroundStyle(.white.opacity(0.7))
    }

    private static func rateLabel(_ rate: Float) -> String {
        rate.formatted(.number.precision(.fractionLength(0...2))) + "×"
    }
}

/// Music's transport feedback: a soft disc behind the glyph while pressed.
private struct NowPlayingButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(
                Circle()
                    .fill(.white.opacity(configuration.isPressed ? 0.15 : 0))
            )
            .scaleEffect(configuration.isPressed ? 0.9 : 1)
            .animation(.spring(response: 0.25, dampingFraction: 0.7), value: configuration.isPressed)
    }
}

// MARK: - Queue List Sheet

private struct QueueListSheet: View {
    @ObservedObject var audioService: AudioPlayerService = .shared
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                if let current = audioService.currentTrack {
                    Section(header: Text("In riproduzione")) {
                        queueRow(track: current, isCurrent: true)
                    }
                }

                let upcoming = audioService.upNext
                if !upcoming.isEmpty {
                    Section(header: Text("In coda · \(upcoming.count)")) {
                        ForEach(upcoming) { track in
                            queueRow(track: track, isCurrent: false)
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Coda di riproduzione")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Fine") {
                        dismiss()
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func queueRow(track: AudioTrack, isCurrent: Bool) -> some View {
        HStack(spacing: 12) {
            if let url = track.artworkURL {
                CachedAsyncImage(url: url) { img in
                    img.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.secondary.opacity(0.2)
                }
                .frame(width: 40, height: 40)
                .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
            } else {
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .fill(Color.secondary.opacity(0.2))
                    .frame(width: 40, height: 40)
                    .overlay(
                        Image(systemName: "waveform")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    )
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(track.title)
                    .font(.subheadline.weight(isCurrent ? .semibold : .regular))
                    .foregroundStyle(isCurrent ? AppTheme.Colors.brandTintAdaptive : .primary)
                    .lineLimit(1)
                Text(track.subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer()

            if isCurrent {
                Image(systemName: "waveform")
                    .font(.caption)
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                    .symbolEffect(.variableColor.iterative, isActive: audioService.isPlaying)
            }
        }
    }
}

// MARK: - Backdrop

/// The artwork's own colours across the whole screen, like Music: the image
/// blurred past recognition and saturated, darkened just enough for white
/// text to read on any cover.
private struct NowPlayingBackdrop: View {
    let url: URL?
    let trackId: String

    var body: some View {
        Color.black
            .overlay {
                Group {
                    if let url {
                        CachedAsyncImage(url: url) { image in
                            image.resizable().scaledToFill()
                        } placeholder: {
                            AppTheme.brandGradient
                        }
                    } else {
                        AppTheme.brandGradient
                    }
                }
                .blur(radius: 70)
                .saturation(1.6)
                .scaleEffect(1.5)
            }
            .overlay {
                LinearGradient(
                    colors: [.black.opacity(0.15), .black.opacity(0.45)],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .clipped()
            .ignoresSafeArea()
            .animation(.easeInOut(duration: 0.6), value: trackId)
    }
}

// MARK: - System Volume + AirPlay (UIKit bridges)

/// Wrapper su `MPVolumeView` per esporre lo slider di volume di sistema.
/// `AVAudioSession.outputVolume` è read-only — questo è l'unico modo di
/// pilotare il volume hw dal player. La route button interna è nascosta:
/// per AirPlay usiamo `AirPlayPickerView` separato, più allineato al pattern
/// Apple Music.
private struct SystemVolumeSlider: UIViewRepresentable {
    func makeUIView(context: Context) -> MPVolumeView {
        let view = MPVolumeView(frame: .zero)
        view.showsRouteButton = false
        view.showsVolumeSlider = true
        view.tintColor = .white
        view.setVolumeThumbImage(thumbImage(), for: .normal)
        return view
    }

    func updateUIView(_ uiView: MPVolumeView, context: Context) {}

    /// Thumb più compatto per matchare lo stile Apple Music — un cerchio
    /// bianco di 16pt invece del default UISlider (chunky).
    private func thumbImage() -> UIImage? {
        let size = CGSize(width: 14, height: 14)
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { ctx in
            UIColor.white.setFill()
            ctx.cgContext.fillEllipse(in: CGRect(origin: .zero, size: size))
        }
    }
}

/// `AVRoutePickerView` wrappato: apre il system route picker (AirPlay,
/// Bluetooth, output disponibili) al tap. Sostituisce l'icona statica
/// `airplayaudio` che non aveva action collegata. Lo usa anche il mini-player
/// largo, con il colore del testo invece del bianco su copertina.
struct AirPlayPickerView: UIViewRepresentable {
    var tint: UIColor = UIColor.white.withAlphaComponent(0.85)

    func makeUIView(context: Context) -> AVRoutePickerView {
        let view = AVRoutePickerView()
        view.tintColor = tint
        view.activeTintColor = UIColor.systemBlue
        view.prioritizesVideoDevices = false
        return view
    }

    func updateUIView(_ uiView: AVRoutePickerView, context: Context) {}
}

// MARK: - Scrubber

/// The only part of the page that follows the position, so its 10 Hz
/// updates redraw this row and nothing else.
private struct NowPlayingProgress: View {
    let duration: TimeInterval
    @ObservedObject private var service: AudioPlayerService = .shared

    var body: some View {
        NowPlayingScrubber(
            duration: max(duration, 0.01),
            currentTime: service.currentTime,
            onSeek: { service.seek(to: $0) }
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel(Text(tr("audio.player.position", fallback: "Posizione di riproduzione")))
        .accessibilityValue(
            NowPlayingScrubber.format(service.currentTime) + " / " + NowPlayingScrubber.format(duration)
        )
    }
}

/// Music's progress bar: a thin rounded track with no thumb, thickening while
/// dragged, elapsed and remaining time underneath.
private struct NowPlayingScrubber: View {
    let duration: TimeInterval
    let currentTime: TimeInterval
    let onSeek: (TimeInterval) -> Void

    @State private var isScrubbing = false
    @State private var scrubValue: TimeInterval = 0

    private var displayedValue: TimeInterval {
        isScrubbing ? scrubValue : currentTime
    }

    var body: some View {
        VStack(spacing: 8) {
            GeometryReader { geo in
                let width = geo.size.width
                let fraction = min(max(displayedValue / duration, 0), 1)
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(.white.opacity(0.22))
                    Capsule()
                        .fill(.white.opacity(isScrubbing ? 1 : 0.75))
                        .frame(width: width * fraction)
                }
                .frame(height: isScrubbing ? 12 : 7)
                .frame(maxHeight: .infinity)
                .contentShape(Rectangle())
                .animation(.spring(response: 0.3, dampingFraction: 0.8), value: isScrubbing)
                .gesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { value in
                            if !isScrubbing {
                                isScrubbing = true
                                playLightHaptic()
                            }
                            let p = min(max(value.location.x / max(width, 1), 0), 1)
                            scrubValue = Double(p) * duration
                        }
                        .onEnded { _ in
                            onSeek(scrubValue)
                            isScrubbing = false
                        }
                )
            }
            .frame(height: 24)

            HStack {
                Text(Self.format(displayedValue))
                Spacer()
                Text("-" + Self.format(max(0, duration - displayedValue)))
            }
            .font(.caption.monospacedDigit().weight(.medium))
            .foregroundStyle(.white.opacity(isScrubbing ? 0.9 : 0.55))
        }
    }

    static func format(_ t: TimeInterval) -> String {
        guard t.isFinite, t >= 0 else { return "0:00" }
        let total = Int(t)
        let h = total / 3600
        let m = (total % 3600) / 60
        let s = total % 60
        return h > 0 ? String(format: "%d:%02d:%02d", h, m, s) : String(format: "%d:%02d", m, s)
    }
}

// MARK: - Presentation

/// Presents the now-playing page from the shell, zooming out of the
/// mini-player (iOS 18+) the way Music opens from its bar. The zoom also
/// brings the interactive pull-down to dismiss. Before iOS 18 it stays a
/// large sheet.
struct NowPlayingPresentation: ViewModifier {
    @Binding var isPresented: Bool
    let namespace: Namespace.ID

    static let sourceID = "now-playing"

    func body(content: Content) -> some View {
        if #available(iOS 18.0, *) {
            content
                .fullScreenCover(isPresented: $isPresented) {
                    NowPlayingFullScreenView()
                        .navigationTransition(.zoom(sourceID: Self.sourceID, in: namespace))
                }
                .environment(\.nowPlayingNamespace, namespace)
        } else {
            content
                .sheet(isPresented: $isPresented) {
                    NowPlayingFullScreenView()
                        .presentationDetents([.large])
                        .presentationDragIndicator(.hidden)
                        .presentationBackground(Color.black)
                }
        }
    }
}

private struct NowPlayingNamespaceKey: EnvironmentKey {
    static let defaultValue: Namespace.ID? = nil
}

extension EnvironmentValues {
    /// Where the mini-player marks itself as the zoom source of the
    /// now-playing page (`NowPlayingPresentation`).
    var nowPlayingNamespace: Namespace.ID? {
        get { self[NowPlayingNamespaceKey.self] }
        set { self[NowPlayingNamespaceKey.self] = newValue }
    }
}

extension View {
    /// Marks the mini-player as the view the now-playing page zooms out of.
    @ViewBuilder
    func nowPlayingTransitionSource(_ namespace: Namespace.ID?) -> some View {
        if #available(iOS 18.0, *), let namespace {
            matchedTransitionSource(id: NowPlayingPresentation.sourceID, in: namespace)
        } else {
            self
        }
    }
}
