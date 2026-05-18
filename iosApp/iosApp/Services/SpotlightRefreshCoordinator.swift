import Foundation
import BackgroundTasks
import Shared

/// Stato osservabile di una sincronizzazione Spotlight.
enum SpotlightSyncPhase: Equatable {
    case idle
    case docsRefreshing
    case docsIndexing(done: Int, total: Int)
    case membersRefreshing
    case membersIndexing(done: Int, total: Int)
    case thumbsDownloading(done: Int, total: Int)

    var isActive: Bool { self != .idle }

    var localizedShort: String {
        switch self {
        case .idle:                            return ""
        case .docsRefreshing:                  return "Scarico documenti…"
        case .docsIndexing(let d, let t):      return "Indicizzo documenti \(d)/\(t)"
        case .membersRefreshing:               return "Scarico registro soci…"
        case .membersIndexing(let d, let t):   return "Indicizzo soci \(d)/\(t)"
        case .thumbsDownloading(let d, let t): return "Foto soci \(d)/\(t)"
        }
    }
}

/// Orchestratore unico per il refresh dati + reindex CoreSpotlight.
@MainActor
@Observable
final class SpotlightRefreshCoordinator {

    static let shared = SpotlightRefreshCoordinator()

    /// Phase corrente — osservata da `SpotlightSyncBadge`.
    /// Aggiornata via callback throttled (~5/s) dal ProgressThrottler,
    /// MAI per ogni singolo item completato.
    var phase: SpotlightSyncPhase = .idle

    /// Diagnostic status persistente.
    private(set) var debugStatus: String =
        UserDefaults.standard.string(forKey: "spotlight.debugStatus") ?? "(mai eseguito)"

    static let bgTaskIdentifier = "it.mensa.app.spotlight-refresh"

    private let rollingExpiration: TimeInterval = 7 * 24 * 60 * 60

    // Throttle/timestamp persistence è in KMP (SpotlightSyncEngine →
    // SQLDelight KeyValue) così wipeAllUserData() lo azzera automaticamente
    // su logout. iOS si limita a chiedere `isDueForSync()` prima di partire.
    private let schemaVersionKey = "spotlight.indexSchemaVersion"

    /// v15 → riscrittura integrale: streaming download, CGImageSource resize,
    /// pre-render icone categoria upfront, ProgressThrottler ~5/s.
    /// v16 → emails/phones per soci. v17 → telemetria timing per fase
    /// v18 → guard auth.doInit prima del primo refresh
    /// v19 → bump per forzare rebuild in sim debug
    private let indexSchemaVersion: Int = 19

    private var inflight: Task<Void, Never>?
    private var isBootstrapped = false

    /// Retained subscription on `koin.auth.authState`. We hold it for the
    /// app's lifetime so a logout → login cycle automatically retriggers
    /// the Spotlight sync without requiring an app relaunch.
    private var authSub: Closeable?
    private var lastSeenAuthenticated: Bool = false

    private init() {}

    // MARK: - Public entry points

    func markBootstrapped() {
        guard !isBootstrapped else { return }
        isBootstrapped = true
        setStatus("bootstrap done")
        scheduleNextBGTask()
        subscribeToAuthState()

        let stored = UserDefaults.standard.integer(forKey: schemaVersionKey)
        if stored != indexSchemaVersion {
            setStatus("schema bump \(stored)→\(indexSchemaVersion) — full rebuild")
            UserDefaults.standard.set(indexSchemaVersion, forKey: schemaVersionKey)
            fullRebuild(reason: "schema-upgrade")
        } else {
            refreshIfNeeded(reason: "bootstrap")
        }
    }

