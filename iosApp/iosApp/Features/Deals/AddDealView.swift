import SwiftUI
import Shared

// MARK: - Bridging helpers

private func toInstant(_ d: Date) -> Kotlinx_datetimeInstant {
    Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
        epochMilliseconds: Int64(d.timeIntervalSince1970 * 1000)
    )
}

private func toDate(_ instant: Kotlinx_datetimeInstant) -> Date {
    Date(timeIntervalSince1970: TimeInterval(instant.toEpochMilliseconds()) / 1000.0)
}

// MARK: - View model

@MainActor
@Observable
final class AddDealViewModel {
    let deal: DealModel?

    // Form fields
    var name: String = ""
    var commercialSector: String = ""
    var vatNumber: String = ""
    var link: String = ""

    var position: LocationModel?

    var hasValidity: Bool = false
    var startDate: Date = .now
    var endDate: Date = Calendar.current.date(byAdding: .year, value: 1, to: .now) ?? .now

    var details: String = ""
    var howToGet: String = ""
    /// Raw eligibility key sent to backend.
    /// Either `"active_members"` or `"active_members and relatives"`.
    var selectedEligibility: String = "active_members"

    // Contact (single primary contact)
    var contactId: String?
    var contactName: String = ""
    var contactEmail: String = ""
    var contactPhone: String = ""
    var contactNote: String = ""

    var saving: Bool = false
    var deleting: Bool = false
    var error: String?
    var dismissed: Bool = false

    init(deal: DealModel?) {
        self.deal = deal
        guard let d = deal else { return }

        self.name = d.name
        self.commercialSector = d.commercialSector
        self.vatNumber = d.vatNumber ?? ""
        self.link = d.link ?? ""
        self.position = d.position
        self.details = d.details ?? ""
        self.howToGet = d.howToGet ?? ""
        if let who = d.who, !who.isEmpty {
            self.selectedEligibility = who
        }

        if let s = d.starting, let e = d.ending {
            self.hasValidity = true
            self.startDate = toDate(s)
            self.endDate = toDate(e)
        }
    }

    var isEditing: Bool {
        if let d = deal { return !d.id.isEmpty }
        return false
    }

    var canSave: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
            !commercialSector.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var emailLooksValid: Bool {
        let e = contactEmail.trimmingCharacters(in: .whitespacesAndNewlines)
        if e.isEmpty { return true }
        return e.contains("@") && e.contains(".")
    }

    func loadContact() async {
        guard let d = deal, !d.id.isEmpty else { return }
        do {
            let list = try await koin.deals.contacts(dealId: d.id)
            if let first = list.first {
                self.contactId = first.id
                self.contactName = first.name
                self.contactEmail = first.email
                self.contactPhone = first.phoneNumber ?? ""
                self.contactNote = first.note ?? ""
            }
        } catch {
            // Non-fatal: form remains usable without an existing contact.
        }
    }

    func save() async {
        guard canSave, !saving else { return }
        if !emailLooksValid {
            error = tr(
                "addons.deals.add.error.invalid_email",
                fallback: "Email non valida"
            )
            return
        }

        saving = true
        defer { saving = false }

        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedSector = commercialSector.trimmingCharacters(in: .whitespacesAndNewlines)

        let draft = DealsRepository.DealDraft(
            name: trimmedName,
            commercialSector: trimmedSector,
            details: trimOrNil(details),
            who: selectedEligibility,
            howToGet: trimOrNil(howToGet),
            link: trimOrNil(link),
            vatNumber: trimOrNil(vatNumber),
            positionId: position?.id,
            starting: hasValidity ? toInstant(startDate) : nil,
            ending: hasValidity ? toInstant(endDate) : nil
        )

        let cName = contactName.trimmingCharacters(in: .whitespacesAndNewlines)
        let cEmail = contactEmail.trimmingCharacters(in: .whitespacesAndNewlines)
        let contact: DealsRepository.ContactDraft? = {
            if cName.isEmpty || cEmail.isEmpty { return nil }
            return DealsRepository.ContactDraft(
                id: contactId,
                name: cName,
                email: cEmail,
                phoneNumber: trimOrNil(contactPhone),
                note: trimOrNil(contactNote)
            )
        }()

        do {
            if let d = deal, !d.id.isEmpty {
                _ = try await koin.deals.update(id: d.id, draft: draft, contact: contact)
            } else {
                _ = try await koin.deals.create(draft: draft, contact: contact)
            }
            dismissed = true
        } catch {
            self.error = (error as NSError).localizedDescription
        }
    }

