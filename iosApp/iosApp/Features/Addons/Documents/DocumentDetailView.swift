import SwiftUI
import Shared

@MainActor @Observable
final class DocumentDetailViewModel {
    var document: DocumentModel?
    var loading = true
    var error: String?

    func load(id: String) async {
        loading = true
        defer { loading = false }
        do {
            document = try await koin.documents.getById(id: id)
        } catch {
            self.error = (error as NSError).localizedDescription
        }
    }
}

/// Apre il PDF di un documento partendo dal solo id.
///
/// Prima qui c'era una scheda con il riassunto AI e il PDF si apriva da un
/// bottone in fondo. Ora toccare un documento lo apre e basta: questa view
/// resta solo per i punti d'ingresso che hanno l'id ma non il file (notifiche,
/// deep link, Spotlight, ricerca) e passa al visore appena l'indirizzo e' noto.
struct DocumentDetailView: View {
    let documentId: String
    @State private var vm = DocumentDetailViewModel()

    var body: some View {
        Group {
            if let url = pdfURL {
                PDFViewerView(url: url)
            } else if vm.loading {
                LoadingDots()
            } else {
                ContentUnavailableView(
                    tr("addons.documents.file_unavailable", fallback: "Documento non disponibile"),
                    systemImage: "doc.questionmark"
                )
            }
        }
        .task { await vm.load(id: documentId) }
    }

    private var pdfURL: URL? {
        guard let d = vm.document, !d.file.isEmpty else { return nil }
        return Files.url(collection: "documents", recordId: d.id, filename: d.file)
    }
}
