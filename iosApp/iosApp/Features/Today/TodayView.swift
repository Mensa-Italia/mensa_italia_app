import SwiftUI
import Shared

/// Landing personal screen — daily overview with Liquid Glass cards.
struct TodayView: View {
    @State private var vm = TodayViewModel()
    @State private var notificationRoute: NotificationTarget?
    @State private var showAllNotifications = false
    /// Tableport's passport: full screen, the phone being the passport.
    @State private var showPassport = false
    @State private var medalStore = MedalStore.shared
    @State private var leaderboardStore = LeaderboardStore.shared
    var onSearchTap: (() -> Void)?

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
            GeometryReader { proxy in
                if let fold = foldLayout(in: proxy) {
                    // Unfolded foldable, or a screen this wide: one column per
                    // half, so no card gets stretched across (or creased by)
                    // the hinge, nor blown up to the whole width.
                    HStack(spacing: 0) {
                        // Left what's going on — next event and notifications —
                        // right the member: card, membership, tickets. The
                        // right half is where the folded phone's screen sits,
                        // so unfolding keeps the card where it already was —
                        // at the same height too: the right column starts as
                        // the single one does, only the left one keeps clear
                        // of the display's rounded corner.
                        column(top: fold.topInset + 8, horizontal: fold.sideMargin) {
                            nextEventSection
                            notificationsSection
                        }
                        .frame(width: max(fold.hinge.minX, 0))

                        Color.clear.frame(width: fold.hinge.width)

                        column(horizontal: fold.sideMargin) {
                            headerSection
                            OfflineBanner()
                            membershipCardSection
                            membershipSection
                            birthdaysSection
                        }
                    }
                } else {
                    column {
                        headerSection
                        OfflineBanner()
                        membershipCardSection
                        membershipSection
                        // Birthdays sit with the member's things, right under
                        // the membership details — here and unfolded alike.
                        birthdaysSection
                        nextEventSection
                        notificationsSection
                        // TODO: SIGs section — integrate koin.sigs into TodayViewModel when SIGs
                        //       are needed here; add SigPreviewCard horizontal scroll.
                    }
                }
            }
        }
        .navigationTitle(tr("app.today.title", fallback: "Today")) // i18n
        .navigationBarHidden(true)
        .task { await vm.load() }
        .task { await medalStore.refresh() }
        .task { await leaderboardStore.refresh() }
        .animation(.smooth, value: ConnectivityStore.shared.isOffline)
        .task {
            // I timbri del passaporto su disco prima che lo si apra: chi lo
            // sfoglia li trova gia' li', come con una cache piena da sempre.
            // Dal database; dalla rete solo se il database e' ancora vuoto.
            var stamps = (try? await koin.stamps.current()) ?? []
            if stamps.isEmpty {
                try? await koin.stamps.refresh(filter: nil, sort: "-created")
                stamps = (try? await koin.stamps.current()) ?? []
            }
            StampImagePrefetcher.warmCollection(stamps)
        }
        .fullScreenCover(isPresented: $showPassport) {
            PassportScreen()
        }
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
            case .accountConfirmation, .dataExportConfirmation:
                ExAppConfirmationSheet(target: target, onDismiss: { notificationRoute = nil })
            case .member(let id):         MemberDetailView(memberId: id)
            case .developerMessage:       EmptyView() // aperto da `openInPlace()`
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

    /// The card lives here now (there's no Card tab any more): tapping it
    /// flips it, as the Card page did.
    private var membershipCardSection: some View {
        MembershipCardHero(
            fullName: vm.userName,
            memberSince: vm.memberSince,
            expiry: vm.membershipExpiry,
            memberId: vm.user?.id ?? "-",
            avatarURL: vm.userAvatar,
            isFullScreen: false,
            avatarSize: 54,
            podiumPlace: koin.featureFlags.enabledLeaderboards().contains(.members)
                ? leaderboardStore.board?.myPodiumPlace.map { Int($0.int32Value) } : nil
        )
        .frame(maxWidth: 420)
        .shadow(color: .black.opacity(0.18), radius: 18, x: 0, y: 8)
    }

    // MARK: - Membership

    /// Same rule as the renewal wall (`Membership` in :shared), so the badge
    /// here and the screen that blocks the app can't disagree.
    private var membershipExpired: Bool {
        Membership.shared.isExpired(user: vm.user)
    }

    /// Expiry with its status, and the tickets, from the former Card page.
    private var membershipSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionLabel(title: tr("card.membership", fallback: "Iscrizione"), icon: "person.text.rectangle")

            VStack(spacing: 0) {
                HStack(alignment: .center, spacing: 12) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(tr("card.expires_on", fallback: "Scadenza"))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Text(vm.membershipExpiry)
                            .font(.headline)
                            .foregroundStyle(.primary)
                    }
                    Spacer(minLength: 8)
                    membershipStatus
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)

                Divider().padding(.leading, 16)

                // Tableport lives with the member's things, not in Explore:
                // full screen from there the passport fought the split view.
                Button {
                    showPassport = true
                } label: {
                    membershipRow(
                        icon: "qrcode.viewfinder",
                        color: .green,
                        title: tr("addons.stamp.short_title", fallback: "Tableport"),
                        // The medals earned, beside the chevron: a reason to
                        // look inside and find out what they are.
                        medalCount: medalStore.medals.isEmpty ? nil : medalStore.earnedCount
                    )
                }
                .buttonStyle(.plain)

                // La classifica sta con il Tableport: si fa con i suoi timbri.
                // Solo con almeno una classifica accesa in `configs`; quella
                // dei soci solo quando e' stata letta, senza non ha niente da dire.
                let boards = koin.featureFlags.enabledLeaderboards()
                let membersOn = boards.contains(.members)
                let membersBoard = leaderboardStore.board.flatMap { $0.rows.isEmpty ? nil : $0 }
                // Arriva dalla rete dopo il resto della card: entra sfumando
                // (the store sets `board` inside withAnimation).
                if !boards.isEmpty, !membersOn || membersBoard != nil {
                    Group {
                        Divider().padding(.leading, 60)

                        NavigationLink {
                            LeaderboardView()
                        } label: {
                            LeaderboardMembershipRow(board: membersOn ? membersBoard : nil)
                        }
                        .buttonStyle(.plain)
                    }
                    .transition(.opacity.combined(with: .move(edge: .top)))
                }

                // Solo con un ticket ancora valido: una voce che apre una
                // lista vuota o di ticket scaduti era rumore.
                if vm.hasUpcomingTickets {
                    Divider().padding(.leading, 60)

                    NavigationLink {
                        TicketsListView()
                    } label: {
                        membershipRow(
                            icon: "ticket.fill",
                            color: AppTheme.Colors.mensaBlue,
                            title: tr("card.my_tickets", fallback: "I miei ticket")
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
            .compatGlass(cornerRadius: 18)
        }
    }

    private func membershipRow(icon: String, color: Color, title: String, medalCount: Int? = nil) -> some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 7, style: .continuous)
                    .fill(color)
                    .frame(width: 30, height: 30)
                Image(systemName: icon)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
            }
            Text(title)
                .font(.body)
                .foregroundStyle(.primary)
            Spacer()
            if let medalCount {
                HStack(spacing: 4) {
                    Image(systemName: "medal.fill")
                        .font(.footnote.weight(.semibold))
                    Text("\(medalCount)")
                        .font(.subheadline.weight(.semibold).monospacedDigit())
                }
                .foregroundStyle(.secondary)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Text(
                    tr("medals.count_a11y", fallback: "%d medaglie").replacingOccurrences(of: "%d", with: "\(medalCount)")
                ))
            }
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }

    @ViewBuilder
    private var membershipStatus: some View {
        if membershipExpired, let url = URL(string: "https://cloud32.mensa.it/rinnovo") {
            Link(destination: url) {
                Text(tr("card.renew", fallback: "Rinnova"))
                    .font(.subheadline.weight(.semibold))
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.regular)
            .tint(AppTheme.Colors.mensaBlue)
        } else {
            HStack(spacing: 6) {
                Image(systemName: "checkmark.seal.fill")
                    .font(.caption.weight(.semibold))
                Text(tr("card.active", fallback: "Attiva"))
                    .font(.caption.weight(.semibold))
            }
            .foregroundStyle(.green)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Capsule().fill(Color.green.opacity(0.15)))
        }
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
            .compatGlass(cornerRadius: 14)
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

    // MARK: - Birthdays

    @ViewBuilder
    private var birthdaysSection: some View {
        if !vm.birthdays.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                sectionLabel(title: tr("today.birthdays.title", fallback: "Compleanni di oggi"), icon: "birthday.cake")
                BirthdaysCard(birthdays: vm.birthdays)
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
                .compatGlass(cornerRadius: 18)
            }
        }
    }

    private func handleNotificationTap(_ n: NotificationModel) {
        Task { try? await koin.notifications.markSeen(id: n.id) }
        if let target = notificationTarget(from: n), !target.openInPlace() {
            notificationRoute = target
        }
    }

    // MARK: - Layout

    private func column<Content: View>(
        top: CGFloat = 8,
        horizontal: CGFloat = 16,
        @ViewBuilder _ content: () -> Content
    ) -> some View {
        ScrollView(.vertical, showsIndicators: false) {
            LazyVStack(spacing: 20) {
                content()
            }
            .padding(.horizontal, horizontal)
            .padding(.top, top)
            .padding(.bottom, 40)
        }
        // Side by side, a column clips to its own width by default, which
        // cut the cards' shadows at the hinge; let them spill over.
        .scrollClipDisabled()
        .refreshableOnReconnect { await vm.refresh() }
    }

    private struct FoldLayout {
        /// The hinge in the proxy's space, margins included.
        var hinge: CGRect
        var topInset: CGFloat
        var sideMargin: CGFloat
    }

    /// Two-column geometry: split at the hinge of an unfolded foldable on its
    /// side (iPhone Duo), down the middle of any screen with room for two
    /// phone-wide columns (the same phone unfolded and upright, where one
    /// column blew the event poster up to the whole screen). Nil on phones
    /// and while folded.
    ///
    /// Insets are read from the system rather than applied with
    /// `containerCornerOffset` / `contentMargins(for: .container)`: on iOS 27.1
    /// those only see the real screen edges, so they leave no margin along the
    /// hinge and treat the camera column as a corner.
    private func foldLayout(in proxy: GeometryProxy) -> FoldLayout? {
        if #available(iOS 27.1, *), let hinge = proxy.verticalHinge {
            // Unfolded there is no status bar on top (top safe area is 0), so
            // start below the display's rounded top-leading corner instead.
            return FoldLayout(
                hinge: hinge,
                topInset: max(8, proxy.containerCornerInsets.topLeading.height),
                sideMargin: max(16, proxy.contentMargins(for: .container).leading)
            )
        }
        guard proxy.size.width >= 600 else { return nil }
        let gutter: CGFloat = 16
        return FoldLayout(
            hinge: CGRect(x: (proxy.size.width - gutter) / 2, y: 0, width: gutter, height: proxy.size.height),
            topInset: 8,
            sideMargin: 16
        )
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
        .compatGlass(cornerRadius: 14)
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
