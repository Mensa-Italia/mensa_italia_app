import SwiftUI
import Shared

// File-private bridge: Data → KotlinByteArray (mirrors the helper in AddEventView.swift).
private extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let result = KotlinByteArray(size: Int32(count))
        for (i, byte) in self.enumerated() {
            result.set(index: Int32(i), value: Int8(bitPattern: byte))
        }
        return result
    }
}

@MainActor @Observable
final class SigListViewModel {
    var sigs: [SigModel] = []
    var loading = false
    var error: String? = nil

    /// Session-stable: il potere "sigs" non cambia in-session. Letto sincrono
    /// dall'auth così il `+` in toolbar appare al primo frame.
    var canControl: Bool {
        hasPower("sigs", user: koin.auth.currentUser.value as? UserModel)
    }

    private var sub: Closeable?

    func start() {
        sub = FlowBridgeKt.subscribe(
            flow: koin.sigs.observeAll(),
            onEach: { [weak self] value in
                Task { @MainActor in
                    self?.sigs = (value as? [SigModel]) ?? []
                }
            },
            onError: { _ in }
        )
        Task { await refresh() }
    }

    func stop() { sub?.close() }

    func refresh() async {
        loading = true
        defer { loading = false }
        do {
            try await koin.sigs.refresh(filter: nil, sort: "name")
        } catch {
            self.error = (error as NSError).localizedDescription
        }
    }

    func create(payload: SigDraftPayload) async {
        let draft = SigDraft(
            name: payload.name,
            link: payload.link,
            groupType: payload.groupType.rawValue,
            description: "",
            imageBytes: payload.imageData?.toKotlinByteArray(),
            imageFilename: payload.imageData != nil ? (payload.imageFilename ?? "cover.jpg") : nil,
            imageContentType: payload.imageContentType ?? "image/jpeg"
        )
        do { _ = try await koin.sigs.create(draft: draft) }
        catch { self.error = (error as NSError).localizedDescription }
    }

    func update(id: String, payload: SigDraftPayload) async {
        let draft = SigDraft(
            name: payload.name,
            link: payload.link,
            groupType: payload.groupType.rawValue,
            description: "",
            imageBytes: payload.imageData?.toKotlinByteArray(),
            imageFilename: payload.imageData != nil ? (payload.imageFilename ?? "cover.jpg") : nil,
            imageContentType: payload.imageContentType ?? "image/jpeg"
        )
        do { _ = try await koin.sigs.update(id: id, draft: draft) }
        catch { self.error = (error as NSError).localizedDescription }
    }

    func delete(id: String) async {
        do { try await koin.sigs.delete(id: id) }
        catch { self.error = (error as NSError).localizedDescription }
    }
}

/// Lightweight `Identifiable` wrapper so we can drive `.sheet(item:)` from a
/// `SigModel` (the KMP-generated class isn't `Identifiable` in Swift).
private struct EditingSig: Identifiable {
    let sig: SigModel
    var id: String { sig.id }
}

private struct PendingDelete: Identifiable {
    let sig: SigModel
    var id: String { sig.id }
}

/// Community filter chip values. "all" matches every group, the others
/// use a substring match against `SigModel.groupType` (which contains
/// values like `sig`, `sig_facebook`, `chat`, `local`, …).
private enum CommunityFilter: Hashable {
    case all
    case type(String) // canonical key, e.g. "sig", "chat", "local"

    var key: String {
        switch self {
        case .all: return "all"
        case .type(let k): return k
        }
    }

    func matches(_ groupType: String) -> Bool {
        switch self {
        case .all: return true
        case .type(let k): return groupType.lowercased().contains(k)
        }
    }
}

private enum CommunityType {
    /// Friendly label for a known group_type key. Falls back to the raw
    /// (prettified) string for any future type we haven't mapped.
    static func label(forKey key: String) -> String {
        switch key {
        case "all":    return tr("community.filter.all", fallback: "Tutti")
        case "sig":    return tr("community.filter.sig", fallback: "SIG")
        case "chat":   return tr("community.filter.telegram", fallback: "Gruppi Telegram")
        case "local":  return tr("community.filter.local", fallback: "Gruppi ufficiali")
        default:
            return key
                .replacingOccurrences(of: "_", with: " ")
                .capitalized
        }
    }

