import SwiftUI
import Shared

/// Pagina 3 del flusso: si corregge l'indirizzo, si dà un nome, si salva.
///
/// È una schermata a sé e non un pannello sopra la mappa perché mappa e
/// tastiera si contendono lo stesso schermo: separandole, il form prende la
/// tastiera senza comprimere niente.
struct NameLocationView: View {
    let draft: NewLocationDraft
    var onSaved: (LocationModel) -> Void
    var onBack: () -> Void
    var onCancel: () -> Void

    @FocusState private var nameFocused: Bool
    /// L'errore "dai un nome" compare solo dopo che l'utente ha davvero
    /// lasciato il campo vuoto, non appena apre la pagina.
    @State private var nameTouched = false
    @State private var showCancelConfirm = false

    var body: some View {
        Form {
            addressSection
            nameSection
            coordinatesSection
            cancelSection
        }
        .navigationTitle(tr("app.location.details.title", fallback: "Dettagli posizione"))
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    onBack()
                } label: {
                    Label(tr("app.location.back", fallback: "Indietro"), systemImage: "chevron.left")
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    Task {
                        if let created = await draft.save() { onSaved(created) }
                    }
                } label: {
                    if draft.saveState == .saving {
                        ProgressView()
                    } else {
                        Text(tr("app.location.save", fallback: "Salva posizione"))
                            .fontWeight(.semibold)
                    }
                }
                .disabled(!draft.canSave)
            }
        }
        .onChange(of: nameFocused) { _, focused in
            if !focused { nameTouched = true }
        }
        .confirmationDialog(
            tr("app.location.cancel.confirm.title", fallback: "Annullare la nuova posizione?"),
            isPresented: $showCancelConfirm,
            titleVisibility: .visible
        ) {
            Button(tr("app.location.cancel.confirm.confirm", fallback: "Annulla posizione"),
                   role: .destructive) {
                onCancel()
            }
            Button(tr("app.location.cancel.confirm.dismiss", fallback: "Continua"), role: .cancel) {}
        } message: {
            Text(tr("app.location.cancel.confirm.body", fallback: "I dati inseriti andranno persi."))
        }
    }

    // MARK: - Indirizzo

    private var addressSection: some View {
        Section {
            TextField(
                addressPlaceholder,
                text: Binding(
                    get: { draft.address },
                    set: { draft.editAddress($0) }
                ),
                axis: .vertical
            )
            .lineLimit(1...3)
        } header: {
            Text(tr("app.location.address", fallback: "Indirizzo"))
        } footer: {
            if draft.geocodeState == .loading {
                HStack(spacing: 6) {
                    ProgressView().controlSize(.small)
                    Text(tr("app.location.address.loading", fallback: "Ricerca indirizzo in corso…"))
                }
            }
        }
    }

    /// Il geocoding non blocca mai il salvataggio: quando fallisce, il campo
    /// resta vuoto e il placeholder invita a scriverlo a mano.
    private var addressPlaceholder: String {
        switch draft.geocodeState {
        case .loading:
            return tr("app.location.address.loading", fallback: "Ricerca indirizzo in corso…")
        case .failed:
            return tr("app.location.address.unavailable", fallback: "Indirizzo non trovato, scrivilo tu")
        case .idle, .done:
            return tr("app.location.address", fallback: "Indirizzo")
        }
    }

    // MARK: - Nome

    private var nameSection: some View {
        Section {
            TextField(
                tr("app.location.name.hint", fallback: "Come vuoi chiamare questo posto"),
                text: Binding(
                    get: { draft.name },
                    set: { draft.editName($0) }
                )
            )
            .focused($nameFocused)
            .submitLabel(.done)
        } header: {
            Text(tr("app.location.name", fallback: "Nome del posto"))
        } footer: {
            if nameTouched, draft.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(tr("app.location.name.required", fallback: "Dai un nome alla posizione"))
                    .foregroundStyle(.red)
            }
        }
    }

    // MARK: - Coordinate e chiusura

    private var coordinatesSection: some View {
        Section {
            Text(tr("app.location.coordinates", fallback: "Lat {lat} · Lon {lon}", [
                "lat": String(format: "%.5f", draft.center.latitude),
                "lon": String(format: "%.5f", draft.center.longitude),
            ]))
            .font(.footnote)
            .foregroundStyle(.secondary)
            .monospacedDigit()
        }
    }

    // Il flusso è presentato a tutto schermo e non si può chiudere con un
    // gesto: senza questa riga, da qui non ci sarebbe modo di uscire senza
    // prima tornare alla mappa.
    private var cancelSection: some View {
        Section {
            if draft.saveState == .failed {
                Text(tr("app.location.error.save",
                        fallback: "Non è stato possibile salvare la posizione. Riprova."))
                    .font(.footnote)
                    .foregroundStyle(.red)
            }
            Button(tr("app.location.cancel", fallback: "Annulla"), role: .destructive) {
                if draft.isDirty {
                    showCancelConfirm = true
                } else {
                    onCancel()
                }
            }
        }
    }
}

#Preview {
    NavigationStack {
        NameLocationView(draft: NewLocationDraft(initial: nil),
                         onSaved: { _ in },
                         onBack: {},
                         onCancel: {})
    }
}
