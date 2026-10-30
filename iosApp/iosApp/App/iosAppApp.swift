import SwiftUI
import Shared
import CoreSpotlight

@main
struct MensaApp: App {
    // AppDelegate hosts Firebase + APNs + UNUserNotificationCenter delegation.
    // See `App/AppDelegate.swift`.
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    init() {
        GothamFont.register()
        // Prima di qualunque cosa tocchi la sessione: porta le voci del
        // Keychain gia' presenti a `ThisDeviceOnly`, cioe' fuori dai backup
        // del dispositivo. Vedi `KeychainHardening`.
        KeychainHardening.harden()
        CachedImageCacheConfig.configureShared()
        CrashLogger_appleKt.installCrashLogger()
        MensaSdk.shared.doInitKoinIos()
        Log.app.info("Koin initialized")
        // Cattura screenshot per gli store (`tools/storekit`): sostituisce
        // nome, foto e numero tessera del socio loggato con un segnaposto,
        // così le immagini pubblicate non espongono i dati di chi ha fatto il
        // login. È solo una property su un oggetto Kotlin, quindi non tocca il
        // database e può stare qui, prima di `BootstrapGate`.
        DemoIdentity.shared.enabled =
            ProcessInfo.processInfo.environment["MENSA_DEMO_IDENTITY"] == "1"
        #if DEBUG
        // Classifica inventata, per catturarne le schermate senza login:
        // vedi `LeaderboardFixtures` e i case `leaderboard_preview` qui sotto.
        LeaderboardFixtures.shared.enabled =
            ProcessInfo.processInfo.environment["MENSA_LEADERBOARD_FIXTURE"] == "1"
        MedalFixtures.shared.enabled =
            ProcessInfo.processInfo.environment["MENSA_MEDALS_FIXTURE"] == "1"
        if let place = ProcessInfo.processInfo.environment["MENSA_LEADERBOARD_ME"].flatMap(Int32.init) {
            LeaderboardFixtures.shared.mePlace = place
        }
        #endif
        // Register the iOS Spotlight sink with KMP. KMP owns the diff +
        // image download + batching; the sink is a thin translator from
        // `SpotlightMemberBlock` → `CSSearchableItem`. clearAll() also
        // covers the wipe-on-logout / wipe-on-session-dead path that the
        // previous `SpotlightCleaner` callback handled.
        SpotlightSinkRegistry.shared.sink = SpotlightSinkImpl.shared
        DebugRefreshHarness.runIfRequested()
        #if DEBUG
        // Registro soci inventato, per provarne ricerca e filtro per regione
        // senza login: con `MENSA_LAUNCH_SCREEN=members`. Vedi `RegSociFixtures`.
        RegSociFixtures.shared.enabled =
            ProcessInfo.processInfo.environment["MENSA_REGSOCI_FIXTURE"] == "1"
        #endif
        // Stripe bootstrap moved into `BootstrapGate` below — it must run
        // AFTER the SQLDelight database is initialised, otherwise the first
        // koin access inside `StripeService.bootstrap` throws
        // `MensaDatabase not initialized` (DB is now a lazy holder filled
        // by `initializeMensaDatabase` from a suspend bootstrap path).
    }

    var body: some Scene {
        WindowGroup {
            BootstrapGate {
                DebugLaunchSelector()
            }
            // Spotlight tap: l'utente cerca su iOS, tocca un risultato →
            // iOS apre l'app passando un NSUserActivity con
            // CSSearchableItemActionType e l'identifier nostro
            // ("member:<id>" / "document:<id>"). Lo stashamo in
            // PendingDeepLink (stessa pipeline delle push) così MainTabView
            // lo drena e apre la sheet di dettaglio.
            // Link universali `https://svc.mensa.it/links/…` (QR dei
            // francobolli, eventi condivisi). Con il ciclo di vita SwiftUI
            // arrivano qui, come gli URL scheme. Stessa pipeline delle push:
            // `MainTabView` li drena quando c'è, anche dopo login o rinnovo.
            .onOpenURL { url in
                PendingDeepLink.shared.open(url: url)
            }
            .onContinueUserActivity(CSSearchableItemActionType) { activity in
                guard let id = activity.userInfo?[CSSearchableItemActivityIdentifier] as? String,
                      let target = SpotlightIndexer.target(forIdentifier: id) else { return }
                PendingDeepLink.shared.set(target)
            }
        }
    }
}

/// Gates the root UI on the async bootstrap chain:
///   1. `initializeMensaDatabase(DriverFactory())` — fills the SQLDelight
///      lazy holder that every repository depends on. Must complete BEFORE
///      any koin accessor is touched.
///   2. `StripeService.bootstrap()` — fetches the Stripe publishable key,
///      in the background (not awaited).
/// Until step 1 is done we show a `LoadingDots` splash; on error we surface
/// it as a `ContentUnavailableView` so the user can see why the app stalled
/// instead of getting a black screen.
struct BootstrapGate<Content: View>: View {
    @ViewBuilder let content: () -> Content
    @State private var ready = false
    @State private var error: String?