    /// Short chip label shown overlaid on each tile.
    static func shortLabel(forGroupType raw: String) -> String {
        let lower = raw.lowercased()
        if lower.contains("chat")  { return tr("community.filter.telegram", fallback: "Gruppi Telegram") }
        if lower.contains("local") { return tr("community.filter.local", fallback: "Gruppi ufficiali") }
        if lower.contains("sig")   { return tr("community.filter.sig", fallback: "SIG") }
        return raw.replacingOccurrences(of: "_", with: " ").capitalized
    }

    static func systemIcon(forGroupType raw: String) -> String {
        let lower = raw.lowercased()
        if lower.contains("facebook") { return "f.cursive.circle.fill" }
        if lower.contains("chat")     { return "paperplane.fill" }
        if lower.contains("local")    { return "mappin.and.ellipse" }
        return "person.3.fill"
    }

    /// Canonical filter key for a raw group_type ("sig_facebook" → "sig").
    static func canonicalKey(forGroupType raw: String) -> String? {
        let lower = raw.lowercased()
        if lower.contains("chat")  { return "chat" }
        if lower.contains("local") { return "local" }
        if lower.contains("sig")   { return "sig" }
        guard !lower.isEmpty else { return nil }
        return lower
    }
}

struct SigListView: View {
    @State private var vm = SigListViewModel()
    @State private var appeared = false
    @State private var filter: CommunityFilter = .all
    @State private var query: String = ""
    @State private var showCreate: Bool = false
    @State private var editingSig: EditingSig? = nil
    @State private var pendingDelete: PendingDelete? = nil

    /// Stable, ordered list of filter keys present in the current dataset.
    /// "sig" / "chat" / "local" surface first (in that order); anything
    /// else is appended alphabetically so future group_types appear too.
    private var availableFilters: [CommunityFilter] {
        var seen: Set<String> = []
        var canonicalKeys: [String] = []
        for s in vm.sigs {
            guard let key = CommunityType.canonicalKey(forGroupType: s.groupType) else { continue }
            if seen.insert(key).inserted {
                canonicalKeys.append(key)
            }
        }
        let preferredOrder = ["sig", "chat", "local"]
        let ordered = preferredOrder.filter(seen.contains)
            + canonicalKeys.filter { !preferredOrder.contains($0) }.sorted()
        return [.all] + ordered.map { CommunityFilter.type($0) }
    }

