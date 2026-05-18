import AVFoundation
import Combine
import MediaPlayer
import SwiftUI
import UIKit

// MARK: - Track model

/// A feature-agnostic audio track descriptor. Any feature (Quid, Podcasts,
/// future addons) constructs an `AudioTrack` from its own domain models and
/// hands it to `AudioPlayerService.shared`. The service knows nothing about
/// Kotlin types or feature semantics.
struct AudioTrack: Equatable, Identifiable {
    /// Stable identifier from the originating feature. Must be collision-resistant
    /// across features — recommended pattern is `"<feature>-<entity>-<id>"`.
    let id: String
    let title: String
    /// Free-form subtitle composed by the caller, e.g. `"Quid · letto da Giulia"`.
    let subtitle: String
    let artworkURL: URL?
    let audioURL: URL
    let duration: TimeInterval
    /// Optional `mensa://` deep link to navigate back to the originating screen
    /// from the full-screen now-playing view. Pure metadata — the service does
    /// not interpret it.
    let originDeepLink: String?
}

// MARK: - Player

/// Generic audio playback service — single `AVPlayer` + `AVAudioSession` +
/// `MPNowPlayingInfoCenter` integration. Drives both the global mini-player
/// (above the tab bar) and the full-screen now-playing sheet via published
/// state.
@MainActor
final class AudioPlayerService: NSObject, ObservableObject {

    static let shared = AudioPlayerService()

    // MARK: Published state

    @Published private(set) var currentTrack: AudioTrack?
    @Published private(set) var isPlaying: Bool = false
    @Published private(set) var progress: Double = 0
    @Published private(set) var currentTime: TimeInterval = 0
    /// Toggled by the mini-player tap (to true) and the full-screen pull-down
    /// gesture (to false). Bound to a `.fullScreenCover` in `MainTabView`.
    @Published var isPresentingFullPlayer: Bool = false

    // MARK: Queue state

    @Published private(set) var queue: [AudioTrack] = []
    @Published private(set) var currentIndex: Int = 0

    var hasNext: Bool { currentIndex < queue.count - 1 }
    var hasPrevious: Bool { currentIndex > 0 }
    var upNext: [AudioTrack] { Array(queue.dropFirst(currentIndex + 1)) }

    // MARK: Private

    private var player: AVPlayer?
    private var timeObserver: Any?
    private var itemDidEndObserver: NSObjectProtocol?
    private var statusObservation: NSKeyValueObservation?
    private var rateObservation: NSKeyValueObservation?
    private var sessionActivated = false

    private lazy var nowPlaying = AudioNowPlayingCenter(player: self)

    // MARK: Init

    private override init() {
        super.init()
    }

    // MARK: - Public API

    /// Play the given track, replacing any current queue.
    /// If a track with the same `id` is already loaded, resumes playback
    /// without swapping the player item.
    func play(_ track: AudioTrack) {
        queue = [track]
        currentIndex = 0
        _playTrack(track)
    }

    /// Replace the entire queue and start playing from the first track.
    func playQueue(_ tracks: [AudioTrack]) {
        guard !tracks.isEmpty else { return }
        queue = tracks
        currentIndex = 0
        _playTrack(tracks[0])
    }

    /// Add a track to the end of the queue. If nothing is playing, start playback.
    func addToQueue(_ track: AudioTrack) {
        if currentTrack == nil {
            playQueue([track])
        } else {
            queue.append(track)
        }
    }

    /// Add multiple tracks to the end of the queue. If nothing is playing, start playback.
    func addToQueue(_ tracks: [AudioTrack]) {
        guard !tracks.isEmpty else { return }
        if currentTrack == nil {
            playQueue(tracks)
        } else {
            queue.append(contentsOf: tracks)
        }
    }

    /// Skip to the next track in the queue. If at the end, rewinds and pauses.
    func skipToNext() {
        guard hasNext else { return }
        currentIndex += 1
        _playTrack(queue[currentIndex])
    }

    /// Go to the previous track, or restart the current track if more than 3 seconds in.
    func skipToPrevious() {
        if currentTime > 3 {
            seek(to: 0)
            return
        }
        guard hasPrevious else {
            seek(to: 0)
            return
        }
        currentIndex -= 1
        _playTrack(queue[currentIndex])
    }

    func pause() {
        player?.pause()
        nowPlaying.update()
    }

    func resume() {
        guard player != nil else { return }
        activateSessionIfNeeded()
        player?.play()
        nowPlaying.update()
    }

