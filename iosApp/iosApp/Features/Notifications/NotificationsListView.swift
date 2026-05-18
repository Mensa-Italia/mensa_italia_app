import SwiftUI
import Shared

// MARK: - Deep-link target

/// Typed destination for tapping a notification. Mirrors Flutter's
/// `handleNotificationActions` in `master_model.dart`.
///
/// The actual parsing lives in shared KMP code
/// (`it.mensa.shared.notifications.NotificationRouter`). This Swift enum is a
/// presentation-layer mirror used by `switch` statements throughout the
/// iOS app; the bridging initializer below converts the KMP sealed-class
/// result into this enum.
enum NotificationTarget: Hashable {
    case event(String)
    case deal(String)
    case singleDocument(String)
    case multipleDocuments
    case ticketPurchase
    case paymentUpdateStatus
    case quid(String)
    case quidArticle(String)
    case quidPdf(String)
    case localOffice(String)
    case accountConfirmation(exAppId: String, callbackUrl: String, notificationId: String?)
    /// Spotlight-only target: tap on a CoreSpotlight result for a member.
    /// Not produced by `NotificationRouter` (no `type=member` push exists);
    /// only by `SpotlightIndexer` via NSUserActivity continuation.
    case member(String)

    /// Map a KMP `NotificationTarget` sealed-class instance to this Swift enum.
    init?(shared: Shared.NotificationTarget) {
        switch shared {
        case let t as NotificationTargetEvent:           self = .event(t.id)
        case let t as NotificationTargetDeal:            self = .deal(t.id)
        case let t as NotificationTargetSingleDocument:  self = .singleDocument(t.id)
        case is NotificationTargetMultipleDocuments:     self = .multipleDocuments
        case is NotificationTargetTicketPurchase:        self = .ticketPurchase
        case is NotificationTargetPaymentUpdateStatus:   self = .paymentUpdateStatus
        case let t as NotificationTargetQuid:            self = .quid(t.categoryId)
        case let t as NotificationTargetQuidArticle:     self = .quidArticle(t.postId)
        case let t as NotificationTargetQuidPdf:         self = .quidPdf(t.recordId)
        case let t as NotificationTargetLocalOffice:     self = .localOffice(t.slug)
        case let t as NotificationTargetAccountConfirmation:
            self = .accountConfirmation(
                exAppId: t.exAppId,
                callbackUrl: t.callbackUrl,
                notificationId: t.notificationId
            )
        default: return nil
        }
    }
}

/// Bridge a stored `NotificationModel.data` (Kotlin `JsonObject`, exposed to
/// Swift as `[String: AnyObject]` of opaque JsonElement) to a flat
/// `[String: String]` the KMP router can consume.
///
/// Each value's `description` returns canonical JSON, so strings come quoted —
/// we strip the surrounding quotes (same pattern as
/// `MemberDetailView.extractFullData`).
private func notificationDataDict(_ notification: NotificationModel) -> [String: String]? {
    guard let raw = notification.data as? [String: AnyObject] else { return nil }
    var out: [String: String] = [:]
    for (k, v) in raw {
        var s = String(describing: v)
        if s.hasPrefix("\"") && s.hasSuffix("\"") && s.count >= 2 {
            s = String(s.dropFirst().dropLast())
        }
        out[k] = s
    }
    return out
}

/// Resolve a `NotificationTarget` for a stored notification by delegating to
/// the shared KMP router.
func notificationTarget(from notification: NotificationModel) -> NotificationTarget? {
    guard let dict = notificationDataDict(notification) else { return nil }
    guard let shared = NotificationRouter.shared.targetFromData(data: dict) else {
        return nil
    }
    return NotificationTarget(shared: shared)
}

/// Resolve a system icon (SF Symbol name) for the leading badge by delegating
/// to the shared KMP router.
func notificationSystemIcon(for notification: NotificationModel) -> String {
    let type = notificationDataDict(notification)?["type"] ?? ""
    return NotificationRouter.shared.systemIconName(type: type)
}

