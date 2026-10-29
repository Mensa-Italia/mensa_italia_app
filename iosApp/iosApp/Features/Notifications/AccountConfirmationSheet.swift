import SwiftUI
import Shared

/// Third-party approval prompt. Mirrors Flutter's `bottom_check_identity.dart`:
/// shows the requesting `ExApp` (name, image) and offers Approva/Rifiuta, con
/// un corpo che dipende da [Kind]:
///
///  - `.identity`: Approva concede il permesso `CHECK_USER_EXISTENCE`, poi fa
///    POST `{accepted: …}` al callback del chiamante;
///  - `.dataExport`: esportazione una tantum di timbri o medaglie. Qualunque
///    risposta va prima al server (`export/decide`), poi all'app esterna con
///    `{accepted, request_id}`.
///
/// Ogni decisione segna come vista la notifica di origine (se c'è). Entrambe
/// funzionano per un account sospeso: il server le tiene nell'allowlist.
/// Mirrors Android `AccountConfirmationSheet`.
struct AccountConfirmationSheet: View {
    /// Cosa si chiede di confermare. Per aggiungere un tipo di richiesta: un
    /// caso qui, il suo corpo in `bodyContent` e la sua risposta in `answer`.
    enum Kind {
        case identity
        case dataExport(requestId: String, scope: String)
    }

    let exAppId: String
    let callbackUrl: String
    let notificationId: String?
    var kind: Kind = .identity
    let onDismiss: () -> Void

    @State private var exApp: ExAppModel?
    @State private var loading = true
    @State private var submitting = false
    @State private var errorMessage: String?
    /// Richiesta di esportazione scaduta o già decisa: riprovare darebbe lo stesso 404/409.
    @State private var gone = false

    private var title: String {
        switch kind {
        case .identity: return tr("ex_app.confirm.title", fallback: "Conferma identità")
        case .dataExport: return tr("ex_app.export.title", fallback: "Esporta i tuoi dati")
        }
    }

    var body: some View {
        NavigationStack {
            content
                .navigationTitle(title)
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

                    bodyContent(for: app)

                    // Una risposta non andata a buon fine: si resta qui, con i
                    // pulsanti per riprovare. Prima l'errore finiva in uno stato
                    // che questa vista non mostra.
                    if let errorMessage {
                        Text(errorMessage)
                            .font(.subheadline)
                            .foregroundStyle(.red)
                            .multilineTextAlignment(.center)
                    }

                    VStack(spacing: 12) {
                        Button {
                            Task { await approve() }
                        } label: {
                            label(approveTitle)
                        }
                        .compatGlassProminentButtonStyle()
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
                    gone
                        ? tr("ex_app.export.error.title", fallback: "Richiesta non più valida")
                        : tr("ex_app.confirm.error.title", fallback: "Impossibile caricare l'app"),
                    systemImage: "exclamationmark.triangle"
                )
            } description: {
                Text(errorMessage ?? tr("ex_app.confirm.error.body",
                                        fallback: "Riprova più tardi."))
            } actions: {
                if !gone {
                    Button(tr("app.retry", fallback: "Riprova")) {
                        Task { await load() }
                    }
                    .compatGlassProminentButtonStyle()
                }
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
            errorMessage = error.userMessage
            exApp = nil
        }
        loading = false
    }

    private func approve() async { await answer(accepted: true) }

    private func deny() async { await answer(accepted: false) }

    private func answer(accepted: Bool) async {
        submitting = true
        errorMessage = nil
        defer { submitting = false }
        do {
            switch kind {
            case .identity:
                if accepted {
                    _ = try await koin.exApps.addPermissions(
                        appId: exAppId,
                        perms: ["CHECK_USER_EXISTENCE"]
                    )
                }
                try await koin.exApps.postCallback(url: callbackUrl, accepted: accepted)
            case .dataExport(let requestId, _):
                _ = try await koin.exApps.answerDataExport(
                    requestId: requestId,
                    callbackUrl: callbackUrl,
                    accepted: accepted
                )
            }
            await markNotificationSeen()
            onDismiss()
        } catch {
            errorMessage = error.userMessage
            // Non c'è più niente da rifare: si passa all'errore con solo "Chiudi",
            // non al pulsante Approva.
            if let kotlin = (error as NSError).userInfo["KotlinException"] as? KotlinThrowable,
               kotlin is ExportRequestGoneException {
                gone = true
                exApp = nil
            }
        }
    }

    // MARK: - Corpo per tipo di richiesta

    private var approveTitle: String {
        switch kind {
        case .identity: return tr("ex_app.confirm.approve", fallback: "Approva")
        case .dataExport: return tr("ex_app.export.approve", fallback: "Consenti l'estrazione")
        }
    }

    @ViewBuilder
    private func bodyContent(for app: ExAppModel) -> some View {
        switch kind {
        case .identity:
            Text(app.description_ ??
                 tr("ex_app.confirm.description.fallback",
                    fallback: "L'app richiede di verificare la tua identità Mensa."))
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 8)
        case .dataExport(_, let scope):
            // Cosa lascia l'app, detto chiaramente, e che l'estrazione è una sola.
            // Un nuovo tipo di dato = un caso di `DataExportScope` e la sua riga qui.
            let what = scope == DataExportScope.medals.wire
                ? tr("ex_app.export.scope.medals", fallback: "le tue medaglie")
                : tr("ex_app.export.scope.stamps", fallback: "i tuoi timbri")
            VStack(spacing: 12) {
                Text(tr("ex_app.export.description",
                        fallback: "{app} chiede di ricevere {what}.",
                        ["app": app.name ?? "", "what": what]))
                    .font(.headline)
                    .multilineTextAlignment(.center)
                Text(tr("ex_app.export.one_time",
                        fallback: "È un'estrazione una tantum: i dati vengono consegnati una sola volta, e per ogni nuova richiesta ti chiederemo di nuovo il consenso. La richiesta scade tra pochi minuti."))
                    .font(.body)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 8)
        }
    }

    private func markNotificationSeen() async {
        guard let id = notificationId else { return }
        try? await koin.notifications.markSeen(id: id)
    }
}

extension NotificationTarget {
    /// Ha un foglio di conferma di un'app esterna (identità o esportazione dati).
    var isExAppConfirmation: Bool {
        switch self {
        case .accountConfirmation, .dataExportConfirmation: return true
        default: return false
        }
    }
}

/// Il foglio giusto per un `NotificationTarget` di conferma: un solo punto dove
/// le viste che presentano un target (tab, Today, lista, dettaglio, account
/// sospeso) scelgono il tipo di richiesta. Per ogni altro target è vuoto.
struct ExAppConfirmationSheet: View {
    let target: NotificationTarget
    let onDismiss: () -> Void

    var body: some View {
        switch target {
        case .accountConfirmation(let appId, let url, let nid):
            AccountConfirmationSheet(exAppId: appId, callbackUrl: url, notificationId: nid, onDismiss: onDismiss)
        case .dataExportConfirmation(let requestId, let appId, let scope, let url, let nid):
            AccountConfirmationSheet(
                exAppId: appId,
                callbackUrl: url,
                notificationId: nid,
                kind: .dataExport(requestId: requestId, scope: scope),
                onDismiss: onDismiss
            )
        default:
            EmptyView()
        }
    }
}