    /// Re-run Spotlight sync whenever auth transitions Anonymous/Unknown →
    /// Authenticated (e.g. fresh login after a logout). The KMP throttle
    /// gate decides whether to actually do work — and since logout wiped
    /// the `KeyValue` table including the throttle timestamp, the first
    /// post-login emit is always due.
    private func subscribeToAuthState() {
        guard authSub == nil else { return }
        let flow = koin.auth.authState as Kotlinx_coroutines_coreFlow
        authSub = subscribeFlow(flow) { [weak self] (state: AuthState) in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let authenticated = state is AuthStateAuthenticated
                let wasAuthenticated = self.lastSeenAuthenticated
                self.lastSeenAuthenticated = authenticated
                if authenticated && !wasAuthenticated {
                    self.refreshIfNeeded(reason: "auth-changed")
                }
            }
        } onError: { err in
            Log.app.error("SpotlightRefreshCoordinator authState sub error: \(err.localizedDescription)")
        }
    }

    /// Opportunistico: KMP decide se è il momento (throttle gate +
    /// timestamp persistito in DB).
    func refreshIfNeeded(reason: String) {
        guard isBootstrapped else { return }
        runDetached(ignoreHashCache: false, throttled: true, reason: reason)
    }

    /// Push / schema upgrade. Ignora throttle E ignora hash cache (forza
    /// re-download di tutte le foto). Usato dove la struttura dell'indice è
    /// cambiata o il backend segnala "dati nuovi disponibili".
    func fullRebuild(reason: String) {
        guard isBootstrapped else { return }
        runDetached(ignoreHashCache: true, throttled: false, reason: reason)
    }

    /// Refresh manuale (debug button): ignora throttle ma RISPETTA la hash
    /// cache → niente download se `data_hash`/`image_hash` invariati.
    func manualRefresh(reason: String) {
        guard isBootstrapped else { return }
        runDetached(ignoreHashCache: false, throttled: false, reason: reason)
    }

    func clearAll() async {
        // Niente da fare lato Swift sul timestamp: vive in DB e wipeAllUserData()
        // lo cancella insieme al resto.
        try? await SpotlightSinkRegistry.shared.sink?.clearAll()
    }

    // MARK: - BGTask

    func registerBGTask() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: Self.bgTaskIdentifier,
            using: nil
        ) { [weak self] task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            Task { @MainActor [weak self] in
                await self?.handleBGTask(refreshTask)
            }
        }
    }

    func scheduleNextBGTask() {
        if isMembershipExpired() { return }
        let request = BGAppRefreshTaskRequest(identifier: Self.bgTaskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 6 * 60 * 60)
        try? BGTaskScheduler.shared.submit(request)
    }

    private func handleBGTask(_ task: BGAppRefreshTask) async {
        // Cold-launch da iOS solo per BGTask: SwiftUI BootstrapGate non gira,
        // il DB SQLDelight non è inizializzato. Lo facciamo qui (idempotente).
        do {
            try await DatabaseModuleKt.initializeMensaDatabase(factory: DriverFactory())
            isBootstrapped = true
        } catch {
            task.setTaskCompleted(success: false)
            return
        }
        scheduleNextBGTask()

        let work = Task { [weak self] in
            await self?.performRefresh(ignoreHashCache: false, throttled: true, reason: "bgtask")
        }
        task.expirationHandler = { work.cancel() }
        await work.value
        task.setTaskCompleted(success: !work.isCancelled)
    }

    // MARK: - Core

    private func runDetached(ignoreHashCache: Bool, throttled: Bool, reason: String) {
        if let inflight, !inflight.isCancelled {
            setStatus("skip (\(reason)) — already in-flight")
            return
        }
        setStatus("starting (\(reason)) ignoreHashCache=\(ignoreHashCache) throttled=\(throttled)")
        inflight = Task { [weak self] in
            await self?.performRefresh(ignoreHashCache: ignoreHashCache, throttled: throttled, reason: reason)
            self?.inflight = nil
        }
    }

    private func performRefresh(ignoreHashCache: Bool, throttled: Bool, reason: String) async {
        let t0 = Date()
        setStatus("performRefresh: enter (\(reason))")

        // BootstrapGate fires `markBootstrapped` non appena il DB è pronto,
        // PRIMA che RootView monti e chiami `koin.auth.doInit()`. Senza
        // questo guard, il primo refresh parte con token nil → API ritorna
        // 0 record. `doInit` è idempotente, possiamo chiamarlo sempre.
        setStatus("auth.doInit()")
        _ = try? await koin.auth.doInit()
        if !(koin.auth.authState.value is AuthStateAuthenticated) {
            setStatus("not authenticated — skip refresh")
            phase = .idle
            return
        }

        let expiration = computeExpiration()
        if expiration <= Date() {
            setStatus("tessera scaduta — clear + skip")
            try? await SpotlightSinkRegistry.shared.sink?.clearAll()
            phase = .idle
            return
        }

        // KMP-owned throttle gate. Salta TUTTE le fasi (docs + soci) se
        // l'ultimo sync è recente. Il timestamp vive nel DB SQLDelight,
        // quindi `wipeAllUserData()` (logout / sessione morta) lo cancella
        // automaticamente → al prossimo login parte sempre un sync pieno.
        if throttled {
            // K/N wraps `Boolean` from a suspend fun as `SharedBoolean?`
            // (NSNumber-bridged). Default to "due" on any error so a
            // transient DB hiccup doesn't permanently freeze the sync.
            let due: Bool = ((try? await koin.spotlightSync.isDueForSync())?.boolValue) ?? true
            if !due {
                setStatus("skip (\(reason)) — KMP throttle gate")
                phase = .idle
                return
            }
        }

        // === 1. Documenti ===
        let tDocs = Date()
        phase = .docsRefreshing
        setStatus("documents.refresh()")
        do { try await koin.documents.refresh() } catch {
            setStatus("documents.refresh FAILED: \(error.localizedDescription)")
        }
        var docsCount = 0
        do {
            let docs = (try await koin.documents.firstSnapshot()) as? [DocumentModel] ?? []
            docsCount = docs.count
            setStatus("documents snapshot: \(docsCount)")
            let tIndex = Date()
            await SpotlightIndexer.indexDocuments(docs, expiration: expiration) { [weak self] done, total in
                self?.phase = .docsIndexing(done: done, total: total)
            }
            setStatus("docs indexed \(docsCount) in \(String(format: "%.2f", Date().timeIntervalSince(tIndex)))s")
        } catch {
            setStatus("documents.snapshot FAILED: \(error.localizedDescription)")
        }
        _ = tDocs

        // === 2-3. Soci + foto ===
        // L'intera pipeline (fetch API + diff hash da DB + download immagini
        // + emit batch a Spotlight) gira ora in KMP. Il sink iOS si limita a
        // tradurre i blocchi in CSSearchableItem + resize/cache thumbnail.
        // Le tre fasi unificate riportano progresso tramite un'unica callback
        // (l'engine non distingue indicizzazione metadata vs. download foto).
        phase = .membersRefreshing
        setStatus("spotlightSync.syncMembers()")
        let tMembers = Date()
        let expirationSeconds = Int64(expiration.timeIntervalSince1970)
        var membersTotal = 0
        do {
            let report = try await koin.spotlightSync.syncMembers(
                expirationEpochSeconds: expirationSeconds,
                ignoreCache: ignoreHashCache,
                onProgress: { [weak self] done, total in
                    self?.phase = .membersIndexing(done: Int(truncating: done), total: Int(truncating: total))
                }
            )
            if let report {
                membersTotal = Int(report.total)
                setStatus(
                    "members done in \(String(format: "%.2f", Date().timeIntervalSince(tMembers)))s " +
                    "total=\(report.total) skip=\(report.skippedUnchanged) " +
                    "reuseData=\(report.reIndexedDataOnly) downloaded=\(report.downloadedImages) " +
                    "noImg=\(report.noImage) del=\(report.deletions) fail=\(report.downloadFailures)"
                )
            } else {
                setStatus("spotlightSync skipped: sink not registered")
            }
        } catch {
            setStatus("spotlightSync FAILED: \(error.localizedDescription)")
        }

        phase = .idle
        // Timestamp scritto da `SpotlightSyncEngine.syncMembers` su success.
        setStatus("DONE in \(String(format: "%.1f", Date().timeIntervalSince(t0)))s docs=\(docsCount) members=\(membersTotal)")
    }

    // MARK: - Helpers

    func setStatus(_ s: String) {
        let stamped = "\(timeString()) \(s)"
        debugStatus = stamped
        UserDefaults.standard.set(stamped, forKey: "spotlight.debugStatus")
        NSLog("[Spotlight] \(stamped)")
    }

    private func timeString() -> String {
        let f = DateFormatter()
        f.dateFormat = "HH:mm:ss"
        return f.string(from: Date())
    }

    private func computeExpiration() -> Date {
        let rollingDeadline = Date(timeIntervalSinceNow: rollingExpiration)
        guard let user = koin.auth.currentUser.value as? UserModel else { return rollingDeadline }
        let membershipDeadline = Date(timeIntervalSince1970: Double(user.expireMembership.epochSeconds))
        return min(rollingDeadline, membershipDeadline)
    }

    private func isMembershipExpired() -> Bool {
        guard let user = koin.auth.currentUser.value as? UserModel else { return false }
        return user.expireMembership.epochSeconds < Int64(Date().timeIntervalSince1970)
    }
}
