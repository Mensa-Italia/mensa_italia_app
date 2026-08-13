import Foundation
import CoreLocation
import MapKit
import Observation
import Shared

/// Stato del reverse geocoding del centro mappa.
enum GeocodeState {
    case idle, loading, done, failed
}

/// Stato della barra di ricerca.
///
/// `unavailable` esiste per parità di contratto con Android, dove senza chiave
/// Google e senza `Geocoder` la ricerca può mancare del tutto. Su iOS MapKit
/// c'è sempre, quindi qui non lo produciamo mai: un provider muto diventa
/// `noResults` e la mappa resta comunque usabile a mano.
enum LocationSearchState {
    case idle, loading, results, noResults, unavailable
}

enum LocationSaveState {
    case idle, saving, failed
}

/// Bozza della posizione in costruzione, condivisa fra la pagina mappa e la
/// pagina "correggi indirizzo e dai un nome".
///
/// Vive quanto il `LocationPickerFlowView` che la possiede: tornare indietro
/// dai dettagli alla mappa non perde né le coordinate né quello che l'utente
/// ha già scritto.
@MainActor @Observable
final class NewLocationDraft {
    /// Camera di partenza quando non sappiamo dove si trova l'utente.
    static let italyCenter = CLLocationCoordinate2D(latitude: 41.9028, longitude: 12.4964)

    /// Sotto questa soglia non si rigeocodifica: il centro mappa si sposta di
    /// qualche metro anche solo rilasciando il dito.
    private static let significantMoveMeters: CLLocationDistance = 25

    var center: CLLocationCoordinate2D
    /// Finché è `false` il punto non è mai stato scelto da nessuno e "Avanti"
    /// resta spento: è quello che impedisce di salvare Roma per sbaglio.
    var pinPositioned = false

    var address = ""
    var addressEditedByUser = false
    var lastGeocodedCenter: CLLocationCoordinate2D?
    var geocodeState: GeocodeState = .idle

    var nameSuggestion = ""
    var name = ""
    var nameEditedByUser = false

    var searchQuery = ""
    var searchResults: [LocationSuggestion] = []
    var searchState: LocationSearchState = .idle
    /// Scegliendo un risultato riscriviamo noi il testo del campo di ricerca:
    /// quel cambio non deve rimettere in moto l'autocomplete e riaprire la
    /// tendina che abbiamo appena chiuso.
    var ignoreNextQueryChange = false

    var saveState: LocationSaveState = .idle

    @ObservationIgnored let search = LocationSearchService()
    @ObservationIgnored private let geocoder = ReverseGeocoderService()
    @ObservationIgnored private var geocodeTask: Task<Void, Never>?

    /// `initial` è la posizione già selezionata dal chiamante: apre la mappa
    /// dove si trova, invece di ripartire dall'Italia intera.
    init(initial: LocationModel?) {
        if let initial {
            let coordinate = CLLocationCoordinate2D(latitude: initial.lat, longitude: initial.lon)
            center = coordinate
            pinPositioned = true
            address = initial.address
            lastGeocodedCenter = coordinate
            geocodeState = initial.address.isEmpty ? .idle : .done
            nameSuggestion = initial.name
            name = initial.name
        } else {
            center = Self.italyCenter
        }

        search.onChange = { [weak self] results, state in
            self?.searchResults = results
            self?.searchState = state
        }
    }

    /// Vero quando chiudere il flusso butterebbe via qualcosa che l'utente ha
    /// scritto a mano.
    var isDirty: Bool { nameEditedByUser || addressEditedByUser }

    var initialRegion: MKCoordinateRegion {
        pinPositioned
            ? MKCoordinateRegion(center: center, latitudinalMeters: 600, longitudinalMeters: 600)
            : MKCoordinateRegion(center: Self.italyCenter,
                                 latitudinalMeters: 800_000,
                                 longitudinalMeters: 800_000)
    }

    // MARK: - Mappa

    /// Il centro è cambiato per volontà dell'utente: un gesto sulla mappa o il
    /// tasto "la mia posizione". Da qui in poi il punto vale come scelto.
    func centerMoved(to coordinate: CLLocationCoordinate2D) {
        center = coordinate
        pinPositioned = true
        scheduleReverseGeocode()
    }

    // MARK: - Ricerca

    func updateQuery(_ value: String) {
        searchQuery = value
        if ignoreNextQueryChange {
            ignoreNextQueryChange = false
            return
        }
        search.update(query: value)
    }

    func clearQuery() {
        searchQuery = ""
        searchResults = []
        searchState = .idle
        search.clear()
    }

