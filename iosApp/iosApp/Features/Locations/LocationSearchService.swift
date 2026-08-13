import Foundation
import MapKit

/// Una riga della tendina di autocomplete.
struct LocationSuggestion: Identifiable, Hashable {
    let id: String
    let title: String
    let subtitle: String

    init(title: String, subtitle: String) {
        // `MKLocalSearchCompletion` è una classe che MapKit ricrea a ogni
        // aggiornamento: usarla come identità di ForEach fa ridisegnare tutta
        // la lista a ogni tasto premuto. Il testo, invece, è stabile.
        self.id = subtitle.isEmpty ? title : "\(title)|\(subtitle)"
        self.title = title
        self.subtitle = subtitle
    }
}

/// Autocomplete di indirizzi e POI via MapKit.
///
/// Nessuna chiave e nessun costo: `MKLocalSearchCompleter` non ha rate limit,
/// `MKLocalSearch` sì (~50 richieste/minuto) ma la chiamiamo solo quando
/// l'utente sceglie davvero un risultato.
@MainActor
final class LocationSearchService: NSObject, MKLocalSearchCompleterDelegate {
    /// Sotto le tre lettere il completer restituisce mezzo mondo.
    private static let minQueryLength = 3
    private static let maxResults = 6
    private static let debounceNanoseconds: UInt64 = 300_000_000
    /// Oltre questo tempo consideriamo il provider muto e sblocchiamo la UI
    /// con "nessun risultato": la ricerca è sempre facoltativa.
    private static let timeoutNanoseconds: UInt64 = 5_000_000_000

    /// Notifica risultati e stato insieme, così chi ascolta non può mostrare
    /// una lista piena con lo stato "nessun risultato".
    var onChange: (([LocationSuggestion], LocationSearchState) -> Void)?

    private let completer = MKLocalSearchCompleter()
    /// Le `MKLocalSearchCompletion` dell'ultimo giro, per id del suggerimento:
    /// servono a risolvere la selezione in coordinate.
    private var completions: [String: MKLocalSearchCompletion] = [:]
    private var debounceTask: Task<Void, Never>?
    private var timeoutTask: Task<Void, Never>?

    override init() {
        super.init()
        completer.resultTypes = [.address, .pointOfInterest]
        completer.delegate = self
    }

    /// Polarizza i suggerimenti sulla porzione di mappa visibile. Senza questa
    /// la ricerca è mondiale e "Via Roma" restituisce l'altro emisfero.
    func setRegion(_ region: MKCoordinateRegion) {
        completer.region = region
    }

    func update(query: String) {
        debounceTask?.cancel()
        timeoutTask?.cancel()

        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= Self.minQueryLength else {
            completions.removeAll()
            completer.queryFragment = ""
            onChange?([], .idle)
            return
        }

        onChange?([], .loading)
        debounceTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: Self.debounceNanoseconds)
            guard !Task.isCancelled, let self else { return }
            self.completer.queryFragment = trimmed
            self.armTimeout()
        }
    }

    /// Chiude la sessione di ricerca senza toccare il testo del campo.
    func clear() {
        debounceTask?.cancel()
        timeoutTask?.cancel()
        completions.removeAll()
        completer.queryFragment = ""
    }

    /// Risolve una riga della tendina in un punto vero sulla mappa.
    func resolve(_ suggestion: LocationSuggestion) async -> MKMapItem? {
        guard let completion = completions[suggestion.id] else { return nil }
        let response = try? await MKLocalSearch(request: .init(completion: completion)).start()
        return response?.mapItems.first
    }

    /// Tasto "Cerca" della tastiera: ricerca completa, si applica il primo
    /// risultato senza passare dalla tendina.
    func topResult(for query: String) async -> MKMapItem? {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        let request = MKLocalSearch.Request()
        request.naturalLanguageQuery = trimmed
        request.region = completer.region
        let response = try? await MKLocalSearch(request: request).start()
        return response?.mapItems.first
    }

    private func armTimeout() {
        timeoutTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: Self.timeoutNanoseconds)
            guard !Task.isCancelled, let self else { return }
            self.completions.removeAll()
            self.onChange?([], .noResults)
        }
    }

    // MARK: - MKLocalSearchCompleterDelegate
    //
    // Il protocollo non è annotato @MainActor: i callback arrivano sul main
    // thread ma il compilatore non lo sa, da qui il salto esplicito.

    nonisolated func completerDidUpdateResults(_ completer: MKLocalSearchCompleter) {
        let items = completer.results
        Task { @MainActor in self.publish(items) }
    }

    nonisolated func completer(_ completer: MKLocalSearchCompleter, didFailWithError error: Error) {
        Task { @MainActor in self.publish([]) }
    }

    private func publish(_ items: [MKLocalSearchCompletion]) {
        timeoutTask?.cancel()
        var resolved: [String: MKLocalSearchCompletion] = [:]
        let suggestions = items.prefix(Self.maxResults).map { item -> LocationSuggestion in
            let suggestion = LocationSuggestion(title: item.title, subtitle: item.subtitle)
            resolved[suggestion.id] = item
            return suggestion
        }
        completions = resolved
        onChange?(suggestions, suggestions.isEmpty ? .noResults : .results)
    }
}
