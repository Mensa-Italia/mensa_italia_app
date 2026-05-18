import SwiftUI
import PDFKit

struct PDFViewerView: View {
    let url: URL
    @State private var document: PDFDocument? = nil
    @State private var loading = true

    var body: some View {
        ZStack {
            if let doc = document {
                PDFKitRepresentedView(document: doc)
                    .ignoresSafeArea(edges: .bottom)
            } else if loading {
                ProgressView()
            } else {
                ContentUnavailableView(
                    tr("addons.documents.pdf_error", fallback: "Impossibile aprire il PDF"),
                    systemImage: "exclamationmark.triangle"
                )
            }
        }
        .navigationTitle(tr("addons.documents.pdf_title", fallback: "Documento"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                ShareLink(item: url) {
                    Image(systemName: "square.and.arrow.up")
                }
            }
        }
        .task { await load() }
    }

    private func load() async {
        loading = true
        let loaded = await Task.detached(priority: .userInitiated) {
            PDFDocument(url: url)
        }.value
        await MainActor.run {
            self.document = loaded
            self.loading = false
        }
    }
}

private struct PDFKitRepresentedView: UIViewRepresentable {
    let document: PDFDocument

    func makeUIView(context: Context) -> PDFView {
        let v = PDFView()
        v.autoScales = true
        v.displayMode = .singlePageContinuous
        v.displayDirection = .vertical
        v.usePageViewController(false)
        v.document = document
        return v
    }

    func updateUIView(_ uiView: PDFView, context: Context) {
        if uiView.document != document {
            uiView.document = document
        }
    }
}
