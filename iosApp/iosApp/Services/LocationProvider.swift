import Foundation
import CoreLocation

/// Una-tantum lookup della posizione utente.
///
/// Stateful (è anche `CLLocationManagerDelegate`), expone un'API async che
/// gestisce la richiesta di authorization. Pensato per chiamate sporadiche
/// (Today screen, filtro eventi), non per tracking continuo. La posizione
/// viene cachata in-memory per la sessione corrente.
@MainActor
final class LocationProvider: NSObject, CLLocationManagerDelegate {
    static let shared = LocationProvider()

    private let manager = CLLocationManager()
    private var waiters: [CheckedContinuation<CLLocation?, Never>] = []
    private(set) var cached: CLLocation?

    private override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyKilometer
    }

    /// Ritorna la posizione più recente (cached) oppure ne richiede una nuova.
    /// Non lancia mai — `nil` significa "non disponibile / negata / timeout".
    func requestOnce(timeoutSeconds: Double = 8) async -> CLLocation? {
        if let cached { return cached }

        let status = manager.authorizationStatus
        if status == .denied || status == .restricted { return nil }

        return await withTaskGroup(of: CLLocation?.self) { group in
            group.addTask { @MainActor [weak self] in
                guard let self else { return nil }
                return await withCheckedContinuation { cont in
                    self.waiters.append(cont)
                    if status == .notDetermined {
                        // Aspettiamo `locationManagerDidChangeAuthorization` per
                        // chiamare `requestLocation()`.
                        self.manager.requestWhenInUseAuthorization()
                    } else {
                        self.manager.requestLocation()
                    }
                }
            }
            group.addTask {
                try? await Task.sleep(nanoseconds: UInt64(timeoutSeconds * 1_000_000_000))
                return nil
            }
            let first = await group.next()
            group.cancelAll()
            return first ?? nil
        }
    }

    // MARK: - CLLocationManagerDelegate

    nonisolated func locationManager(_ m: CLLocationManager,
                                     didUpdateLocations locs: [CLLocation]) {
        let loc = locs.last
        Task { @MainActor in
            self.cached = loc
            self.flushWaiters(with: loc)
        }
    }

    nonisolated func locationManager(_ m: CLLocationManager,
                                     didFailWithError error: Error) {
        Task { @MainActor in
            self.flushWaiters(with: nil)
        }
    }

    nonisolated func locationManagerDidChangeAuthorization(_ m: CLLocationManager) {
        let status = m.authorizationStatus
        Task { @MainActor in
            switch status {
            case .authorizedWhenInUse, .authorizedAlways:
                if !self.waiters.isEmpty { self.manager.requestLocation() }
            case .denied, .restricted:
                self.flushWaiters(with: nil)
            default:
                break
            }
        }
    }

    private func flushWaiters(with location: CLLocation?) {
        let list = waiters
        waiters.removeAll()
        for cont in list { cont.resume(returning: location) }
    }
}
