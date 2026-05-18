import SwiftUI
import Shared

/// Landing personal screen — daily overview with Liquid Glass cards.
struct TodayView: View {
    @State private var vm = TodayViewModel()
    @State private var notificationRoute: NotificationTarget?
    @State private var showAllNotifications = false
    var onSearchTap: (() -> Void)? = nil

    var body: some View {
        ZStack {
            // Subtle brand gradient background
            LinearGradient(
                colors: [
                    Color(.systemBackground),
                    Color.indigo.opacity(0.05),
                    Color(.systemBackground)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            // No spinner branch: RootViewModel pre-warms the SQLDelight cache
            // during the splash, so by the time we mount the cache flow has
            // either already emitted (data on screen instantly) or is one
            // frame away. A brief empty layout is far less jarring than the
            // spinner-flash → content swap we used to do here.
            ScrollView(.vertical, showsIndicators: false) {
                LazyVStack(spacing: 20) {
                    headerSection
                    membershipCardSection
                    nextEventSection
                    notificationsSection
                    // TODO: SIGs section — integrate koin.sigs into TodayViewModel when SIGs
                    //       are needed here; add SigPreviewCard horizontal scroll.
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 40)
            }
            .refreshable { await vm.refresh() }
        }
        .navigationTitle(tr("app.today.title", fallback: "Today")) // i18n
        .navigationBarHidden(true)
        .task { await vm.load() }
        .navigationDestination(isPresented: $showAllNotifications) {
            NotificationsListView()
        }
        .navigationDestination(item: $notificationRoute) { target in
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
                    onDismiss: { notificationRoute = nil }
                )
            case .member(let id):         MemberDetailView(memberId: id)
            }
        }
    }

    // MARK: - Header

    private var headerSection: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(vm.greeting)
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(.primary)
                Text(vm.userName)
                    .font(.title.weight(.heavy))
                    .foregroundStyle(.primary)
            }

            Spacer()

            Circle()
                .fill(Color.accentColor.opacity(0.15))
                .frame(width: 44, height: 44)
                .overlay {
                    if let url = vm.userAvatar {
                        CachedAsyncImage(url: url) { img in
                            img.resizable().scaledToFill()
                        } placeholder: {
                            Image(systemName: "person.fill")
                                .foregroundStyle(Color.accentColor)
                        }
                        .clipShape(Circle())
                    } else {
                        Image(systemName: "person.fill")
                            .foregroundStyle(Color.accentColor)
                    }
                }
        }
        .padding(.top, 8)
    }

    // MARK: - Membership Card

    @ViewBuilder
    private var membershipCardSection: some View {
        // NavigationLink navigates inside the same NavigationStack owned by MainTabView.
        // The tab switch to Card tab is handled via AppTab coordinator — for now deep-link to CardView.
        NavigationLink(destination: CardView()) {
            MembershipCardHero(
                fullName: vm.userName,
                memberSince: vm.memberSince,
                expiry: vm.membershipExpiry,
                memberId: vm.user?.id ?? "-",
                avatarURL: vm.userAvatar,
                isFullScreen: false,
                avatarSize: 54
            )
            .frame(height: 180)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Quick Search

    private var quickSearchSection: some View {
        Button(action: { onSearchTap?() }) {
            HStack(spacing: 12) {
                Image(systemName: "magnifyingglass")
                    .font(.title3)
                    .foregroundStyle(.secondary)
                Text(tr("app.search.placeholder", fallback: "Cerca persone, eventi, deal…")) // i18n
                    .font(.body)
                    .foregroundStyle(.secondary)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .glassEffect(.regular, in: .rect(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }

    // MARK: - Next Event

    @ViewBuilder
    private var nextEventSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionLabel(title: tr("app.today.next_event", fallback: "Prossimo evento"), icon: "calendar") // i18n

            if let event = vm.nextEvent {
                NavigationLink(destination: EventDetailView(eventId: event.id)) {
                    EventRowCard(event: event)
                }
                .buttonStyle(.plain)
            } else {
                emptyState(icon: "calendar.badge.exclamationmark", message: tr("app.today.no_event", fallback: "Nessun evento in arrivo")) // i18n
            }
        }
    }

    // MARK: - Notifications

    @ViewBuilder
    private var notificationsSection: some View {
        if !vm.notifications.isEmpty {
            let preview = Array(vm.notifications.prefix(3))
            VStack(alignment: .leading, spacing: 12) {
                sectionLabel(title: tr("app.today.recent_notifications", fallback: "Notifiche recenti"), icon: "bell") // i18n

                VStack(spacing: 0) {
                    ForEach(Array(preview.enumerated()), id: \.element.id) { _, notif in
                        Button {
                            handleNotificationTap(notif)
                        } label: {
                            NotificationListRow(notification: notif)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 12)
                                .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .contextMenu {
                            if notificationTarget(from: notif) != nil {
                                Button {
                                    handleNotificationTap(notif)
                                } label: {
                                    Label(tr("notifications.actions.open", fallback: "Apri"),
                                          systemImage: "arrow.up.right.square")
                                }
                            }
                            if notif.seen == nil {
                                Button {
                                    Task { try? await koin.notifications.markSeen(id: notif.id) }
                                } label: {
                                    Label(tr("notifications.actions.mark_read", fallback: "Segna come letta"),
                                          systemImage: "envelope.open")
                                }
                            }
                            Button(role: .destructive) {
                                Task { try? await koin.notifications.removeOne(id: notif.id) }
                            } label: {
                                Label(tr("notifications.actions.delete", fallback: "Elimina"),
                                      systemImage: "trash")
                            }
                        }
                        if notif.id != preview.last?.id {
                            Divider().padding(.leading, 64)
                        }
                    }

                    Divider().padding(.leading, 16)

                    Button {
                        showAllNotifications = true
                    } label: {
                        HStack(spacing: 6) {
                            Text(tr("app.today.notifications.see_all", fallback: "Vedi tutte")) // i18n
                                .font(.subheadline.weight(.medium))
                            Image(systemName: "chevron.right")
                                .font(.caption.weight(.semibold))
                            Spacer()
                        }
                        .foregroundStyle(Color.accentColor)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
                .glassEffect(.regular, in: .rect(cornerRadius: 18))
            }
        }
    }

    private func handleNotificationTap(_ n: NotificationModel) {
        Task { try? await koin.notifications.markSeen(id: n.id) }
        if let target = notificationTarget(from: n) {
            notificationRoute = target
        }
    }

    // MARK: - Helpers

    private func sectionLabel(title: String, icon: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: icon)
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text(title.uppercased())
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
                .tracking(0.5)
        }
    }

    private func emptyState(icon: String, message: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundStyle(.secondary)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .glassEffect(.regular, in: .rect(cornerRadius: 14))
    }
}

/// Placeholder for the Event detail screen.
struct EventDetailPlaceholderView: View {
    let eventId: String
    var body: some View {
        ContentUnavailableView(tr("app.event.title", fallback: "Evento"), systemImage: "calendar") // i18n
            .navigationTitle(tr("app.event.title", fallback: "Evento")) // i18n
    }
}

#Preview {
    NavigationStack {
        TodayView()
    }
}