    func delete() async {
        guard let d = deal, !d.id.isEmpty, !deleting else { return }
        deleting = true
        defer { deleting = false }
        do {
            try await koin.deals.delete(id: d.id)
            dismissed = true
        } catch {
            self.error = (error as NSError).localizedDescription
        }
    }

    private func trimOrNil(_ s: String) -> String? {
        let t = s.trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}

// MARK: - View

/// Native rewrite of the Flutter `AddonDealsAddView` — create / edit a deal
/// plus its primary contact in a single SwiftUI form. Reuses
/// `LocationPickerSheet` (same component as the Add Event flow) for picking
/// the deal's optional location.
struct AddDealView: View {
    @Environment(\.dismiss) private var dismiss

    @State private var vm: AddDealViewModel
    @State private var showLocationPicker = false
    @State private var showDeleteConfirm = false

    init(deal: DealModel? = nil) {
        _vm = State(initialValue: AddDealViewModel(deal: deal))
    }

    var body: some View {
        Form {
            infoSection
            locationSection
            validitySection
            detailsSection
            contactSection
        }
        .navigationTitle(
            vm.isEditing
                ? tr("addons.deals.edit.title", fallback: "Modifica deal")
                : tr("addons.deals.add.title", fallback: "Nuovo deal")
        )
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbar }
        .sheet(isPresented: $showLocationPicker) {
            LocationPickerSheet(
                onPicked: { loc in
                    vm.position = loc
                    showLocationPicker = false
                },
                onCancelled: { showLocationPicker = false }
            )
        }
        .alert(
            tr("app.error.title", fallback: "Errore"),
            isPresented: Binding(
                get: { vm.error != nil },
                set: { if !$0 { vm.error = nil } }
            )
        ) {
            Button("OK") { vm.error = nil }
        } message: {
            Text(vm.error ?? "")
        }
        .confirmationDialog(
            tr("addons.deals.delete.confirm.title", fallback: "Eliminare il deal?"),
            isPresented: $showDeleteConfirm,
            titleVisibility: .visible
        ) {
            Button(tr("app.delete", fallback: "Elimina"), role: .destructive) {
                Task { await vm.delete() }
            }
            Button(tr("app.cancel", fallback: "Annulla"), role: .cancel) {}
        } message: {
            Text(tr(
                "addons.deals.delete.confirm.body",
                fallback: "Questa azione non può essere annullata."
            ))
        }
        .task {
            await vm.loadContact()
        }
        .onChange(of: vm.dismissed) { _, dismissed in
            if dismissed { dismiss() }
        }
    }

    // MARK: - Sections

    @ViewBuilder
    private var infoSection: some View {
        Section(tr("addons.deals.add.section.info", fallback: "Informazioni")) {
            TextField(
                tr("addons.deals.add.field.name", fallback: "Nome convenzione"),
                text: $vm.name
            )
            TextField(
                tr("addons.deals.add.field.sector", fallback: "Settore"),
                text: $vm.commercialSector
            )
            TextField(
                tr("addons.deals.add.field.vat", fallback: "P. IVA"),
                text: $vm.vatNumber
            )
            .keyboardType(.numbersAndPunctuation)
            TextField(
                tr("addons.deals.add.field.link", fallback: "Link"),
                text: $vm.link
            )
            .keyboardType(.URL)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
        }
    }

