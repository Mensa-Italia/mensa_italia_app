import SwiftUI
import Shared

/// Public-area variant of `LocalOfficesListView`: lista nativa dei gruppi
/// locali, pre-login. Niente card custom — riga `List` + `NavigationLink`
/// standard, iOS 26 applica Liquid Glass alle righe.
struct PublicLocalOfficesListView: View {
    @State private var offices: [LocalOfficeModel] = []
    @State private var refreshing = false
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
        List {
            ForEach(filtered, id: \.id) { office in
                // NavigationLink con destination inline — niente
                // `.navigationDestination(for:)` perche' quando lo
                // registri dentro la view stessa che lo deve risolvere puo'
                // dare effetti strani (tap che torna indietro invece di
                // pushare). Push diretto = comportamento certo.
                NavigationLink {
                    PublicLocalOfficeDetailView(officeId: office.id)
                } label: {
                    PublicLocalOfficeRow(office: office)
                }
            }
        }
        .listStyle(.insetGrouped)
        .overlay {
            if offices.isEmpty && refreshing {
                ProgressView()
            } else if offices.isEmpty {
                ContentUnavailableView(
                    tr("public.local_offices.empty.title", fallback: "Nessun gruppo locale"),
                    systemImage: "building.2",
                    description: Text(tr(
                        "public.local_offices.empty.description",
                        fallback: "Non sono ancora disponibili gruppi locali."
                    ))
                )
            } else if filtered.isEmpty {
                ContentUnavailableView.search(text: query)
            }
        }
        .refreshable { await refresh() }
        .navigationTitle(tr("public.local_offices.title", fallback: "Gruppi locali"))
        .navigationBarTitleDisplayMode(.large)
        .searchable(
            text: $query,
            placement: .navigationBarDrawer(displayMode: .automatic),
            prompt: Text(tr("public.local_offices.search_prompt", fallback: "Cerca per regione"))
        )
        .task { start() }
        .onDisappear { stop() }
    }

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
        do { try await koin.localOffices.refreshAllOfficesPublic() } catch { }
    }
}

/// Navigation value for the public detail view. Kept separate from
/// `LocalOfficeRoute` so the public list never accidentally lands users on
/// the authenticated detail screen.
struct PublicLocalOfficeRoute: Hashable {
    let officeId: String
}

private struct PublicLocalOfficeRow: View {
    let office: LocalOfficeModel

    private var thumbURL: URL? {
        guard !office.image.isEmpty else { return nil }
        // Path della view pubblica (no auth richiesta) — i binari sono gli
        // stessi della collection origin.
        return Files.url(
            collection: "view_local_office",
            recordId: office.id,
            filename: office.image,
            thumb: "0x500"
        )
    }

    var body: some View {
        HStack(spacing: 12) {
            Group {
                if let url = thumbURL {
                    CachedAsyncImage(url: url) { img in
                        img.resizable().scaledToFill()
                    } placeholder: {
                        Image(systemName: "building.2")
                            .foregroundStyle(.secondary)
                    }
                } else {
                    Image(systemName: "building.2")
                        .foregroundStyle(.secondary)
                }
            }
            .frame(width: 44, height: 44)
            .background(Color(.tertiarySystemFill))
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))

            VStack(alignment: .leading, spacing: 2) {
                Text(office.name)
                    .font(.body)
                    .lineLimit(1)
                if !office.region.isEmpty {
                    Text(office.region)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
        }
        .padding(.vertical, 2)
    }
}
