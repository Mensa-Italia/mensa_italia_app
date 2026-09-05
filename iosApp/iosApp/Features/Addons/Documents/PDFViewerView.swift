import SwiftUI
import PDFKit

struct PDFViewerView: View {
    let url: URL
    @State private var document: PDFDocument?
    /// Il PDF scaricato su disco. Serve a due cose: darlo a PDFKit e passarlo
    /// al foglio di condivisione al posto dell'URL remoto.
    @State private var localFile: URL?
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
                // Si condivide il file, non l'URL. Un `/api/files/...` passato
                // a qualcun altro risponde 404: il backend serve gli allegati
                // solo a chi si autentica, e chi riceve il link non ha la
                // nostra sessione. Il bottone compare quando il file c'e'.
                if let localFile {
                    ShareLink(item: localFile) {
                        Image(systemName: "square.and.arrow.up")
                    }
                }
            }
        }
        .task { await load() }
    }

    /// Scarica il PDF con l'header e poi lo apre da disco.
    ///
    /// `PDFDocument(url:)` su un URL remoto se la fa la rete da solo, dentro
    /// PDFKit, e non c'e' modo di infilarci un header: da quando il backend
    /// serve gli allegati solo a richieste autenticate, quella strada prende un
    /// 404 e torna nil — cioe' "Impossibile aprire il PDF" su ogni documento.
    private func load() async {
        loading = true
        let file = await MensaAuth.downloadToTemporaryFile(from: url)
        let loaded = file.flatMap { PDFDocument(url: $0) }
        await MainActor.run {
            self.localFile = file
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
