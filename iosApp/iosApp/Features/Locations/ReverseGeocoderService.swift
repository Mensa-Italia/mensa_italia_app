import Foundation
import CoreLocation

/// Formattazione condivisa fra reverse geocoding e risultati di ricerca.
///
/// Entrambi i percorsi consegnano un `CLPlacemark` (`MKPlacemark` è una sua
/// sottoclasse) e devono produrre la stessa stringa: se divergessero, gli
/// indirizzi salvati cercando un posto non somiglierebbero a quelli salvati
/// spostando la mappa, e la collection `positions` diventerebbe illeggibile.
enum PlacemarkFormatter {
    /// Ricalca il `formatted_address` di Google, che è il formato dei record
    /// già in produzione — es. "Via Donato Creti, 32, 40128 Bologna BO, Italia".
    static func address(_ p: CLPlacemark) -> String {
        var parts: [String] = []

        if let street = p.thoroughfare {
            if let number = p.subThoroughfare {
                parts.append("\(street), \(number)")
            } else {
                parts.append(street)
            }
        }

        var cityLine: [String] = []
        if let cap = p.postalCode { cityLine.append(cap) }
        if let city = p.locality { cityLine.append(city) }
        // In Italia `subAdministrativeArea` è la sigla della provincia ("BO").
        // Quando coincide col comune (Bologna/Bologna) ripeterla è solo rumore.
        if let province = p.subAdministrativeArea ?? p.administrativeArea,
           province != p.locality {
            cityLine.append(province)
        }
        if !cityLine.isEmpty { parts.append(cityLine.joined(separator: " ")) }

        if let country = p.country { parts.append(country) }
        return parts.joined(separator: ", ")
    }

    /// Nome proposto: il POI se il punto ne ha uno, altrimenti la via col
    /// civico — per un indirizzo civico `name` vale già "Via Donato Creti, 32".
    static func suggestedName(_ p: CLPlacemark) -> String {
        if let poi = p.areasOfInterest?.first, !poi.isEmpty { return poi }
        return p.name ?? p.thoroughfare ?? ""
    }
}

/// Reverse geocoding on-device del centro mappa.
///
/// È un `actor` perché `CLGeocoder` accetta una richiesta alla volta e
/// risponde `kCLErrorDomain 2` alla seconda: serializzando qui, chi chiama può
/// limitarsi al debounce senza preoccuparsi delle sovrapposizioni.
/// Non richiede nessuna chiave: l'indirizzo si ottiene anche offline dalla
/// cache di sistema.
actor ReverseGeocoderService {
    struct Result {
        let address: String
        let name: String
    }

    private let geocoder = CLGeocoder()

    /// `nil` significa "indirizzo non disponibile" — device senza geocoder,
    /// rete assente, o coordinate in mezzo al mare. Non lancia mai: chi chiama
    /// mostra un placeholder, non un errore.
    func lookup(latitude: Double, longitude: Double) async -> Result? {
        if geocoder.isGeocoding { geocoder.cancelGeocode() }
        let location = CLLocation(latitude: latitude, longitude: longitude)
        guard let placemark = try? await geocoder.reverseGeocodeLocation(location).first else {
            return nil
        }
        let formatted = PlacemarkFormatter.address(placemark)
        guard !formatted.isEmpty else { return nil }
        return Result(address: formatted, name: PlacemarkFormatter.suggestedName(placemark))
    }
}