    private var filteredSigs: [SigModel] {
        let base = vm.sigs.filter { filter.matches($0.groupType) }
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return base }
        return base.filter {
            $0.name.lowercased().contains(q)
                || $0.description_.lowercased().contains(q)
        }
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(Array(filteredSigs.enumerated()), id: \.element.id) { idx, sig in
                    NavigationLink(value: sig.id) {
                        SigRowCard(sig: sig)
                    }
                    .buttonStyle(.plain)
                    .modifier(StaggerAppear(index: idx))
                    .contextMenu {
                        if !sig.link.isEmpty, let url = URL(string: sig.link) {
                            Link(destination: url) {
                                Label(tr("app.open_link", fallback: "Apri link"), systemImage: "safari")
                            }
                        }
                        if vm.canControl {
                            Button {
                                editingSig = EditingSig(sig: sig)
                            } label: {
                                Label(
                                    tr("sigs.action.edit", fallback: "Modifica"),
                                    systemImage: "pencil"
                                )
                            }
                            Button(role: .destructive) {
                                pendingDelete = PendingDelete(sig: sig)
                            } label: {
                                Label(
                                    tr("sigs.action.delete", fallback: "Elimina"),
                                    systemImage: "trash"
                                )
                            }
                        }
                    }
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        if vm.canControl {
                            Button(role: .destructive) {
                                pendingDelete = PendingDelete(sig: sig)
                            } label: {
                                Label(
                                    tr("sigs.action.delete", fallback: "Elimina"),
                                    systemImage: "trash"
                                )
                            }
                            Button {
                                editingSig = EditingSig(sig: sig)
                            } label: {
                                Label(
                                    tr("sigs.action.edit", fallback: "Modifica"),
                                    systemImage: "pencil"
                                )
                            }
                            .tint(AppTheme.Colors.brandPrimary)
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 12)
            .animation(.spring(response: 0.45, dampingFraction: 0.88), value: filter)
        }
        .refreshable { await vm.refresh() }
        .overlay {
            if vm.loading && vm.sigs.isEmpty {
                LoadingDots()
            } else if vm.sigs.isEmpty {
                ContentUnavailableView(
                    tr("community.empty", fallback: "Nessun gruppo"),
                    systemImage: "person.3",
                    description: Text(tr("community.empty_description", fallback: "Non ci sono gruppi disponibili al momento."))
                )
            } else if filteredSigs.isEmpty {
                ContentUnavailableView(
                    tr("community.no_matches", fallback: "Nessun risultato"),
                    systemImage: "magnifyingglass",
                    description: Text(tr("community.no_matches_description", fallback: "Prova un altro filtro o un'altra ricerca."))
                )
            }
        }
        .navigationTitle(tr("community.list.title", fallback: "Community"))
        .cleanNavBar()
        .searchable(
            text: $query,
            placement: .navigationBarDrawer(displayMode: .automatic),
            prompt: Text(tr("community.search.prompt", fallback: "Cerca un gruppo"))
        )
        .toolbar {
            filterToolbarItem
            addToolbarItem
        }
        .navigationDestination(for: String.self) { sigId in
            SigDetailView(sigId: sigId)
        }
        .task {
            vm.start()
            withAnimation { appeared = true }
        }
        .onDisappear { vm.stop() }
        .sheet(isPresented: $showCreate) {
            AddSigSheet(
                initial: nil,
                onSubmitted: { payload in
                    Task {
                        await vm.create(payload: payload)
                        showCreate = false
                    }
                },
                onCancelled: { showCreate = false }
            )
        }
        .sheet(item: $editingSig) { editing in
            AddSigSheet(
                initial: editing.sig,
                onSubmitted: { payload in
                    Task {
                        await vm.update(id: editing.sig.id, payload: payload)
                        editingSig = nil
                    }
                },
                onDeleteRequested: {
                    Task {
                        await vm.delete(id: editing.sig.id)
                        editingSig = nil
                    }
                },
                onCancelled: { editingSig = nil }
            )
        }
        .alert(
            tr("sigs.delete.confirm.title", fallback: "Eliminare?"),
            isPresented: Binding(
                get: { pendingDelete != nil },
                set: { if !$0 { pendingDelete = nil } }
            ),
            presenting: pendingDelete
        ) { item in
            Button(tr("app.cancel", fallback: "Annulla"), role: .cancel) {
                pendingDelete = nil
            }
            Button(tr("sigs.action.delete", fallback: "Elimina"), role: .destructive) {
                Task {
                    await vm.delete(id: item.sig.id)
                    pendingDelete = nil
                }
            }
        } message: { _ in
            Text(tr(
                "sigs.delete.confirm.body",
                fallback: "L'azione non è annullabile."
            ))
        }
        .alert(tr("app.error.title", fallback: "Errore"), isPresented: Binding(
            get: { vm.error != nil },
            set: { if !$0 { vm.error = nil } }
        )) {
            Button("OK") { vm.error = nil }
        } message: {
            Text(vm.error ?? "")
        }
    }

    // MARK: - Add toolbar item

    @ToolbarContentBuilder private var addToolbarItem: some ToolbarContent {
        if vm.canControl {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showCreate = true
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .accessibilityLabel(Text(tr(
                            "sigs.action.add",
                            fallback: "Aggiungi community"
                        )))
                }
                .tint(AppTheme.Colors.brandTintAdaptive)
            }
        }
    }

    // MARK: - Filter (toolbar Menu)

    /// Il bottone filtro è SEMPRE visibile — non sappiamo ancora QUALI
    /// filtri verranno popolati (dipende dai dati che stanno caricando) ma
    /// sappiamo che ci saranno. Mostrare il bottone subito evita lo "snap"
    /// in toolbar quando arrivano le sigs.
    @ToolbarContentBuilder private var filterToolbarItem: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Menu {
                Picker(selection: $filter, label: EmptyView()) {
                    ForEach(availableFilters, id: \.self) { f in
                        Text(CommunityType.label(forKey: f.key)).tag(f)
                    }
                }
            } label: {
                Image(systemName: filter == .all
                      ? "line.3.horizontal.decrease.circle"
                      : "line.3.horizontal.decrease.circle.fill")
                    .accessibilityLabel(Text(tr(
                        "community.filter.label",
                        fallback: "Filtra gruppi"
                    )))
            }
            .tint(AppTheme.Colors.brandTintAdaptive)
        }
    }
}

