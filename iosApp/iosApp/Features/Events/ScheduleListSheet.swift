import SwiftUI

struct ScheduleListSheet: View {
    /// Two-way binding so edits propagate back to the parent integrator
    /// without needing an explicit save callback. Caller holds the array
    /// and persists it when the user saves the parent event.
    @Binding var schedules: [EventScheduleDraftSwift]
    var onClose: () -> Void = {}

    @State private var addingNew: Bool = false
    @State private var editing: EventScheduleDraftSwift? = nil

    private static let dayHeaderFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.dateFormat = "EEEE, d MMMM"
        return f
    }()

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.dateFormat = "HH:mm"
        return f
    }()

    private var visibleSchedules: [EventScheduleDraftSwift] {
        schedules
            .filter { !$0.isDeleted }
            .sorted { $0.whenStart < $1.whenStart }
    }

    private var groupedByDay: [(key: Date, items: [EventScheduleDraftSwift])] {
        let calendar = Calendar.current
        let groups = Dictionary(grouping: visibleSchedules) { schedule in
            calendar.startOfDay(for: schedule.whenStart)
        }
        return groups
            .map { (key: $0.key, items: $0.value.sorted { $0.whenStart < $1.whenStart }) }
            .sorted { $0.key < $1.key }
    }

    var body: some View {
        NavigationStack {
            Group {
                if visibleSchedules.isEmpty {
                    ContentUnavailableView(
                        tr("events.schedule.empty.title", fallback: "Nessun orario"),
                        systemImage: "calendar.badge.plus",
                        description: Text(tr(
                            "events.schedule.empty.body",
                            fallback: "Tocca '+' per aggiungere un orario al programma."
                        ))
                    )
                } else {
                    List {
                        ForEach(groupedByDay, id: \.key) { group in
                            Section {
                                ForEach(group.items) { schedule in
                                    Button {
                                        editing = schedule
                                    } label: {
                                        scheduleRow(schedule)
                                    }
                                    .buttonStyle(.plain)
                                    .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                        Button(role: .destructive) {
                                            removeOrMarkDeleted(schedule)
                                        } label: {
                                            Label(
                                                tr("app.delete", fallback: "Elimina"),
                                                systemImage: "trash"
                                            )
                                        }
                                    }
                                }
                            } header: {
                                Text(Self.dayHeaderFormatter.string(from: group.key).capitalized)
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                }
            }
            .navigationTitle(tr("events.schedule.title", fallback: "Programma"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(tr("app.done", fallback: "Fine")) {
                        onClose()
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        addingNew = true
                    } label: {
                        Image(systemName: "plus.circle.fill")
                    }
                }
            }
            .sheet(isPresented: $addingNew) {
                ScheduleEditorSheet(
                    initial: nil,
                    onSaved: { draft in
                        schedules.append(draft)
                        addingNew = false
                    },
                    onDeleteRequested: {
                        addingNew = false
                    },
                    onCancelled: {
                        addingNew = false
                    }
                )
            }
            .sheet(item: $editing) { draft in
                ScheduleEditorSheet(
                    initial: draft,
                    onSaved: { updated in
                        if let idx = schedules.firstIndex(where: { $0.stableId == updated.stableId }) {
                            schedules[idx] = updated
                        }
                        editing = nil
                    },
                    onDeleteRequested: {
                        if let idx = schedules.firstIndex(where: { $0.stableId == draft.stableId }) {
                            if let existingId = schedules[idx].id, !existingId.hasPrefix("DELETE:") {
                                schedules[idx].id = "DELETE:\(existingId)"
                            } else if schedules[idx].id == nil {
                                schedules.remove(at: idx)
                            }
                        }
                        editing = nil
                    },
                    onCancelled: {
                        editing = nil
                    }
                )
            }
        }
    }

    @ViewBuilder
    private func scheduleRow(_ schedule: EventScheduleDraftSwift) -> some View {
        HStack {
            Text(schedule.title)
                .fontWeight(.bold)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(Self.timeFormatter.string(from: schedule.whenStart))
                .monospacedDigit()
                .foregroundStyle(.secondary)
        }
        .contentShape(Rectangle())
    }

    private func removeOrMarkDeleted(_ schedule: EventScheduleDraftSwift) {
        guard let idx = schedules.firstIndex(where: { $0.stableId == schedule.stableId }) else {
            return
        }
        if let existingId = schedules[idx].id, !existingId.hasPrefix("DELETE:") {
            schedules[idx].id = "DELETE:\(existingId)"
        } else if schedules[idx].id == nil {
            schedules.remove(at: idx)
        }
    }
}
