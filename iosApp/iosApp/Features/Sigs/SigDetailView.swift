import SwiftUI
import Shared

// File-private bridge: Data → KotlinByteArray (same helper as
// SigListView.swift; redeclared here because that one is fileprivate).
private extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let result = KotlinByteArray(size: Int32(count))
        for (i, byte) in self.enumerated() {
            result.set(index: Int32(i), value: Int8(bitPattern: byte))
        }
        return result
    }
}

@MainActor @Observable
final class SigDetailViewModel {
    var sig: SigModel?
    var loading = false
    var error: String?
    var deleting = false
    private var sub: Closeable?

    /// Cache-first: subscribe to the list flow (filtered locally for our id)
    /// and fetch a fresh copy in parallel.
    func start(id: String) {
        sub = FlowBridgeKt.subscribe(
            flow: koin.sigs.observeAll(),
            onEach: { [weak self] value in
                let list = (value as? [SigModel]) ?? []
                let match = list.first(where: { $0.id == id })
                Task { @MainActor in
                    if let match { self?.sig = match }
                }
            },
            onError: { _ in }
        )
        Task { await load(id: id) }
    }

    func stop() { sub?.close() }

    func load(id: String) async {
        // Don't toggle loading if the cache already gave us a row.
        if sig == nil { loading = true }
        defer { loading = false }
        do {
            sig = try await koin.sigs.getById(id: id)
        } catch {
            self.error = (error as NSError).localizedDescription
        }
    }

    /// Mirrors `SigListViewModel.update` so the detail can show the same
    /// edit sheet without going back to the list.
    func update(id: String, payload: SigDraftPayload) async {
        let draft = SigDraft(
            name: payload.name,
            link: payload.link,
            groupType: payload.groupType.rawValue,
            description: "",
            imageBytes: payload.imageData?.toKotlinByteArray(),
            imageFilename: payload.imageData != nil ? (payload.imageFilename ?? "cover.jpg") : nil,
            imageContentType: payload.imageContentType ?? "image/jpeg"
        )
        do {
            sig = try await koin.sigs.update(id: id, draft: draft)
        } catch {
            self.error = (error as NSError).localizedDescription
        }
    }

    /// Returns true on success so the view can dismiss back to the list.
    func delete(id: String) async -> Bool {
        deleting = true
        defer { deleting = false }
        do {
            try await koin.sigs.delete(id: id)
            return true
        } catch {
            self.error = (error as NSError).localizedDescription
            return false
        }
    }
}

struct SigDetailView: View {
    let sigId: String
    @State private var vm = SigDetailViewModel()
    @State private var heroAppeared = false
    @State private var editSheet = false
    @State private var confirmDelete = false
    @Environment(\.dismiss) private var dismiss

    /// Mirrors `SigListViewModel.canControl` / `EventDetailView.canEditEvent`:
    /// read the auth StateFlow **synchronously** so the toolbar is correct
    /// on the very first frame — no Flow collector, no pop-in.
    private var canControl: Bool {
        hasPower("sigs", user: koin.auth.currentUser.value as? UserModel)
    }

    var body: some View {
        Group {
            if vm.loading && vm.sig == nil {
                LoadingDots()
            } else if let sig = vm.sig {
                content(sig)
            } else if let err = vm.error {
                ContentUnavailableView(err, systemImage: "exclamationmark.triangle")
            } else {
                LoadingDots()
            }
        }
        .navigationTitle(vm.sig?.name ?? "")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            // Modifica / Elimina visible from the first frame for users with
            // the `sigs` power. Voices stay disabled until the model arrives.
            ToolbarItem(placement: .topBarTrailing) {
                if canControl {
                    Menu {
                        Button {
                            editSheet = true
                        } label: {
                            Label(
                                tr("sigs.action.edit", fallback: "Modifica"),
                                systemImage: "pencil"
                            )
                        }
                        .disabled(vm.sig == nil)
                        Button(role: .destructive) {
                            confirmDelete = true
                        } label: {
                            Label(
                                tr("sigs.action.delete", fallback: "Elimina"),
                                systemImage: "trash"
                            )
                        }
                        .disabled(vm.sig == nil)
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                    .disabled(vm.deleting)
                }
            }
        }
        .sheet(isPresented: $editSheet) {
            if let s = vm.sig {
                AddSigSheet(
                    initial: s,
                    onSubmitted: { payload in
                        Task {
                            await vm.update(id: s.id, payload: payload)
                            editSheet = false
                        }
                    },
                    onDeleteRequested: {
                        editSheet = false
                        confirmDelete = true
                    },
                    onCancelled: { editSheet = false }
                )
            }
        }
        .alert(
            tr("sigs.delete.confirm.title", fallback: "Eliminare?"),
            isPresented: $confirmDelete,
            presenting: vm.sig
        ) { sig in
            Button(tr("app.cancel", fallback: "Annulla"), role: .cancel) { }
            Button(tr("sigs.action.delete", fallback: "Elimina"), role: .destructive) {
                Task {
                    let ok = await vm.delete(id: sig.id)
                    if ok { dismiss() }
                }
            }
        } message: { _ in
            Text(tr(
                "sigs.delete.confirm.body",
                fallback: "L'azione non è annullabile."
            ))
        }
        .alert(tr("app.error.title", fallback: "Errore"), isPresented: Binding(
            get: { vm.error != nil },
            set: { if !$0 { vm.error = nil } }
        )) {
            Button("OK") { vm.error = nil }
        } message: {
            Text(vm.error ?? "")
        }
        .task {
            vm.start(id: sigId)
            withAnimation(.spring(response: 0.55, dampingFraction: 0.85)) {
                heroAppeared = true
            }
        }
        .onDisappear { vm.stop() }
    }

