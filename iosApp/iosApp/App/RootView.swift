import SwiftUI
import Shared

@MainActor @Observable
final class RootViewModel {
    var phase: RootPhase = .loading
    private var authSub: Closeable?
    private var userSub: Closeable?

    /// True when this app process was launched with NO persisted session, i.e.
    /// the user is about to (or just did) sign in interactively. Used by the
    /// DEBUG onboarding force-show below: we only want the onboarding flow on
    /// fresh logins, not every cold launch with a restored session.
    private var sessionRestored: Bool = false

    func start() async {
        // 1. Bootstrap i18n. Honour the per-app language override the user
        //    set in Profile → Lingua, otherwise follow the device locale.
        let preferred = LocaleManager.shared.preferredTag
        try? await koin.i18n.bootstrap(preferred: preferred)
        Log.auth.info("i18n bootstrapped for locale: \(preferred)")
        // Subscribe to the catalog ready flow so future language switches
        // bump LocaleManager.version and re-render the SwiftUI tree.
        LocaleManager.shared.startObservingCatalog()

        // 2. Initialize auth state from persisted token. Capture whether the
        //    init landed us in Authenticated — that means we're resuming an
        //    existing session and the DEBUG onboarding force-show should NOT
        //    trigger. (Fresh logins flip `sessionRestored` back to false
        //    implicitly because they happen after this initial check.)
        try? await koin.auth.doInit()
        sessionRestored = koin.auth.authState.value is AuthStateAuthenticated

        // 2b. Pre-warm the local SQLDelight cache for the Today screen so the
        //     first frame after the splash is already populated. We tolerate
        //     a tiny extension of the splash (one DB read) to avoid the much
        //     uglier "spinner flash → content" jank on the landing screen.
        //     Only meaningful for an authenticated session — for anonymous
        //     users we land on Login, which doesn't read the cache.
        if sessionRestored {
            await prewarmTodayCache()
        }

        // 3. Subscribe to authState Flow
        let authFlow = koin.auth.authState as Kotlinx_coroutines_coreFlow
        authSub = subscribeFlow(authFlow) { [weak self] (_: AuthState) in
            Task { @MainActor [weak self] in
                await self?.evaluatePhase()
            }
        } onError: { err in
            Log.auth.error("authState error: \(err.localizedDescription)")
        }

        // 4. Subscribe to currentUser so phase re-evaluates when user record arrives
        let userFlow = koin.auth.currentUser as Kotlinx_coroutines_coreFlow
        userSub = subscribeOptionalFlow(userFlow) { [weak self] (_: UserModel?) in
            Task { @MainActor [weak self] in
                await self?.evaluatePhase()
            }
        } onError: { _ in }

        // 5. Avvia il writer del payload Watch. Osserva auth.currentUser +
        //    events.observeAll() in background e mantiene aggiornato il JSON
        //    nell'App Group `group.it.mensa.app` consumato dalla Watch app e
        //    dal Widget complication. No-op se la Watch app non e' installata.
        WatchPayloadWriter.shared.start()

        Task { await self.evaluatePhase() }
    }

    func stop() {
        authSub?.close()
        authSub = nil
        userSub?.close()
        userSub = nil
    }

    /// Block on a single emission of `events.observeAll()` so the Today
    /// screen has data to render the moment it appears. The flow is a hot
    /// SQLDelight query — its first emission is essentially synchronous (the
    /// DB read), so this adds at most a few milliseconds to the splash. We
    /// cap the wait at 600ms anyway, so a pathological DB hang can't keep us
    /// stuck on the splash forever.
    private func prewarmTodayCache() async {
        let flow = koin.events.observeAll() as Kotlinx_coroutines_coreFlow
        await withCheckedContinuation { (cont: CheckedContinuation<Void, Never>) in
            var resumed = false
            var sub: Closeable?
            sub = subscribeFlow(flow) { (_: NSArray) in
                guard !resumed else { return }
                resumed = true
                sub?.close()
                cont.resume()
            } onError: { _ in
                guard !resumed else { return }
                resumed = true
                sub?.close()
                cont.resume()
            }
            // Safety timeout — splash should never wait more than ~600ms on us.
            Task {
                try? await Task.sleep(nanoseconds: 600_000_000)
                guard !resumed else { return }
                resumed = true
                sub?.close()
                cont.resume()
            }
        }
    }

