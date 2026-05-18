import SwiftUI
import Shared

@MainActor @Observable
final class EventCalendarViewModel {
    var events: [EventModel] = []
    var selectedDate: Date = Calendar.current.startOfDay(for: Date())
    private var sub: Closeable?

    func start() {
        guard sub == nil else { return }
        let flow = koin.events.observeAll() as Kotlinx_coroutines_coreFlow
        sub = subscribeFlow(flow) { [weak self] (list: NSArray) in
            Task { @MainActor [weak self] in
                self?.events = (list as? [EventModel]) ?? []
            }
        }
        Task { try? await koin.events.refresh(filter: nil, sort: "when_end") }
    }

    func stop() { sub?.close(); sub = nil }

    /// Set of calendar-day start timestamps (in seconds) that have at least one event.
    var daysWithEvents: Set<Date> {
        var set = Set<Date>()
        let cal = Calendar.current
        for e in events {
            let start = cal.startOfDay(for: EventDateUtil.date(e.whenStart))
            let end = cal.startOfDay(for: EventDateUtil.date(e.whenEnd))
            var d = start
            while d <= end {
                set.insert(d)
                guard let next = cal.date(byAdding: .day, value: 1, to: d) else { break }
                d = next
            }
        }
        return set
    }

    func events(on day: Date) -> [EventModel] {
        let cal = Calendar.current
        let dayStart = cal.startOfDay(for: day)
        guard let dayEnd = cal.date(byAdding: .day, value: 1, to: dayStart) else { return [] }
        return events.filter { e in
            let s = EventDateUtil.date(e.whenStart)
            let en = EventDateUtil.date(e.whenEnd)
            return s < dayEnd && en >= dayStart
        }.sorted { $0.whenStart.epochSeconds < $1.whenStart.epochSeconds }
    }
}

struct EventCalendarView: View {
    @State private var vm = EventCalendarViewModel()
    @State private var displayedMonth: Date = Calendar.current.startOfDay(for: Date())
    @State private var monthTransitionEdge: Edge = .trailing
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                CalendarMonthView(
                    displayedMonth: $displayedMonth,
                    transitionEdge: $monthTransitionEdge,
                    selectedDate: $vm.selectedDate,
                    daysWithEvents: vm.daysWithEvents,
                    eventCount: { vm.events(on: $0).count }
                )
                .padding(.horizontal)

                let day = vm.selectedDate
                let dayEvents = vm.events(on: day)

                HStack {
                    Text(EventDateUtil.fullFormatter.string(from: day))
                        .font(.headline)
                    Spacer()
                    Text("\(dayEvents.count) \(tr("events.calendar.events_short", fallback: "eventi"))")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal)

                if dayEvents.isEmpty {
                    ContentUnavailableView(
                        tr("events.calendar.empty_day.title", fallback: "Nessun evento"),
                        systemImage: "calendar",
                        description: Text(tr("events.calendar.empty_day.description", fallback: "Non ci sono eventi in questa giornata."))
                    )
                    .padding(.top, 30)
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(Array(dayEvents.enumerated()), id: \.element.id) { idx, e in
                            NavigationLink {
                                EventDetailView(eventId: e.id)
                            } label: {
                                EventRowCard(event: e)
                            }
                            .buttonStyle(.plain)
                            .padding(.horizontal)
                            .modifier(StaggerAppear(index: idx))
                        }
                    }
                }

                Color.clear.frame(height: 24)
            }
            .padding(.top, 8)
        }
        .navigationTitle(tr("events.calendar.title", fallback: "Calendario eventi"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(tr("app.done", fallback: "Fine")) { dismiss() }
            }
        }
        .task { vm.start() }
        .onDisappear { vm.stop() }
    }
}

// MARK: - Calendar Month Grid

private struct CalendarMonthView: View {
    @Binding var displayedMonth: Date
    @Binding var transitionEdge: Edge
    @Binding var selectedDate: Date
    let daysWithEvents: Set<Date>
    let eventCount: (Date) -> Int

    private var calendar: Foundation.Calendar { Foundation.Calendar.current }

    private var monthTitle: String {
        let fmt = DateFormatter()
        fmt.locale = Locale.current
        fmt.dateFormat = "LLLL yyyy"
        return fmt.string(from: displayedMonth).capitalized
    }

    private var weekdaySymbols: [String] {
        let fmt = DateFormatter()
        fmt.locale = Locale.current
        let symbols: [String] = fmt.veryShortStandaloneWeekdaySymbols ?? []
        guard !symbols.isEmpty else { return [] }
        let firstWeekday = calendar.firstWeekday - 1
        let head = Array(symbols[firstWeekday...])
        let tail = Array(symbols[..<firstWeekday])
        return head + tail
    }

    var body: some View {
        VStack(spacing: 12) {
            header
            weekdayHeader
            gridContainer
        }
    }

