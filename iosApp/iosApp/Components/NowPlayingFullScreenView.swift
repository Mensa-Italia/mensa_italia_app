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

struct NowPlayingFullScreenView: View {
    @ObservedObject var audioService: AudioPlayerService = .shared
    @Environment(\.openURL) private var openURL
    @State private var showingQueue = false

    var body: some View {
        Group {
            if let track = audioService.currentTrack {
                content(for: track)
                    // Backdrop dietro al content. `ignoresSafeArea` lo
                    // estende ai bordi della sheet (che è già sotto la
                    // status bar grazie al detent .large).
                    .background(backdrop(for: track).ignoresSafeArea())
            } else {
                ProgressView()
                    .tint(.white)
            }
        }
        .preferredColorScheme(.dark)
    }

    /// Background "blurred artwork" che si estende dietro status bar e home
    /// indicator. Il sheet di sistema disegna i propri angoli arrotondati
    /// (`presentationCornerRadius`) sopra quest'area: i pixel dietro la status
    /// bar quindi appaiono ai bordi superiori della card.
    @ViewBuilder
    private func backdrop(for track: AudioTrack) -> some View {
        ZStack {
            Color.black
            BlurredArtworkBackdrop(url: track.artworkURL, trackId: track.id)
            LinearGradient(
                colors: [
                    Color.black.opacity(0.0),
                    Color.black.opacity(0.35),
                    Color.black.opacity(0.75)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
        }
    }

    @ViewBuilder
    private func content(for track: AudioTrack) -> some View {
        GeometryReader { geo in
            // Apple Music usa ~55-60% dell'altezza per l'artwork. Cappiamo
            // alla width-meno-padding per evitare overflow su iPad portrait.
            let artworkSide = min(geo.size.width - 48, geo.size.height * 0.5)
            VStack(spacing: 0) {
                // Grabber
                Capsule()
                    .fill(Color.white.opacity(0.4))
                    .frame(width: 36, height: 5)
                    .padding(.top, 8)
                    .padding(.bottom, 8)

                // Top bar
                topBar(for: track)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 12)

                Spacer(minLength: 8)

                // Artwork prende il suo aspect ratio naturale, vincolato a
                // un quadrato max così che gli album quadrati riempiano il
                // box e gli artwork landscape (podcast) restino al loro ratio
                // senza letterbox visibile.
                artwork(for: track)
                    .frame(maxWidth: artworkSide, maxHeight: artworkSide)

                Spacer(minLength: 8)

                // Title block
                titleBlock(for: track)
                    .padding(.horizontal, 24)
                    .padding(.top, 8)

                // Scrubber
                CustomAudioScrubber(
                    duration: max(track.duration, 0.01),
                    currentTime: audioService.currentTime,
                    onSeek: { newTime in
                        audioService.seek(to: newTime)
                    }
                )
                .accessibilityElement(children: .combine)
                .accessibilityLabel("Posizione di riproduzione")
                .accessibilityValue(timestampAccessibility(current: audioService.currentTime, total: track.duration))
                .padding(.top, 12)

                // Timestamps
                timestamps(track: track)
                    .padding(.horizontal, 24)
                    .padding(.top, 4)
                    .padding(.bottom, 12)

                // Transport
                transportControls()
                    .padding(.horizontal, 32)
                    .padding(.bottom, 20)

                // Secondary
                secondaryActions()
                    .padding(.horizontal, 48)
                    .padding(.bottom, 16)
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
        .sheet(isPresented: $showingQueue) {
            QueueListSheet()
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
                .presentationBackground(.ultraThinMaterial)
        }
    }

    @ViewBuilder
    private func topBar(for track: AudioTrack) -> some View {
        HStack {
            Button {
                audioService.dismissFullPlayer()
            } label: {
                Image(systemName: "chevron.down")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 36, height: 36)
                    .background(Circle().fill(Color.white.opacity(0.08)))
            }
            .accessibilityLabel("Chiudi")

            Spacer()

            VStack(spacing: 2) {
                Text("In riproduzione da")
                    .font(.caption2)
                    .foregroundStyle(.white.opacity(0.6))
                Text(track.subtitle)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.9))
                    .lineLimit(1)
            }

            Spacer()

            Menu {
                if let link = track.originDeepLink, let url = URL(string: link) {
                    Button {
                        openURL(url)
                    } label: {
                        Label("Apri sorgente", systemImage: "arrow.up.right.square")
                    }
                }
                Button(role: .destructive) {
                    audioService.stop()
                } label: {
                    Label("Interrompi riproduzione", systemImage: "stop.fill")
                }
            } label: {
                Image(systemName: "ellipsis")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 36, height: 36)
                    .background(Circle().fill(Color.white.opacity(0.08)))
            }
            .accessibilityLabel("Altre opzioni")
        }
    }

    @ViewBuilder
    private func artwork(for track: AudioTrack) -> some View {
        // Placeholder = `Color.clear`: `CachedAsyncImage` lo renderizza in
        // ZStack DIETRO l'immagine (vedi `Support/CachedAsyncImage.swift`),
        // un fill colorato qui significherebbe "quadrato visibile dietro
        // l'artwork landscape" — non vogliamo.
        Group {
            if let url = track.artworkURL {
                CachedAsyncImage(url: url) { image in
                    image
                        .resizable()
                        .scaledToFit()
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .shadow(color: .black.opacity(0.45), radius: 30, x: 0, y: 14)
                } placeholder: {
                    Color.clear
                }
            } else {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(AppTheme.brandGradient)
                    .aspectRatio(1, contentMode: .fit)
                    .shadow(color: .black.opacity(0.45), radius: 30, x: 0, y: 14)
            }
        }
        .scaleEffect(audioService.isPlaying ? 1.0 : 0.92)
        .animation(.spring(response: 0.5, dampingFraction: 0.7), value: audioService.isPlaying)
    }

    @ViewBuilder
    private func titleBlock(for track: AudioTrack) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(track.title)
                .font(.title3.weight(.semibold))
                .foregroundStyle(.white)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
            Text(track.subtitle)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func timestamps(track: AudioTrack) -> some View {
        HStack {
            Text(formatTime(audioService.currentTime))
            Spacer()
            Text("-" + formatTime(max(0, track.duration - audioService.currentTime)))
        }
        .font(.caption.monospacedDigit().weight(.medium))
        .foregroundStyle(.white.opacity(0.6))
    }

    @ViewBuilder
    private func transportControls() -> some View {
        HStack {
            if audioService.queue.count > 1 {
                Button {
                    playLightHaptic()
                    audioService.skipToPrevious()
                } label: {
                    Image(systemName: "backward.fill")
                        .font(.system(size: 28, weight: .regular))
                        .foregroundStyle(.white.opacity(audioService.hasPrevious ? 1.0 : 0.3))
                }
                .disabled(!audioService.hasPrevious)
                .accessibilityLabel("Traccia precedente")
            } else {
                Button {
                    playLightHaptic()
                    audioService.skipBackward(15)
                } label: {
                    Image(systemName: "gobackward.15")
                        .font(.system(size: 28, weight: .regular))
                        .foregroundStyle(.white)
                }
                .accessibilityLabel("Indietro di 15 secondi")
            }

            Spacer()

            Button {
                playLightHaptic()
                audioService.toggle()
            } label: {
                ZStack {
                    Circle()
                        .fill(Color.white)
                        .frame(width: 72, height: 72)
                    Image(systemName: audioService.isPlaying ? "pause.fill" : "play.fill")
                        .font(.system(size: 32, weight: .black))
                        .foregroundStyle(.black)
                        .offset(x: audioService.isPlaying ? 0 : 2)
                }
            }
            .accessibilityLabel(audioService.isPlaying ? "Pausa" : "Riproduci")

            Spacer()

            if audioService.queue.count > 1 {
                Button {
                    playLightHaptic()
                    audioService.skipToNext()
                } label: {
                    Image(systemName: "forward.fill")
                        .font(.system(size: 28, weight: .regular))
                        .foregroundStyle(.white.opacity(audioService.hasNext ? 1.0 : 0.3))
                }
                .disabled(!audioService.hasNext)
                .accessibilityLabel("Traccia successiva")
            } else {
                Button {
                    playLightHaptic()
                    audioService.skipForward(15)
                } label: {
                    Image(systemName: "goforward.15")
                        .font(.system(size: 28, weight: .regular))
                        .foregroundStyle(.white)
                }
                .accessibilityLabel("Avanti di 15 secondi")
            }
        }
    }

    @ViewBuilder
    private func secondaryActions() -> some View {
        VStack(spacing: 18) {
            // Volume slider di sistema. Wrappiamo `MPVolumeView` perché:
            //  - è l'UNICO modo per pilotare il volume di sistema da app
            //    (AVAudioSession.outputVolume è read-only);
            //  - sopra a un widget Apple-Music-like che NON funzionasse
            //    sarebbe peggio del non averlo.
            // I due speaker glyph laterali sono solo affordance visuale,
            // l'interazione vive nello slider centrale.
            HStack(spacing: 12) {
                Image(systemName: "speaker.fill")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.5))
                SystemVolumeSlider()
                    .frame(height: 30)
                Image(systemName: "speaker.wave.3.fill")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.5))
            }

            // Azioni: AirPlay (sistema), Queue (sempre tappabile — apre
            // la lista anche con un solo elemento per coerenza UX).
            HStack {
                AirPlayPickerView()
                    .frame(width: 40, height: 40)
                    .accessibilityLabel("Output audio")

                Spacer()

                Button {
                    showingQueue = true
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "list.bullet")
                            .font(.system(size: 20, weight: .regular))
                        if audioService.queue.count > 1 {
                            Text("\(audioService.queue.count)")
                                .font(.caption.weight(.semibold))
                        }
                    }
                    .foregroundStyle(.white.opacity(0.85))
                    .frame(minWidth: 40, minHeight: 40)
                }
                .accessibilityLabel("Coda di riproduzione")
            }
        }
    }

    private func formatTime(_ t: TimeInterval) -> String {
        guard t.isFinite, t >= 0 else { return "0:00" }
        let total = Int(t)
        let m = total / 60
        let s = total % 60
        return String(format: "%d:%02d", m, s)
    }

    private func timestampAccessibility(current: TimeInterval, total: TimeInterval) -> String {
        "\(formatTime(current)) di \(formatTime(total))"
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

// MARK: - Blurred Backdrop

private struct BlurredArtworkBackdrop: View {
    let url: URL?
    let trackId: String

    var body: some View {
        ZStack {
            if let url {
                CachedAsyncImage(url: url) { image in
                    image
                        .resizable()
                        .scaledToFill()
                } placeholder: {
                    AppTheme.brandGradient
                }
            } else {
                AppTheme.brandGradient
            }
        }
        .blur(radius: 60)
        .scaleEffect(1.3)
        .opacity(0.55)
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
/// `airplayaudio` che non aveva action collegata.
private struct AirPlayPickerView: UIViewRepresentable {
    func makeUIView(context: Context) -> AVRoutePickerView {
        let view = AVRoutePickerView()
        view.tintColor = UIColor.white.withAlphaComponent(0.85)
        view.activeTintColor = UIColor.systemBlue
        view.prioritizesVideoDevices = false
        return view
    }

    func updateUIView(_ uiView: AVRoutePickerView, context: Context) {}
}

// MARK: - Custom Audio Scrubber

private struct CustomAudioScrubber: View {
    let duration: TimeInterval
    let currentTime: TimeInterval
    let onSeek: (TimeInterval) -> Void

    @State private var isScrubbing: Bool = false
    @State private var scrubValue: TimeInterval = 0
    @State private var didHaptic: Bool = false

    private let thumbRadius: CGFloat = 7

    private var displayedValue: TimeInterval {
        isScrubbing ? scrubValue : currentTime
    }

    var body: some View {
        GeometryReader { geo in
            let W = geo.size.width
            let R = thumbRadius
            let trackHeight: CGFloat = isScrubbing ? 8 : 4
            let safeDuration = max(duration, 0.01)
            let pct = min(max(displayedValue / safeDuration, 0), 1)
            let cx = R + (W - 2 * R) * CGFloat(pct)
            let midY = geo.size.height / 2

            ZStack(alignment: .leading) {
                Capsule()
                    .fill(.ultraThinMaterial)
                    .frame(width: max(0, W - 2 * R), height: trackHeight)
                    .position(x: W / 2, y: midY)

                Capsule()
                    .fill(Color.white)
                    .frame(width: max(0, cx - R), height: trackHeight)
                    .position(x: R + max(0, cx - R) / 2, y: midY)

                Circle()
                    .fill(Color.white)
                    .frame(width: 2 * R, height: 2 * R)
                    .shadow(color: .black.opacity(0.35), radius: 4, x: 0, y: 1)
                    .scaleEffect(isScrubbing ? 1.4 : 1.0)
                    .position(x: cx, y: midY)
            }
            .frame(width: W, height: geo.size.height)
            .contentShape(Rectangle())
            .animation(.spring(response: 0.3, dampingFraction: 0.8), value: isScrubbing)
            .animation(isScrubbing ? nil : .linear(duration: 0.1), value: currentTime)
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        if !isScrubbing {
                            isScrubbing = true
                        }
                        if !didHaptic {
                            playLightHaptic()
                            didHaptic = true
                        }
                        let x = min(max(value.location.x, R), W - R)
                        let p = (x - R) / max(W - 2 * R, 1)
                        scrubValue = Double(p) * safeDuration
                    }
                    .onEnded { _ in
                        let final = scrubValue
                        onSeek(final)
                        isScrubbing = false
                        didHaptic = false
                    }
            )
        }
        .frame(height: 36)
        .padding(.horizontal, 24)
    }
}
