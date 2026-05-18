import SwiftUI
import Shared

@MainActor
@Observable
final class NotificationsListViewModel {
    var notifications: [NotificationModel] = []
    var loading = true
    var refreshing = false
    var error: String? = nil

    private var sub: Closeable?

    enum Group: String, CaseIterable, Identifiable {
        case today, yesterday, week, older
        var id: String { rawValue }
        var titleKey: String {
            switch self {
            case .today: return "notifications.group.today"
            case .yesterday: return "notifications.group.yesterday"
            case .week: return "notifications.group.week"
            case .older: return "notifications.group.older"
            }
        }
        var fallback: String {
            switch self {
            case .today: return "Oggi"
            case .yesterday: return "Ieri"
            case .week: return "Settimana scorsa"
            case .older: return "Più vecchie"
            }
        }
    }

    struct Section: Identifiable {
        let group: Group
        let items: [NotificationModel]
        var id: String { group.id }
    }

    var sections: [Section] {
        let cal = Calendar.current
        let now = Date()
        var buckets: [Group: [NotificationModel]] = [:]
        for n in notifications {
            let date = Date(timeIntervalSince1970: Double(n.created.epochSeconds))
            let group: Group
            if cal.isDateInToday(date) {
                group = .today
            } else if cal.isDateInYesterday(date) {
                group = .yesterday
            } else if let days = cal.dateComponents([.day], from: date, to: now).day, days < 7 {
                group = .week
            } else {
                group = .older
            }
            buckets[group, default: []].append(n)
        }
        return Group.allCases.compactMap { g in
            guard let items = buckets[g], !items.isEmpty else { return nil }
            return Section(group: g, items: items.sorted {
                $0.created.epochSeconds > $1.created.epochSeconds
            })
        }
    }

    func load() async {
        loading = true
        defer { loading = false }
        // Subscribe cache-first
        sub?.close()
        let flow = koin.notifications.observeAll() as Kotlinx_coroutines_coreFlow
        sub = subscribeFlow(flow) { [weak self] (list: NSArray) in
            Task { @MainActor [weak self] in
                self?.notifications = (list as? [NotificationModel]) ?? []
            }
        }
        await refresh()
    }

    func refresh() async {
        refreshing = true
        defer { refreshing = false }
        do {
            try await koin.notifications.refresh()
        } catch {
            self.error = error.localizedDescription
        }
    }

    func markSeen(_ n: NotificationModel) async {
        try? await koin.notifications.markSeen(id: n.id)
    }

    var unreadCount: Int { notifications.filter { $0.seen == nil }.count }

    func delete(_ n: NotificationModel) async {
        try? await koin.notifications.removeOne(id: n.id)
    }

    func markAllSeen() async {
        try? await koin.notifications.markAllSeen()
    }
}
