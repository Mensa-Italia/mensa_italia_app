import SwiftUI
import PhotosUI
import Shared

/// Group-type keys recognised by the Mensa Italia backend.
/// Order in the picker matches the Flutter app verbatim.
enum SigGroupType: String, CaseIterable, Identifiable {
    case sigFacebook  = "sig_facebook"
    case sigGeneric   = "sig"
    case local        = "local"
    case chatWhatsapp = "chat_whatsapp"
    case chatTelegram = "chat_telegram"
    case chat         = "chat"

    var id: String { rawValue }

    /// Human-readable label (Italian fallback) — uses `tr(key, fallback:)`.
    var label: String {
        switch self {
        case .sigFacebook:  return tr("sigs.type.sig_facebook",  fallback: "SIG Facebook")
        case .sigGeneric:   return tr("sigs.type.sig",           fallback: "SIG Generic")
        case .local:        return tr("sigs.type.local",         fallback: "Gruppo locale")
        case .chatWhatsapp: return tr("sigs.type.chat_whatsapp", fallback: "Chat WhatsApp")
        case .chatTelegram: return tr("sigs.type.chat_telegram", fallback: "Chat Telegram")
        case .chat:         return tr("sigs.type.chat",          fallback: "Chat")
        }
    }

    /// SF Symbol for the type — used as a leading glyph in the picker and list rows elsewhere.
    var systemImage: String {
        switch self {
        case .sigFacebook:  return "f.circle.fill"
        case .sigGeneric:   return "person.3.fill"
        case .local:        return "mappin.and.ellipse"
        case .chatWhatsapp: return "bubble.left.and.bubble.right.fill"
        case .chatTelegram: return "paperplane.fill"
        case .chat:         return "message.fill"
        }
    }

    init?(rawString: String) { self.init(rawValue: rawString) }
}

/// Payload returned by `AddSigSheet` to its caller.
/// `imageData == nil` in edit mode means "keep existing image".
struct SigDraftPayload {
    var name: String
    var link: String
    var groupType: SigGroupType
    var imageData: Data?
    var imageFilename: String?
    var imageContentType: String?
}

struct AddSigSheet: View {
    /// nil → create. non-nil → edit (form is pre-populated).
    var initial: SigModel?

    /// Called when the user taps "Crea" / "Aggiorna" with the assembled payload.
    /// Caller is responsible for persisting via `koin.sigs.create/update` and dismissing.
    var onSubmitted: (SigDraftPayload) -> Void

    /// Called when the user taps the destructive delete CTA (only present in edit mode).
    /// Caller persists the delete via `koin.sigs.delete(id:)` and dismisses.
    var onDeleteRequested: () -> Void = {}

    /// Called on cancel. Default no-op.
    var onCancelled: () -> Void = {}

    @State private var name: String
    @State private var link: String
    @State private var groupType: SigGroupType
    @State private var imageData: Data?
    @State private var imageFilename: String?
    @State private var imageContentType: String?
    @State private var pickerItem: PhotosPickerItem?
    @State private var showPhotoPicker: Bool = false
    @State private var showDeleteConfirm: Bool = false

    init(
        initial: SigModel? = nil,
        onSubmitted: @escaping (SigDraftPayload) -> Void,
        onDeleteRequested: @escaping () -> Void = {},
        onCancelled: @escaping () -> Void = {}
    ) {
        self.initial = initial
        self.onSubmitted = onSubmitted
        self.onDeleteRequested = onDeleteRequested
        self.onCancelled = onCancelled

        _name = State(initialValue: initial?.name ?? "")
        _link = State(initialValue: initial?.link ?? "")
        let seedType: SigGroupType = {
            if let raw = initial?.groupType, let t = SigGroupType(rawValue: raw) {
                return t
            }
            return .sigGeneric
        }()
        _groupType = State(initialValue: seedType)
        _imageData = State(initialValue: nil)
        _imageFilename = State(initialValue: nil)
        _imageContentType = State(initialValue: nil)
    }

    private var isEditing: Bool { initial != nil }

    private var trimmedName: String {
        name.trimmingCharacters(in: .whitespacesAndNewlines)
    }
    private var trimmedLink: String {
        link.trimmingCharacters(in: .whitespacesAndNewlines)
    }
    private var canSubmit: Bool {
        !trimmedName.isEmpty && !trimmedLink.isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    coverPicker
                        .listRowInsets(EdgeInsets(top: 8, leading: 8, bottom: 8, trailing: 8))
                        .listRowBackground(Color.clear)
                }

