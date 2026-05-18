import SwiftUI
import Shared

// MARK: - Podcast Audio Factory

enum PodcastAudioFactory {
    static func trackId(episodeId: String) -> String {
        "podcast-episode-\(episodeId)"
    }

    static func makeTrack(from episode: PodcastEpisode, podcastTitle: String) -> AudioTrack? {
        guard let urlString = episode.audioUrl, let url = URL(string: urlString) else { return nil }
        return AudioTrack(
            id: trackId(episodeId: episode.id),
            title: episode.title,
            subtitle: podcastTitle,
            artworkURL: episode.imageUrl.flatMap { URL(string: $0) },
            audioURL: url,
            duration: TimeInterval(episode.durationSeconds),
            originDeepLink: nil
        )
    }
}

// MARK: - Episodes View

struct PodcastEpisodesView: View {
    let podcastId: String
    let podcastTitle: String

    @State private var episodes: [PodcastEpisode] = []
    @State private var refreshing = false
    @State private var sub: Closeable? = nil
    @ObservedObject private var audioService = AudioPlayerService.shared

    var body: some View {
        List {
            ForEach(episodes, id: \.id) { episode in
                EpisodeRow(
                    episode: episode,
                    podcastTitle: podcastTitle,
                    audioService: audioService
                )
                .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
                .contextMenu {
                    Button {
                        if let track = PodcastAudioFactory.makeTrack(from: episode, podcastTitle: podcastTitle) {
                            AudioPlayerService.shared.play(track)
                        }
                    } label: {
                        Label("Riproduci ora", systemImage: "play.fill")
                    }

                    Button {
                        if let track = PodcastAudioFactory.makeTrack(from: episode, podcastTitle: podcastTitle) {
                            AudioPlayerService.shared.addToQueue(track)
                        }
                    } label: {
                        Label("Aggiungi alla coda", systemImage: "text.badge.plus")
                    }

                    Button {
                        playFromEpisode(episode)
                    } label: {
                        Label("Riproduci da qui", systemImage: "arrow.down.to.line.compact")
                    }
                }
            }
        }
        .listStyle(.plain)
        .refreshable { await refresh() }
        .overlay {
            if episodes.isEmpty {
                if refreshing {
                    LoadingDots()
                } else {
                    ContentUnavailableView(
                        tr("addons.podcasts.no_episodes", fallback: "Nessun episodio"),
                        systemImage: "waveform",
                        description: Text(tr(
                            "addons.podcasts.no_episodes_description",
                            fallback: "Non ci sono ancora episodi per questo podcast."
                        ))
                    )
                }
            }
        }
        .navigationTitle(podcastTitle)
        .cleanNavBar()
        // Sappiamo a prescindere che un podcast avrà episodi (altrimenti
        // non sarebbe un podcast) — il menu di riproduzione è sempre in
        // toolbar dal primo frame. Le singole azioni guardano `episodes`
        // a runtime e fanno no-op se vuoto (caso edge: feed appena creato).
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button {
                        playAllEpisodes()
                    } label: {
                        Label("Riproduci tutti", systemImage: "play.fill")
                    }
                    Button {
                        shuffleAllEpisodes()
                    } label: {
                        Label("Riproduzione casuale", systemImage: "shuffle")
                    }
                    Button {
                        addAllToQueue()
                    } label: {
                        Label("Aggiungi alla coda", systemImage: "text.badge.plus")
                    }
                } label: {
                    Image(systemName: "play.circle.fill")
                        .font(.system(size: 22))
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                }
                .disabled(episodes.isEmpty)
            }
        }
        .task { start() }
        .onDisappear { stop() }
    }

    // MARK: - Playback Actions

    private func playAllEpisodes() {
        let tracks = episodes.compactMap { PodcastAudioFactory.makeTrack(from: $0, podcastTitle: podcastTitle) }
        guard !tracks.isEmpty else { return }
        AudioPlayerService.shared.playQueue(tracks)
    }

    private func shuffleAllEpisodes() {
        var tracks = episodes.compactMap { PodcastAudioFactory.makeTrack(from: $0, podcastTitle: podcastTitle) }
        tracks.shuffle()
        guard !tracks.isEmpty else { return }
        AudioPlayerService.shared.playQueue(tracks)
    }

    private func addAllToQueue() {
        let tracks = episodes.compactMap { PodcastAudioFactory.makeTrack(from: $0, podcastTitle: podcastTitle) }
        guard !tracks.isEmpty else { return }
        AudioPlayerService.shared.addToQueue(tracks)
    }

    private func playFromEpisode(_ episode: PodcastEpisode) {
        guard let startIndex = episodes.firstIndex(where: { $0.id == episode.id }) else { return }
        let remaining = episodes[startIndex...]
        let tracks = remaining.compactMap { PodcastAudioFactory.makeTrack(from: $0, podcastTitle: podcastTitle) }
        guard !tracks.isEmpty else { return }
        AudioPlayerService.shared.playQueue(tracks)
    }

    // MARK: - Data

    private func start() {
        sub?.close()
        sub = FlowBridgeKt.subscribe(
            flow: koin.podcasts.observeEpisodes(podcastId: podcastId),
            onEach: { value in
                Task { @MainActor in
                    self.episodes = (value as? [PodcastEpisode]) ?? []
                }
            },
            onError: { _ in }
        )
        Task { await refresh() }
    }

    private func stop() { sub?.close(); sub = nil }

    private func refresh() async {
        refreshing = true
        defer { refreshing = false }
        do { try await koin.podcasts.refreshEpisodes(podcastId: podcastId) } catch { }
    }
}

