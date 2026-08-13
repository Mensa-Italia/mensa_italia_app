import SwiftUI
import Shared

/// Pagina 1 del flusso: le posizioni già salvate dall'utente.
///
/// Toccare una riga chiude subito il flusso restituendola al chiamante; il "+"
/// porta alla mappa per crearne una nuova.
struct SavedLocationsView: View {
    var onPicked: (LocationModel) -> Void
    var onAddClick: () -> Void
    var onCancel: () -> Void

    @State private var model = SavedLocationsModel()
    @State private var pendingDelete: LocationModel?

    var body: some View {
        content
            .navigationTitle(tr("app.location.picker.title", fallback: "Le tue posizioni"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(tr("app.location.cancel", fallback: "Annulla")) { onCancel() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        onAddClick()
                    } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel(tr("app.location.add", fallback: "Aggiungi"))
                }
            }
            .confirmationDialog(
                tr("app.location.delete.confirm.title", fallback: "Eliminare questa posizione?"),
                isPresented: Binding(
                    get: { pendingDelete != nil },
                    set: { if !$0 { pendingDelete = nil } }
                ),
                titleVisibility: .visible,
                presenting: pendingDelete
            ) { loc in
                Button(tr("app.location.delete.confirm.confirm", fallback: "Elimina"),
                       role: .destructive) {
                    model.delete(loc)
                    pendingDelete = nil
                }
                Button(tr("app.location.delete.confirm.dismiss", fallback: "Annulla"),
                       role: .cancel) {
                    pendingDelete = nil
                }
            } message: { loc in
                Text(loc.name)
            }
            .alert(
                tr("app.error.title", fallback: "Errore"),
                isPresented: Binding(
                    get: { model.error != nil },
                    set: { if !$0 { model.error = nil } }
                )
            ) {
                Button("OK") { model.error = nil }
            } message: {
                Text(model.error ?? "")
            }
            .task { model.start() }
            .onDisappear { model.stop() }
    }

    @ViewBuilder
    private var content: some View {
        if !model.hasLoaded {
            VStack {
                Spacer()
                ProgressView()
                Spacer()
            }
        } else if model.locations.isEmpty {
            ContentUnavailableView(
                tr("app.location.empty.title", fallback: "Nessuna posizione salvata"),
                systemImage: "mappin.slash",
                description: Text(tr("app.location.empty.body", fallback: "Tocca 'Aggiungi' per crearne una."))
            )
        } else {
            List {
                ForEach(model.locations, id: \.id) { loc in
                    SavedLocationRow(location: loc)
                        .contentShape(.rect)
                        .onTapGesture { onPicked(loc) }
                        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                            Button(role: .destructive) {
                                pendingDelete = loc
                            } label: {
                                Label(tr("app.delete", fallback: "Elimina"),
                                      systemImage: "trash")
                            }
                        }
                }
            }
            .listStyle(.insetGrouped)
        }
    }
}

// MARK: - Row

private struct SavedLocationRow: View {
    let location: LocationModel

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "mappin.circle.fill")
                .font(.title2)
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            VStack(alignment: .leading, spacing: 2) {
                Text(location.name)
                    .font(.body)
                    .foregroundStyle(.primary)
                Text(location.address.isEmpty ? location.name : location.address)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Model

@MainActor @Observable
final class SavedLocationsModel {
    var locations: [LocationModel] = []
    var hasLoaded = false
    var error: String?

    private var sub: Closeable?

    func start() {
        guard sub == nil else { return }
        sub = FlowBridgeKt.subscribe(
            flow: koin.locations.observeAll(),
            onEach: { [weak self] value in
                let list = (value as? [LocationModel]) ?? []
                Task { @MainActor in
                    self?.locations = list
                    self?.hasLoaded = true
                }
            },
            onError: { _ in }
        )
        Task {
            do {
                try await koin.locations.refresh()
            } catch {
                // Silenzio sul refresh fallito: i dati locali restano visibili.
            }
            await MainActor.run { self.hasLoaded = true }
        }
    }

    func stop() {
        sub?.close()
        sub = nil
    }

    func delete(_ loc: LocationModel) {
        Task {
            do {
                try await koin.locations.deleteOne(id: loc.id)
            } catch {
                await MainActor.run {
                    self.error = (error as NSError).localizedDescription
                }
            }
        }
    }
}

#Preview {
    NavigationStack {
        SavedLocationsView(onPicked: { _ in }, onAddClick: {}, onCancel: {})
    }
}
