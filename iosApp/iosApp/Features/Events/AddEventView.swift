import SwiftUI
import PhotosUI
import Shared

// MARK: - Permissions

/// Mirrors Flutter's `MasterModel.allowControlEvents` predicate.
/// Snapshots the current user's powers at view-creation time.
@MainActor
struct EventPermissions {
    let powers: [String]

    init(user: UserModel?) {
        self.powers = user?.powers ?? []
    }

    func has(_ power: String) -> Bool {
        powers.contains(power)
            || powers.contains("\(power)_helper")
            || powers.contains("super")
    }

    var allowControlEvents: Bool { has("events") }
}

// MARK: - Bridging helpers

private func toInstant(_ d: Date) -> Kotlinx_datetimeInstant {
    Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
        epochMilliseconds: Int64(d.timeIntervalSince1970 * 1000)
    )
}

private func toDate(_ instant: Kotlinx_datetimeInstant) -> Date {
    Date(timeIntervalSince1970: TimeInterval(instant.toEpochMilliseconds()) / 1000.0)
}

private extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let result = KotlinByteArray(size: Int32(count))
        for (i, byte) in self.enumerated() {
            result.set(index: Int32(i), value: Int8(bitPattern: byte))
        }
        return result
    }
}

// MARK: - View model

@MainActor
@Observable
final class AddEventViewModel {
    // Editing target (nil = create)
    let event: EventModel?

    // Form fields
    var name: String = ""
    var description: String = ""
    var infoLink: String = ""
    var startDate: Date
    var endDate: Date
    var position: LocationModel? = nil

    var isOnline: Bool = false
    var isNational: Bool = false
    var isSpot: Bool = false

    var imageData: Data? = nil
    var imageFilename: String? = nil
    var imageContentType: String? = nil

    var schedules: [EventScheduleDraftSwift] = []

    var saving = false
    var error: String? = nil
    var dismissed = false

    init(event: EventModel?) {
        self.event = event
        if let e = event {
            self.name = e.name
            // Kotlin/Native renames `description` → `description_` on Swift to avoid
            // colliding with NSObject's `description` (which returns the toString
            // of the entire object — that's the bug we're fixing here).
            self.description = e.description_
            self.infoLink = e.infoLink
            self.startDate = toDate(e.whenStart)
            self.endDate = toDate(e.whenEnd)
            self.position = e.position
            self.isNational = e.isNational
            self.isSpot = e.isSpot
            self.isOnline = (e.position == nil)
        } else {
            let tomorrow = Calendar.current.date(byAdding: .day, value: 1, to: Date()) ?? Date()
            self.startDate = tomorrow
            self.endDate = Calendar.current.date(byAdding: .hour, value: 2, to: tomorrow) ?? tomorrow
        }
    }

    /// Hydrate schedules for the event being edited (no-op in create mode).
    func loadSchedules() async {
        guard let id = event?.id, !id.isEmpty else { return }
        do {
            try await koin.eventSchedules.refresh(eventId: id)
            let list = try await koin.eventSchedules.firstSnapshot(eventId: id)
            self.schedules = list.map { s in
                EventScheduleDraftSwift(
                    id: s.id,
                    stableId: UUID(),
                    title: s.title,
                    description: s.description_,   // KMP `description` → `description_` on Swift
                    infoLink: s.infoLink,
                    whenStart: toDate(s.whenStart),
                    whenEnd: toDate(s.whenEnd),
                    maxExternalGuests: Int(s.maxExternalGuests),
                    price: s.price,
                    isSubscriptable: s.isSubscriptable
                )
            }
        } catch {
            // Non-fatal: just leave list empty.
        }
    }

