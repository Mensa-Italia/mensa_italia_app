import SwiftUI
import MapKit
import Combine
import Shared

/// Sheet that lets the user drop a pin on a map, fill in a name + address,
/// and persist a new `LocationModel` to PocketBase via the shared repository.
///
/// Mirrors the Flutter `MapPickerView` behaviour.
struct MapLocationPickerSheet: View {
    /// Called with the freshly persisted LocationModel (carries new `id`).
    var onCreated: (LocationModel) -> Void
    /// Called when the user dismisses without creating a location.
    var onCancelled: () -> Void = {}

    // Default region: centred on Italy.
    @State private var camera: MapCameraPosition = .region(
        MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: 41.9028, longitude: 12.4964),
            span: MKCoordinateSpan(latitudeDelta: 10, longitudeDelta: 10)
        )
    )
    @State private var currentCenter = CLLocationCoordinate2D(latitude: 41.9028, longitude: 12.4964)

    @State private var name: String = ""
    @State private var address: String = ""
    @State private var searchQuery: String = ""
    @State private var searchResults: [MKLocalSearchCompletion] = []
    @State private var showResults = false

    @State private var saving = false
    @State private var errorMessage: String?

    @State private var reverseGeocodeTask: Task<Void, Never>?
    @State private var addressEditedByUser = false

    @StateObject private var searchHelper = LocalSearchCompleterHelper()

    var body: some View {
        NavigationStack {
            ZStack(alignment: .top) {
                mapLayer

                VStack(spacing: 0) {
                    searchField
                    if showResults && !searchResults.isEmpty {
                        resultsList
                    }
                    Spacer(minLength: 0)
                }
            }
            .safeAreaInset(edge: .bottom) {
                bottomPanel
            }
            .navigationTitle(tr("app.location.map.title", fallback: "Scegli posizione"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(tr("app.cancel", fallback: "Annulla")) { onCancelled() }
                }
            }
            .alert(
                tr("app.error.title", fallback: "Errore"),
                isPresented: Binding(
                    get: { errorMessage != nil },
                    set: { if !$0 { errorMessage = nil } }
                )
            ) {
                Button("OK") { errorMessage = nil }
            } message: {
                Text(errorMessage ?? "")
            }
            .onChange(of: searchQuery) { _, newValue in
                searchHelper.update(query: newValue)
                showResults = !newValue.trimmingCharacters(in: .whitespaces).isEmpty
            }
            .onReceive(searchHelper.$results) { results in
                searchResults = results
            }
            .onMapCameraChange(frequency: .onEnd) { context in
                currentCenter = context.camera.centerCoordinate
                scheduleReverseGeocode(for: context.camera.centerCoordinate)
            }
            .task {
                // Initial reverse geocode for default center.
                scheduleReverseGeocode(for: currentCenter)
            }
        }
    }

    // MARK: - Map + pin overlay

    private var mapLayer: some View {
        ZStack {
            Map(position: $camera)
                .mapStyle(.standard(elevation: .realistic))
                .ignoresSafeArea(edges: .bottom)

            // Fixed center pin overlay.
            Image(systemName: "mappin")
                .font(.system(size: 34, weight: .bold))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                .shadow(radius: 3, y: 2)
                .offset(y: -16) // raise so the tip points at the centre
                .allowsHitTesting(false)
        }
    }

    // MARK: - Search field & autocomplete list

    private var searchField: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
            TextField(
                tr("app.location.search.placeholder", fallback: "Cerca un indirizzo…"),
                text: $searchQuery
            )
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .submitLabel(.search)
            if !searchQuery.isEmpty {
                Button {
                    searchQuery = ""
                    showResults = false
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .padding(.horizontal)
        .padding(.top, 8)
    }

    private var resultsList: some View {
        VStack(spacing: 0) {
            ForEach(searchResults.prefix(6), id: \.self) { item in
                Button {
                    select(completion: item)
                } label: {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.title)
                            .font(.subheadline)
                            .foregroundStyle(.primary)
                        if !item.subtitle.isEmpty {
                            Text(item.subtitle)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.vertical, 8)
                    .padding(.horizontal, 14)
                    .contentShape(.rect)
                }
                .buttonStyle(.plain)
                Divider().opacity(0.5)
            }
        }
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .padding(.horizontal)
        .padding(.top, 6)
    }

    // MARK: - Bottom panel

    private var bottomPanel: some View {
        VStack(spacing: 12) {
            VStack(spacing: 8) {
                TextField(
                    tr("app.location.name", fallback: "Nome del posto"),
                    text: $name
                )
                .textFieldStyle(.roundedBorder)

                TextField(
                    tr("app.location.address", fallback: "Indirizzo"),
                    text: Binding(
                        get: { address },
                        set: { newValue in
                            address = newValue
                            addressEditedByUser = true
                        }
                    )
                )
                .textFieldStyle(.roundedBorder)
            }

            Button {
                Task { await save() }
            } label: {
                HStack {
                    if saving { ProgressView().tint(.white) }
                    Text(tr("app.location.save", fallback: "Salva posizione"))
                        .fontWeight(.semibold)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 44)
            }
            .buttonStyle(.borderedProminent)
            .tint(AppTheme.Colors.brandPrimary)
            .disabled(saving || !canSave)
        }
        .padding(.horizontal)
        .padding(.vertical, 12)
        .background(.regularMaterial)
    }

    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty
            && !address.trimmingCharacters(in: .whitespaces).isEmpty
    }

    // MARK: - Actions

    private func select(completion: MKLocalSearchCompletion) {
        showResults = false
        searchQuery = completion.title
        let request = MKLocalSearch.Request(completion: completion)
        let search = MKLocalSearch(request: request)
        search.start { response, _ in
            guard let item = response?.mapItems.first else { return }
            let coord = item.placemark.coordinate
            Task { @MainActor in
                withAnimation(.easeInOut(duration: 0.4)) {
                    camera = .region(
                        MKCoordinateRegion(
                            center: coord,
                            span: MKCoordinateSpan(latitudeDelta: 0.005, longitudeDelta: 0.005)
                        )
                    )
                }
                currentCenter = coord
                if name.trimmingCharacters(in: .whitespaces).isEmpty {
                    name = item.name ?? completion.title
                }
                addressEditedByUser = false
                scheduleReverseGeocode(for: coord)
            }
        }
    }

    private func scheduleReverseGeocode(for coord: CLLocationCoordinate2D) {
        reverseGeocodeTask?.cancel()
        reverseGeocodeTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 400_000_000)
            if Task.isCancelled { return }
            let location = CLLocation(latitude: coord.latitude, longitude: coord.longitude)
            let geocoder = CLGeocoder()
            do {
                let placemarks = try await geocoder.reverseGeocodeLocation(location)
                guard !Task.isCancelled, let placemark = placemarks.first else { return }
                let formatted = formatPlacemark(placemark)
                if !addressEditedByUser, !formatted.isEmpty {
                    address = formatted
                }
            } catch {
                // Silently ignore — user can type manually.
            }
        }
    }

    private func formatPlacemark(_ p: CLPlacemark) -> String {
        var parts: [String] = []
        if let s = p.thoroughfare {
            if let n = p.subThoroughfare { parts.append("\(s), \(n)") } else { parts.append(s) }
        }
        if let city = p.locality { parts.append(city) }
        if let country = p.country { parts.append(country) }
        return parts.joined(separator: ", ")
    }

    private func save() async {
        guard canSave, !saving else { return }
        saving = true
        defer { saving = false }
        let userId = (koin.auth.currentUser.value as? UserModel)?.id ?? ""
        do {
            let trimmedName = name.trimmingCharacters(in: .whitespaces)
            let trimmedAddress = address.trimmingCharacters(in: .whitespaces)
            let created = try await koin.locations.createAndAddLocal(
                name: trimmedName,
                address: trimmedAddress,
                lat: currentCenter.latitude,
                lon: currentCenter.longitude,
                createdBy: userId
            )
            onCreated(created)
        } catch {
            errorMessage = (error as NSError).localizedDescription
        }
    }
}

// MARK: - MKLocalSearchCompleter wrapper

@MainActor
private final class LocalSearchCompleterHelper: NSObject, ObservableObject, MKLocalSearchCompleterDelegate {
    @Published var results: [MKLocalSearchCompletion] = []

    private let completer: MKLocalSearchCompleter

    override init() {
        self.completer = MKLocalSearchCompleter()
        super.init()
        completer.resultTypes = [.address, .pointOfInterest]
        completer.delegate = self
    }

    func update(query: String) {
        let trimmed = query.trimmingCharacters(in: .whitespaces)
        if trimmed.isEmpty {
            results = []
            completer.queryFragment = ""
            return
        }
        completer.queryFragment = trimmed
    }

    nonisolated func completerDidUpdateResults(_ completer: MKLocalSearchCompleter) {
        let newResults = completer.results
        Task { @MainActor in self.results = newResults }
    }

    nonisolated func completer(_ completer: MKLocalSearchCompleter, didFailWithError error: Error) {
        Task { @MainActor in self.results = [] }
    }
}

#Preview {
    MapLocationPickerSheet(onCreated: { _ in }, onCancelled: {})
}