// MARK: - Episode Row

private struct EpisodeRow: View {
    let episode: PodcastEpisode
    let podcastTitle: String
    @ObservedObject var audioService: AudioPlayerService

    private var trackId: String { PodcastAudioFactory.trackId(episodeId: episode.id) }

    private var isCurrentTrack: Bool {
        audioService.currentTrack?.id == trackId
    }

    private var isPlaying: Bool {
        isCurrentTrack && audioService.isPlaying
    }

    private var thumbnailURL: URL? {
        guard let raw = episode.imageUrl, !raw.isEmpty else { return nil }
        return URL(string: raw)
    }

    var body: some View {
        Button {
            handlePlayTap()
        } label: {
            HStack(spacing: 12) {
                // Thumbnail
                thumbnailView
                    .frame(width: 48, height: 48)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))

                // Title + duration
                VStack(alignment: .leading, spacing: 3) {
                    Text(episode.title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.primary)
                        .lineLimit(2)

                    Text(formatDuration(episode.durationSeconds))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                // Play / pause button
                playButton
                    .frame(width: 36, height: 36)
            }
            .padding(.vertical, 4)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // MARK: - Subviews

    @ViewBuilder
    private var thumbnailView: some View {
        CachedAsyncImage(url: thumbnailURL) { img in
            img.resizable().aspectRatio(contentMode: .fill)
        } placeholder: {
            AppTheme.brandGradient
                .overlay(
                    Image(systemName: "headphones")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.85))
                )
        }
    }

    @ViewBuilder
    private var playButton: some View {
        ZStack {
            Circle()
                .fill(isCurrentTrack
                    ? AppTheme.Colors.brandTintAdaptive.opacity(0.15)
                    : Color.secondary.opacity(0.12)
                )

            if isCurrentTrack && audioService.isPlaying {
                // Waveform animation for actively playing track
                WaveformIndicator()
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                    .frame(width: 18, height: 16)
            } else {
                Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(
                        isCurrentTrack
                            ? AppTheme.Colors.brandTintAdaptive
                            : Color.primary
                    )
                    .offset(x: isPlaying ? 0 : 1) // optical centering for play.fill
            }
        }
    }

    // MARK: - Actions

    private func handlePlayTap() {
        guard let track = PodcastAudioFactory.makeTrack(from: episode, podcastTitle: podcastTitle) else { return }
        if isCurrentTrack {
            audioService.toggle()
        } else {
            audioService.play(track)
        }
    }

    // MARK: - Helpers

    private func formatDuration(_ seconds: Int32) -> String {
        let mins = Int(seconds) / 60
        if mins < 1 { return "< 1 min" }
        return "\(mins) min"
    }
}

// MARK: - Waveform Indicator

/// Animated equalizer bars shown when an episode is actively playing.
private struct WaveformIndicator: View {
    @State private var phase = false

    private let barHeights: [CGFloat] = [0.5, 1.0, 0.7, 0.9, 0.6]

    var body: some View {
        HStack(alignment: .bottom, spacing: 2) {
            ForEach(Array(barHeights.enumerated()), id: \.offset) { idx, maxH in
                RoundedRectangle(cornerRadius: 1.5, style: .continuous)
                    .frame(
                        width: 2.5,
                        height: phase
                            ? maxH * 16
                            : max(3, (1 - maxH) * 8)
                    )
                    .animation(
                        .easeInOut(duration: 0.45)
                            .repeatForever(autoreverses: true)
                            .delay(Double(idx) * 0.08),
                        value: phase
                    )
            }
        }
        .onAppear { phase = true }
        .onDisappear { phase = false }
    }
}