    @ViewBuilder
    private func content(_ sig: SigModel) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                hero(sig)
                    .opacity(heroAppeared ? 1 : 0)
                    .offset(y: heroAppeared ? 0 : 16)

                VStack(alignment: .leading, spacing: 10) {
                    Text(sig.name)
                        .font(.title.bold())
                        .foregroundStyle(.primary)
                        .fixedSize(horizontal: false, vertical: true)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    if !sig.groupType.isEmpty {
                        HStack(spacing: 6) {
                            Image(systemName: sig.groupType.contains("facebook") ? "f.cursive.circle.fill" : "person.3.fill")
                            Text(sig.groupType.replacingOccurrences(of: "_", with: " ").capitalized)
                                .lineLimit(1)
                        }
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Capsule().fill(AppTheme.Colors.brandPrimary.opacity(0.12)))
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .opacity(heroAppeared ? 1 : 0)
                .offset(y: heroAppeared ? 0 : 12)
                .animation(.spring(response: 0.5, dampingFraction: 0.85).delay(0.06), value: heroAppeared)

                if !sig.description_.isEmpty {
                    GlassCard(padding: 16, cornerRadius: 20) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(tr("sigs.about", fallback: "Descrizione"))
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(.secondary)
                                .textCase(.uppercase)
                            Text(sig.description_)
                                .font(.body)
                                .foregroundStyle(.primary)
                                .multilineTextAlignment(.leading)
                                .fixedSize(horizontal: false, vertical: true)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .opacity(heroAppeared ? 1 : 0)
                    .offset(y: heroAppeared ? 0 : 12)
                    .animation(.spring(response: 0.5, dampingFraction: 0.85).delay(0.12), value: heroAppeared)
                }

                actions(sig)
                    .opacity(heroAppeared ? 1 : 0)
                    .offset(y: heroAppeared ? 0 : 12)
                    .animation(.spring(response: 0.5, dampingFraction: 0.85).delay(0.18), value: heroAppeared)

                Text(tr("sigs.members_unavailable", fallback: "L'elenco iscritti non è ancora disponibile da remoto."))
                    .font(.caption)
                    .foregroundStyle(.tertiary)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.top, 8)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
        }
    }

    @ViewBuilder
    private func hero(_ sig: SigModel) -> some View {
        // Preserve the image's natural aspect ratio: use `scaledToFit` and let
        // the rendered height derive from the screen width × image aspect.
        // For the empty / loading / no-image case we fall back to a 16:9 brand
        // gradient so the layout doesn't collapse to zero height.
        Group {
            if let url = imageURL(for: sig) {
                CachedAsyncImage(url: url) { img in
                    img.resizable().aspectRatio(contentMode: .fit)
                } placeholder: {
                    AppTheme.brandGradient
                        .aspectRatio(16.0/9.0, contentMode: .fit)
                }
            } else {
                AppTheme.brandGradient
                    .aspectRatio(16.0/9.0, contentMode: .fit)
            }
        }
        .frame(maxWidth: .infinity)
        .overlay {
            LinearGradient(
                colors: [.black.opacity(0.0), .black.opacity(0.35)],
                startPoint: .top,
                endPoint: .bottom
            )
        }
        .overlay(alignment: .bottomLeading) {
            HStack(spacing: 8) {
                Image(systemName: "star.fill")
                Text(tr("sigs.badge", fallback: "Community"))
                    .font(.caption.weight(.semibold))
            }
            .foregroundStyle(.white)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(.ultraThinMaterial, in: Capsule())
            .padding(12)
        }
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    /// Mirrors EventDetailView's pattern: short-circuit fully-qualified URLs,
    /// otherwise build a PocketBase file URL from the record metadata.
    private func imageURL(for sig: SigModel) -> URL? {
        guard !sig.image.isEmpty else { return nil }
        if sig.image.hasPrefix("http") { return URL(string: sig.image) }
        return Files.url(
            collection: "sigs",
            recordId: sig.id,
            filename: sig.image,
            thumb: "1200x0"
        )
    }

    /// Single CTA: merge of the previous placeholder "Iscriviti" button and the
    /// "Visita la pagina" link. The backend has no real join/leave endpoint,
    /// so joining IS opening the external link.
    @ViewBuilder
    private func actions(_ sig: SigModel) -> some View {
        if !sig.link.isEmpty, let url = URL(string: sig.link) {
            Link(destination: url) {
                HStack(spacing: 10) {
                    Image(systemName: sig.groupType.contains("facebook")
                          ? "f.cursive.circle.fill"
                          : "person.fill.badge.plus")
                    Text(sig.groupType.contains("facebook")
                         ? tr("sigs.join_facebook", fallback: "Unisciti al gruppo Facebook")
                         : tr("sigs.join", fallback: "Unisciti alla community"))
                        .fontWeight(.semibold)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(AppTheme.brandGradient, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                .foregroundStyle(.white)
            }
        }
    }
}
