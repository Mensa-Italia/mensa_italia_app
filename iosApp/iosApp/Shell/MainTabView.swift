import SwiftUI
import Combine

/// 5-tab shell with iOS 26 Liquid Glass tab bar.
/// iOS 26 automatically applies the Liquid Glass style to TabView.
struct MainTabView: View {
    @State private var selectedTab: AppTab = {
        let args = ProcessInfo.processInfo.arguments
        if let i = args.firstIndex(of: "--initial-tab"), i + 1 < args.count {
            switch args[i + 1] {
            case "today": return .today
            case "discover": return .discover
            case "search": return .search
            case "card": return .card
            case "profile": return .profile
            default: return .today
            }
        }
        return .today
    }()

    /// Push deep-link destination presented as a modal sheet over the tab bar.
    /// Mirrors Flutter's `handleNotificationActions` (master_model.dart): a
    /// tapped notification opens the referenced object regardless of which tab
    /// the user is on.
    @State private var pushDestination: NotificationTarget?

    /// Slot shared with `AppDelegate` for cold-launch taps that arrive before
    /// this view is mounted (mirrors Flutter `getInitialMessage`). We observe
    /// `target` so a tap that lands the value while we're already alive also
    /// gets consumed without relying on `NotificationCenter` timing.
    @StateObject private var pendingDeepLink = PendingDeepLink.shared

    /// Local mirrors of `AudioPlayerService` state, bridged via `onReceive`
    /// /`onChange` so this view doesn't observe the audio service directly —
    /// that would re-render the whole TabView at the 10 Hz time-observer
    /// cadence and reset NavigationStack contents.
    @State private var hasAudioTrack: Bool = false
    @State private var isPresentingFullPlayer: Bool = false

    /// Live-measured bottom inset of the iOS 26 floating tab bar
    /// (tab-bar pill height + home-indicator). Captured by reading
    /// `safeAreaInsets.bottom` from inside a Tab's content — at that depth
    /// the system includes the tab bar's footprint. The hardcoded 92pt is
    /// only the initial fallback before the first measurement arrives.
    @State private var tabBarBottomInset: CGFloat = 92

    /// Gap adjustment between the mini-player and the floating tab bar.
    /// The measured `safeAreaInsets.bottom` from inside a Tab includes a
    /// generous system padding above the floating tab bar (~30pt on iPhone),
    /// so we subtract to tuck the mini-player close to the tab bar
    /// Apple-Music-style.
    private let miniPlayerGap: CGFloat = -24

