import SwiftUI
import Shared

// MARK: - Route

struct LocalOfficeLinktreeRoute: Hashable {
    let officeId: String
}

// MARK: - View

struct LocalOfficeLinktreeView: View {
    let officeId: String

    // Resolved office (for hero)
    @State private var office: LocalOfficeModel?

    // Per-office data
    @State private var linktree: [LocalOfficeLinktreeRowModel] = []
    @State private var admins: [LocalOfficeAdminModel]       = []
    @State private var assistants: [LocalOfficeAssistantModel]   = []

    // Editor state
    @State private var creatingLinkMode: LinkEditorMode?
    @State private var editingLink: LocalOfficeLinktreeRowModel?
    @State private var linkToDelete: LocalOfficeLinktreeRowModel?
    @State private var showDeleteLinkConfirm = false

    // Edit mode for drag-to-reorder
    @State private var editMode: EditMode = .inactive

    // Flow subscriptions
    @State private var linktreeSub: Closeable?
    @State private var adminsSub: Closeable?
    @State private var assistantsSub: Closeable?

    // MARK: - Permission gate

    /// Session-stable: cambia solo a login/logout.
    private var currentUser: UserModel? {
        koin.auth.currentUser.value as? UserModel
    }

    /// I poteri di edit hanno due path:
    ///   1. Utente "super" → autorizzato globalmente, deciso SINCRONO al
    ///      mount della view, nessuna attesa di flow.
    ///   2. Utente admin/assistant di questo specifico local office →
    ///      richiede `admins`/`assistants` dal flow, quindi async per forza.
    private var canEdit: Bool {
        guard let user = currentUser else { return false }
        if user.powers.contains("super") { return true }
        let adminIds = admins.map { $0.user }
        let assistantIds = assistants.map { $0.user }
        return adminIds.contains(user.id) || assistantIds.contains(user.id)
    }

    // MARK: - Sorted linktree helpers

    private var sorted: [LocalOfficeLinktreeRowModel] {
        linktree.sorted { $0.sortOrder < $1.sortOrder }
    }

    private var rootLinks: [LocalOfficeLinktreeRowModel] {
        sorted.filter { $0.parent == "" && $0.kind == "link" }
    }

    private var sections: [LocalOfficeLinktreeRowModel] {
        sorted.filter { $0.parent == "" && $0.kind == "section" }
    }

    private func children(of section: LocalOfficeLinktreeRowModel) -> [LocalOfficeLinktreeRowModel] {
        sorted.filter { $0.parent == section.id }
    }

    // MARK: - Body

