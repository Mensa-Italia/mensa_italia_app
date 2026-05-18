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
        CachedImageCacheConfig.configureShared()
        CrashLogger_appleKt.installCrashLogger()
        MensaSdk.shared.doInitKoinIos()
        Log.app.info("Koin initialized")
        // Register the iOS Spotlight sink with KMP. KMP owns the diff +
        // image download + batching; the sink is a thin translator from
        // `SpotlightMemberBlock` → `CSSearchableItem`. clearAll() also
        // covers the wipe-on-logout / wipe-on-session-dead path that the
        // previous `SpotlightCleaner` callback handled.
        SpotlightSinkRegistry.shared.sink = SpotlightSinkImpl.shared
        DebugRefreshHarness.runIfRequested()
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
///   2. `StripeService.bootstrap()` — fetches the Stripe publishable key.
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
                try? await StripeService.bootstrap()
                ready = true
                // DB ora pronto: sblocca Spotlight refresh + schedula BGTask.
                // Vedi `SpotlightRefreshCoordinator.markBootstrapped()`.
                SpotlightRefreshCoordinator.shared.markBootstrapped()
            } catch {
                self.error = error.localizedDescription
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
                    case "boutique": BoutiqueView()
                    case "tableport": TableportStampView()
                    case "notifications": NotificationsListView()
                    case "addonsHub": AddonsHubView()
                    case "nowPlaying": DebugNowPlayingPreview()
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
