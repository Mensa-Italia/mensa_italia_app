import SwiftUI
import Shared

/// Third-party data-access approval prompt. Mirrors Flutter's
/// `bottom_check_identity.dart`: shows the requesting `ExApp` (name, image,
/// description) and offers Approva/Rifiuta. Approva grants the
/// `CHECK_USER_EXISTENCE` permission, then POSTs `{accepted: …}` to the
/// caller's callback URL. Either decision marks the originating notification
/// (when present) as seen.
struct AccountConfirmationSheet: View {
    let exAppId: String
    let callbackUrl: String
    let notificationId: String?
    let onDismiss: () -> Void

    @State private var exApp: ExAppModel?
    @State private var loading = true
    @State private var submitting = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            content
                .navigationTitle(tr("ex_app.confirm.title", fallback: "Conferma identità"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button(tr("app.close", fallback: "Chiudi")) { onDismiss() }
                            .disabled(submitting)
                    }
                }
        }
        .presentationDetents([.medium, .large])
        .task { await load() }
    }

    @ViewBuilder
    private var content: some View {
        if loading {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let app = exApp {
            ScrollView {
                VStack(spacing: 24) {
                    Text(app.name ?? "")
                        .font(.title2.bold())
                        .multilineTextAlignment(.center)

                    if let id = app.id, let filename = app.image, !filename.isEmpty,
                       let url = Files.url(collection: "ex_apps", recordId: id, filename: filename) {
                        CachedAsyncImage(url: url) { img in
                            img.resizable().aspectRatio(contentMode: .fit)
                        } placeholder: {
                            AppTheme.brandGradient
                        }
                        .frame(width: 180, height: 180)
                        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                    }

                    Text(app.description_ ??
                         tr("ex_app.confirm.description.fallback",
                            fallback: "L'app richiede di verificare la tua identità Mensa."))
                        .font(.body)
                        .multilineTextAlignment(.center)
                        .foregroundStyle(.secondary)
                        .padding(.horizontal, 8)

                    VStack(spacing: 12) {
                        Button {
                            Task { await approve() }
                        } label: {
                            label(tr("ex_app.confirm.approve", fallback: "Approva"))
                        }
                        .buttonStyle(.glassProminent)
                        .tint(AppTheme.Colors.mensaBlue)
                        .disabled(submitting)

                        Button {
                            Task { await deny() }
                        } label: {
                            label(tr("ex_app.confirm.deny", fallback: "Rifiuta"))
                        }
                        .buttonStyle(.bordered)
                        .tint(.secondary)
                        .disabled(submitting)
                    }
                    .padding(.top, 8)
                }
                .padding(24)
            }
        } else {
            ContentUnavailableView {
                Label(
                    tr("ex_app.confirm.error.title", fallback: "Impossibile caricare l'app"),
                    systemImage: "exclamationmark.triangle"
                )
            } description: {
                Text(errorMessage ?? tr("ex_app.confirm.error.body",
                                        fallback: "Riprova più tardi."))
            } actions: {
                Button(tr("app.retry", fallback: "Riprova")) {
                    Task { await load() }
                }
                .buttonStyle(.glassProminent)
            }
        }
    }

    @ViewBuilder
    private func label(_ text: String) -> some View {
        if submitting {
            ProgressView()
                .frame(maxWidth: .infinity)
                .padding(.vertical, 6)
        } else {
            Text(text)
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 4)
        }
    }

    // MARK: - Actions

    private func load() async {
        loading = true
        errorMessage = nil
        do {
            exApp = try await koin.exApps.getExApp(appId: exAppId)
        } catch {
            errorMessage = error.localizedDescription
            exApp = nil
        }
        loading = false
    }

    private func approve() async {
        submitting = true
        defer { submitting = false }
        do {
            _ = try await koin.exApps.addPermissions(
                appId: exAppId,
                perms: ["CHECK_USER_EXISTENCE"]
            )
            try await koin.exApps.postCallback(url: callbackUrl, accepted: true)
            await markNotificationSeen()
            onDismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func deny() async {
        submitting = true
        defer { submitting = false }
        do {
            try await koin.exApps.postCallback(url: callbackUrl, accepted: false)
            await markNotificationSeen()
            onDismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func markNotificationSeen() async {
        guard let id = notificationId else { return }
        try? await koin.notifications.markSeen(id: id)
    }
}
