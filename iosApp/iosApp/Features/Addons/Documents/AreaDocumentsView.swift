import SwiftUI
import Shared

@MainActor
@Observable
final class DocumentsListViewModel {
    var items: [DocumentModel] = []
    var loading = true
    var error: String? = nil
    var searchText: String = ""
    var selectedCategory: String? = nil

    private var sub: Closeable?

    func load() async {
        loading = true
        defer { loading = false }
        sub?.close()
        let flow = koin.documents.observeAll() as Kotlinx_coroutines_coreFlow
        sub = subscribeFlow(flow) { [weak self] (list: NSArray) in
            Task { @MainActor [weak self] in
                self?.items = (list as? [DocumentModel]) ?? []
            }
        }
        await refresh()
    }

    func refresh() async {
        do { try await koin.documents.refresh() }
        catch { self.error = (error as NSError).localizedDescription }
    }

    var categories: [String] {
        Array(Set(items.map(\.category).filter { !$0.isEmpty })).sorted()
    }

    var filtered: [DocumentModel] {
        items.filter { d in
            (selectedCategory == nil || d.category == selectedCategory) &&
            (searchText.isEmpty
             || d.name.localizedCaseInsensitiveContains(searchText)
             || (d.description_ ?? "").localizedCaseInsensitiveContains(searchText))
        }
    }
}

struct AreaDocumentsView: View {
    @State private var vm = DocumentsListViewModel()

    private let dateFmt: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.dateStyle = .medium
        return f
    }()

    var body: some View {
        contentList
            .navigationTitle(tr("addons.documents.title", fallback: "Area Documenti"))
            .navigationBarTitleDisplayMode(.large)
            .cleanNavBar()
            .searchable(
                text: $vm.searchText,
                placement: .navigationBarDrawer(displayMode: .automatic),
                prompt: tr("addons.documents.search_placeholder", fallback: "Cerca un documento…")
            )
            .toolbar { filterToolbarItem }
            .refreshable { await vm.refresh() }
            .task { await vm.load() }
    }

    /// Filtro sempre in toolbar — le categorie specifiche si popolano dal
    /// flow ma sappiamo a prescindere che esisteranno.
    @ToolbarContentBuilder private var filterToolbarItem: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Menu {
                Picker(
                    selection: $vm.selectedCategory,
                    label: EmptyView()
                ) {
                    Text(tr("addons.documents.all", fallback: "Tutti"))
                        .tag(String?.none)
                    ForEach(vm.categories, id: \.self) { cat in
                        Text(localizedCategory(cat)).tag(String?.some(cat))
                    }
                }
            } label: {
                Image(systemName: vm.selectedCategory == nil
                      ? "line.3.horizontal.decrease.circle"
                      : "line.3.horizontal.decrease.circle.fill")
                    .accessibilityLabel(Text(tr(
                        "addons.documents.filter",
                        fallback: "Filtra per categoria"
                    )))
            }
            .tint(AppTheme.Colors.brandTintAdaptive)
        }
    }

    @ViewBuilder private var contentList: some View {
        List {
            ForEach(vm.filtered, id: \.id) { d in
                NavigationLink(destination: DocumentDetailView(documentId: d.id)) {
                    DocumentRow(
                        doc: d,
                        dateString: dateFmt.string(from: Date(
                            timeIntervalSince1970: Double(d.created.epochSeconds)
                        ))
                    )
                }
                .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                    if let url = Files.url(
                        collection: "documents",
                        recordId: d.id,
                        filename: d.file
                    ) {
                        ShareLink(item: url) {
                            Label(
                                tr("common.share", fallback: "Condividi"),
                                systemImage: "square.and.arrow.up"
                            )
                        }
                        .tint(AppTheme.Colors.brandPrimary)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .overlay {
            if vm.loading && vm.items.isEmpty {
                ProgressView().scaleEffect(1.2)
            } else if vm.items.isEmpty {
                ContentUnavailableView(
                    tr("addons.documents.empty", fallback: "Nessun documento"),
                    systemImage: "doc.text",
                    description: Text(tr(
                        "addons.documents.empty_description",
                        fallback: "Trascina giù per aggiornare."
                    ))
                )
            } else if vm.filtered.isEmpty {
                ContentUnavailableView.search(text: vm.searchText)
            }
        }
    }

}

struct DocumentRow: View {
    let doc: DocumentModel
    let dateString: String

    var body: some View {
        HStack(alignment: .center, spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(AppTheme.Colors.brandPrimary.opacity(0.15))
                    .frame(width: 40, height: 40)
                Image(systemName: iconName(for: doc.file))
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                    .font(.system(size: 18, weight: .semibold))
            }
            VStack(alignment: .leading, spacing: 3) {
                Text(doc.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(2)
                if let desc = doc.description_, !desc.isEmpty {
                    Text(desc)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                HStack(spacing: 6) {
                    if !doc.category.isEmpty {
                        Text(localizedCategory(doc.category))
                            .font(.caption2.weight(.medium))
                            .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                        Text("·")
                            .font(.caption2)
                            .foregroundStyle(.tertiary)
                    }
                    Text(dateString)
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, 4)
    }

    private func iconName(for file: String) -> String {
        switch (file as NSString).pathExtension.lowercased() {
        case "pdf": return "doc.richtext.fill"
        case "doc", "docx": return "doc.text.fill"
        case "xls", "xlsx", "csv": return "tablecells.fill"
        case "ppt", "pptx", "key": return "rectangle.on.rectangle.angled.fill"
        case "jpg", "jpeg", "png", "heic", "webp", "gif": return "photo.fill"
        case "zip", "rar", "7z": return "doc.zipper"
        case "mp4", "mov", "m4v": return "play.rectangle.fill"
        default: return "doc.fill"
        }
    }
}

/// Documents category fields are translation keys (eg. `verbali_delibere`,
/// `materiale_comunicazione`). Mirror `OrgChartView.localizedGroupTitle`:
/// look up Tolgee with a prettified fallback so the user never sees the raw
/// snake_case key (underscores → spaces, Title Case).
fileprivate func localizedCategory(_ raw: String) -> String {
    guard !raw.isEmpty else { return "" }
    let pretty = raw
        .replacingOccurrences(of: "_", with: " ")
        .replacingOccurrences(of: "-", with: " ")
        .capitalized
    return tr(raw, fallback: pretty)
}

#Preview {
    NavigationStack { AreaDocumentsView() }
}
