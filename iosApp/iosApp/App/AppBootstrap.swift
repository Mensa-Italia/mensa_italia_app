import Foundation
import Shared

/// Porta il database SQLDelight allo stato "pronto" una volta sola per
/// processo, da qualunque punto d'ingresso.
///
/// Prima lo faceva solo `BootstrapGate`, cioe' la finestra. In un avvio senza
/// interfaccia (BGTask di Spotlight, push silente) la vista non monta,
/// `initializeMensaDatabase` non gira, e il primo tocco alla globale `koin`
/// — che risolve subito tutti i repository, `AuthRepository` compreso, e
/// quello vuole il database — finisce in `MensaDatabase not initialized`.
/// Da Kotlin a Swift quell'errore non e' dichiarato, quindi e' un `abort()`:
/// il crash della 22.5.1 (3 nov, `Role: Non UI`, da
/// `PushTokenStore.ensureRegistered` sul token FCM che Firebase consegna a
/// ogni avvio).
///
/// Regola: chi puo' girare prima che la finestra esista aspetta
/// `waitUntilReady()` prima di toccare `koin`. L'inizializzazione in se' e'
/// idempotente lato Kotlin; qui si evita di lanciarla due volte in parallelo
/// e si ricorda l'esito.
@MainActor
final class AppBootstrap {
    static let shared = AppBootstrap {
        try await DatabaseModuleKt.initializeMensaDatabase(factory: DriverFactory())
    }

    typealias Initializer = @Sendable () async throws -> Void

    private let initializer: Initializer
    private var inflight: (id: Int, task: Task<Void, Error>)?
    private var lastId = 0

    /// `true` dopo che l'inizializzazione e' andata a buon fine una volta.
    private(set) var isReady = false

    /// `initializer` e' iniettabile per i test; l'app usa `shared`.
    init(initializer: @escaping Initializer) {
        self.initializer = initializer
    }

    /// Avvia l'inizializzazione senza aspettarla. Per `MensaApp.init`: cosi'
    /// parte anche negli avvii senza interfaccia, e la finestra, quando c'e',
    /// la trova gia' in corso.
    func start() {
        Task { try? await waitUntilReady() }
    }

    /// Aspetta che il database sia pronto. Chi arriva mentre
    /// l'inizializzazione e' in corso si accoda alla stessa; se e' fallita,
    /// il prossimo chiamante la ritenta e riceve il suo errore.
    func waitUntilReady() async throws {
        if isReady { return }
        let entry: (id: Int, task: Task<Void, Error>)
        if let inflight {
            entry = inflight
        } else {
            lastId += 1
            let initializer = self.initializer
            entry = (lastId, Task { try await initializer() })
            inflight = entry
        }
        do {
            try await entry.task.value
            isReady = true
        } catch {
            if inflight?.id == entry.id { inflight = nil }
            throw error
        }
    }
}
