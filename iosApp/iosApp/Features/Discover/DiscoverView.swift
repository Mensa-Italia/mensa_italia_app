import SwiftUI
import Shared

enum DiscoverRoute: Hashable {
    case events
    case eventsMap
    case eventsCalendar
    case deals
    case sigs
    case members
    case addonsHub
    case boutique
    case documents
    case tableport
    case quid
    case podcasts
    case testAssistant
    case notifications
    case localOffices
}

struct DiscoverView: View {
    @State private var notificationsSub: Closeable?
    @State private var unreadNotificationsCount: Int = 0

    /// Test Assistant is gated to members holding the `testmakers` power
    /// (same rule as the Flutter `allowTestMakerAddon`). I powers sono
    /// stabili per tutta la sessione (cambiano solo a login/logout, e in
    /// quel caso RootView smonta questa view) → lettura sincrona dall'auth.
    private var canSeeTestAssistant: Bool {
        hasPower("testmakers", user: koin.auth.currentUser.value as? UserModel)
    }

    var body: some View {
        List {
            // Section 1 — Community / browse
            Section(header: header(tr("app.discover.community", fallback: "Community"))) {
                row(icon: "building.2.fill",
                    color: .cyan,
                    title: tr("local_offices.title", fallback: "Gruppi locali"),
                    route: .localOffices)
                row(icon: "calendar",
                    color: AppTheme.Colors.mensaBlue,
                    title: tr("views.events.title", fallback: "Eventi"),
                    route: .events)
                row(icon: "doc.text.fill",
                    color: .teal,
                    title: tr("addons.documents.title", fallback: "Documenti Area"),
                    route: .documents)
                row(icon: "person.crop.rectangle.stack.fill",
                    color: .pink,
                    title: tr("members.registry.title", fallback: "Registro Soci"),
                    route: .members)
                row(icon: "person.3.fill",
                    color: .purple,
                    title: tr("app.discover.groups", fallback: "Gruppi e interessi"),
                    route: .sigs)
                row(icon: "tag.fill",
                    color: .orange,
                    title: tr("addons.deals.title", fallback: "Deal & convenzioni"),
                    route: .deals)
            }

            // Section 2 — Addons (Test Assistant power-gated)
            Section(header: header(tr("app.discover.addons", fallback: "Addons"))) {
                row(icon: "qrcode.viewfinder",
                    color: .green,
                    title: tr("addons.stamp.title", fallback: "Tableport · Francobolli"),
                    route: .tableport)
                row(icon: "newspaper.fill",
                    color: .indigo,
                    title: tr("addons.quid.title", fallback: "Quid"),
                    route: .quid)
                row(icon: "headphones",
                    color: .cyan,
                    title: tr("addons.podcasts.title", fallback: "Podcast"),
                    route: .podcasts)
                row(icon: "bag.fill",
                    color: .red,
                    title: tr("addons.boutique.title", fallback: "Boutique"),
                    route: .boutique)
                if canSeeTestAssistant {
                    row(icon: "graduationcap.fill",
                        color: .brown,
                        title: tr("addons.testassistant.title", fallback: "Test Assistant"),
                        route: .testAssistant)
                }
                row(icon: "square.grid.2x2.fill",
                    color: AppTheme.Colors.mensaBlue,
                    title: tr("app.discover.addons_hub", fallback: "Tutti gli addon"),
                    route: .addonsHub)
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(tr("app.discover.title", fallback: "Esplora"))
        .cleanNavBar()
        .toolbar {
            // Notifications moved out of an explicit list row into a top-right
            // toolbar action — the Mail / Music / Wallet pattern. Bell glyph,
            // brand-tinted, opens the same NotificationsListView.
            // Badge shows unread count (only when > 0).
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink(value: DiscoverRoute.notifications) {
                    notificationsBellIcon
                }
                .tint(AppTheme.Colors.brandTintAdaptive)
            }
        }
        .navigationDestination(for: DiscoverRoute.self) { route in
            destination(for: route)
        }
        .task {
            // Subscribe to notifications flow to track unread count for badge.
            notificationsSub?.close()
            let notifFlow = koin.notifications.observeAll() as Kotlinx_coroutines_coreFlow
            notificationsSub = subscribeFlow(notifFlow) { (list: NSArray) in
                Task { @MainActor in
                    let notifications = (list as? [NotificationModel]) ?? []
                    self.unreadNotificationsCount = notifications.filter { $0.seen == nil }.count
                }
            }
        }
        .onDisappear {
            notificationsSub?.close()
            notificationsSub = nil
        }
    }

    @ViewBuilder
    private func destination(for route: DiscoverRoute) -> some View {
        switch route {
        case .events:           EventListView()
        case .eventsMap:        EventMapView()
        case .eventsCalendar:   EventCalendarView()
        case .deals:            DealListView()
        case .sigs:             SigListView()
        case .members:          MembersDirectoryView()
        case .addonsHub:        AddonsHubView()
        case .boutique:         BoutiqueView()
        case .documents:        AreaDocumentsView()
        case .tableport:        TableportStampView()
        case .quid:             QuidIssuesView()
        case .podcasts:         PodcastsListView()
        case .testAssistant:    TestAssistantView()
        case .notifications:    NotificationsListView()
        case .localOffices:     LocalOfficesListView()
        }
    }

    private func header(_ text: String) -> some View {
        Text(text).font(.title3.bold()).textCase(nil)
    }

    private func row(icon: String, color: Color, title: String, route: DiscoverRoute) -> some View {
        NavigationLink(value: route) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(color.gradient)
                        .frame(width: 32, height: 32)
                    Image(systemName: icon)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(.white)
                }
                Text(title)
                    .font(.body)
            }
            .padding(.vertical, 2)
        }
    }

    @ViewBuilder
    private var notificationsBellIcon: some View {
        if unreadNotificationsCount > 0 {
            Image(systemName: "bell")
                .accessibilityLabel(Text(tr("notifications.title", fallback: "Notifiche")))
                .badge(unreadNotificationsCount)
        } else {
            Image(systemName: "bell")
                .accessibilityLabel(Text(tr("notifications.title", fallback: "Notifiche")))
        }
    }
}

#Preview {
    NavigationStack { DiscoverView() }
}
