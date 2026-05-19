import SwiftUI
import Shared

@MainActor @Observable
final class DocumentDetailViewModel {
    var document: DocumentModel?
    var summary: String?
    var loading = true
    var summaryLoading = false
    var summaryFailed = false
    var error: String?

    func load(id: String) async {
        loading = true
        defer { loading = false }
        do {
            document = try await koin.documents.getById(id: id)
        } catch {
            self.error = (error as NSError).localizedDescription
        }
        await loadSummary()
    }

    func loadSummary() async {
        guard let doc = document, !doc.elaborated.isEmpty else {
            summaryFailed = true
            return
        }
        summaryLoading = true
        summaryFailed = false
        defer { summaryLoading = false }
        do {
            if let elab = try await koin.documents.getElaborated(elaboratedId: doc.elaborated) {
                summary = elab.iaResume
            } else {
                summaryFailed = true
            }
        } catch {
            summaryFailed = true
        }
    }
}

struct DocumentDetailView: View {
    let documentId: String
    @State private var vm = DocumentDetailViewModel()
    @State private var showPDF = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                hero
                summarySection
                Spacer(minLength: 12)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
        }
        .navigationTitle(tr("addons.documents.detail_title", fallback: "Documento"))
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .bottom) {
            if let url = pdfURL {
                Button {
                    showPDF = true
                } label: {
                    Label(
                        tr("addons.documents.open_pdf", fallback: "Apri PDF"),
                        systemImage: "doc.richtext"
                    )
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 4)
                }
                .buttonStyle(.glassProminent)
                .tint(AppTheme.Colors.mensaBlue)
                .padding(.horizontal, 20)
                .padding(.bottom, 12)
                .navigationDestination(isPresented: $showPDF) {
                    PDFViewerView(url: url)
                }
            }
        }
        .task { await vm.load(id: documentId) }
        .overlay {
            if vm.loading { LoadingDots() }
        }
    }

    private var pdfURL: URL? {
        guard let d = vm.document, !d.file.isEmpty else { return nil }
        return Files.url(collection: "documents", recordId: d.id, filename: d.file)
    }

    @ViewBuilder
    private var hero: some View {
        if let d = vm.document {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 12) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(AppTheme.Colors.mensaCyan.opacity(0.22))
                        Image(systemName: "doc.text.fill")
                            .font(.title)
                            .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                    }
                    .frame(width: 64, height: 64)

                    VStack(alignment: .leading, spacing: 6) {
                        Text(d.name)
                            .font(.title2.weight(.bold))
                            .lineLimit(3)
                        if !d.category.isEmpty {
                            Text(d.category)
                                .font(.caption.weight(.semibold))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(Capsule().fill(AppTheme.Colors.mensaBlue.opacity(0.12)))
                                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                        }
                    }
                    Spacer(minLength: 0)
                }
                if let desc = d.description_, !desc.isEmpty {
                    Text(desc)
                        .font(.body)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(18)
            .background(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .fill(.ultraThinMaterial)
            )
        }
    }

    @ViewBuilder
    private var summarySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "sparkles")
                    .foregroundStyle(AppTheme.Colors.mensaCyan)
                Text(tr("addons.documents.ai_summary", fallback: "Riassunto AI"))
                    .font(.headline)
            }
            if vm.summaryLoading {
                ProgressView().controlSize(.small)
            } else if let s = vm.summary, !s.isEmpty {
                MarkdownText(text: s)
                    .font(.body)
                    .foregroundStyle(.primary)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
            } else if vm.summaryFailed {
                Text(tr("addons.documents.summary_unavailable", fallback: "Riassunto non disponibile"))
                    .font(.callout)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(.ultraThinMaterial)
        )
    }
}

/// Block-level markdown renderer with support for paragraphs, ATX headings
/// (`#`..`###`), bullet lists (`-`, `*`, `+`), numbered lists (`1.`, `2.`…),
/// and nested lists (2- or 4-space indentation). Inline syntax (bold,
/// italic, links, inline code) is delegated to `AttributedString(markdown:)`.
private struct MarkdownText: View {
    let text: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ForEach(Array(blocks.enumerated()), id: \.offset) { _, block in
                MarkdownBlockView(block: block)
            }
        }
    }

    private var blocks: [MarkdownBlock] {
        MarkdownParser.shared.parse(source: text) as? [MarkdownBlock] ?? []
    }
}

@ViewBuilder
private func markdownBlockView(_ block: MarkdownBlock) -> some View {
    if let h = block as? MarkdownBlockHeading {
        Text(attributed(h.text))
            .font(h.level == 1 ? .title2.bold()
                  : h.level == 2 ? .title3.bold()
                  : .headline)
            .fixedSize(horizontal: false, vertical: true)
    } else if let p = block as? MarkdownBlockParagraph {
        Text(attributed(p.text))
            .fixedSize(horizontal: false, vertical: true)
    } else if let l = block as? MarkdownBlockList {
        let items = (l.items as? [MarkdownListItem]) ?? []
        VStack(alignment: .leading, spacing: 4) {
            ForEach(Array(items.enumerated()), id: \.offset) { idx, item in
                MarkdownListItemView(item: item, orderedIndex: idx + 1)
            }
        }
    }
}

private struct MarkdownBlockView: View {
    let block: MarkdownBlock

    var body: some View {
        markdownBlockView(block)
    }
}

private struct MarkdownListItemView: View {
    let item: MarkdownListItem
    let orderedIndex: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(bullet)
                    .font(.body.monospacedDigit())
                    .foregroundStyle(.secondary)
                    .frame(minWidth: 18, alignment: .leading)
                Text(attributed(item.text))
                    .fixedSize(horizontal: false, vertical: true)
            }
            let children = (item.children as? [MarkdownListItem]) ?? []
            if !children.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    ForEach(Array(children.enumerated()), id: \.offset) { idx, child in
                        MarkdownListItemView(item: child, orderedIndex: idx + 1)
                    }
                }
                .padding(.leading, 22)
            }
        }
        .padding(.leading, CGFloat(Int(item.depth)) * 16)
    }

    /// Use the original ordinal for numbered items so "1." rendered as
    /// "1." (not auto-renumbered) — keeps fidelity with author intent.
    private var bullet: String {
        if item.marker.hasSuffix(".") { return item.marker }
        switch Int(item.depth) {
        case 0: return "•"
        case 1: return "◦"
        default: return "▪"
        }
    }
}

private func attributed(_ s: String) -> AttributedString {
    let opts = AttributedString.MarkdownParsingOptions(
        interpretedSyntax: .inlineOnlyPreservingWhitespace
    )
    if let a = try? AttributedString(markdown: s, options: opts) {
        return a
    }
    return AttributedString(s)
}