// MARK: - Title / body rendering helpers

/// Builds the localized title using Tolgee key `<tr>.title` with named params.
/// Mirrors Flutter: `notification.title.tr(namedArgs: notification.trNamedParams)`
/// where `title = "$tr.title"`.
func notificationTitle(_ n: NotificationModel) -> String {
    guard !n.tr.isEmpty else { return tr("notifications.fallback.title", fallback: "Notifica") }
    let key = "\(n.tr).title"
    return tr(key, fallback: key, n.trNamedParams)
}

/// Builds the localized body using Tolgee key `<tr>.body` with named params.
func notificationBody(_ n: NotificationModel) -> String {
    guard !n.tr.isEmpty else { return "" }
    let key = "\(n.tr).body"
    return tr(key, fallback: key, n.trNamedParams)
}

// MARK: - Routing

/// Pushed routes within the Notifications feature.
enum NotificationsRoute: Hashable {
    case manager
    case target(NotificationTarget)
}

// MARK: - List

struct NotificationsListView: View {
    @State private var vm = NotificationsListViewModel()
    @State private var route: NotificationsRoute?
    @State private var unsupportedAlert = false

    var body: some View {
        Group {
            if vm.loading && vm.notifications.isEmpty {
                ProgressView().scaleEffect(1.3)
            } else if vm.notifications.isEmpty {
                ContentUnavailableView(
                    tr("notifications.empty.title", fallback: "Nessuna notifica"),
                    systemImage: "bell.slash",
                    description: Text(tr("notifications.empty.body", fallback: "Le tue notifiche appariranno qui"))
                )
            } else {
                List {
                    ForEach(vm.sections) { section in
                        Section {
                            ForEach(section.items, id: \.id) { n in
                                Button {
                                    handleTap(n)
                                } label: {
                                    NotificationListRow(notification: n)
                                        .contentShape(Rectangle())
                                }
                                .buttonStyle(.plain)
                                .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                    Button(role: .destructive) {
                                        Task { await vm.delete(n) }
                                    } label: {
                                        Label(
                                            tr("notifications.actions.delete", fallback: "Elimina"),
                                            systemImage: "trash"
                                        )
                                    }
                                    if n.seen == nil {
                                        Button {
                                            Task { await vm.markSeen(n) }
                                        } label: {
                                            Label(
                                                tr("notifications.actions.mark_read", fallback: "Segna come letta"),
                                                systemImage: "envelope.open"
                                            )
                                        }
                                        .tint(AppTheme.Colors.brandPrimary)
                                    }
                                }
                                .swipeActions(edge: .leading, allowsFullSwipe: true) {
                                    if n.seen == nil {
                                        Button {
                                            Task { await vm.markSeen(n) }
                                        } label: {
                                            Label(
                                                tr("notifications.actions.mark_read", fallback: "Segna come letta"),
                                                systemImage: "envelope.open"
                                            )
                                        }
                                        .tint(AppTheme.Colors.brandPrimary)
                                    }
                                }
                                .contextMenu {
                                    Button {
                                        handleTap(n)
                                    } label: {
                                        Label(
                                            tr("notifications.actions.open", fallback: "Apri"),
                                            systemImage: "arrow.up.right.square"
                                        )
                                    }
                                    if n.seen == nil {
                                        Button {
                                            Task { await vm.markSeen(n) }
                                        } label: {
                                            Label(
                                                tr("notifications.actions.mark_read", fallback: "Segna come letta"),
                                                systemImage: "envelope.open"
                                            )
                                        }
                                    }
                                    Button(role: .destructive) {
                                        Task { await vm.delete(n) }
                                    } label: {
                                        Label(
                                            tr("notifications.actions.delete", fallback: "Elimina"),
                                            systemImage: "trash"
                                        )
                                    }
                                }
                            }
                        } header: {
                            Text(tr(section.group.titleKey, fallback: section.group.fallback))
                        }
                    }
                }
                .listStyle(.insetGrouped)
                .refreshable { await vm.refresh() }
            }
        }
        .navigationTitle(tr("notifications.title", fallback: "Notifiche"))
        .navigationBarTitleDisplayMode(.large)
        .cleanNavBar()
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button {
                        Task { await vm.markAllSeen() }
                    } label: {
                        Label(
                            tr("notifications.actions.mark_all_read", fallback: "Leggi tutto"),
                            systemImage: "checkmark.circle"
                        )
                    }
                    .disabled(vm.unreadCount == 0)

