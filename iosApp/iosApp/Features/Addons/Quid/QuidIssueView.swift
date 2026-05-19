import SwiftUI
import Shared

/// Shows the articles inside a single Quid issue.
struct QuidIssueView: View {
    let issueId: Int64
    /// Optional — passed when navigating from the issues list. When entering via
    /// a deep link we only have the id, so the view resolves the name from
    /// `koin.quid.observeIssues()` (triggers a refresh if cache is cold).
    let initialName: String

    @State private var articles: [QuidArticle] = []
    @State private var resolvedName: String
    @State private var refreshing = false
    @State private var appeared = false
    @State private var sub: Closeable?
    @State private var issuesSub: Closeable?
    @State private var query: String = ""
    @State private var loadingPlaylist = false

    init(issueId: Int64, issueName: String = "") {
        self.issueId = issueId
        self.initialName = issueName
        self._resolvedName = State(initialValue: issueName)
    }

    /// Case/diacritic-insensitive filter over title, excerpt, categories.
    private var filtered: [QuidArticle] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return articles }
        let needle = q.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
        return articles.filter { a in
            let haystack = [a.titlePlain, a.excerptPlain, a.categoryNames.joined(separator: " ")]
                .joined(separator: " ")
                .folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
            return haystack.contains(needle)
        }
    }

    var body: some View {
        // Base is ALWAYS a ScrollView so `.searchable` has a scrollable anchor
        // even when the list is empty (otherwise the search bar flickers / fails
        // to render). Loading and empty states live in the overlay — same pattern
        // as EventListView.
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(Array(filtered.enumerated()), id: \.element.id) { idx, article in
                    // Destination inline: vedi commento in `PodcastsListView`.
                    NavigationLink {
                        QuidArticleView(articleId: article.id)
                    } label: {
                        QuidArticleCard(article: article)
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
            if articles.isEmpty {
                if refreshing {
                    LoadingDots()
                } else {
                    ContentUnavailableView(
                        tr("addons.quid.empty", fallback: "Nessun articolo"),
                        systemImage: "magazine",
                        description: Text(tr(
                            "addons.quid.empty_description",
                            fallback: "Non ci sono articoli disponibili al momento."
                        ))
                    )
                }
            } else if filtered.isEmpty {
                ContentUnavailableView(
                    tr("addons.quid.no_results", fallback: "Nessun risultato"),
                    systemImage: "magnifyingglass",
                    description: Text(tr(
                        "addons.quid.no_results_description",
                        fallback: "Prova con un altro termine di ricerca."
                    ))
                )
            }
        }
        .navigationTitle(resolvedName.isEmpty ? tr("addons.quid.title", fallback: "Quid") : resolvedName)
        .navigationBarTitleDisplayMode(.inline)
        .cleanNavBar()
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                if !articles.isEmpty {
                    Menu {
                        Button {
                            Task { await playAllAudio() }
                        } label: {
                            Label("Riproduci tutti gli audio", systemImage: "play.fill")
                        }
                        Button {
                            Task { await addAllAudioToQueue() }
                        } label: {
                            Label("Aggiungi alla coda", systemImage: "text.badge.plus")
                        }
                    } label: {
                        if loadingPlaylist {
                            ProgressView()
                                .tint(AppTheme.Colors.brandTintAdaptive)
                        } else {
                            Image(systemName: "play.circle.fill")
                                .font(.system(size: 22))
                                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                        }
                    }
                    .disabled(loadingPlaylist)
                }
            }
        }
        .searchable(
            text: $query,
            placement: .navigationBarDrawer(displayMode: .automatic),
            prompt: Text(tr("addons.quid.search_prompt", fallback: "Cerca articoli"))
        )
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
            flow: koin.quid.observeIssueArticles(issueId: issueId),
            onEach: { value in
                Task { @MainActor in
                    self.articles = (value as? [QuidArticle]) ?? []
                }
            },
            onError: { _ in }
        )

        // Resolve the issue name if we entered without it (deep link case).
        // Subscribe to the issues flow so we pick it up as soon as the cache
        // fills; also trigger a one-shot refresh in case it's empty.
        if resolvedName.isEmpty {
            issuesSub?.close()
            issuesSub = FlowBridgeKt.subscribe(
                flow: koin.quid.observeIssues(),
                onEach: { value in
                    let list = (value as? [QuidIssue]) ?? []
                    if let match = list.first(where: { $0.id == self.issueId }) {
                        Task { @MainActor in self.resolvedName = match.name }
                    }
                },
                onError: { _ in }
            )
            Task { try? await koin.quid.refreshIssues() }
        }

        Task { await refresh() }
    }

    private func stop() {
        sub?.close(); sub = nil
        issuesSub?.close(); issuesSub = nil
    }

    private func playAllAudio() async {
        loadingPlaylist = true
        defer { loadingPlaylist = false }

        let tracks = await fetchAllAudioTracks()
        guard !tracks.isEmpty else { return }
        AudioPlayerService.shared.playQueue(tracks)
    }

    private func addAllAudioToQueue() async {
        loadingPlaylist = true
        defer { loadingPlaylist = false }

        let tracks = await fetchAllAudioTracks()
        guard !tracks.isEmpty else { return }
        AudioPlayerService.shared.addToQueue(tracks)
    }

    /// Fetches audio for all articles in the issue, returning only those that have narration.
    /// Articles are fetched concurrently using a TaskGroup.
    private func fetchAllAudioTracks() async -> [AudioTrack] {
        await withTaskGroup(of: (Int, AudioTrack?).self) { group in
            for (index, article) in articles.enumerated() {
                group.addTask {
                    guard let audio = try? await koin.quid.getAudioForArticle(wpId: article.id) else {
                        return (index, nil)
                    }
                    let coverURL: URL? = article.coverImageUrl.flatMap { URL(string: $0) }
                    let track = QuidAudioFactory.makeTrack(
                        audio: audio,
                        articleId: article.id,
                        articleTitle: article.titlePlain,
                        artworkURL: coverURL
                    )
                    return (index, track)
                }
            }

            var results: [(Int, AudioTrack)] = []
            for await (index, track) in group {
                if let track { results.append((index, track)) }
            }
            // Sort by original article order
            return results.sorted { $0.0 < $1.0 }.map(\.1)
        }
    }

    private func refresh() async {
        refreshing = true
        defer { refreshing = false }
        do { try await koin.quid.refreshIssueArticles(issueId: issueId) } catch { }
    }
}
