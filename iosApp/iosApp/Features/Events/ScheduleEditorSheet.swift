import SwiftUI

struct ScheduleEditorSheet: View {
    /// nil → create new. non-nil → edit existing.
    var initial: EventScheduleDraftSwift?
    /// Called when the user taps "Salva" with the (possibly-edited) draft.
    /// Caller appends-or-replaces it in the parent list and dismisses.
    var onSaved: (EventScheduleDraftSwift) -> Void
    /// Called when the user taps "Elimina" in edit mode.
    /// Caller marks the draft with `id = "DELETE:\(originalId)"` and dismisses.
    /// (The editor itself does not mutate the draft for deletion; it just
    /// signals the intent.)
    var onDeleteRequested: () -> Void = {}
    var onCancelled: () -> Void = {}

    @State private var title: String
    @State private var description: String
    @State private var infoLink: String
    @State private var whenStart: Date
    @State private var whenEnd: Date
    @State private var maxExternalGuests: Int
    @State private var priceText: String
    @State private var isSubscriptable: Bool

    @State private var validationMessage: String?
    @State private var showValidationAlert: Bool = false
    @State private var showDeleteConfirm: Bool = false

    init(
        initial: EventScheduleDraftSwift?,
        onSaved: @escaping (EventScheduleDraftSwift) -> Void,
        onDeleteRequested: @escaping () -> Void = {},
        onCancelled: @escaping () -> Void = {}
    ) {
        self.initial = initial
        self.onSaved = onSaved
        self.onDeleteRequested = onDeleteRequested
        self.onCancelled = onCancelled

        let seed = initial ?? EventScheduleDraftSwift()
        _title = State(initialValue: seed.title)
        _description = State(initialValue: seed.description)
        _infoLink = State(initialValue: seed.infoLink)
        _whenStart = State(initialValue: seed.whenStart)
        _whenEnd = State(initialValue: seed.whenEnd)
        _maxExternalGuests = State(initialValue: seed.maxExternalGuests)
        _isSubscriptable = State(initialValue: seed.isSubscriptable)
        // Format price using a localized decimal separator to allow re-parse.
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 0
        let priceString: String
        if seed.price == 0 && initial == nil {
            priceString = ""
        } else {
            priceString = formatter.string(from: NSNumber(value: seed.price)) ?? "\(seed.price)"
        }
        _priceText = State(initialValue: priceString)
    }

    private var isEditingExisting: Bool {
        guard let id = initial?.id else { return false }
        return !id.hasPrefix("DELETE:")
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(tr("events.schedule.section.info", fallback: "Informazioni")) {
                    TextField(
                        tr("events.schedule.field.title", fallback: "Titolo"),
                        text: $title
                    )
                    TextField(
                        tr("events.schedule.field.description", fallback: "Descrizione"),
                        text: $description,
                        axis: .vertical
                    )
                    .lineLimit(3...8)
                    TextField(
                        tr("events.schedule.field.info_link", fallback: "Link info (opzionale)"),
                        text: $infoLink
                    )
                    .keyboardType(.URL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                }

                Section(tr("events.schedule.section.when", fallback: "Quando")) {
                    DatePicker(
                        tr("events.schedule.field.start", fallback: "Inizio"),
                        selection: $whenStart,
                        displayedComponents: [.date, .hourAndMinute]
                    )
                    DatePicker(
                        tr("events.schedule.field.end", fallback: "Fine"),
                        selection: $whenEnd,
                        displayedComponents: [.date, .hourAndMinute]
                    )
                }

                Section(tr("events.schedule.section.booking", fallback: "Prenotazioni")) {
                    Toggle(
                        tr("events.schedule.field.subscriptable", fallback: "Prenotabile"),
                        isOn: $isSubscriptable
                    )
                    if isSubscriptable {
                        Stepper(
                            value: $maxExternalGuests,
                            in: 0...500
                        ) {
                            Text("\(tr("events.schedule.field.max_guests", fallback: "Posti per esterni")): \(maxExternalGuests)")
                        }
                        TextField(
                            tr("events.schedule.field.price", fallback: "Prezzo (€)"),
                            text: $priceText
                        )
                        .keyboardType(.decimalPad)
                    }
                }

                if isEditingExisting {
                    Section {
                        Button(role: .destructive) {
                            showDeleteConfirm = true
                        } label: {
                            Text(tr("events.schedule.delete", fallback: "Elimina orario"))
                                .frame(maxWidth: .infinity, alignment: .center)
                        }
                    }
                }
            }
            .navigationTitle(
                initial == nil
                    ? tr("events.schedule.editor.new", fallback: "Nuovo orario")
                    : tr("events.schedule.editor.edit", fallback: "Modifica orario")
            )
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(tr("app.cancel", fallback: "Annulla")) {
                        onCancelled()
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(tr("app.save", fallback: "Salva")) {
                        save()
                    }
                }
            }
            .alert(
                tr("events.schedule.validation.title", fallback: "Dati non validi"),
                isPresented: $showValidationAlert,
                presenting: validationMessage
            ) { _ in
                Button(tr("app.ok", fallback: "OK"), role: .cancel) { }
            } message: { msg in
                Text(msg)
            }
            .confirmationDialog(
                tr("events.schedule.delete.confirm", fallback: "Eliminare questo orario?"),
                isPresented: $showDeleteConfirm,
                titleVisibility: .visible
            ) {
                Button(
                    tr("events.schedule.delete", fallback: "Elimina orario"),
                    role: .destructive
                ) {
                    onDeleteRequested()
                }
                Button(tr("app.cancel", fallback: "Annulla"), role: .cancel) { }
            }
        }
    }

    private func save() {
        let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmedTitle.isEmpty {
            validationMessage = tr(
                "events.schedule.validation.title_required",
                fallback: "Il titolo è obbligatorio."
            )
            showValidationAlert = true
            return
        }
        if whenEnd < whenStart {
            validationMessage = tr(
                "events.schedule.validation.end_before_start",
                fallback: "La fine non può precedere l'inizio."
            )
            showValidationAlert = true
            return
        }

        let parsedPrice = parsePrice(priceText)

        let draft = EventScheduleDraftSwift(
            id: initial?.id,
            stableId: initial?.stableId ?? UUID(),
            title: trimmedTitle,
            description: description,
            infoLink: infoLink.trimmingCharacters(in: .whitespacesAndNewlines),
            whenStart: whenStart,
            whenEnd: whenEnd,
            maxExternalGuests: isSubscriptable ? maxExternalGuests : 0,
            price: isSubscriptable ? parsedPrice : 0,
            isSubscriptable: isSubscriptable
        )
        onSaved(draft)
    }

    private func parsePrice(_ s: String) -> Double {
        let trimmed = s.trimmingCharacters(in: .whitespaces)
        if trimmed.isEmpty { return 0 }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        if let n = formatter.number(from: trimmed) {
            return n.doubleValue
        }
        // Fallback: swap comma for dot
        let normalized = trimmed.replacingOccurrences(of: ",", with: ".")
        return Double(normalized) ?? 0
    }
}