    private func evaluatePhase() async {
        let state = koin.auth.authState.value
        if state is AuthStateAuthenticated {
            // We need the user record (currentUser) to evaluate the 24h gate.
            // If not loaded yet, stay on .loading (splash) — currentUser
            // subscription will re-call evaluatePhase when it arrives.
            guard let user = koin.auth.currentUser.value as? UserModel else {
                phase = .loading
                return
            }
            #if DEBUG
            // DEBUG override: every FRESH login lands on onboarding so we can
            // iterate on it. If the session was already restored from disk at
            // boot (`sessionRestored == true`), fall through to the prod
            // policy — we don't want to re-see the onboarding on every cold
            // launch. Env var MENSA_SKIP_ONBOARDING=1 still forces .main even
            // for fresh logins, useful when testing post-login screens.
            if ProcessInfo.processInfo.environment["MENSA_SKIP_ONBOARDING"] == "1" {
                phase = .main
            } else if sessionRestored {
                phase = (try? await koin.onboarding.shouldShow(user: user)) == true ? .onboarding : .main
            } else {
                phase = .onboarding
            }
            #else
            phase = (try? await koin.onboarding.shouldShow(user: user)) == true ? .onboarding : .main
            #endif
        } else if state is AuthStateAnonymous {
            phase = .anonymous
        } else {
            phase = .loading
        }
    }
}

struct RootView: View {
    @State private var vm = RootViewModel()
    @StateObject private var locale = LocaleManager.shared
    /// Preferenza utente: "esplora senza account" trasforma l'area pubblica
    /// nella root di lancio. Persiste tra cold-launch via UserDefaults. Da
    /// PublicAreaView, "Sei socio? Accedi" la riporta a false.
    @AppStorage("guestModeEnabled") private var guestMode: Bool = false
    private let previewBypass = ProcessInfo.processInfo.arguments.contains("--preview-authed")
    private let previewLogin = ProcessInfo.processInfo.arguments.contains("--preview-login")
    private let previewOnboarding = ProcessInfo.processInfo.arguments.contains("--preview-onboarding")
    private let initialPage: Int = {
        let args = ProcessInfo.processInfo.arguments
        if let idx = args.firstIndex(of: "--initial-page"), args.indices.contains(idx + 1) {
            return Int(args[idx + 1]) ?? 0
        }
        return 0
    }()

    var body: some View {
        Group {
            if previewOnboarding {
                OnboardingView(initialPage: initialPage, onComplete: {})
            } else if previewBypass {
                MainTabView()
            } else if previewLogin {
                LoginView()
            } else {
                switch vm.phase {
                case .loading:
                    SplashView()
                case .anonymous:
                    // Swap-root: se l'utente ha scelto "esplora senza
                    // account", PublicAreaShell e' la pagina di lancio
                    // (niente push, niente back). Dentro PublicArea c'e'
                    // un "Sei socio? Accedi" che rimette guestMode = false
                    // e RootView torna a LoginView.
                    if guestMode {
                        PublicAreaShell()
                            .transition(.opacity.combined(with: .move(edge: .leading)))
                    } else {
                        LoginView()
                            .transition(.opacity.combined(with: .move(edge: .trailing)))
                    }
                case .onboarding:
                    OnboardingView(onComplete: { [weak vm] in
                        vm?.phase = .main
                    })
                case .main:
                    MainTabView()
                }
            }
        }
        .animation(.easeInOut(duration: 0.35), value: guestMode)
        // When the user switches language, LocaleManager bumps `version` →
        // re-mounting the entire content forces every `tr()` lookup to resolve
        // against the freshly-loaded catalog. Heavy-handed but correct, and
        // language changes happen at most a handful of times per session.
        .id(locale.version)
        .task {
            // Always bootstrap i18n, even in preview modes, so the translation
            // catalog is loaded for tr() lookups.
            if previewBypass || previewLogin || previewOnboarding {
                let preferred = String(Locale.current.identifier.split(separator: "_").first ?? "it")
                try? await koin.i18n.bootstrap(preferred: preferred)
            } else {
                await vm.start()
            }
        }
        .onDisappear { vm.stop() }
        .animation(.smooth, value: vm.phase)
    }
}

// MARK: - Splash
//
// Matches the static UILaunchScreen (solid Mensa blue + white M+globe glyph)
// so the hand-off from the system launch surface is visually invisible. We add
// the LoadingDots once auth.init() takes longer than a frame; the dots themselves
// fade in to acknowledge the wait without breaking the continuity.

private struct SplashView: View {
    @State private var showDots = false
    var body: some View {
        ZStack {
            AppTheme.Colors.mensaBlue.ignoresSafeArea()
            VStack(spacing: 36) {
                MensaMark(size: 120, inBlueBadge: false)
                    .accessibilityLabel("Mensa Italia")
                LoadingDots()
                    .opacity(showDots ? 1 : 0)
            }
        }
        .task {
            try? await Task.sleep(nanoseconds: 250_000_000)
            withAnimation(.easeOut(duration: 0.4)) { showDots = true }
        }
    }
}
