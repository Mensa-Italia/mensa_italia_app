import SwiftUI
import Shared

// MARK: - Editor mode

enum LinkEditorMode {
    case createSection
    case createLink
    case edit(existing: LocalOfficeLinktreeRowModel)
}

// MARK: - Sheet

struct LocalOfficeLinkEditorSheet: View {
    let officeId: String
    /// All sibling entries sharing the same parent — used to auto-compute sort_order for new entries.
    let siblings: [LocalOfficeLinktreeRowModel]
    let parentCandidates: [LocalOfficeLinktreeRowModel]  // already filtered to kind == "section"
    let mode: LinkEditorMode

    // Form state
    @State private var kind: String          // "section" or "link"
    @State private var title: String
    @State private var url: String
    @State private var icon: String
    @State private var parentId: String      // "" = root
    @State private var sortOrder: Int        // computed, not exposed in UI
    @State private var active: Bool

    // UI state
    @State private var saving = false
    @State private var errorMessage: String? = nil
    @State private var showError = false

    @Environment(\.dismiss) private var dismiss

    init(officeId: String, siblings: [LocalOfficeLinktreeRowModel], parentCandidates: [LocalOfficeLinktreeRowModel], mode: LinkEditorMode) {
        self.officeId = officeId
        self.siblings = siblings
        self.mode = mode

        // Auto-compute sort_order = (max existing in same parent) + 1 for new entries.
        let maxOrder = siblings.map { Int($0.sortOrder) }.max() ?? 0

        switch mode {
        case .createSection:
            _kind = State(initialValue: "section")
            _title = State(initialValue: "")
            _url = State(initialValue: "")
            _icon = State(initialValue: "")
            _parentId = State(initialValue: "")
            _sortOrder = State(initialValue: maxOrder + 1)
            _active = State(initialValue: true)
            self.parentCandidates = parentCandidates
        case .createLink:
            _kind = State(initialValue: "link")
            _title = State(initialValue: "")
            _url = State(initialValue: "")
            _icon = State(initialValue: "")
            _parentId = State(initialValue: "")
            _sortOrder = State(initialValue: maxOrder + 1)
            _active = State(initialValue: true)
            self.parentCandidates = parentCandidates
        case .edit(let existing):
            _kind = State(initialValue: existing.kind)
            _title = State(initialValue: existing.title)
            _url = State(initialValue: existing.url)
            _icon = State(initialValue: existing.icon)
            _parentId = State(initialValue: existing.parent)
            _sortOrder = State(initialValue: Int(existing.sortOrder))
            _active = State(initialValue: true)   // view model doesn't expose active; default true
            // Exclude self from parent candidates (can't be its own parent)
            self.parentCandidates = parentCandidates.filter { $0.id != existing.id }
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                // Kind picker
                Section {
                    Picker(selection: $kind) {
                        Text(tr("local_office.editor.kind.section", fallback: "Sezione"))
                            .tag("section")
                        Text(tr("local_office.editor.kind.link", fallback: "Link"))
                            .tag("link")
                    } label: {
                        EmptyView()
                    }
                    .pickerStyle(.segmented)
                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                }

                // Title (required)
                Section {
                    TextField(
                        tr("local_office.editor.title", fallback: "Titolo"),
                        text: $title
                    )
                }

                // URL — only when kind == link
                if kind == "link" {
                    Section {
                        TextField(
                            tr("local_office.editor.url", fallback: "URL"),
                            text: $url
                        )
                        .keyboardType(.URL)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                    }
                }

                // Icon
                Section {
                    TextField(
                        tr("local_office.editor.icon", fallback: "Icona"),
                        text: $icon
                    )
                    Text(tr("local_office.editor.icon.hint", fallback: "Inserisci un emoji o il nome di un SF Symbol"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                // Parent picker — show when there are candidate sections
                if !parentCandidates.isEmpty {
                    Section(tr("local_office.editor.parent", fallback: "Sezione padre")) {
                        Picker(selection: $parentId) {
                            Text(tr("local_office.editor.parent.none", fallback: "Nessuna (root)"))
                                .tag("")
                            ForEach(parentCandidates, id: \.id) { section in
                                Text(section.title).tag(section.id)
                            }
                        } label: {
                            EmptyView()
                        }
                        .pickerStyle(.inline)
                        .labelsHidden()
                    }
                }

                // Active toggle
                Section {
                    Toggle(tr("local_office.editor.active", fallback: "Attivo"), isOn: $active)
                }
            }
            .navigationTitle(navTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(tr("local_office.editor.cancel", fallback: "Annulla")) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if saving {
                        ProgressView()
                    } else {
                        Button(tr("local_office.editor.save", fallback: "Salva")) {
                            Task { await save() }
                        }
                        .fontWeight(.semibold)
                        .disabled(title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
            }
            .alert(
                tr("app.error.title", fallback: "Errore"),
                isPresented: $showError,
                presenting: errorMessage
            ) { _ in
                Button("OK", role: .cancel) {}
            } message: { msg in
                Text(msg)
            }
        }
    }

    // MARK: - Helpers

    private var navTitle: String {
        switch mode {
        case .createSection:
            return tr("local_office.links.add_section", fallback: "Aggiungi sezione")
        case .createLink:
            return tr("local_office.links.add_link", fallback: "Aggiungi link")
        case .edit:
            return tr("local_office.links.edit", fallback: "Modifica voce")
        }
    }

    private var isCreating: Bool {
        switch mode {
        case .createSection, .createLink: return true
        case .edit: return false
        }
    }

    private func save() async {
        let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedTitle.isEmpty else { return }

        saving = true
        defer { saving = false }

        do {
            switch mode {
            case .createSection, .createLink:
                // Use the field-by-field create wrapper. Constructing the
                // `LocalOfficeLinkRecord` from Swift with a Boolean field
                // dropped the value across the K/N bridge (active landed as
                // `false` server-side even when the Toggle was `true`).
                _ = try await koin.localOffices.createLinkFromFields(
                    officeId: officeId,
                    kind: kind,
                    parent: parentId,
                    title: trimmedTitle,
                    url: url.trimmingCharacters(in: .whitespacesAndNewlines),
                    icon: icon.trimmingCharacters(in: .whitespacesAndNewlines),
                    sortOrder: Int32(sortOrder),
                    active: active
                )

            case .edit(let existing):
                _ = try await koin.localOffices.updateLinkFields(
                    officeId: officeId,
                    id: existing.id,
                    kind: kind,
                    parent: parentId,
                    title: trimmedTitle,
                    url: url.trimmingCharacters(in: .whitespacesAndNewlines),
                    icon: icon.trimmingCharacters(in: .whitespacesAndNewlines),
                    sortOrder: KotlinInt(integerLiteral: sortOrder),
                    active: KotlinBoolean(bool: active)
                )
            }
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
    }
}
