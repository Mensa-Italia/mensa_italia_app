import Shared
import SwiftUI

/// Root of the "Gruppo locale" tab: the member's local office, built from
/// the blocks of the office page (`LocalOfficeBlocks.swift`) and of Today
/// (`TodayBlocks.swift`, `EventRowCard`, `DealCardView`), so it reads like
/// the rest of the app. For the office's admins, the way into the admin
/// area. Mirrors Android `LocalGroupScreen`. Lives in the tab's NavigationStack.
struct LocalGroupView: View {
    @State private var store = LocalGroupStore.shared
    @State private var appeared = false

    var body: some View {
        content
            .navigationTitle(tr("local_group.title", fallback: "Gruppo locale"))
            .navigationBarTitleDisplayMode(.inline)
            .cleanNavBar()
            // Only when there is nothing to show: `.task` reruns on every
            // return from a pushed screen. Pull to refresh does the rest.
            .task { if store.needsInitialLoad { await store.refresh() } }
            .navigationDestination(for: RegSociRoute.self) { route in
                MemberDetailView(memberId: route.id)
            }
            .navigationDestination(for: LocalOfficeLinktreeRoute.self) { route in
                LocalOfficeLinktreeView(officeId: route.officeId)
            }
    }

    @ViewBuilder
    private var content: some View {
        let state = store.state
        if let snapshot = store.snapshot {
            page(snapshot, failed: state is LocalGroupStateFailed)
        } else if state is LocalGroupStateNoRegion {
            unavailable(tr("local_group.state.no_region", fallback: "Il registro soci non indica la tua regione."),
                        systemImage: "mappin.slash", retry: false)
        } else if state is LocalGroupStateNoOffice {
            unavailable(tr("local_group.state.no_office", fallback: "Nessun gruppo locale per la tua regione."),
                        systemImage: "building.2", retry: false)
        } else if state is LocalGroupStateFailed {
            unavailable(tr("local_group.state.failed", fallback: "Non siamo riusciti a caricare il gruppo locale."),
                        systemImage: "wifi.exclamationmark", retry: true)
        } else {
            LoadingDots()
        }
    }

    private func unavailable(_ message: String, systemImage: String, retry: Bool) -> some View {
        ContentUnavailableView {
            Label(tr("local_group.title", fallback: "Gruppo locale"), systemImage: systemImage)
        } description: {
            Text(message)
        } actions: {
            if retry {
                Button(tr("local_group.retry", fallback: "Riprova")) {
                    Task { await store.refresh() }
                }
                .buttonStyle(.bordered)
            }
        }
    }

    // MARK: - The office page

    /// Same composition as `LocalOfficeView.officeContent`: hero, intro, then the sections.
    private func page(_ snapshot: LocalGroupSnapshot, failed: Bool) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                LocalOfficeHero(office: snapshot.office)
                LocalOfficeIntro(office: snapshot.office)

                if failed {
                    block(0) {
                        Button {
                            Task { await store.refresh() }
                        } label: {
                            TodayEmptyState(
                                icon: "exclamationmark.triangle",
                                message: tr("local_group.state.failed", fallback: "Non siamo riusciti a caricare il gruppo locale.")
                                    + " " + tr("local_group.retry", fallback: "Riprova")
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }

                if !snapshot.admins.isEmpty {
                    block(1) {
                        LocalOfficeAdminsSection(
                            admins: snapshot.admins,
                            title: tr("local_group.section.secretariat", fallback: "Segreteria"),
                            showsMail: true
                        )
                    }
                }

                if !snapshot.links.isEmpty {
                    block(2) {
                        LocalOfficeLinktreeSection(
                            officeId: snapshot.office.id,
                            rows: snapshot.links,
                            title: tr("local_group.section.contacts", fallback: "Contatti")
                        )
                    }
                }

                block(3) { rankingSection(snapshot.ranking) }

                if let stats = snapshot.stats {
                    block(4) { membersSection(stats) }
                }

                block(5) { nextEventSection(snapshot.nextEvent) }

                block(6) { dealsSection(snapshot.deals) }

                if snapshot.isAdmin {
                    block(7) { adminSection(snapshot) }
                }

                Color.clear.frame(height: 40)
            }
        }
        .ignoresSafeArea(.container, edges: .top)
        .refreshableOnReconnect { await store.refresh() }
        .onAppear {
            withAnimation(.easeOut(duration: 0.35)) { appeared = true }
        }
    }

    private func block<Content: View>(_ index: Int, @ViewBuilder content: @escaping () -> Content) -> some View {
        LocalOfficeSectionBlock(index: index, appeared: appeared, content: content)
    }