    func toggle() {
        if isPlaying { pause() } else { resume() }
    }

    /// Stops playback, releases the player, clears the queue, and clears
    /// `currentTrack` so the mini-player disappears.
    func stop() {
        queue = []
        currentIndex = 0
        tearDown()
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        isPresentingFullPlayer = false
    }

    func seek(to seconds: TimeInterval) {
        let clamped = max(0, min(seconds, currentTrack?.duration ?? seconds))
        let time = CMTime(seconds: clamped, preferredTimescale: 600)
        player?.seek(to: time, toleranceBefore: .zero, toleranceAfter: .zero)
        currentTime = clamped
        if let dur = currentTrack?.duration, dur > 0 {
            progress = clamped / dur
        }
        nowPlaying.update()
    }

    func skipForward(_ seconds: TimeInterval = 15) {
        seek(to: currentTime + seconds)
    }

    func skipBackward(_ seconds: TimeInterval = 15) {
        seek(to: currentTime - seconds)
    }

    func presentFullPlayer() {
        guard currentTrack != nil else { return }
        isPresentingFullPlayer = true
    }

    func dismissFullPlayer() {
        isPresentingFullPlayer = false
    }

    // MARK: - Private helpers

    /// Core playback setup. Called by `play(_:)`, `playQueue(_:)`,
    /// `skipToNext()`, and `skipToPrevious()`. Does NOT touch `queue` or
    /// `currentIndex` — callers manage those before calling this.
    private func _playTrack(_ track: AudioTrack) {
        if track.id == currentTrack?.id {
            resume()
            return
        }
        tearDown()

        currentTrack = track
        activateSessionIfNeeded()

        let item = AVPlayerItem(url: track.audioURL)
        player = AVPlayer(playerItem: item)

        observeItem(item)
        observeRate()

        player?.play()
        nowPlaying.configure(track: track)
    }

    private func activateSessionIfNeeded() {
        guard !sessionActivated else { return }
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .spokenAudio, policy: .default)
            try session.setActive(true)
            sessionActivated = true
        } catch {
            // Activation can fail if another app holds an exclusive session
            // (e.g. an active phone call). Silently ignore; the system will
            // route audio anyway once the conflicting session releases.
            _ = error
        }
    }

    private func observeItem(_ item: AVPlayerItem) {
        statusObservation = item.observe(\.status, options: [.new]) { [weak self] item, _ in
            if item.status == .failed {
                Task { @MainActor [weak self] in
                    self?.stop()
                }
            }
        }

        itemDidEndObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: item,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if self.hasNext {
                    // Advance to next track in queue automatically.
                    self.skipToNext()
                } else {
                    // End of queue: rewind to start and pause. Do NOT call stop() —
                    // that tears down the player and dismisses the full-screen
                    // sheet, which the user perceives as a spurious "back to home"
                    // when they tap play after auto-completion.
                    self.player?.seek(to: .zero)
                    self.player?.pause()
                    self.currentTime = 0
                    self.progress = 0
                    self.nowPlaying.update()
                }
            }
        }

        // Tight cadence (10 Hz) so the full-screen scrubber moves smoothly
        // — the SwiftUI scrubber animates linearly between ticks, but the
        // jitter is clearly visible if the underlying value updates less
        // frequently than ~6 Hz.
        let interval = CMTime(seconds: 0.1, preferredTimescale: 600)
        timeObserver = player?.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
            guard let self else { return }
            Task { @MainActor [weak self] in
                guard let self else { return }
                let secs = time.seconds
                self.currentTime = secs.isNaN ? 0 : secs
                let dur = self.currentTrack?.duration ?? 0
                self.progress = dur > 0 ? min(1, max(0, secs / dur)) : 0
                self.nowPlaying.updateElapsed()
            }
        }
    }

    private func observeRate() {
        rateObservation = player?.observe(\.timeControlStatus, options: [.new]) { [weak self] player, _ in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.isPlaying = player.timeControlStatus == .playing
            }
        }
    }

    /// Tears down AVPlayer state only. Does NOT clear `queue` or `currentIndex`
    /// so that internal navigation (skipToNext/skipToPrevious) can safely call
    /// this before setting up the next item. `stop()` clears the queue first,
    /// then calls tearDown().
    private func tearDown() {
        if let observer = timeObserver {
            player?.removeTimeObserver(observer)
            timeObserver = nil
        }
        if let obs = itemDidEndObserver {
            NotificationCenter.default.removeObserver(obs)
            itemDidEndObserver = nil
        }
        statusObservation = nil
        rateObservation = nil
        player?.pause()
        player = nil
        isPlaying = false
        progress = 0
        currentTime = 0
        currentTrack = nil
    }
}