    func save(permissions: EventPermissions, ownerId: String) async {
        saving = true
        defer { saving = false }

        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedDescription = description.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedLink = infoLink.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedName.isEmpty else {
            error = tr("events.add.validation.name", fallback: "Il nome è obbligatorio")
            return
        }
        guard !trimmedDescription.isEmpty else {
            error = tr("events.add.validation.description", fallback: "La descrizione è obbligatoria")
            return
        }
        guard endDate >= startDate else {
            error = tr("events.add.validation.dates", fallback: "La fine deve essere dopo l'inizio")
            return
        }

        // Permission-driven flag override (matches Flutter exactly).
        var finalIsOnline = isOnline
        var finalIsNational = isNational
        var finalIsSpot = isSpot
        if !permissions.allowControlEvents {
            finalIsOnline = false
            finalIsNational = false
            finalIsSpot = true
        }

        if !finalIsOnline {
            guard position != nil else {
                error = tr(
                    "events.add.validation.location",
                    fallback: "Seleziona una posizione o segna l'evento come online"
                )
                return
            }
        }

        // Build KMP schedule drafts.
        let kSchedules: [ScheduleDraft] = schedules.map { s in
            ScheduleDraft(
                id: s.id,
                title: s.title,
                description: s.description,
                infoLink: s.infoLink,
                whenStart: toInstant(s.whenStart),
                whenEnd: toInstant(s.whenEnd),
                maxExternalGuests: Int32(s.maxExternalGuests),
                price: s.price,
                isSubscriptable: s.isSubscriptable
            )
        }

        let draft = EventDraft(
            name: trimmedName,
            description: trimmedDescription,
            infoLink: trimmedLink,
            whenStart: toInstant(startDate),
            whenEnd: toInstant(endDate),
            isNational: finalIsNational,
            isSpot: finalIsSpot,
            ownerId: ownerId,
            positionId: finalIsOnline ? nil : position?.id,
            imageBytes: imageData?.toKotlinByteArray(),
            imageFilename: imageData != nil ? (imageFilename ?? "cover.jpg") : nil,
            imageContentType: imageContentType ?? "image/jpeg",
            schedules: kSchedules
        )

        do {
            if let editing = event {
                _ = try await koin.events.update(id: editing.id, draft: draft)
            } else {
                _ = try await koin.events.create(draft: draft)
            }
            dismissed = true
        } catch {
            self.error = error.localizedDescription
        }
    }

    func delete() async {
        guard let id = event?.id, !id.isEmpty else { return }
        saving = true
        defer { saving = false }
        do {
            try await koin.events.delete(id: id)
            dismissed = true
        } catch {
            self.error = error.localizedDescription
        }
    }
}

// MARK: - View

struct AddEventView: View {
    var event: EventModel? = nil

    @State private var vm: AddEventViewModel
    @State private var permissions: EventPermissions
    @State private var ownerId: String

    @State private var showLocationPicker = false
    @State private var showScheduleList = false
    @State private var showCardBuilder = false
    @State private var showImageOptions = false
    @State private var showPhotoPicker = false
    @State private var pickerItem: PhotosPickerItem? = nil
    @State private var showDeleteConfirm = false

    @Environment(\.dismiss) private var dismiss

    init(event: EventModel? = nil) {
        self.event = event
        let user = koin.auth.currentUser.value as? UserModel
        self._vm = State(initialValue: AddEventViewModel(event: event))
        self._permissions = State(initialValue: EventPermissions(user: user))
        self._ownerId = State(initialValue: user?.id ?? "")
    }

    private var isEditing: Bool { event != nil }

    private var visibleScheduleCount: Int {
        vm.schedules.filter { !$0.isDeleted }.count
    }