/// Full-width community card mirroring EventRowCard: aspect-fit image
/// (preserves rectangular community art), type chip overlaid on top-leading
/// with a readability scrim, and title underneath.
private struct SigRowCard: View {
    let sig: SigModel

    private var imageURL: URL? {
        guard !sig.image.isEmpty else { return nil }
        if sig.image.hasPrefix("http") { return URL(string: sig.image) }
        return Files.url(
            collection: "sigs",
            recordId: sig.id,
            filename: sig.image,
            thumb: "800x0"
        )
    }

    private var gradientPlaceholder: some View {
        LinearGradient(
            colors: [
                AppTheme.Colors.brandPrimary.opacity(0.55),
                AppTheme.Colors.brandSecondary.opacity(0.55)
            ],
            startPoint: .topLeading, endPoint: .bottomTrailing
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if imageURL != nil {
                heroImage
            }
            metaBlock
        }
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .glassEffect(.regular, in: .rect(cornerRadius: 20))
    }

    @ViewBuilder
    private var heroImage: some View {
        ZStack(alignment: .topLeading) {
            Group {
                if let url = imageURL {
                    CachedAsyncImage(url: url) { img in
                        img.resizable().aspectRatio(contentMode: .fit)
                    } placeholder: {
                        gradientPlaceholder.frame(height: 160)
                    }
                }
            }
            .frame(maxWidth: .infinity)
            .frame(maxHeight: 200)
            .clipped()

            // Top scrim for chip readability.
            LinearGradient(
                colors: [Color.black.opacity(0.45), Color.black.opacity(0.0)],
                startPoint: .top, endPoint: .bottom
            )
            .frame(height: 70)
            .allowsHitTesting(false)

            if !sig.groupType.isEmpty {
                overlayChip
                    .padding(.horizontal, 12)
                    .padding(.top, 12)
            }
        }
    }

    /// Chip "dark glass" per overlay sopra l'immagine — stesso pattern usato
    /// in `EventRowCard`: solid 78% nero, bordo brand, white text. Contrasto
    /// garantito sia in light che in dark mode su qualunque immagine.
    private var overlayChip: some View {
        HStack(spacing: 6) {
            Image(systemName: CommunityType.systemIcon(forGroupType: sig.groupType))
                .font(.caption2)
            Text(CommunityType.shortLabel(forGroupType: sig.groupType))
                .font(.caption2.weight(.semibold))
                .lineLimit(1)
        }
        .padding(.horizontal, 9).padding(.vertical, 4)
        .foregroundStyle(.white)
        .background(Color.black.opacity(0.78), in: Capsule())
        .overlay(
            Capsule().strokeBorder(AppTheme.Colors.brandPrimary, lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.35), radius: 3, y: 1)
    }

    /// Variante tonal del chip per la modalità "senza hero": vive dentro il
    /// meta block su superficie chiara → bg tonal tint, testo tinted.
    private var inlineChip: some View {
        HStack(spacing: 6) {
            Image(systemName: CommunityType.systemIcon(forGroupType: sig.groupType))
                .font(.caption2)
            Text(CommunityType.shortLabel(forGroupType: sig.groupType))
                .font(.caption2.weight(.semibold))
                .lineLimit(1)
        }
        .padding(.horizontal, 8).padding(.vertical, 3)
        .foregroundStyle(AppTheme.Colors.brandPrimary)
        .background(AppTheme.Colors.brandPrimary.opacity(0.15), in: Capsule())
    }

    @ViewBuilder
    private var metaBlock: some View {
        VStack(alignment: .leading, spacing: 6) {
            // Quando manca l'hero, il chip categoria vive inline (versione
            // tonal). Con hero, il chip è già overlay sull'immagine.
            if imageURL == nil, !sig.groupType.isEmpty {
                inlineChip
                    .padding(.bottom, 2)
            }

            Text(sig.name)
                .font(.headline.bold())
                .lineLimit(2)
                .foregroundStyle(.primary)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)

            if !sig.description_.isEmpty {
                Text(sig.description_)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
    }
}

#Preview {
    NavigationStack {
        SigListView()
    }
}
