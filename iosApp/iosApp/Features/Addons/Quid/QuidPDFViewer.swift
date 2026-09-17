import SwiftUI
import QuickLook

/// Full-screen PDF viewer for legacy Quid issues hosted on WordPress.
/// Downloads the remote PDF to a stable temp-file path, then presents
/// it via QLPreviewController so the user gets native zoom, page-swipe
/// and share-sheet for free.
struct QuidPDFViewer: View {
    let url: URL
    let title: String

    @State private var localFile: URL?
    @State private var loadError: String?

    var body: some View {
        Group {
            if let localFile {
                QLPreview(file: localFile)
                    .ignoresSafeArea(edges: .bottom)
            } else if let loadError {
                ContentUnavailableView(
                    tr("addons.quid.error", fallback: "Errore"),
                    systemImage: "doc.text",
                    description: Text(loadError)
                )
            } else {
                ProgressView(tr("addons.quid.loading", fallback: "Caricamento…"))
                    .controlSize(.large)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .task { await download() }
    }

    // MARK: - Download

    private func download() async {
        do {
            let (tmp, _) = try await URLSession.shared.download(from: url)
            // Use a stable filename derived from the URL so the QL title looks right
            // and repeated opens skip re-downloading (temp dir survives the session).
            let dest = FileManager.default.temporaryDirectory
                .appendingPathComponent(url.lastPathComponent)
            try? FileManager.default.removeItem(at: dest)
            try FileManager.default.moveItem(at: tmp, to: dest)
            await MainActor.run { self.localFile = dest }
        } catch is CancellationError {
            // .task cancelled on disappear — don't show an error.
        } catch {
            await MainActor.run { self.loadError = error.localizedDescription }
        }
    }
}

// MARK: - UIViewControllerRepresentable wrapper

private struct QLPreview: UIViewControllerRepresentable {
    let file: URL

    func makeUIViewController(context: Context) -> QLPreviewController {
        let c = QLPreviewController()
        c.dataSource = context.coordinator
        return c
    }

    /// Ricarica solo se il file e' davvero cambiato.
    ///
    /// Prima qui c'era un `reloadData()` secco. SwiftUI chiama
    /// `updateUIViewController` non solo quando cambia `file`, ma a ogni
    /// rivalutazione di un antenato o dell'ambiente — la rotazione basta —
    /// e `reloadData()` ricostruisce l'anteprima da capo: si tornava a pagina
    /// uno alla scala di partenza, buttando via lo zoom di chi stava leggendo.
    ///
    /// Il confronto sta nel Coordinator perche' e' l'unica cosa che sopravvive
    /// alla ricreazione della struct.
    func updateUIViewController(_ uiViewController: QLPreviewController, context: Context) {
        guard context.coordinator.file != file else { return }
        context.coordinator.file = file
        uiViewController.reloadData()
    }

    func makeCoordinator() -> Coordinator { Coordinator(file: file) }

    final class Coordinator: NSObject, QLPreviewControllerDataSource {
        var file: URL
        init(file: URL) { self.file = file }
        func numberOfPreviewItems(in controller: QLPreviewController) -> Int { 1 }
        func previewController(_ controller: QLPreviewController,
                               previewItemAt index: Int) -> QLPreviewItem {
            file as QLPreviewItem
        }
    }
}