// MARK: - NowPlayingCenter

/// Handles `MPNowPlayingInfoCenter` updates and `MPRemoteCommandCenter`
/// registration. Targets registered once for the lifetime of the app.
@MainActor
final class AudioNowPlayingCenter {

    private weak var player: AudioPlayerService?
    private var artworkCache: UIImage?
    private var artworkURL: URL?
    private var commandsRegistered = false

    init(player: AudioPlayerService) {
        self.player = player
    }

    func configure(track: AudioTrack) {
        artworkCache = nil
        artworkURL = track.artworkURL
        registerCommandsOnce()
        buildInfo(track: track)
        if let url = track.artworkURL {
            Task { [weak self] in
                guard let self else { return }
                if let data = try? Data(contentsOf: url),
                   let image = UIImage(data: data) {
                    self.artworkCache = image
                    self.update()
                }
            }
        }
    }

    func update() {
        guard let track = player?.currentTrack else { return }
        buildInfo(track: track)
    }

    func updateElapsed() {
        guard var info = MPNowPlayingInfoCenter.default().nowPlayingInfo,
              let player else { return }
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = player.currentTime
        info[MPNowPlayingInfoPropertyPlaybackRate] = player.isPlaying ? 1.0 : 0.0
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func buildInfo(track: AudioTrack) {
        guard let player else { return }
        var info: [String: Any] = [
            MPMediaItemPropertyTitle: track.title,
            MPMediaItemPropertyArtist: track.subtitle,
            MPMediaItemPropertyPlaybackDuration: track.duration,
            MPNowPlayingInfoPropertyElapsedPlaybackTime: player.currentTime,
            MPNowPlayingInfoPropertyPlaybackRate: player.isPlaying ? 1.0 : 0.0,
        ]
        if let cachedImage = artworkCache {
            let artwork = MPMediaItemArtwork(boundsSize: CGSize(width: 600, height: 600)) { _ in cachedImage }
            info[MPMediaItemPropertyArtwork] = artwork
        }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func registerCommandsOnce() {
        guard !commandsRegistered else { return }
        commandsRegistered = true

        let rc = MPRemoteCommandCenter.shared()

        rc.playCommand.isEnabled = true
        rc.playCommand.addTarget { [weak self] _ in
            Task { @MainActor [weak self] in self?.player?.resume() }
            return .success
        }

        rc.pauseCommand.isEnabled = true
        rc.pauseCommand.addTarget { [weak self] _ in
            Task { @MainActor [weak self] in self?.player?.pause() }
            return .success
        }

        rc.togglePlayPauseCommand.isEnabled = true
        rc.togglePlayPauseCommand.addTarget { [weak self] _ in
            Task { @MainActor [weak self] in self?.player?.toggle() }
            return .success
        }

        rc.skipForwardCommand.isEnabled = true
        rc.skipForwardCommand.preferredIntervals = [15]
        rc.skipForwardCommand.addTarget { [weak self] _ in
            Task { @MainActor [weak self] in self?.player?.skipForward(15) }
            return .success
        }

        rc.skipBackwardCommand.isEnabled = true
        rc.skipBackwardCommand.preferredIntervals = [15]
        rc.skipBackwardCommand.addTarget { [weak self] _ in
            Task { @MainActor [weak self] in self?.player?.skipBackward(15) }
            return .success
        }

        rc.changePlaybackPositionCommand.isEnabled = true
        rc.changePlaybackPositionCommand.addTarget { [weak self] event in
            guard let e = event as? MPChangePlaybackPositionCommandEvent else { return .commandFailed }
            Task { @MainActor [weak self] in self?.player?.seek(to: e.positionTime) }
            return .success
        }

        rc.nextTrackCommand.isEnabled = true
        rc.nextTrackCommand.addTarget { [weak self] _ in
            Task { @MainActor [weak self] in self?.player?.skipToNext() }
            return .success
        }

        rc.previousTrackCommand.isEnabled = true
        rc.previousTrackCommand.addTarget { [weak self] _ in
            Task { @MainActor [weak self] in self?.player?.skipToPrevious() }
            return .success
        }
    }
}
