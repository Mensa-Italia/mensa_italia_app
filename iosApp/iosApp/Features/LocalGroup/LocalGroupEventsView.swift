import Shared
import SwiftUI

/// The office's events, upcoming and past, for its admins, laid out as the
/// events list (`EventListSectionHeader`, `EventRowCard`): a tap opens the
/// detail (which already offers edit and delete to them), "Nuovo evento"
/// the usual creation screen. Mirrors Android `LocalGroupEventsScreen`.
struct LocalGroupEventsView: View {
    let snapshot: LocalGroupSnapshot

    @State private var events: [EventModel] = []
    @State private var loaded = false
    @State private var failed = false
    @State private var showAdd = false

    private var now: Int64 { Int64(Date().timeIntervalSince1970) }

    /// Split on the end, as `EventListView` does: an event still running stays in "Prossimi".
    private var upcoming: [EventModel] {
        events.filter { $0.whenEnd.epochSeconds >= now }
            .sorted { $0.whenStart.epochSeconds < $1.whenStart.epochSeconds }
    }

    private var past: [EventModel] {
        events.filter { $0.whenEnd.epochSeconds < now }
            .sorted { $0.whenStart.epochSeconds > $1.whenStart.epochSeconds }
    }

    var body: some View {
        content
            .navigationTitle(tr("local_group.admin.events", fallback: "Eventi della sede"))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showAdd = true } label: {
                        Label(tr("local_group.events.new", fallback: "Nuovo evento"), systemImage: "plus")
                    }
                }
            }
            .sheet(isPresented: $showAdd, onDismiss: { Task { await load() } }) {
                NavigationStack { AddEventView() }
            }
            // Again on every return from a detail, where the event may have been edited or deleted.
            .task { await load() }
    }

    @ViewBuilder
    private var content: some View {
        if !loaded {
            LoadingDots()
        } else if events.isEmpty {
            ContentUnavailableView {
                Label(tr("local_group.events.empty", fallback: "Nessun evento della sede"), systemImage: "calendar")
            } description: {
                if failed {
                    Text(tr("local_group.state.failed", fallback: "Non siamo riusciti a caricare il gruppo locale."))
                }
            } actions: {
                if failed {
                    Button(tr("local_group.retry", fallback: "Riprova")) { Task { await load() } }
                        .buttonStyle(.bordered)
                }
            }
        } else {
            // Same layout as `EventListView.content`.
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if !upcoming.isEmpty {
                        EventListSectionHeader(title: tr("local_group.events.upcoming", fallback: "Prossimi"),
                                               count: upcoming.count)
                        LazyVStack(spacing: 12) {
                            ForEach(Array(upcoming.enumerated()), id: \.element.id) { idx, ev in
                                NavigationLink(value: LocalGroupRoute.event(id: ev.id)) {
                                    EventRowCard(event: ev)
                                }
                                .buttonStyle(.plain)
                                .padding(.horizontal)
                                .modifier(StaggerAppear(index: idx))
                            }
                        }
                    }
                    if !past.isEmpty {
                        EventListSectionHeader(title: tr("local_group.events.past", fallback: "Passati"),
                                               count: past.count)
                        LazyVStack(spacing: 12) {
                            ForEach(Array(past.enumerated()), id: \.element.id) { idx, ev in
                                NavigationLink(value: LocalGroupRoute.event(id: ev.id)) {
                                    EventRowCard(event: ev)
                                        .opacity(0.7)
                                }
                                .buttonStyle(.plain)
                                .padding(.horizontal)
                                .modifier(StaggerAppear(index: idx))
                            }
                        }
                    }
                    Color.clear.frame(height: 24)
                }
                .padding(.top, 4)
            }
            .refreshableOnReconnect { await load() }
        }
    }

    private func load() async {
        do {
            events = try await koin.localGroup.loadOfficeEvents(officeId: snapshot.office.id)
            failed = false
        } catch {
            if error is CancellationError { return }
            failed = true
        }
        loaded = true
    }
}
