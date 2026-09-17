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

/// La `PDFView` di PDFKit: pinch, trascinamento e doppio tap li fa gia' lei.
///
/// L'unica cosa che qui andava sistemata e' la durata dello zoom. `autoScales`
/// vuol dire "riadatta la scala ogni volta che cambi misura": lasciato acceso
/// per sempre, ogni rimisurazione della view — ruotare il telefono, lo Split
/// View su iPad, la barra di navigazione che passa da grande a compatta —
/// ricalcolava la scala di riempimento e cancellava l'ingrandimento scelto da
/// chi stava leggendo un comma in corpo otto.
///
/// Quindi: `autoScales` acceso per il primo layout, che e' quello che fa
/// entrare la pagina nello schermo, e poi spento. Da li' in avanti la scala e'
/// dell'utente, e alle rimisurazioni successive la riportiamo noi.
private struct PDFKitRepresentedView: UIViewRepresentable {
    let document: PDFDocument

    /// Una `PDFView` che avvisa quando cambia misura.
    ///
    /// Non basta agganciarsi a `updateUIView`: SwiftUI lo chiama quando
    /// rivaluta il body, e su iPad la rotazione non cambia la size class,
    /// quindi puo' non arrivare affatto. Con `autoScales` spento nessuno
    /// rifarebbe il conto e la pagina resterebbe alla scala di prima, storta.
    /// `layoutSubviews` invece scatta sempre, perche' e' il layout vero.
    final class BoundsAwarePDFView: PDFView {
        var onResize: ((BoundsAwarePDFView) -> Void)?
        private var lastSize: CGSize = .zero

        override func layoutSubviews() {
            super.layoutSubviews()
            guard bounds.size != lastSize else { return }
            lastSize = bounds.size
            onResize?(self)
        }
    }

    func makeUIView(context: Context) -> BoundsAwarePDFView {
        let v = BoundsAwarePDFView()
        v.autoScales = true
        v.displayMode = .singlePageContinuous
        v.displayDirection = .vertical
        v.usePageViewController(false)
        v.document = document
        context.coordinator.observe(v)
        v.onResize = { [coordinator = context.coordinator] view in
            coordinator.applyScale(to: view)
        }
        return v
    }

    func updateUIView(_ uiView: BoundsAwarePDFView, context: Context) {
        if uiView.document != document {
            uiView.document = document
            context.coordinator.reset()
        }
        context.coordinator.applyScale(to: uiView)
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    final class Coordinator {
        private var didFit = false
        /// La scala scelta col pizzico, `nil` finche' l'utente non zooma.
        private var userScale: CGFloat?
        /// L'ultimo valore scritto da noi, per non scambiarlo per un gesto.
        private var lastApplied: CGFloat?
        /// La registrazione al centro notifiche, da togliere quando si esce.
        private var observer: NSObjectProtocol?

        deinit {
            // `addObserver(forName:object:queue:using:)` restituisce un token e
            // il blocco resta registrato finche' non lo si rimuove: senza
            // questo, ogni documento aperto ne lasciava uno per sempre.
            if let observer { NotificationCenter.default.removeObserver(observer) }
        }

        func reset() {
            didFit = false
            userScale = nil
            lastApplied = nil
        }

        func observe(_ view: PDFView) {
            if let observer { NotificationCenter.default.removeObserver(observer) }
            observer = NotificationCenter.default.addObserver(
                forName: .PDFViewScaleChanged,
                object: view,
                queue: .main
            ) { [weak self, weak view] _ in
                guard let self, let view, self.didFit else { return }
                let current = view.scaleFactor
                // Anche le nostre assegnazioni fanno scattare questa notifica:
                // se le registrassimo come zoom dell'utente, la scala di
                // riempimento di ieri diventerebbe il "volere dell'utente" di oggi.
                if let lastApplied = self.lastApplied, abs(current - lastApplied) < 0.0001 { return }
                self.userScale = current
            }
        }

        /// Primo layout utile: si congela il fit. Dai successivi in poi si
        /// rimette la scala dell'utente, che altrimenti PDFKit sovrascriverebbe.
        func applyScale(to view: PDFView) {
            guard view.bounds.width > 0, view.document != nil else { return }
            let fit = view.scaleFactorForSizeToFit
            guard fit > 0 else { return }

            // La scala di riempimento cambia con i bounds, quindi i limiti vanno
            // rifatti ogni volta: dopo una rotazione il minimo di prima non
            // farebbe piu' entrare la pagina nello schermo.
            view.minScaleFactor = fit
            view.maxScaleFactor = fit * 8

            // Chi non ha mai zoomato continua a vedere la pagina intera a ogni
            // rimisurazione, esattamente come faceva `autoScales`.
            let target = userScale.map { max(fit, $0) } ?? fit
            lastApplied = target

            if !didFit {
                didFit = true
                view.autoScales = false
            }
            view.scaleFactor = target
        }
    }
}