    var body: some View {
        ZStack(alignment: .bottom) {
        TabView(selection: $selectedTab) {
            Tab(tr("app.tab.today", fallback: "Today"), systemImage: "sparkles", value: AppTab.today) { // i18n
                NavigationStack {
                    TodayView()
                }
                // Measure the bottom safe-area inset from inside a Tab — at
                // this depth `safeAreaInsets.bottom` includes the floating
                // tab bar's reserved footprint plus the home indicator,
                // which is exactly what we need to position the mini-player.
                .background(
                    GeometryReader { proxy in
                        Color.clear
                            .preference(
                                key: TabBarBottomInsetKey.self,
                                value: proxy.safeAreaInsets.bottom
                            )
                    }
                )
            }
            Tab(tr("app.tab.discover", fallback: "Discover"), systemImage: "square.grid.2x2", value: AppTab.discover) { // i18n
                NavigationStack {
                    DiscoverView()
                }
            }
            Tab(tr("app.tab.search", fallback: "Search"), systemImage: "magnifyingglass", value: AppTab.search, role: .search) { // i18n
                NavigationStack {
                    SearchView()
                }
            }
            Tab(tr("app.tab.card", fallback: "Card"), systemImage: "person.text.rectangle", value: AppTab.card) { // i18n
                NavigationStack {
                    CardView()
                }
            }
            Tab(tr("app.tab.profile", fallback: "Profile"), systemImage: "person.crop.circle", value: AppTab.profile) { // i18n
                NavigationStack {
                    ProfileView()
                }
            }
        }
        .task {
            // Ask for push permission once we're past auth + onboarding.
            // Non-blocking; the system dialog is owned by UNUserNotificationCenter.
            _ = await PushPermissionRequester.requestIfNeeded()
            // If FCM has already produced a token before login completed,
            // flush it to the backend now that we know the user id.
            await PushTokenStore.shared.uploadIfPossible()
        }
        .onReceive(NotificationCenter.default.publisher(for: .mensaDeepLink)) { note in
            if let target = note.userInfo?[PushDeepLinkRouter.payloadKey] as? NotificationTarget {
                pushDestination = target
                _ = pendingDeepLink.consume()
            }
        }
        .onAppear {
            // Cold-launch path: AppDelegate stashed the target before
            // MainTabView was mounted (RootView was still on SplashView
            // while auth/onboarding evaluated). Drain it now.
            if let target = pendingDeepLink.consume() {
                pushDestination = target
            }
        }
        .onChange(of: pendingDeepLink.target) { _, newValue in
            // Belt-and-braces: any push tap that lands a target while we're
            // already alive gets consumed even if the NotificationCenter
            // publisher missed it (e.g. observer attached after the post).
            if let target = newValue {
                pushDestination = target
                _ = pendingDeepLink.consume()
            }
        }
        .sheet(item: $pushDestination) { target in
            // AccountConfirmationSheet is self-contained (brings its own
            // NavigationStack + presentationDetents) — present it directly
            // instead of wrapping in another NavigationStack.
            if case .accountConfirmation(let appId, let url, let nid) = target {
                AccountConfirmationSheet(
                    exAppId: appId,
                    callbackUrl: url,
                    notificationId: nid,
                    onDismiss: { pushDestination = nil }
                )
            } else {
                NavigationStack {
                    pushDestinationView(for: target)
                }
            }
        }
        // Mini-player floats above the iOS 26 tab bar as a SIBLING of the
        // TabView inside this ZStack. The TabView itself receives NO modifier
        // change when audio state flips — only the sibling appears/disappears
        // — so NavigationStack state is preserved across playback events.
        // Hardcoded bottom inset (`miniPlayerBottomInset`) clears the floating
        // tab bar's visual footprint.
        if hasAudioTrack {
            MiniAudioPlayer()
                .padding(.horizontal, 12)
                .padding(.bottom, tabBarBottomInset + miniPlayerGap)
                .transition(.move(edge: .bottom).combined(with: .opacity))
        }
        } // end ZStack
        .animation(.spring(response: 0.4, dampingFraction: 0.86), value: hasAudioTrack)
        .onPreferenceChange(TabBarBottomInsetKey.self) { newValue in
            if newValue > 0 && tabBarBottomInset != newValue {
                tabBarBottomInset = newValue
            }
        }
        // Sheet con detent `.large` — pattern Apple-canonical (Developer
        // Forums 820932): l'esatta animazione Apple Music è private API,
        // questo è il match più stretto possibile con SwiftUI puro.
        .sheet(isPresented: $isPresentingFullPlayer) {
            NowPlayingFullScreenView()
                .presentationDetents([.large])
                .presentationDragIndicator(.hidden)
                .presentationBackground(Color.black)
                .presentationCornerRadius(28)
        }
        .onReceive(
            AudioPlayerService.shared.$currentTrack
                .map { $0 != nil }
                .removeDuplicates()
        ) { active in
            hasAudioTrack = active
        }
        .onReceive(
            AudioPlayerService.shared.$isPresentingFullPlayer.removeDuplicates()
        ) { presenting in
            if isPresentingFullPlayer != presenting {
                isPresentingFullPlayer = presenting
            }
        }
        .onChange(of: isPresentingFullPlayer) { _, newValue in
            if AudioPlayerService.shared.isPresentingFullPlayer != newValue {
                AudioPlayerService.shared.isPresentingFullPlayer = newValue
            }
        }
    }

    @ViewBuilder
    private func pushDestinationView(for target: NotificationTarget) -> some View {
        switch target {
        case .event(let id):          EventDetailView(eventId: id)
        case .deal(let id):           DealDetailView(dealId: id)
        case .singleDocument(let id): DocumentDetailView(documentId: id)
        case .multipleDocuments:      AreaDocumentsView()
        case .ticketPurchase:         TicketsListView()
        case .paymentUpdateStatus:    ReceiptsListView()
        case .quid(let id):           QuidIssueView(issueId: Int64(id) ?? 0, issueName: "Quid")
        case .quidArticle(let id):    QuidArticleView(articleId: Int64(id) ?? 0)
        case .quidPdf(let id):        QuidPDFDeepLinkLoader(recordId: Int64(id) ?? 0)
        case .localOffice(let slug):  LocalOfficeBySlugLoader(slug: slug)
        case .accountConfirmation:    EmptyView() // handled directly in .sheet
        case .member(let id):         MemberDetailView(memberId: id)
        }
    }
}

/// Bubbles up the bottom safe-area inset measured from inside a Tab's
/// content. iOS 26 puts the floating tab bar inside the bottom safe area,
/// so this value equals `tab-bar pill height + home-indicator` and is
/// exactly the offset we need to float the mini-player above the tab bar.
private struct TabBarBottomInsetKey: PreferenceKey {
    static let defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        let next = nextValue()
        if next > 0 { value = next }
    }
}

// `NotificationTarget` is declared in `Features/Notifications/NotificationsListView.swift`
// and conforms to `Hashable`. We need `Identifiable` for `.sheet(item:)`.
extension NotificationTarget: Identifiable {
    public var id: String {
        switch self {
        case .event(let id):          return "event:\(id)"
        case .deal(let id):           return "deal:\(id)"
        case .singleDocument(let id): return "singleDocument:\(id)"
        case .multipleDocuments:      return "multipleDocuments"
        case .ticketPurchase:         return "ticketPurchase"
        case .paymentUpdateStatus:    return "paymentUpdateStatus"
        case .quid(let id):           return "quid:\(id)"
        case .quidArticle(let id):    return "quidArticle:\(id)"
        case .quidPdf(let id):        return "quidPdf:\(id)"
        case .localOffice(let slug):  return "localOffice:\(slug)"
        case .accountConfirmation(let appId, _, _): return "accountConfirmation:\(appId)"
        case .member(let id):         return "member:\(id)"
        }
    }
}