    var body: some View {
        Group {
            if ready {
                content()
            } else if let error {
                ContentUnavailableView(
                    error,
                    systemImage: "exclamationmark.triangle"
                )
            } else {
                LoadingDots()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .task {
            do {
                try await DatabaseModuleKt.initializeMensaDatabase(factory: DriverFactory())
                // In sottofondo: la chiave Stripe arriva da `configs`, cioe'
                // dalla rete, e l'avvio la aspettava per pagamenti che si
                // aprono molto piu' tardi. Ogni flusso di `StripeService` la
                // richiede comunque da se' prima di usarla.
                Task { try? await StripeService.bootstrap() }
                ready = true
                // DB ora pronto: sblocca Spotlight refresh + schedula BGTask.
                // Vedi `SpotlightRefreshCoordinator.markBootstrapped()`.
                SpotlightRefreshCoordinator.shared.markBootstrapped()
            } catch {
                self.error = error.userMessage
            }
        }
    }
}

/// Reads `MENSA_LAUNCH_SCREEN` env to render a specific view directly for
/// crash isolation. Falls back to normal RootView.
struct DebugLaunchSelector: View {
    private let screen = ProcessInfo.processInfo.environment["MENSA_LAUNCH_SCREEN"]
    @State private var didAuth = false
    var body: some View {
        if let target = screen {
            NavigationStack {
                Group {
                    switch target {
                    case "documents": AreaDocumentsView()
                    case "events": EventListView()
                    case "deals": DealListView()
                    case "sigs": SigListView()
                    case "members": MembersDirectoryView()
                    case "tableport": TableportStampView()
                    case "notifications": NotificationsListView()
                    case "addonsHub": AddonsHubView()
                    case "nowPlaying": DebugNowPlayingPreview()
                    case "leaderboard_preview": LeaderboardView()
                    case "medals_preview": MedalCabinetView()
                    case "leaderboard_preview_card": DebugLeaderboardCardPreview()
                    default: Text("Unknown screen: \(target)")
                    }
                }
            }
            .task {
                if !didAuth, let email = ProcessInfo.processInfo.environment["MENSA_AUTOLOGIN_EMAIL"], let pwd = ProcessInfo.processInfo.environment["MENSA_AUTOLOGIN_PWD"] {
                    didAuth = true
                    _ = try? await koin.auth.doInit()
                    if !(koin.auth.authState.value is AuthStateAuthenticated) {
                        _ = try? await koin.auth.login(email: email, password: pwd)
                    }
                }
            }
        } else {
            RootView()
        }
    }
}

/// Direct preview of `NowPlayingFullScreenView` for HIG verification without
/// having to navigate through login + article. Injects a mock track and
/// presents the player as a sheet (mirroring the production presentation).
private struct DebugNowPlayingPreview: View {
    @State private var presented = false
    var body: some View {
        ZStack {
            Color(.systemBackground).ignoresSafeArea()
            Button("Open Player") {
                let track = AudioTrack(
                    id: "debug-1",
                    title: "Il futuro del Mensa raccontato in dieci minuti",
                    subtitle: "Quid · letto da Giulia",
                    artworkURL: URL(string: "https://picsum.photos/seed/quidcover/800/800"),
                    audioURL: URL(string: "https://download.samplelib.com/mp3/sample-15s.mp3")!,
                    duration: 15,
                    originDeepLink: nil
                )
                AudioPlayerService.shared.play(track)
                AudioPlayerService.shared.presentFullPlayer()
                presented = true
            }
            .buttonStyle(.borderedProminent)
        }
        .sheet(isPresented: $presented) {
            NowPlayingFullScreenView()
                .presentationDetents([.large])
                .presentationDragIndicator(.hidden)
                .presentationBackground(Color.black)
                .presentationCornerRadius(28)
        }
        .task {
            // Auto-open on launch so screenshots/preview capture it immediately.
            try? await Task.sleep(nanoseconds: 300_000_000)
            let track = AudioTrack(
                id: "debug-1",
                title: "Il futuro del Mensa raccontato in dieci minuti",
                subtitle: "Quid · letto da Giulia",
                artworkURL: URL(string: "https://picsum.photos/seed/quidcover/800/800"),
                audioURL: URL(string: "https://download.samplelib.com/mp3/sample-15s.mp3")!,
                duration: 15,
                originDeepLink: nil
            )
            AudioPlayerService.shared.play(track)
            AudioPlayerService.shared.presentFullPlayer()
            presented = true
        }
    }
}

/// Today's membership card, with the podium stamp on its back, and the
/// ranking's row, on the made-up ranking (`MENSA_LEADERBOARD_FIXTURE=1`,
/// `MENSA_LEADERBOARD_ME=1` to stand on the podium), for screenshots
/// without the login.
private struct DebugLeaderboardCardPreview: View {
    @State private var store = LeaderboardStore.shared

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("Today").font(.largeTitle.weight(.heavy)).padding(.bottom, 8)
                MembershipCardHero(
                    fullName: "Giorgia Bianchi",
                    memberSince: "2024",
                    expiry: "31/12/2026",
                    memberId: "12345",
                    podiumPlace: store.board?.myPodiumPlace.map { Int($0.int32Value) }
                )
                .shadow(color: .black.opacity(0.18), radius: 18, x: 0, y: 8)
                if let board = store.board {
                    Label("ISCRIZIONE", systemImage: "person.text.rectangle")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.secondary)
                        .padding(.top, 12)
                    NavigationLink { LeaderboardView() } label: { LeaderboardMembershipRow(board: board) }
                        .buttonStyle(.plain)
                        .background(RoundedRectangle(cornerRadius: 18, style: .continuous).fill(Color(.secondarySystemBackground)))
                }
            }
            .padding(16)
        }
        .background(Color(.systemBackground))
        .task { await store.refresh() }
    }
}