    var body: some View {
        Form {
            if permissions.allowControlEvents {
                coverImageSection
                typeSection
            }
            detailsSection
            if !vm.isOnline {
                whereSection
            }
            whenSection
            scheduleSection
        }
        .navigationTitle(
            isEditing
                ? tr("events.edit.title", fallback: "Modifica evento")
                : tr("events.add.title", fallback: "Nuovo evento")
        )
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbarContent }
        .sheet(isPresented: $showLocationPicker) {
            LocationPickerSheet(
                onPicked: { loc in
                    vm.position = loc
                    showLocationPicker = false
                },
                onCancelled: { showLocationPicker = false }
            )
        }
        .sheet(isPresented: $showScheduleList) {
            ScheduleListSheet(
                schedules: $vm.schedules,
                onClose: { showScheduleList = false }
            )
        }
        .sheet(isPresented: $showCardBuilder) {
            EventCardBuilderSheet(
                onConfirmed: { data in
                    vm.imageData = data
                    vm.imageFilename = "event_card.png"
                    vm.imageContentType = "image/png"
                    showCardBuilder = false
                },
                onCancelled: { showCardBuilder = false }
            )
        }
        .photosPicker(
            isPresented: $showPhotoPicker,
            selection: $pickerItem,
            matching: .images,
            photoLibrary: .shared()
        )
        .onChange(of: pickerItem) { _, newItem in
            guard let item = newItem else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self) {
                    vm.imageData = data
                    vm.imageFilename = "cover.jpg"
                    vm.imageContentType = "image/jpeg"
                }
                pickerItem = nil
            }
        }
        .confirmationDialog(
            tr("events.add.image.pick", fallback: "Tocca per scegliere una copertina"),
            isPresented: $showImageOptions,
            titleVisibility: .visible
        ) {
            Button(tr("events.add.image.from_library", fallback: "Scegli dalla libreria")) {
                showPhotoPicker = true
            }
            Button(tr("events.add.image.generate", fallback: "Genera con AI")) {
                showCardBuilder = true
            }
            Button(tr("app.cancel", fallback: "Annulla"), role: .cancel) {}
        }
        .alert(
            tr("events.delete.confirm.title", fallback: "Eliminare l'evento?"),
            isPresented: $showDeleteConfirm
        ) {
            Button(tr("app.cancel", fallback: "Annulla"), role: .cancel) {}
            Button(tr("app.delete", fallback: "Elimina"), role: .destructive) {
                Task {
                    await vm.delete()
                    if vm.dismissed { dismiss() }
                }
            }
        } message: {
            Text(tr(
                "events.delete.confirm.body",
                fallback: "L'azione non può essere annullata."
            ))
        }
        .alert(
            tr("app.error.title", fallback: "Errore"),
            isPresented: Binding(
                get: { vm.error != nil },
                set: { if !$0 { vm.error = nil } }
            )
        ) {
            Button("OK", role: .cancel) { vm.error = nil }
        } message: {
            Text(vm.error ?? "")
        }
        .task { await vm.loadSchedules() }
    }

    // MARK: Sections

    @ViewBuilder
    private var coverImageSection: some View {
        Section {
            Button {
                showImageOptions = true
            } label: {
                coverImageContent
            }
            .buttonStyle(.plain)
        } header: {
            Text(tr("events.add.section.cover", fallback: "Copertina"))
        }
    }

    @ViewBuilder
    private var coverImageContent: some View {
        // 1528 / 603 ≈ 2.53:1 (matches Flutter aspect ratio).
        let aspect: CGFloat = 1528.0 / 603.0
        let cornerRadius: CGFloat = 16

        if let data = vm.imageData, let ui = UIImage(data: data) {
            Image(uiImage: ui)
                .resizable()
                .scaledToFill()
                .aspectRatio(aspect, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
        } else if let e = event, let url = imageURL(for: e) {
            CachedAsyncImage(url: url) { img in
                img.resizable().scaledToFill()
            } placeholder: {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .aspectRatio(aspect, contentMode: .fit)
            }
            .aspectRatio(aspect, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
        } else {
            ZStack {
                RoundedRectangle(cornerRadius: cornerRadius)
                    .strokeBorder(
                        Color.secondary.opacity(0.5),
                        style: StrokeStyle(lineWidth: 1, dash: [6])
                    )
                VStack(spacing: 8) {
                    Image(systemName: "photo.badge.plus")
                        .font(.system(size: 32))
                        .foregroundStyle(.secondary)
                    Text(tr("events.add.image.pick", fallback: "Tocca per scegliere una copertina"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding()
            }
            .aspectRatio(aspect, contentMode: .fit)
        }
    }

    private func imageURL(for event: EventModel) -> URL? {
        guard !event.image.isEmpty else { return nil }
        if event.image.hasPrefix("http") { return URL(string: event.image) }
        return Files.url(
            collection: "events",
            recordId: event.id,
            filename: event.image,
            thumb: "1200x0"
        )
    }

    @ViewBuilder
    private var typeSection: some View {
        Section(tr("events.add.section.type", fallback: "Tipo evento")) {
            Toggle(tr("events.add.field.online", fallback: "Online"), isOn: $vm.isOnline)
            Toggle(tr("events.add.field.national", fallback: "Evento nazionale"), isOn: $vm.isNational)
            Toggle(tr("events.add.field.spot", fallback: "Spot"), isOn: $vm.isSpot)
        }
    }

    @ViewBuilder
    private var detailsSection: some View {
        Section(tr("events.add.section.info", fallback: "Dettagli")) {
            TextField(
                tr("events.add.field.name", fallback: "Nome evento"),
                text: $vm.name
            )
            TextField(
                tr("events.add.field.description", fallback: "Descrizione"),
                text: $vm.description,
                axis: .vertical
            )
            .lineLimit(3...8)
            TextField(
                tr("events.add.field.info_link", fallback: "Link info (opzionale)"),
                text: $vm.infoLink
            )
            .keyboardType(.URL)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
        }
    }

    @ViewBuilder
    private var whereSection: some View {
        Section(tr("events.add.section.where", fallback: "Dove")) {
            Button {
                showLocationPicker = true
            } label: {
                HStack {
                    if let pos = vm.position {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(pos.name.isEmpty
                                 ? tr("events.add.location.unnamed", fallback: "Posizione")
                                 : pos.name)
                                .foregroundStyle(.primary)
                            if !pos.address.isEmpty {
                                Text(pos.address)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    } else {
                        Text(tr("events.add.location.pick", fallback: "Seleziona una posizione"))
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
        }
    }

    @ViewBuilder
    private var whenSection: some View {
        Section(tr("events.add.section.when", fallback: "Quando")) {
            DatePicker(
                tr("events.add.field.start", fallback: "Inizio"),
                selection: $vm.startDate,
                displayedComponents: [.date, .hourAndMinute]
            )
            DatePicker(
                tr("events.add.field.end", fallback: "Fine"),
                selection: $vm.endDate,
                in: vm.startDate...,
                displayedComponents: [.date, .hourAndMinute]
            )
        }
    }

    @ViewBuilder
    private var scheduleSection: some View {
        Section(tr("events.add.section.schedule", fallback: "Programma")) {
            Button {
                showScheduleList = true
            } label: {
                HStack {
                    Label(
                        tr("events.add.schedule.count", fallback: "Sessioni"),
                        systemImage: "calendar.badge.clock"
                    )
                    Spacer()
                    Text("\(visibleScheduleCount)")
                        .monospacedDigit()
                        .foregroundStyle(.secondary)
                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.tertiary)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: Toolbar

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .cancellationAction) {
            Button(tr("app.cancel", fallback: "Annulla")) { dismiss() }
                .disabled(vm.saving)
        }
        ToolbarItem(placement: .confirmationAction) {
            if vm.saving {
                ProgressView()
            } else {
                Button(
                    isEditing
                        ? tr("app.update", fallback: "Aggiorna")
                        : tr("app.save", fallback: "Salva")
                ) {
                    Task {
                        await vm.save(permissions: permissions, ownerId: ownerId)
                        if vm.dismissed { dismiss() }
                    }
                }
                .fontWeight(.semibold)
            }
        }
        if isEditing && permissions.allowControlEvents {
            ToolbarItem(placement: .destructiveAction) {
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

#Preview {
    NavigationStack { AddEventView() }
}