                    Button {
                        route = .manager
                    } label: {
                        Label(
                            tr("notifications.actions.preferences", fallback: "Preferenze"),
                            systemImage: "slider.horizontal.3"
                        )
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .accessibilityLabel(tr("notifications.actions.more", fallback: "Altre azioni"))
                }
            }
        }
        .navigationDestination(item: $route) { r in
            switch r {
            case .manager:
                NotificationManagerView()
            case .target(let t):
                destinationView(for: t)
            }
        }
        .alert(
            tr("notifications.unsupported.title", fallback: "Notifica non navigabile"),
            isPresented: $unsupportedAlert
        ) {
            Button("OK", role: .cancel) { }
        } message: {
            Text(tr("notifications.unsupported.body",
                    fallback: "Questa notifica non ha un contenuto collegato."))
        }
        .task { await vm.load() }
    }

    @ViewBuilder
    private func destinationView(for target: NotificationTarget) -> some View {
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
        case .accountConfirmation(let appId, let url, let nid):
            AccountConfirmationSheet(
                exAppId: appId,
                callbackUrl: url,
                notificationId: nid,
                onDismiss: { route = nil }
            )
        case .member(let id):         MemberDetailView(memberId: id)
        }
    }

    private func handleTap(_ n: NotificationModel) {
        // 1) Mark as seen on the backend (mirrors Flutter `Api().seeNotification`).
        Task { await vm.markSeen(n) }

        // 2) Mirror Flutter's `handleNotificationActions`: navigate directly to
        //    the referenced object. For unsupported / missing-data notifications,
        //    surface an inline alert instead of opening a detail screen.
        if let target = notificationTarget(from: n) {
            route = .target(target)
        } else {
            unsupportedAlert = true
        }
    }
}

// MARK: - Row

struct NotificationListRow: View {
    let notification: NotificationModel

    private var isUnread: Bool { notification.seen == nil }

    private var relative: String {
        let d = Date(timeIntervalSince1970: Double(notification.created.epochSeconds))
        let f = RelativeDateTimeFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.unitsStyle = .short
        return f.localizedString(for: d, relativeTo: Date())
    }

    private var iconName: String { notificationSystemIcon(for: notification) }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            ZStack {
                Circle()
                    .fill(isUnread ? AppTheme.Colors.brandPrimary.opacity(0.15) : Color(.tertiarySystemFill))
                    .frame(width: 40, height: 40)
                Image(systemName: iconName)
                    .font(.system(size: 16))
                    .foregroundStyle(isUnread ? AppTheme.Colors.brandPrimary : .secondary)
            }
            .padding(.top, 2)
            VStack(alignment: .leading, spacing: 3) {
                Text(notificationTitle(notification))
                    .font(.subheadline)
                    .fontWeight(isUnread ? .semibold : .regular)
                    .foregroundStyle(.primary)
                    .lineLimit(2)
                let body = notificationBody(notification)
                if !body.isEmpty {
                    Text(body)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(3)
                }
                Text(relative)
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
            Spacer()
            if isUnread {
                Circle()
                    .fill(AppTheme.Colors.brandPrimary)
                    .frame(width: 8, height: 8)
                    .padding(.top, 6)
            }
        }
    }
}

#Preview {
    NavigationStack { NotificationsListView() }
}
