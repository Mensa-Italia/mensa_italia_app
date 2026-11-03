import XCTest
@testable import iosApp

/// `AppBootstrap` e il percorso del token FCM prima che il database sia
/// pronto: la situazione del crash 22.5.1 (avvio senza interfaccia,
/// `BootstrapGate` mai montato, token consegnato da Firebase dopo un secondo).
@MainActor
final class AppBootstrapTests: XCTestCase {

    private struct Boom: Error {}

    private actor Counter {
        private(set) var value = 0
        func increment() { value += 1 }
        func incrementAndGet() -> Int { value += 1; return value }
    }

    /// Chi aspetta mentre l'inizializzazione e' in corso si accoda alla
    /// stessa: una sola esecuzione, tutti pronti insieme.
    func testConcurrentWaitersShareOneInitialization() async throws {
        let runs = Counter()
        let bootstrap = AppBootstrap {
            await runs.increment()
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        XCTAssertFalse(bootstrap.isReady)

        let first = Task { try await bootstrap.waitUntilReady() }
        let second = Task { try await bootstrap.waitUntilReady() }
        let third = Task { try await bootstrap.waitUntilReady() }
        try await first.value
        try await second.value
        try await third.value

        XCTAssertTrue(bootstrap.isReady)
        let n = await runs.value
        XCTAssertEqual(n, 1)
    }

    /// Un avvio fallito rilancia l'errore a chi aspettava e non lascia lo
    /// stato "in corso": la chiamata dopo riprova da capo.
    func testFailureIsRethrownAndRetriedOnNextCall() async throws {
        let attempts = Counter()
        let bootstrap = AppBootstrap {
            if await attempts.incrementAndGet() == 1 { throw Boom() }
        }

        do {
            try await bootstrap.waitUntilReady()
            XCTFail("la prima inizializzazione doveva fallire")
        } catch is Boom {}
        XCTAssertFalse(bootstrap.isReady)

        try await bootstrap.waitUntilReady()
        XCTAssertTrue(bootstrap.isReady)
        let n = await attempts.value
        XCTAssertEqual(n, 2)
    }

    func testOnceReadyInitializerIsNotCalledAgain() async throws {
        let runs = Counter()
        let bootstrap = AppBootstrap { await runs.increment() }
        try await bootstrap.waitUntilReady()
        try await bootstrap.waitUntilReady()
        let n = await runs.value
        XCTAssertEqual(n, 1)
    }

    /// Token FCM arrivato prima del database, e avvio che fallisce:
    /// `ensureRegistered` deve uscire senza toccare `koin` e tenere il token
    /// per le chiamate dopo il login (`RootView`, `MainTabView`).
    func testTokenBeforeFailedBootstrapIsKeptAndNotRegistered() async {
        let bootstrap = AppBootstrap { throw Boom() }
        let store = PushTokenStore(bootstrap: bootstrap)

        store.handle(token: "fcm-token")
        await store.ensureRegistered()

        XCTAssertEqual(store.currentToken, "fcm-token")
        XCTAssertFalse(bootstrap.isReady)
    }

    /// Avvio lento: `ensureRegistered` lo aspetta fino in fondo invece di
    /// toccare `koin` prima del tempo.
    func testEnsureRegisteredWaitsForBootstrap() async throws {
        // Dopo l'attesa `ensureRegistered` legge `koin.auth`, quindi il
        // database vero dell'app ospite deve essere pronto.
        try await AppBootstrap.shared.waitUntilReady()

        let finished = Counter()
        let bootstrap = AppBootstrap {
            try await Task.sleep(nanoseconds: 100_000_000)
            await finished.increment()
        }
        let store = PushTokenStore(bootstrap: bootstrap)

        store.handle(token: "fcm-token")
        await store.ensureRegistered()

        XCTAssertTrue(bootstrap.isReady)
        let n = await finished.value
        XCTAssertEqual(n, 1)
    }
}