    @ViewBuilder
    private var locationSection: some View {
        Section(tr("addons.deals.add.section.location", fallback: "Sede")) {
            Button {
                showLocationPicker = true
            } label: {
                HStack {
                    if let pos = vm.position {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(pos.name.isEmpty
                                 ? tr("addons.deals.add.location.unnamed", fallback: "Posizione")
                                 : pos.name)
                                .foregroundStyle(.primary)
                            if !pos.address.isEmpty {
                                Text(pos.address)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    } else {
                        Text(tr(
                            "addons.deals.add.location.pick",
                            fallback: "Seleziona sede (opzionale)"
                        ))
                        .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.tertiary)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if vm.position != nil {
                Button(role: .destructive) {
                    vm.position = nil
                } label: {
                    Text(tr("addons.deals.add.location.remove", fallback: "Rimuovi sede"))
                }
            }
        }
    }

    @ViewBuilder
    private var validitySection: some View {
        Section(tr("addons.deals.add.section.validity", fallback: "Validità")) {
            Toggle(
                tr("addons.deals.add.field.has_validity", fallback: "Imposta date di validità"),
                isOn: $vm.hasValidity
            )
            if vm.hasValidity {
                DatePicker(
                    tr("app.deals.from", fallback: "Dal"),
                    selection: $vm.startDate,
                    displayedComponents: [.date, .hourAndMinute]
                )
                DatePicker(
                    tr("app.deals.until", fallback: "Fino al"),
                    selection: $vm.endDate,
                    displayedComponents: [.date, .hourAndMinute]
                )
            }
        }
    }

    @ViewBuilder
    private var detailsSection: some View {
        Section(tr("addons.deals.add.section.description", fallback: "Dettagli")) {
            TextField(
                tr("addons.deals.add.field.details", fallback: "Dettagli del deal"),
                text: $vm.details,
                axis: .vertical
            )
            .lineLimit(3...8)

            Picker(
                tr("addons.deals.add.field.who", fallback: "A chi è rivolto"),
                selection: $vm.selectedEligibility
            ) {
                Text(tr(
                    "addons.deals.add.who.active_members",
                    fallback: "Soci attivi"
                ))
                .tag("active_members")
                Text(tr(
                    "addons.deals.add.who.active_members_relatives",
                    fallback: "Soci attivi e familiari"
                ))
                .tag("active_members and relatives")
            }

            TextField(
                tr("addons.deals.add.field.howtoget", fallback: "Come ottenere il deal"),
                text: $vm.howToGet,
                axis: .vertical
            )
            .lineLimit(2...5)
        }
    }

    @ViewBuilder
    private var contactSection: some View {
        Section {
            TextField(
                tr("addons.deals.add.field.contact_name", fallback: "Nome"),
                text: $vm.contactName
            )
            TextField(
                tr("addons.deals.add.field.contact_email", fallback: "Email"),
                text: $vm.contactEmail
            )
            .keyboardType(.emailAddress)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            TextField(
                tr("addons.deals.add.field.contact_phone", fallback: "Telefono"),
                text: $vm.contactPhone
            )
            .keyboardType(.phonePad)
            TextField(
                tr("addons.deals.add.field.contact_note", fallback: "Note"),
                text: $vm.contactNote,
                axis: .vertical
            )
            .lineLimit(1...4)
        } header: {
            Text(tr("addons.deals.add.section.contact", fallback: "Contatto principale"))
        } footer: {
            Text(tr(
                "addons.deals.add.section.contact.footer",
                fallback: "(Nascosto al pubblico)"
            ))
        }
    }

    // MARK: - Toolbar

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        ToolbarItem(placement: .topBarLeading) {
            Button(tr("app.cancel", fallback: "Annulla")) { dismiss() }
                .disabled(vm.saving || vm.deleting)
        }
        ToolbarItem(placement: .topBarTrailing) {
            if vm.saving {
                ProgressView()
            } else {
                Button(tr("app.save", fallback: "Salva")) {
                    Task { await vm.save() }
                }
                .disabled(!vm.canSave || vm.deleting)
            }
        }
        if vm.isEditing {
            ToolbarItem(placement: .destructiveAction) {
                if vm.deleting {
                    ProgressView()
                } else {
                    Button(role: .destructive) {
                        showDeleteConfirm = true
                    } label: {
                        Image(systemName: "trash")
                    }
                    .disabled(vm.saving)
                }
            }
        }
    }
}

#Preview {
    NavigationStack {
        AddDealView()
    }
}
