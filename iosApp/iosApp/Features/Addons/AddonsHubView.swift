import SwiftUI
import Shared

/// 2-column grid of addons available to the current user.
/// Cache-first via `koin.addons.observeAll()`, refresh in background.
struct AddonsHubView: View {
    @State private var addons: [AddonModel] = []
    @State private var refreshing = false
    @State private var appeared = false
    @State private var addonsSub: Closeable? = nil

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    /// User session-stable: cambia solo a login/logout. RootView smonta la
    /// view in entrambi i casi → lettura sincrona dall'auth.
    private var currentUser: UserModel? {
        koin.auth.currentUser.value as? UserModel
    }

    private var visibleAddons: [AddonModel] {
        addons.filter { userCanSeeAddon($0, user: currentUser) }
    }

    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(Array(visibleAddons.enumerated()), id: \.element.id) { idx, addon in
                    NavigationLink(value: AddonRoute.from(addon: addon)) {
                        AddonGridCell(addon: addon)
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
            .padding(.bottom, 24)
            .padding(.top, 8)
        }
        .refreshable { await refresh() }
        .overlay {
            if visibleAddons.isEmpty && !refreshing {
                ContentUnavailableView(
                    tr("addons.hub.empty", fallback: "Nessun addon disponibile"),
                    systemImage: "puzzlepiece.extension",
                    description: Text(tr(
                        "addons.hub.empty_description",
                        fallback: "Non hai accesso ad alcun addon al momento."
                    ))
                )
            }
        }
        .navigationTitle(tr("addons.hub.title", fallback: "Addon"))
        .cleanNavBar()
        .navigationDestination(for: AddonRoute.self) { route in
            switch route {
            case .stamp:
                TableportStampView()
            case .boutique:
                BoutiqueView()
            case .quid:
                QuidIssuesView()
            case .podcasts:
                PodcastsListView()
            case .external(let id, let url):
                ExternalAddonWebView(addonId: id, baseUrl: url)
            case .placeholder(let title):
                ContentUnavailableView(title, systemImage: "wrench.and.screwdriver")
                    .navigationTitle(title)
            }
        }
        .navigationDestination(for: BoutiqueProductRoute.self) { route in
            BoutiqueProductView(productId: route.productId)
        }
        .navigationDestination(for: QuidArticleRoute.self) { route in
            QuidArticleView(articleId: route.articleId)
        }
        .navigationDestination(for: PodcastRoute.self) { route in
            PodcastEpisodesView(podcastId: route.podcastId, podcastTitle: route.podcastTitle)
        }
        .task {
            start()
            withAnimation(.easeOut(duration: 0.35)) { appeared = true }
        }
        .onDisappear { stop() }
    }

    private func start() {
        addonsSub?.close()
        addonsSub = FlowBridgeKt.subscribe(
            flow: koin.addons.observeAll(),
            onEach: { value in
                Task { @MainActor in
                    self.addons = (value as? [AddonModel]) ?? []
                }
            },
            onError: { _ in }
        )

        Task { await refresh() }
    }

    private func stop() {
        addonsSub?.close(); addonsSub = nil
    }

    private func refresh() async {
        refreshing = true
        defer { refreshing = false }
        do { try await koin.addons.refresh() } catch { }
    }
}

// MARK: - Routing

enum AddonRoute: Hashable {
    case stamp
    case boutique
    case quid
    case podcasts
    case external(id: String, url: String)
    case placeholder(title: String)

    /// Map a Kotlin `AddonModel` to a Swift route.
    static func from(addon: AddonModel) -> AddonRoute {
        if !addon.url.isEmpty {
            return .external(id: addon.id, url: addon.url)
        }
        switch addon.id {
        case "stamp": return .stamp
        case "boutique": return .boutique
        case "quid": return .quid
        case "podcasts": return .podcasts
        default:
            return .placeholder(title: addon.name.isEmpty ? addon.id : addon.name)
        }
    }
}

struct BoutiqueProductRoute: Hashable {
    let productId: String
}

struct QuidArticleRoute: Hashable {
    let articleId: Int64
}

// MARK: - Cell

private struct AddonGridCell: View {
    let addon: AddonModel

    private var iconURL: URL? {
        guard !addon.icon.isEmpty else { return nil }
        return Files.url(collection: "addons", recordId: addon.id, filename: addon.icon)
    }

    var body: some View {
        GlassCard(padding: 14, cornerRadius: 16) {
            VStack(alignment: .leading, spacing: 10) {
                CachedAsyncImage(url: iconURL) { img in
                    img.resizable().aspectRatio(contentMode: .fit)
                } placeholder: {
                    AppTheme.brandGradient
                        .overlay(
                            Image(systemName: "puzzlepiece.extension.fill")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundStyle(.white.opacity(0.85))
                        )
                }
                .frame(width: 40, height: 40)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

                Text(addon.name.isEmpty ? addon.id : addon.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(1)

                if !addon.description_.isEmpty {
                    Text(addon.description_)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