    private var header: some View {
        HStack {
            Button {
                changeMonth(by: -1)
            } label: {
                Image(systemName: "chevron.left")
                    .font(.headline)
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                    .frame(width: 44, height: 44)
            }
            Spacer()
            Text(monthTitle)
                .font(.headline)
            Spacer()
            Button {
                changeMonth(by: 1)
            } label: {
                Image(systemName: "chevron.right")
                    .font(.headline)
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                    .frame(width: 44, height: 44)
            }
        }
    }

    private var weekdayHeader: some View {
        HStack(spacing: 0) {
            ForEach(Array(weekdaySymbols.enumerated()), id: \.offset) { _, s in
                Text(s)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private var gridContainer: some View {
        let columns = Array(repeating: GridItem(.flexible(), spacing: 0), count: 7)
        let days = monthDays()
        return LazyVGrid(columns: columns, spacing: 6) {
            ForEach(days, id: \.self) { date in
                dayCell(for: date)
            }
        }
        .id(calendar.dateComponents([Foundation.Calendar.Component.year, Foundation.Calendar.Component.month], from: displayedMonth))
        .transition(.asymmetric(
            insertion: .opacity.combined(with: .move(edge: transitionEdge)),
            removal: .opacity.combined(with: .move(edge: transitionEdge == .trailing ? .leading : .trailing))
        ))
    }

    private func dayCell(for date: Date) -> some View {
        let dayStart = calendar.startOfDay(for: date)
        let inMonth = calendar.isDate(date, equalTo: displayedMonth, toGranularity: Foundation.Calendar.Component.month)
        let isSelected = calendar.isDate(dayStart, inSameDayAs: selectedDate)
        let isToday = calendar.isDateInToday(date)
        let dayNumber = calendar.component(Foundation.Calendar.Component.day, from: date)
        let hasEvents = daysWithEvents.contains(dayStart)
        let count = hasEvents ? eventCount(dayStart) : 0
        let dotCount = min(max(count, 0), 3)

        return Button {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                selectedDate = dayStart
                if !inMonth {
                    transitionEdge = date > displayedMonth ? .trailing : .leading
                    if let m = calendar.dateInterval(of: Foundation.Calendar.Component.month, for: date)?.start {
                        displayedMonth = m
                    }
                }
            }
        } label: {
            VStack(spacing: 2) {
                ZStack {
                    if isSelected {
                        Circle()
                            .fill(AppTheme.Colors.brandPrimary)
                            .frame(width: 34, height: 34)
                    } else if isToday {
                        Circle()
                            .stroke(AppTheme.Colors.brandPrimary, lineWidth: 1)
                            .frame(width: 34, height: 34)
                    }
                    Text("\(dayNumber)")
                        .font(.callout)
                        .fontWeight(isToday || isSelected ? .semibold : .regular)
                        .foregroundStyle(
                            isSelected
                            ? Color.white
                            : (inMonth ? Color.primary : Color.secondary.opacity(0.5))
                        )
                }
                .frame(height: 34)

                HStack(spacing: 3) {
                    if dotCount > 0 {
                        ForEach(0..<dotCount, id: \.self) { _ in
                            Circle()
                                .fill(AppTheme.Colors.brandSecondary)
                                .frame(width: 5, height: 5)
                        }
                    } else {
                        Color.clear.frame(width: 5, height: 5)
                    }
                }
                .frame(height: 6)
                .opacity(inMonth ? 1.0 : 0.4)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 2)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(EventDateUtil.fullFormatter.string(from: date)))
    }

    private func changeMonth(by delta: Int) {
        guard let next = calendar.date(byAdding: Foundation.Calendar.Component.month, value: delta, to: displayedMonth) else { return }
        withAnimation(.easeInOut(duration: 0.25)) {
            transitionEdge = delta > 0 ? .trailing : .leading
            displayedMonth = calendar.dateInterval(of: Foundation.Calendar.Component.month, for: next)?.start ?? next
        }
    }

    /// Returns the dates filling a 6-row × 7-col grid covering the displayed month,
    /// including leading days from the previous month and trailing days from the next.
    private func monthDays() -> [Date] {
        guard let monthInterval = calendar.dateInterval(of: Foundation.Calendar.Component.month, for: displayedMonth) else { return [] }
        let firstOfMonth = monthInterval.start
        let firstWeekday = calendar.component(Foundation.Calendar.Component.weekday, from: firstOfMonth)
        let leading = (firstWeekday - calendar.firstWeekday + 7) % 7
        guard let gridStart = calendar.date(byAdding: Foundation.Calendar.Component.day, value: -leading, to: firstOfMonth) else { return [] }

        var dates: [Date] = []
        for i in 0..<42 {
            if let d = calendar.date(byAdding: Foundation.Calendar.Component.day, value: i, to: gridStart) {
                dates.append(d)
            }
        }
        return dates
    }
}

#Preview {
    NavigationStack { EventCalendarView() }
}
