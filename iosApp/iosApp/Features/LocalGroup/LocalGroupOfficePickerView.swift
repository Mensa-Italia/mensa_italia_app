import Shared
import SwiftUI

/// Board only: the sheet that picks which office the "Gruppo locale" tab
/// shows for this session. The member's own office first, then every office
/// by name; a checkmark on the current choice. Mirrors Android
/// `LocalGroupOfficePicker`.
struct LocalGroupOfficePickerView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var store = LocalGroupStore.shared
    @State private var offices: [LocalOfficeModel] = []
    @State private var loaded = false
    @State private var failed = false
    @State private var query = ""

    /// Same matching as `LocalOfficesListView`: name and region, accents and case ignored.
    private var filtered: [LocalOfficeModel] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return offices }
        let needle = q.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
        return offices.filter { o in
            "\(o.name) \(o.region)"
                .folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
                .contains(needle)
        }
    }

    var body: some View {
        NavigationStack {
            List {
                if query.isEmpty {
                    Section {
                        row(title: tr("local_group.board.mine", fallback: "Il mio gruppo locale"),
                            subtitle: nil,
                            selected: store.selectedOfficeId == nil) { choose(nil) }
                    }
                }
                Section {
                    ForEach(filtered, id: \.id) { office in
                        row(title: office.name,
                            subtitle: office.region.isEmpty ? nil : office.region,
                            selected: store.selectedOfficeId == office.id) { choose(office.id) }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .overlay {
                if !loaded {
                    ProgressView()
                } else if offices.isEmpty {
                    ContentUnavailableView(
                        tr("local_offices.empty", fallback: "Nessun gruppo locale"),
                        systemImage: failed ? "wifi.exclamationmark" : "building.2",
                        description: Text(failed
                                          ? tr("local_group.state.failed", fallback: "Non siamo riusciti a caricare il gruppo locale.")
                                          : tr("local_offices.empty_description", fallback: "Non sono ancora disponibili gruppi locali."))
                    )
                } else if filtered.isEmpty {
                    ContentUnavailableView.search(text: query)
                }
            }
            .searchable(
                text: $query,
                placement: .navigationBarDrawer(displayMode: .always),
                prompt: Text(tr("local_offices.search_prompt", fallback: "Cerca per regione"))
            )
            .navigationTitle(tr("local_group.offices.title", fallback: "Gruppi locali"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(tr("app.cancel", fallback: "Annulla")) { dismiss() }
                }
            }
            .task { await load() }
        }
    }

    /// A plain picker row: the office's name, its region under it, a checkmark when chosen.
    private func row(title: String, subtitle: String?, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .foregroundStyle(Color.primary)
                    if let subtitle {
                        Text(subtitle)
                            .font(.footnote)
                            .foregroundStyle(Color.secondary)
                    }
                }
                Spacer()
                if selected {
                    Image(systemName: "checkmark")
                        .font(.body.weight(.semibold))
                        .foregroundStyle(Color.accentColor)
                }
            }
            .contentShape(Rectangle())
        }
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    private func choose(_ id: String?) {
        dismiss()
        Task { await store.selectOffice(id) }
    }

    private func load() async {
        guard !loaded else { return }
        do {
            offices = try await koin.localGroup.listOffices()
            failed = false
        } catch {
            if error is CancellationError { return }
            failed = true
        }
        loaded = true
    }
}