    var body: some View {
        Group {
            if canEdit {
                editableContent
            } else {
                readOnlyContent
            }
        }
        .navigationTitle(tr("local_office.linktree.title", fallback: "Link utili"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if canEdit {
                ToolbarItem(placement: .primaryAction) {
                    Menu {
                        Button {
                            creatingLinkMode = .createSection
                        } label: {
                            Label(
                                tr("local_office.linktree.add_section", fallback: "Aggiungi sezione"),
                                systemImage: "folder.badge.plus"
                            )
                        }
                        Button {
                            creatingLinkMode = .createLink
                        } label: {
                            Label(
                                tr("local_office.linktree.add_link", fallback: "Aggiungi link"),
                                systemImage: "link.badge.plus"
                            )
                        }
                    } label: {
                        Image(systemName: "plus")
                    }
                }
                ToolbarItem(placement: .secondaryAction) {
                    EditButton()
                }
            }
        }
        .environment(\.editMode, $editMode)
        .task { await start() }
        .onDisappear { stop() }
        .sheet(item: $creatingLinkMode) { linkMode in
            LocalOfficeLinkEditorSheet(
                officeId: officeId,
                siblings: linktree,
                parentCandidates: linktree.filter { $0.kind == "section" },
                mode: linkMode
            )
        }
        .sheet(item: $editingLink) { link in
            LocalOfficeLinkEditorSheet(
                officeId: officeId,
                siblings: linktree.filter { $0.parent == link.parent },
                parentCandidates: linktree.filter { $0.kind == "section" },
                mode: .edit(existing: link)
            )
        }
        .alert(
            tr("local_office.links.delete_confirm", fallback: "Vuoi eliminare questa voce?"),
            isPresented: $showDeleteLinkConfirm,
            presenting: linkToDelete
        ) { link in
            Button(tr("local_office.editor.delete", fallback: "Elimina"), role: .destructive) {
                Task { await deleteLink(link) }
            }
            Button(tr("local_office.editor.cancel", fallback: "Annulla"), role: .cancel) {}
        } message: { link in
            Text(link.title)
        }
    }

    // MARK: - Editable content (List + onMove)

    @ViewBuilder
    private var editableContent: some View {
        List {
            // Compact hero
            if let office = office {
                Section {
                    heroRow(office)
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            }

            // Root-level links
            if !rootLinks.isEmpty {
                Section {
                    ForEach(rootLinks, id: \.id) { link in
                        editableLinktreeRow(link)
                    }
                    .onMove { from, to in
                        Task { await moveRootLinks(from: from, to: to) }
                    }
                }
            }

            // Sections with their children
            ForEach(sections, id: \.id) { section in
                Section {
                    ForEach(children(of: section), id: \.id) { link in
                        editableLinktreeRow(link)
                    }
                    .onMove { from, to in
                        Task { await moveChildren(of: section, from: from, to: to) }
                    }
                } header: {
                    editableSectionHeader(section)
                }
            }

            // Empty state
            if linktree.isEmpty {
                Section {
                    Text(tr("local_office.linktree.empty", fallback: "Nessun link"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.vertical, 16)
                }
                .listRowBackground(Color.clear)
            }
        }
        .listStyle(.insetGrouped)
    }

    @ViewBuilder
    private func editableLinktreeRow(_ link: LocalOfficeLinktreeRowModel) -> some View {
        HStack(spacing: 12) {
            iconView(link.icon)
                .frame(width: 24, height: 24)
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)

            Text(link.title)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.primary)
                .lineLimit(1)

            Spacer(minLength: 0)

            Menu {
                Button {
                    editingLink = link
                } label: {
                    Label(tr("local_office.linktree.edit", fallback: "Modifica"), systemImage: "pencil")
                }
                Button(role: .destructive) {
                    linkToDelete = link
                    showDeleteLinkConfirm = true
                } label: {
                    Label(tr("local_office.editor.delete", fallback: "Elimina"), systemImage: "trash")
                }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .font(.body)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }

    @ViewBuilder
    private func editableSectionHeader(_ section: LocalOfficeLinktreeRowModel) -> some View {
        HStack {
            Text(section.title.uppercased())
                .font(.caption2.weight(.semibold))
                .tracking(1.4)
                .foregroundStyle(.secondary)

            Spacer(minLength: 0)

            Menu {
                Button {
                    editingLink = section
                } label: {
                    Label(tr("local_office.linktree.edit", fallback: "Modifica"), systemImage: "pencil")
                }
                Button(role: .destructive) {
                    linkToDelete = section
                    showDeleteLinkConfirm = true
                } label: {
                    Label(tr("local_office.editor.delete", fallback: "Elimina"), systemImage: "trash")
                }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
    }

    // MARK: - Read-only content

    @ViewBuilder
    private var readOnlyContent: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 8) {
                // Compact hero
                if let office = office {
                    heroRow(office)
                        .padding(.horizontal, 20)
                        .padding(.bottom, 8)
                }

                // Root-level links
                ForEach(rootLinks, id: \.id) { link in
                    readOnlyLinktreeRow(link)
                        .padding(.horizontal, 20)
                }

                // Sections with children
                ForEach(sections, id: \.id) { section in
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(section.title.uppercased())
                                .font(.caption2.weight(.semibold))
                                .tracking(1.4)
                                .foregroundStyle(.secondary)
                                .padding(.horizontal, 4)
                            Spacer(minLength: 0)
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 8)

                        ForEach(children(of: section), id: \.id) { link in
                            readOnlyLinktreeRow(link)
                                .padding(.horizontal, 20)
                        }
                    }
                }

                if linktree.isEmpty {
                    Text(tr("local_office.linktree.empty", fallback: "Nessun link"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.vertical, 24)
                }

                Color.clear.frame(height: 32)
            }
            .padding(.top, 16)
        }
    }

    @ViewBuilder
    private func readOnlyLinktreeRow(_ row: LocalOfficeLinktreeRowModel) -> some View {
        Button {
            if let url = URL(string: row.url) {
                UIApplication.shared.open(url)
            }
        } label: {
            HStack(spacing: 12) {
                iconView(row.icon)
                    .frame(width: 24, height: 24)
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)

                Text(row.title)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.primary)
                    .lineLimit(1)

                Spacer(minLength: 0)

                Image(systemName: "chevron.up.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
            }
            .frame(height: 52)
            .padding(.horizontal, 16)
            .glassEffect(.regular, in: .rect(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }

    // MARK: - Compact hero

    @ViewBuilder
    private func heroRow(_ office: LocalOfficeModel) -> some View {
        let coverURL: URL? = {
            guard !office.image.isEmpty else { return nil }
            return Files.url(
                collection: "local_offices",
                recordId: office.id,
                filename: office.image,
                thumb: "0x500"
            )
        }()

        HStack(spacing: 12) {
            Group {
                if let url = coverURL {
                    CachedAsyncImage(url: url) { img in
                        img.resizable().scaledToFill()
                    } placeholder: {
                        AppTheme.brandGradient
                    }
                } else {
                    AppTheme.brandGradient
                        .overlay(
                            Image(systemName: "building.2.fill")
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundStyle(.white.opacity(0.85))
                        )
                }
            }
            .frame(width: 60, height: 60)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            VStack(alignment: .leading, spacing: 2) {
                Text(office.name)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(.primary)
                    .lineLimit(1)

                if !office.region.isEmpty {
                    Text(office.region)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }

            Spacer(minLength: 0)
        }
        .padding(.vertical, 8)
    }

    // MARK: - Icon helper

    @ViewBuilder
    private func iconView(_ raw: String) -> some View {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.unicodeScalars.count == 1 && trimmed.unicodeScalars.first.map({ $0.value > 127 }) == true {
            Text(trimmed).font(.system(size: 18))
        } else if let sfForBrand = brandSFSymbol(trimmed.lowercased()) {
            Image(systemName: sfForBrand)
        } else if !trimmed.isEmpty && UIImage(systemName: trimmed) != nil {
            Image(systemName: trimmed)
        } else {
            Image(systemName: "link")
        }
    }

    private func brandSFSymbol(_ name: String) -> String? {
        switch name {
        case "instagram":  return "camera.filters"
        case "facebook":   return "f.cursive.circle.fill"
        case "twitter":    return "bird.fill"
        case "tiktok":     return "music.note"
        case "youtube":    return "play.rectangle.fill"
        case "telegram":   return "paperplane.fill"
        case "whatsapp":   return "message.fill"
        case "linkedin":   return "person.crop.rectangle.stack.fill"
        case "github":     return "chevron.left.forwardslash.chevron.right"
        case "email":      return "envelope.fill"
        default:           return nil
        }
    }

    // MARK: - Drag-to-reorder

    private func moveRootLinks(from: IndexSet, to: Int) async {
        var reordered = rootLinks
        reordered.move(fromOffsets: from, toOffset: to)
        await persistOrder(reordered)
    }

    private func moveChildren(of section: LocalOfficeLinktreeRowModel, from: IndexSet, to: Int) async {
        var reordered = children(of: section)
        reordered.move(fromOffsets: from, toOffset: to)
        await persistOrder(reordered)
    }

    private func persistOrder(_ items: [LocalOfficeLinktreeRowModel]) async {
        await withTaskGroup(of: Void.self) { group in
            for (index, item) in items.enumerated() {
                let newOrder = index + 1
                if item.sortOrder != Int32(newOrder) {
                    group.addTask {
                        _ = try? await koin.localOffices.updateLinkFields(
                            officeId: officeId,
                            id: item.id,
                            kind: nil,
                            parent: nil,
                            title: nil,
                            url: nil,
                            icon: nil,
                            sortOrder: KotlinInt(integerLiteral: newOrder),
                            active: nil
                        )
                    }
                }
            }
        }
    }

    // MARK: - Delete

    private func deleteLink(_ link: LocalOfficeLinktreeRowModel) async {
        do {
            try await koin.localOffices.deleteLink(officeId: officeId, id: link.id)
        } catch {}
    }

    // MARK: - Data lifecycle

    private func start() async {
        // Resolve office for the hero
        if let resolved = try? await koin.localOffices.officeById(id: officeId) {
            await MainActor.run { office = resolved }
        }

        subscribeAll()
    }

    private func subscribeAll() {
        linktreeSub?.close()
        linktreeSub = FlowBridgeKt.subscribe(
            flow: koin.localOffices.observeLinktree(officeId: officeId),
            onEach: { value in
                Task { @MainActor in
                    self.linktree = (value as? [LocalOfficeLinktreeRowModel]) ?? []
                }
            },
            onError: { _ in }
        )

        adminsSub?.close()
        adminsSub = FlowBridgeKt.subscribe(
            flow: koin.localOffices.observeAdmins(officeId: officeId),
            onEach: { value in
                Task { @MainActor in
                    self.admins = (value as? [LocalOfficeAdminModel]) ?? []
                }
            },
            onError: { _ in }
        )

        assistantsSub?.close()
        assistantsSub = FlowBridgeKt.subscribe(
            flow: koin.localOffices.observeAssistants(officeId: officeId),
            onEach: { value in
                Task { @MainActor in
                    self.assistants = (value as? [LocalOfficeAssistantModel]) ?? []
                }
            },
            onError: { _ in }
        )
    }

    private func stop() {
        linktreeSub?.close();    linktreeSub = nil
        adminsSub?.close();      adminsSub = nil
        assistantsSub?.close();  assistantsSub = nil
    }
}
