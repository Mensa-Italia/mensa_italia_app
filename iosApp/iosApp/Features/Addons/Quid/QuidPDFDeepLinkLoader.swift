import SwiftUI
import Shared

/// Resolves a `mensa://quid-pdf/<record_id>` deep link to a real `QuidPDFViewer`.
///
/// The deep link carries the positive issue number (1…12); in our in-app model
/// the matching `QuidIssue` row has `id = -recordId` (see `QuidApi.fetchPdfArchive`).
/// We subscribe to `koin.quid.observeIssues()` for the lookup, triggering a
/// `refreshIssues()` on appear if the cache hasn't been seeded yet — then push
/// the standard `QuidPDFViewer` once we have the PDF URL.
struct QuidPDFDeepLinkLoader: View {
    let recordId: Int64

    @State private var resolved: QuidIssue?
    @State private var notFound = false
    @State private var sub: Closeable?

    var body: some View {
        Group {
            if let resolved, let url = resolved.pdfUrl.flatMap({ URL(string: $0) }) {
                QuidPDFViewer(url: url, title: resolved.name)
            } else if notFound {
                ContentUnavailableView(
                    tr("addons.quid.error", fallback: "Errore"),
                    systemImage: "doc.text",
                    description: Text(tr(
                        "addons.quid.pdf_not_found",
                        fallback: "Numero PDF non trovato."
                    ))
                )
            } else {
                ProgressView(tr("addons.quid.loading", fallback: "Caricamento…"))
                    .controlSize(.large)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .task { await start() }
        .onDisappear { sub?.close(); sub = nil }
    }

    private func start() async {
        sub?.close()
        sub = FlowBridgeKt.subscribe(
            flow: koin.quid.observeIssues(),
            onEach: { value in
                let list = (value as? [QuidIssue]) ?? []
                let match = list.first(where: { $0.id == -recordId })
                Task { @MainActor in
                    if let match {
                        self.resolved = match
                    } else if !list.isEmpty {
                        // Cache populated but no match → genuine miss.
                        self.notFound = true
                    }
                }
            },
            onError: { _ in
                Task { @MainActor in self.notFound = true }
            }
        )
        do { try await koin.quid.refreshIssues() } catch { }
    }
}
