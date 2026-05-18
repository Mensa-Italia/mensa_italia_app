import SwiftUI
import Shared

// MARK: - Helpers

private func toInstant(_ d: Date) -> Kotlinx_datetimeInstant {
    Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
        epochMilliseconds: Int64(d.timeIntervalSince1970 * 1000)
    )
}

private func toDate(_ instant: Kotlinx_datetimeInstant) -> Date {
    Date(timeIntervalSince1970: TimeInterval(instant.toEpochMilliseconds()) / 1000.0)
}

// MARK: - Editor mode

enum TestDateEditorMode {
    case create
    case edit(existing: LocalOfficeTestDateModel)
}

// MARK: - Sheet

struct LocalOfficeTestDateEditorSheet: View {
    let officeId: String
    let assistantsCandidates: [LocalOfficeAssistantModel]
    let mode: TestDateEditorMode

    // Form state
    @State private var date: Date
    @State private var location: String
    @State private var notes: String
    @State private var maxParticipants: Int
    @State private var selectedAssistants: Set<String>

    // UI state
    @State private var saving = false
    @State private var errorMessage: String? = nil
    @State private var showError = false

    @Environment(\.dismiss) private var dismiss

    init(officeId: String, assistantsCandidates: [LocalOfficeAssistantModel], mode: TestDateEditorMode) {
        self.officeId = officeId
        self.assistantsCandidates = assistantsCandidates
        self.mode = mode

        switch mode {
        case .create:
            let tomorrow = Calendar.current.date(byAdding: .day, value: 1, to: Date()) ?? Date()
            _date = State(initialValue: tomorrow)
            _location = State(initialValue: "")
            _notes = State(initialValue: "")
            _maxParticipants = State(initialValue: 0)
            _selectedAssistants = State(initialValue: [])
        case .edit(let existing):
            _date = State(initialValue: toDate(existing.date))
            _location = State(initialValue: existing.location)
            _notes = State(initialValue: existing.notes)
            _maxParticipants = State(initialValue: Int(existing.maxParticipants))
            _selectedAssistants = State(initialValue: Set(existing.assistants))
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    DatePicker(
                        tr("local_office.editor.date", fallback: "Data e ora"),
                        selection: $date,
                        displayedComponents: [.date, .hourAndMinute]
                    )
                    .environment(\.locale, Locale(identifier: "it_IT"))
                }

                Section {
                    TextField(
                        tr("local_office.editor.location", fallback: "Luogo"),
                        text: $location
                    )
                }

                Section {
                    TextField(
                        tr("local_office.editor.notes", fallback: "Note"),
                        text: $notes,
                        axis: .vertical
                    )
                    .lineLimit(3...6)
                }

                Section {
                    Stepper(
                        value: $maxParticipants,
                        in: 0...500
                    ) {
                        HStack {
                            Text(tr("local_office.editor.max_participants", fallback: "Max partecipanti"))
                            Spacer()
                            Text("\(maxParticipants)")
                                .foregroundStyle(.secondary)
                                .monospacedDigit()
                        }
                    }
                }

                if !assistantsCandidates.isEmpty {
                    Section(tr("local_office.editor.assistants", fallback: "Assistenti")) {
                        ForEach(assistantsCandidates, id: \.id) { assistant in
                            Button {
                                if selectedAssistants.contains(assistant.user) {
                                    selectedAssistants.remove(assistant.user)
                                } else {
                                    selectedAssistants.insert(assistant.user)
                                }
                            } label: {
                                HStack {
                                    Text(assistant.name)
                                        .foregroundStyle(.primary)
                                    Spacer()
                                    if selectedAssistants.contains(assistant.user) {
                                        Image(systemName: "checkmark")
                                            .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                                            .fontWeight(.semibold)
                                    }
                                }
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .navigationTitle(isCreating
                ? tr("local_office.test_dates.add", fallback: "Aggiungi sessione")
                : tr("local_office.test_dates.edit", fallback: "Modifica sessione")
            )
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(tr("local_office.editor.cancel", fallback: "Annulla")) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if saving {
                        ProgressView()
                    } else {
                        Button(tr("local_office.editor.save", fallback: "Salva")) {
                            Task { await save() }
                        }
                        .fontWeight(.semibold)
                    }
                }
            }
            .alert(
                tr("app.error.title", fallback: "Errore"),
                isPresented: $showError,
                presenting: errorMessage
            ) { _ in
                Button("OK", role: .cancel) {}
            } message: { msg in
                Text(msg)
            }
        }
    }

    // MARK: - Helpers

    private var isCreating: Bool {
        if case .create = mode { return true }
        return false
    }

    private func save() async {
        saving = true
        defer { saving = false }

        let assistantsList = Array(selectedAssistants)

        do {
            switch mode {
            case .create:
                // Use the typed-field create wrapper to keep the K/N bridge
                // out of the data-class constructor path (mirrors the link
                // editor — same precaution against silently-dropped values).
                _ = try await koin.localOffices.createTestDateFromFields(
                    officeId: officeId,
                    date: toInstant(date),
                    location: location.trimmingCharacters(in: .whitespacesAndNewlines),
                    notes: notes.trimmingCharacters(in: .whitespacesAndNewlines),
                    maxParticipants: Int32(maxParticipants),
                    assistants: assistantsList
                )

            case .edit(let existing):
                _ = try await koin.localOffices.updateTestDateFields(
                    officeId: officeId,
                    id: existing.id,
                    date: toInstant(date),
                    location: location.trimmingCharacters(in: .whitespacesAndNewlines),
                    notes: notes.trimmingCharacters(in: .whitespacesAndNewlines),
                    maxParticipants: KotlinInt(integerLiteral: maxParticipants),
                    assistants: assistantsList
                )
            }
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
    }
}
