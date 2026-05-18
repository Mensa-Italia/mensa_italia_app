import SwiftUI
import Shared

/// Index of every Local Office (regional group). Used as the landing screen
/// for the "Gruppi locali" entry in Discover. Each row is a wide magazine-feel
/// card with the office cover, name, region and bio — tap routes to
/// `LocalOfficeView` for the full linktree page.
///
/// Backed by `koin.localOffices.observeAllOffices()`; refreshable.
struct LocalOfficesListView: View {
    @State private var offices: [LocalOfficeModel] = []
    @State private var refreshing = false
    @State private var appeared = false
    @State private var sub: Closeable? = nil
    @State private var query: String = ""

    private var filtered: [LocalOfficeModel] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return offices }
        let needle = q.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
        return offices.filter { o in
            let hay = [o.name, o.region, o.bio]
                .joined(separator: " ")
                .folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
            return hay.contains(needle)
        }
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(Array(filtered.enumerated()), id: \.element.id) { idx, office in
                    // Route by PocketBase id — the only unambiguous primary key.
                    NavigationLink(value: LocalOfficeRoute(officeId: office.id)) {
                        LocalOfficeListCard(office: office)
                    }
                    .buttonStyle(.plain)
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 14)
                    .animation(
                        .spring(response: 0.5, dampingFraction: 0.86)
                            .delay(Double(min(idx, 12)) * 0.05),
                        value: appeared
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
        .refreshable { await refresh() }
        .overlay {
            if offices.isEmpty {
                if refreshing {
                    LoadingDots()
                } else {
                    ContentUnavailableView(
                        tr("local_offices.empty", fallback: "Nessun gruppo locale"),
                        systemImage: "building.2",
                        description: Text(tr(
                            "local_offices.empty_description",
                            fallback: "Non sono ancora disponibili gruppi locali."
                        ))
                    )
                }
            } else if filtered.isEmpty {
                ContentUnavailableView(
                    tr("local_offices.no_results", fallback: "Nessun risultato"),
                    systemImage: "magnifyingglass",
                    description: Text(tr(
                        "local_offices.no_results_description",
                        fallback: "Prova con un altro termine di ricerca."
                    ))
                )
            }
        }
        .navigationTitle(tr("local_offices.title", fallback: "Gruppi locali"))
        .cleanNavBar()
        .searchable(
            text: $query,
            placement: .navigationBarDrawer(displayMode: .automatic),
            prompt: Text(tr("local_offices.search_prompt", fallback: "Cerca per regione"))
        )
        .navigationDestination(for: LocalOfficeRoute.self) { route in
            LocalOfficeView(officeId: route.officeId)
        }
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
            flow: koin.localOffices.observeAllOffices(),
            onEach: { value in
                Task { @MainActor in
                    self.offices = (value as? [LocalOfficeModel]) ?? []
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
        do { try await koin.localOffices.refreshAllOffices() } catch { }
    }
}

// MARK: - Card

private struct LocalOfficeListCard: View {
    let office: LocalOfficeModel

    private var coverURL: URL? {
        guard !office.image.isEmpty else { return nil }
        return Files.url(
            collection: "local_offices",
            recordId: office.id,
            filename: office.image,
            thumb: "0x500"
        )
    }

    var body: some View {
        GlassCard(padding: 0, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 0) {
                // Cover image — locked 16:9 with `Color.clear` overlay technique
                // so portrait sources don't blow up the card height.
                Color.clear
                    .aspectRatio(16 / 9, contentMode: .fit)
                    .overlay {
                        CachedAsyncImage(url: coverURL) { img in
                            img.resizable().aspectRatio(contentMode: .fill)
                        } placeholder: {
                            AppTheme.brandGradient
                                .overlay(
                                    Image(systemName: "building.2.fill")
                                        .font(.system(size: 28, weight: .semibold))
                                        .foregroundStyle(.white.opacity(0.85))
                                )
                        }
                    }
                    .clipped()
                    .clipShape(
                        UnevenRoundedRectangle(
                            topLeadingRadius: 18,
                            bottomLeadingRadius: 0,
                            bottomTrailingRadius: 0,
                            topTrailingRadius: 18,
                            style: .continuous
                        )
                    )

                VStack(alignment: .leading, spacing: 6) {
                    Text(tr("local_offices.kicker", fallback: "GRUPPO LOCALE"))
                        .font(.caption2.weight(.semibold))
                        .tracking(1.4)
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)

                    Text(office.name)
                        .font(.title3.weight(.bold))
                        .foregroundStyle(.primary)
                        .lineLimit(1)

                    if !office.bio.isEmpty {
                        Text(office.bio)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}