    /// Applica una riga della tendina. Ritorna `false` se MapKit non è
    /// riuscito a risolverla, così la vista sa di non dover animare la camera.
    func apply(_ suggestion: LocationSuggestion) async -> Bool {
        guard let item = await search.resolve(suggestion) else {
            searchState = .noResults
            return false
        }
        apply(item, queryText: suggestion.title)
        return true
    }

    /// Tasto "Cerca" della tastiera.
    func submitSearch() async -> Bool {
        guard let item = await search.topResult(for: searchQuery) else {
            searchState = .noResults
            return false
        }
        apply(item, queryText: item.name ?? searchQuery)
        return true
    }

    private func apply(_ item: MKMapItem, queryText: String) {
        // Il risultato porta già con sé l'indirizzo: geocodificare di nuovo lo
        // stesso punto sarebbe solo una corsa fra due fonti.
        geocodeTask?.cancel()

        let placemark = item.placemark
        center = placemark.coordinate
        pinPositioned = true

        let formatted = PlacemarkFormatter.address(placemark)
        address = formatted
        addressEditedByUser = false
        lastGeocodedCenter = placemark.coordinate
        geocodeState = formatted.isEmpty ? .failed : .done

        let suggested = item.name ?? PlacemarkFormatter.suggestedName(placemark)
        if !suggested.isEmpty {
            nameSuggestion = suggested
            if !nameEditedByUser { name = suggested }
        }

        searchQuery = queryText
        searchResults = []
        searchState = .idle
        ignoreNextQueryChange = true
        search.clear()
    }

    // MARK: - Indirizzo

    func editAddress(_ value: String) {
        address = value
        addressEditedByUser = true
    }

    func editName(_ value: String) {
        name = value
        nameEditedByUser = true
    }

    private func scheduleReverseGeocode() {
        guard pinPositioned else { return }
        if let last = lastGeocodedCenter, distance(last, center) <= Self.significantMoveMeters {
            return
        }

        // Spostarsi davvero altrove rende obsoleta la correzione manuale
        // dell'indirizzo: senza questo si salverebbero le coordinate di un
        // posto e l'indirizzo di un altro, senza nessun avviso.
        addressEditedByUser = false

        geocodeTask?.cancel()
        geocodeState = .loading
        let target = center
        geocodeTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 500_000_000)
            guard !Task.isCancelled, let self else { return }
            let result = await self.geocoder.lookup(latitude: target.latitude,
                                                    longitude: target.longitude)
            guard !Task.isCancelled else { return }
            guard let result else {
                // Nessun alert: l'indirizzo si scrive a mano nella pagina dopo.
                self.geocodeState = .failed
                return
            }
            self.lastGeocodedCenter = target
            self.geocodeState = .done
            if !self.addressEditedByUser { self.address = result.address }
            if !self.nameEditedByUser, !result.name.isEmpty {
                self.nameSuggestion = result.name
                self.name = result.name
            }
        }
    }

    private func distance(_ a: CLLocationCoordinate2D, _ b: CLLocationCoordinate2D) -> CLLocationDistance {
        CLLocation(latitude: a.latitude, longitude: a.longitude)
            .distance(from: CLLocation(latitude: b.latitude, longitude: b.longitude))
    }

    // MARK: - Salvataggio

    var canSave: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && saveState != .saving
    }

    /// Ritorna la posizione appena creata, o `nil` se il POST è fallito: in
    /// quel caso `saveState` vale `.failed` e la pagina resta dov'è.
    func save() async -> LocationModel? {
        guard canSave else { return nil }
        saveState = .saving
        // Android creava `created_by: ""`, formato che nessun record salvato
        // ha: con `refresh()` che fa deleteAll() la posizione spariva.
        let userId = (koin.auth.currentUser.value as? UserModel)?.id ?? ""
        do {
            let created = try await koin.locations.createAndAddLocal(
                name: name.trimmingCharacters(in: .whitespacesAndNewlines),
                address: address.trimmingCharacters(in: .whitespacesAndNewlines),
                lat: Self.round7(center.latitude),
                lon: Self.round7(center.longitude),
                createdBy: userId
            )
            saveState = .idle
            return created
        } catch {
            saveState = .failed
            return nil
        }
    }

    /// Le coordinate a 13-15 decimali sono la firma dei record legacy senza
    /// indirizzo: tagliamo a 7 (~1 cm), che è la precisione degli altri.
    private static func round7(_ value: Double) -> Double {
        (value * 10_000_000).rounded() / 10_000_000
    }
}
