import SwiftUI
import Shared

/// Every page the "Gruppo locale" tab can push. The tab pushes ONLY by value:
/// a page pushed by view that then pushes by value (EventListView and
/// DealListView do) scrambles the NavigationStack.
enum LocalGroupRoute: Hashable {
    case leaderboard
    case medals
    case newMembers(officeId: String)
    case event(id: String)
    case allEvents
    case deal(id: String)
    case allDeals(regionKey: String)
    case admin
    case adminMembers
    case adminStats
    case adminEvents
    case association
}

extension View {
    /// Registers the destinations of `LocalGroupRoute` and `RegSociRoute`.
    /// Put it on the root of any NavigationStack that shows a LocalGroup page.
    func localGroupDestinations() -> some View {
        navigationDestination(for: LocalGroupRoute.self) { route in
            switch route {
            case .leaderboard:
                LeaderboardView()
            case .medals:
                MedalCabinetView(showRegion: true)
            case .newMembers(let officeId):
                LocalGroupNewMembersView(officeId: officeId)
            case .event(let id):
                EventDetailView(eventId: id)
            case .allEvents:
                EventListView()
            case .deal(let id):
                DealDetailView(dealId: id)
            case .allDeals(let regionKey):
                DealListView(initialRegion: regionKey.isEmpty ? nil : regionKey)
            case .admin:
                LocalGroupSnapshotPage { LocalGroupAdminView(snapshot: $0) }
            case .adminMembers:
                LocalGroupSnapshotPage { LocalGroupMembersView(snapshot: $0) }
            case .adminStats:
                LocalGroupSnapshotPage { LocalGroupStatsView(snapshot: $0) }
            case .adminEvents:
                LocalGroupSnapshotPage { LocalGroupEventsView(snapshot: $0) }
            case .association:
                LocalGroupAssociationView()
            }
        }
        .navigationDestination(for: RegSociRoute.self) { route in
            MemberDetailView(memberId: route.id)
        }
        .navigationDestination(for: LocalOfficeLinktreeRoute.self) { route in
            LocalOfficeLinktreeView(officeId: route.officeId)
        }
    }
}

/// An admin page built on the tab's current snapshot; the failed state's
/// text when there is none.
private struct LocalGroupSnapshotPage<Content: View>: View {
    @ViewBuilder let content: (LocalGroupSnapshot) -> Content
    @State private var store = LocalGroupStore.shared

    var body: some View {
        if let snapshot = store.snapshot {
            content(snapshot)
        } else {
            ContentUnavailableView(
                tr("local_group.title", fallback: "Gruppo locale"),
                systemImage: "wifi.exclamationmark",
                description: Text(tr("local_group.state.failed", fallback: "Non siamo riusciti a caricare il gruppo locale."))
            )
        }
    }
}
