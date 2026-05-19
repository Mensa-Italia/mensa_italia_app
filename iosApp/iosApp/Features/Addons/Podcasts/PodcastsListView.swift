import SwiftUI
import Shared

// MARK: - List View

struct PodcastsListView: View {
    @State private var podcasts: [Podcast] = []
    @State private var refreshing = false
    @State private var appeared = false
    @State private var sub: Closeable?

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(Array(podcasts.enumerated()), id: \.element.id) { idx, podcast in
                    // Destination inline (vs `NavigationLink(value:)` +
                    // `navigationDestination(for:)`): la doppia
                    // registrazione del tipo Hashable rendeva il push
                    // ambiguo in alcuni stack annidati (es. flusso public
                    // area), facendolo "popare" indietro al tap. Inline =
                    // push deterministico in qualsiasi NavigationStack.
                    NavigationLink {
                        PodcastEpisodesView(podcastId: podcast.id, podcastTitle: podcast.title)
                    } label: {
                        PodcastCard(podcast: podcast)
                    }
                    .buttonStyle(.plain)
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 14)
                    .animation(
                        .spring(response: 0.5, dampingFraction: 0.86)
                            .delay(Double(min(idx, 12)) * 0.06),
                        value: appeared
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
        .refreshable { await refresh() }
        .overlay {
            if podcasts.isEmpty {
                if refreshing {
                    LoadingDots()
                } else {
                    ContentUnavailableView(
                        tr("addons.podcasts.empty", fallback: "Nessun podcast"),
                        systemImage: "headphones",
                        description: Text(tr(
                            "addons.podcasts.empty_description",
                            fallback: "Non sono ancora disponibili podcast."
                        ))
                    )
                }
            }
        }
        .navigationTitle(tr("addons.podcasts.title", fallback: "Podcast"))
        .cleanNavBar()
        .task {
            start()
            withAnimation(.easeOut(duration: 0.35)) { appeared = true }
        }
        .onDisappear { stop() }
    }

    // MARK: - Data

    private func start() {
        sub?.close()
        sub = FlowBridgeKt.subscribe(
            flow: koin.podcasts.observePodcasts(),
            onEach: { value in
                Task { @MainActor in
                    self.podcasts = (value as? [Podcast]) ?? []
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
        do { try await koin.podcasts.refreshPodcasts() } catch { }
    }
}

// MARK: - Podcast Card

struct PodcastCard: View {
    let podcast: Podcast

    private var coverURL: URL? {
        guard let raw = podcast.imageUrl, !raw.isEmpty else { return nil }
        return URL(string: raw)
    }

    private var episodeCountText: String {
        let count = podcast.episodesCount
        if count == 1 {
            return tr("addons.podcasts.episode_count_one", fallback: "1 episodio")
        } else {
            let template = tr("addons.podcasts.episode_count_other", fallback: "%lld episodi")
            return String(format: template, Int64(count))
        }
    }

    var body: some View {
        GlassCard(padding: 0, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 0) {
                // Cover image — landscape 16:9
                Color.clear
                    .aspectRatio(16 / 9, contentMode: .fit)
                    .overlay {
                        CachedAsyncImage(url: coverURL) { img in
                            img.resizable().aspectRatio(contentMode: .fill)
                        } placeholder: {
                            AppTheme.brandGradient
                                .overlay(
                                    Image(systemName: "headphones")
                                        .font(.system(size: 40, weight: .semibold))
                                        .foregroundStyle(.white.opacity(0.85))
                                )
                        }
                    }
                    // Gradient overlay with title at bottom
                    .overlay(alignment: .bottom) {
                        LinearGradient(
                            colors: [.clear, .black.opacity(0.72)],
                            startPoint: .center,
                            endPoint: .bottom
                        )
                        .overlay(alignment: .bottomLeading) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(podcast.title)
                                    .font(.headline.weight(.bold))
                                    .foregroundStyle(.white)
                                    .lineLimit(2)
                                    .shadow(color: .black.opacity(0.5), radius: 4, x: 0, y: 2)

                                // Episode count badge
                                HStack(spacing: 4) {
                                    Image(systemName: "waveform")
                                        .font(.caption2.weight(.semibold))
                                    Text(episodeCountText)
                                        .font(.caption.weight(.semibold))
                                }
                                .foregroundStyle(.white.opacity(0.9))
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(.black.opacity(0.35), in: Capsule())
                            }
                            .padding(.horizontal, 14)
                            .padding(.bottom, 14)
                        }
                    }
                    .clipped()
                    .clipShape(
                        UnevenRoundedRectangle(
                            topLeadingRadius: 18,
                            bottomLeadingRadius: 18,
                            bottomTrailingRadius: 18,
                            topTrailingRadius: 18,
                            style: .continuous
                        )
                    )
            }
        }
    }
}

// MARK: - Route

struct PodcastRoute: Hashable {
    let podcastId: String
    let podcastTitle: String
}