    /// A section of cards: the office page's title, then the content.
    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            LocalOfficeSectionHeader(title: title)
            content()
        }
    }

    // MARK: Ranking and medals (rows of Today's membership card)

    private func rankingSection(_ ranking: LeaderboardRegionModel?) -> some View {
        section(tr("local_group.section.ranking", fallback: "Classifica e medaglie")) {
            VStack(spacing: 0) {
                if let ranking {
                    NavigationLink {
                        LeaderboardView()
                    } label: {
                        TodayMembershipRow(
                            icon: "trophy.fill",
                            color: .orange,
                            title: tr("local_group.ranking.position", fallback: "Posizione in classifica"),
                            detail: tr("local_group.ranking.points", fallback: "{0} punti",
                                       ["0": "\(Int(ranking.score.rounded()))"]),
                            value: "\(Int(ranking.position))°"
                        )
                    }
                    .buttonStyle(.plain)

                    Divider().padding(.leading, 60)
                }

                NavigationLink {
                    MedalCabinetView(showRegion: true)
                } label: {
                    TodayMembershipRow(
                        icon: "medal.fill",
                        color: .yellow,
                        title: tr("local_group.medals", fallback: "Medaglie della regione")
                    )
                }
                .buttonStyle(.plain)
            }
            .compatGlass(cornerRadius: 18)
        }
    }

    // MARK: Members

    private func membersSection(_ stats: LocalOfficeStatsModel) -> some View {
        section(tr("local_group.section.members", fallback: "Soci")) {
            VStack(spacing: 0) {
                TodayMembershipRow(
                    icon: "person.3.fill",
                    color: .indigo,
                    title: tr("local_group.members.active", fallback: "Iscritti in regione"),
                    value: "\(stats.membersActive)",
                    showsChevron: false
                )
                Divider().padding(.leading, 60)
                TodayMembershipRow(
                    icon: "person.badge.plus",
                    color: .green,
                    title: tr("local_group.members.new_week", fallback: "Nuovi nell'ultima settimana"),
                    value: "\(stats.membersNew7d)",
                    showsChevron: false
                )
            }
            .compatGlass(cornerRadius: 18)
        }
    }

    // MARK: Next event (as Today's)

    private func nextEventSection(_ event: EventModel?) -> some View {
        section(tr("local_group.events.next", fallback: "Prossimo evento")) {
            if let event {
                NavigationLink(destination: EventDetailView(eventId: event.id)) {
                    EventRowCard(event: event)
                }
                .buttonStyle(.plain)
            } else {
                TodayEmptyState(icon: "calendar.badge.exclamationmark",
                                message: tr("local_group.events.none", fallback: "Nessun evento in programma"))
            }
            seeAll(tr("local_group.events.all", fallback: "Tutti gli eventi")) { EventListView() }
        }
    }

    // MARK: Deals (as the deals list)

    private func dealsSection(_ deals: [DealModel]) -> some View {
        section(tr("local_group.section.deals", fallback: "Convenzioni")) {
            if deals.isEmpty {
                TodayEmptyState(icon: "tag.slash",
                                message: tr("local_group.deals.none", fallback: "Nessuna convenzione attiva in regione"))
            } else {
                LazyVStack(spacing: 10) {
                    ForEach(Array(deals.prefix(3)), id: \.id) { deal in
                        NavigationLink(destination: DealDetailView(dealId: deal.id)) {
                            DealCardView(deal: deal)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            seeAll(tr("local_group.deals.all", fallback: "Tutte le convenzioni")) { DealListView() }
        }
    }

    // MARK: Admin area

    /// One card, no title above it: the row already says "Area segreteria".
    private func adminSection(_ snapshot: LocalGroupSnapshot) -> some View {
        NavigationLink {
            LocalGroupAdminView(snapshot: snapshot)
        } label: {
            TodayMembershipRow(
                icon: "person.badge.key.fill",
                color: .purple,
                title: tr("local_group.admin.title", fallback: "Area segreteria"),
                detail: tr("local_group.admin.subtitle", fallback: "Soci, statistiche ed eventi della sede")
            )
        }
        .buttonStyle(.plain)
        .compatGlass(cornerRadius: 18)
    }

    /// Today's "Vedi tutte" line, in its own glass card under the section's content.
    private func seeAll<Destination: View>(_ title: String, @ViewBuilder destination: @escaping () -> Destination) -> some View {
        NavigationLink {
            destination()
        } label: {
            TodaySeeAllRow(title: title)
        }
        .buttonStyle(.plain)
        .compatGlass(cornerRadius: 18)
    }
}
