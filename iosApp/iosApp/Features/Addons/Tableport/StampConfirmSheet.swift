import SwiftUI
import Shared

/// Confirmation sheet shown after a successful QR scan.
/// Fetches the stamp via `koin.access.stampsApi.getStamp(id:, code:)`,
/// then on confirm calls `addStamp` and triggers a stamps refresh.
struct StampConfirmSheet: View {
    let stampId: String
    let code: String
    let onDone: () -> Void

    @State private var stamp: StampModel? = nil
    @State private var loading = true
    @State private var saving = false
    @State private var errorMessage: String? = nil

    var body: some View {
        NavigationStack {
            content
                .navigationTitle(tr("addons.stamp.confirm.title", fallback: "Nuovo francobollo"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button(tr("app.cancel", fallback: "Annulla")) { onDone() }
                    }
                }
        }
        .task { await loadStamp() }
        .alert(
            tr("app.error.title", fallback: "Errore"),
            isPresented: Binding(
                get: { errorMessage != nil },
                set: { if !$0 { errorMessage = nil } }
            )
        ) {
            Button("OK") { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
    }

    @ViewBuilder
    private var content: some View {
        if loading {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let stamp {
            ScrollView {
                VStack(spacing: 18) {
                    if !stamp.image.isEmpty,
                       let url = Files.url(
                            collection: "stamps",
                            recordId: stamp.id,
                            filename: stamp.image,
                            thumb: "800x600"
                       ) {
                        CachedAsyncImage(url: url) { img in
                            img.resizable().aspectRatio(contentMode: .fill)
                        } placeholder: {
                            AppTheme.brandGradient
                        }
                        .frame(height: 220)
                        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                    }

                    Text(stamp.description_.isEmpty ? stamp.id : stamp.description_)
                        .font(.title3.bold())
                        .multilineTextAlignment(.center)

                    Button {
                        Task { await confirm(stamp: stamp) }
                    } label: {
                        if saving {
                            ProgressView()
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 8)
                        } else {
                            Text(tr("addons.stamp.confirm.cta", fallback: "Conferma"))
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 4)
                        }
                    }
                    .buttonStyle(.glassProminent)
                    .tint(AppTheme.Colors.mensaBlue)
                    .disabled(saving)
                }
                .padding(20)
            }
        } else {
            ContentUnavailableView(
                tr("addons.stamp.confirm.not_found", fallback: "Francobollo non trovato"),
                systemImage: "exclamationmark.triangle",
                description: Text(tr(
                    "addons.stamp.confirm.not_found_description",
                    fallback: "Il QR scansionato non corrisponde a un francobollo valido."
                ))
            )
        }
    }

    private func loadStamp() async {
        loading = true
        defer { loading = false }
        do {
            let result = try await koin.stamps.verify(id: stampId, code: code)
            self.stamp = result
        } catch {
            self.errorMessage = (error as NSError).localizedDescription
        }
    }

    private func confirm(stamp: StampModel) async {
        saving = true
        defer { saving = false }
        guard let user = koin.auth.currentUser.value as? UserModel, !user.id.isEmpty else {
            errorMessage = tr(
                "addons.stamp.confirm.no_user",
                fallback: "Devi essere autenticato per aggiungere un francobollo."
            )
            return
        }
        do {
            try await koin.stamps.claim(stampId: stamp.id, code: code)
            onDone()
        } catch {
            errorMessage = (error as NSError).localizedDescription
        }
    }
}
