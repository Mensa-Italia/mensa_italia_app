import AVFoundation
import Shared
import SwiftUI

/// In-article audio narration banner — Apple Podcasts–style.
/// Sits between the byline hairline and the article body. Drives the
/// generic `AudioPlayerService`; the article is identified by the stable
/// track id `"quid-article-<articleId>"` produced by `QuidAudioFactory`.
struct QuidNarrationBanner: View {

    let audio: QuidArticleAudio
    let articleId: Int64
    let articleTitle: String
    let artworkURL: URL?

    @ObservedObject private var audioService = AudioPlayerService.shared

    // MARK: - Derived state

    private var trackId: String { QuidAudioFactory.trackId(articleId: articleId) }

    private var isThisArticleLoaded: Bool {
        audioService.currentTrack?.id == trackId
    }

    private var isThisArticlePlaying: Bool {
        isThisArticleLoaded && audioService.isPlaying
    }

    private var formattedDuration: String {
        let dur = Int(audio.durationSeconds)
        let minutes = dur / 60
        let seconds = dur % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    private var displayProgress: Double {
        isThisArticleLoaded ? audioService.progress : 0
    }

    // MARK: - View

    var body: some View {
        GlassCard(padding: 14, cornerRadius: 14) {
            VStack(spacing: 8) {
                HStack(spacing: 12) {
                    // Thumbnail
                    if let artworkURL {
                        CachedAsyncImage(url: artworkURL) { img in
                            img.resizable().aspectRatio(contentMode: .fill)
                        } placeholder: {
                            Color.secondary.opacity(0.2)
                        }
                        .frame(width: 32, height: 32)
                        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
                    } else {
                        RoundedRectangle(cornerRadius: 6, style: .continuous)
                            .fill(Color.secondary.opacity(0.2))
                            .frame(width: 32, height: 32)
                            .overlay(
                                Image(systemName: "waveform")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(.secondary)
                            )
                    }

                    // Text labels
                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 4) {
                            Text(tr("addons.quid.audio.listen", fallback: "Ascolta"))
                                .font(.system(.footnote, design: .serif).weight(.semibold))
                                .foregroundStyle(.primary)
                            Text("· " + String(format: tr("addons.quid.audio.narrated_by", fallback: "letto da %@"), audio.voice) + " · " + formattedDuration)
                                .font(.system(.footnote, design: .serif))
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                    }

                    Spacer()

                    // Play/pause button
                    Button {
                        handlePlayPause()
                    } label: {
                        ZStack {
                            Circle()
                                .fill(Color.primary.opacity(0.1))
                                .frame(width: 44, height: 44)
                            Image(systemName: isThisArticlePlaying ? "pause.fill" : "play.fill")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(.primary)
                                .offset(x: isThisArticlePlaying ? 0 : 1) // optical center for play glyph
                        }
                    }
                    .accessibilityLabel(
                        Text(isThisArticlePlaying
                             ? tr("addons.quid.audio.pause", fallback: "Pausa")
                             : tr("addons.quid.audio.play", fallback: "Riproduci"))
                    )
                }

                // Progress bar
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Rectangle()
                            .fill(Color.primary.opacity(0.12))
                            .frame(height: 2)
                        Rectangle()
                            .fill(AppTheme.Colors.mensaBlue)
                            .frame(width: geo.size.width * displayProgress, height: 2)
                    }
                    .clipShape(Capsule())
                }
                .frame(height: 2)
            }
        }
    }

    // MARK: - Actions

    private func handlePlayPause() {
        if isThisArticleLoaded {
            audioService.toggle()
        } else if let track = QuidAudioFactory.makeTrack(
            audio: audio,
            articleId: articleId,
            articleTitle: articleTitle,
            artworkURL: artworkURL
        ) {
            audioService.play(track)
        }
    }
}

/// Builds an `AudioTrack` from a Quid `QuidArticleAudio` record. Keeping the
/// translation in one place avoids drift between the banner and the article
/// toolbar button.
enum QuidAudioFactory {
    static func trackId(articleId: Int64) -> String {
        "quid-article-\(articleId)"
    }

    static func subtitle(voice: String) -> String {
        // Italian-first; matches the brand voice. The "Quid · letto da X"
        // composition mirrors what the in-article banner shows.
        "Quid · " + String(format: tr("addons.quid.audio.narrated_by", fallback: "letto da %@"), voice)
    }

    static func makeTrack(
        audio: QuidArticleAudio,
        articleId: Int64,
        articleTitle: String,
        artworkURL: URL?
    ) -> AudioTrack? {
        guard let url = URL(string: audio.audioUrl) else { return nil }
        return AudioTrack(
            id: trackId(articleId: articleId),
            title: articleTitle,
            subtitle: subtitle(voice: audio.voice),
            artworkURL: artworkURL,
            audioURL: url,
            duration: TimeInterval(Int(audio.durationSeconds)),
            originDeepLink: "mensa://quid-article/\(articleId)"
        )
    }
}
