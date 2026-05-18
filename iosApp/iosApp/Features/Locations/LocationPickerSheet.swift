import SwiftUI
import Shared

/// Sheet that lists the user's saved PocketBase `positions` (LocationModel).
///
/// Mirrors the Flutter `LocationListPickerView` behaviour:
/// - Tap a row to pick.
/// - Swipe to delete (soft delete via `LocationsRepository.deleteOne`).
/// - "Aggiungi" toolbar button opens the map picker for creating a new location.
struct LocationPickerSheet: View {
    /// Called when the user taps a row to confirm a location.
    /// Caller is responsible for dismissing the sheet.
    var onPicked: (LocationModel) -> Void
    /// Called when the user dismisses without picking. Default no-op.
    var onCancelled: () -> Void = {}

    @State private var vm = LocationPickerViewModel()
    @State private var showingMapPicker = false
    @State private var pendingDelete: LocationModel? = nil

    var body: some View {
        NavigationStack {
            content
                .navigationTitle(tr("app.location.picker.title", fallback: "Le tue posizioni"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button(tr("app.cancel", fallback: "Annulla")) { onCancelled() }
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(tr("app.location.add", fallback: "Aggiungi")) {
                            showingMapPicker = true
                        }
                        .fontWeight(.semibold)
                    }
                }
                .sheet(isPresented: $showingMapPicker) {
                    MapLocationPickerSheet(
                        onCreated: { _ in
                            // Repository upserts into local DB → list refreshes via observeAll().
                            showingMapPicker = false
                        },
                        onCancelled: { showingMapPicker = false }
                    )
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
                    Button(tr("app.delete", fallback: "Elimina"), role: .destructive) {
                        vm.delete(loc)
                        pendingDelete = nil
                    }
                    Button(tr("app.cancel", fallback: "Annulla"), role: .cancel) {
                        pendingDelete = nil
                    }
                } message: { loc in
                    Text(loc.name)
                }
                .alert(
                    tr("app.error.title", fallback: "Errore"),
                    isPresented: Binding(
                        get: { vm.error != nil },
                        set: { if !$0 { vm.error = nil } }
                    )
                ) {
                    Button("OK") { vm.error = nil }
                } message: {
                    Text(vm.error ?? "")
                }
                .task { vm.start() }
                .onDisappear { vm.stop() }
        }
    }

    @ViewBuilder
    private var content: some View {
        if !vm.hasLoaded {
            VStack {
                Spacer()
                ProgressView()
                Spacer()
            }
        } else if vm.locations.isEmpty {
            ContentUnavailableView(
                tr("app.location.empty.title", fallback: "Nessuna posizione salvata"),
                systemImage: "mappin.slash",
                description: Text(tr("app.location.empty.body", fallback: "Tocca 'Aggiungi' per crearne una."))
            )
        } else {
            List {
                ForEach(vm.locations, id: \.id) { loc in
                    LocationRow(location: loc)
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

private struct LocationRow: View {
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

// MARK: - ViewModel

@MainActor @Observable
final class LocationPickerViewModel {
    var locations: [LocationModel] = []
    var hasLoaded = false
    var error: String? = nil

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
                // Keep silent on refresh failure — local data still shows.
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
    LocationPickerSheet(onPicked: { _ in }, onCancelled: {})
}