                Section(tr("sigs.section.details", fallback: "Dettagli")) {
                    TextField(
                        tr("sigs.field.name", fallback: "Nome"),
                        text: $name
                    )
                    TextField(
                        tr("sigs.field.link", fallback: "Link"),
                        text: $link
                    )
                    .keyboardType(.URL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                }

                Section(tr("sigs.section.type", fallback: "Tipo")) {
                    Picker(
                        tr("sigs.section.type", fallback: "Tipo"),
                        selection: $groupType
                    ) {
                        ForEach(SigGroupType.allCases) { type in
                            Label(type.label, systemImage: type.systemImage)
                                .tag(type)
                        }
                    }
                    .pickerStyle(.menu)
                }

                if isEditing {
                    Section {
                        Button(role: .destructive) {
                            showDeleteConfirm = true
                        } label: {
                            Text(tr("sigs.delete", fallback: "Elimina community"))
                                .frame(maxWidth: .infinity, alignment: .center)
                        }
                    }
                }
            }
            .navigationTitle(
                isEditing
                    ? tr("sigs.edit.title", fallback: "Modifica community")
                    : tr("sigs.add.title", fallback: "Nuova community")
            )
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(tr("app.cancel", fallback: "Annulla")) {
                        onCancelled()
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(
                        isEditing
                            ? tr("app.update", fallback: "Aggiorna")
                            : tr("app.create", fallback: "Crea")
                    ) {
                        submit()
                    }
                    .disabled(!canSubmit)
                }
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
                        await MainActor.run {
                            imageData = data
                            imageFilename = "cover.jpg"
                            imageContentType = "image/jpeg"
                        }
                    }
                    await MainActor.run { pickerItem = nil }
                }
            }
            .confirmationDialog(
                tr("sigs.delete.confirm.title", fallback: "Eliminare questa community?"),
                isPresented: $showDeleteConfirm,
                titleVisibility: .visible
            ) {
                Button(
                    tr("sigs.delete", fallback: "Elimina community"),
                    role: .destructive
                ) {
                    onDeleteRequested()
                }
                Button(tr("app.cancel", fallback: "Annulla"), role: .cancel) { }
            } message: {
                Text(tr(
                    "sigs.delete.confirm.body",
                    fallback: "Questa azione non è annullabile."
                ))
            }
        }
    }

    // MARK: - Cover picker

    @ViewBuilder
    private var coverPicker: some View {
        Button {
            showPhotoPicker = true
        } label: {
            coverContent
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var coverContent: some View {
        ZStack {
            if let data = imageData, let ui = UIImage(data: data) {
                Image(uiImage: ui)
                    .resizable()
                    .scaledToFill()
            } else if let url = remoteCoverURL() {
                CachedAsyncImage(url: url) { img in
                    img.resizable().scaledToFill()
                } placeholder: {
                    placeholderBackground
                }
            } else {
                placeholderBackground
                VStack(spacing: 6) {
                    Image(systemName: "photo.badge.plus")
                        .font(.title2)
                        .foregroundStyle(.secondary)
                    Text(tr("sigs.cover.pick", fallback: "Tocca per scegliere una copertina"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 16)
                }
            }

            if isEditing, imageData != nil {
                VStack {
                    HStack {
                        Spacer()
                        Button {
                            imageData = nil
                            imageFilename = nil
                            imageContentType = nil
                        } label: {
                            Label(
                                tr("sigs.cover.remove", fallback: "Rimuovi"),
                                systemImage: "xmark.circle.fill"
                            )
                            .labelStyle(.titleAndIcon)
                            .font(.caption.weight(.semibold))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(.ultraThinMaterial, in: Capsule())
                            .foregroundStyle(.primary)
                        }
                        .buttonStyle(.plain)
                        .padding(10)
                    }
                    Spacer()
                }
            }
        }
        .aspectRatio(1528.0 / 603.0, contentMode: .fit)
        .frame(maxWidth: .infinity)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .contentShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }

    @ViewBuilder
    private var placeholderBackground: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(Color(.secondarySystemBackground))
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .strokeBorder(
                    Color.secondary.opacity(0.5),
                    style: StrokeStyle(lineWidth: 1.5, dash: [4, 4])
                )
        }
    }

    private func remoteCoverURL() -> URL? {
        guard let sig = initial, !sig.image.isEmpty else { return nil }
        if sig.image.hasPrefix("http") { return URL(string: sig.image) }
        return Files.url(
            collection: "sigs",
            recordId: sig.id,
            filename: sig.image,
            thumb: "1500x600"
        )
    }

    // MARK: - Submit

    private func submit() {
        guard canSubmit else { return }
        let payload = SigDraftPayload(
            name: trimmedName,
            link: trimmedLink,
            groupType: groupType,
            imageData: imageData,
            imageFilename: imageFilename,
            imageContentType: imageContentType
        )
        onSubmitted(payload)
    }
}
