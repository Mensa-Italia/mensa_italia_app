import SwiftUI
import Shared

/// Account sospeso: il server risponde 403 `account_blocked` a ogni richiesta.
///
/// Prima non c'era uno stato per questo: ogni schermata mostrava il suo errore
/// generico, e un logout avrebbe solo portato a un nuovo accesso, subito
/// rifiutato allo stesso modo. Qui si resta fermi con un messaggio chiaro.
/// "Riprova" rilegge il socio dal server: se la sospensione è stata tolta,
/// `AccountStatus` (:shared) si spegne e `RootView` cambia fase da sola. Stile
/// del muro del rinnovo (`OfflineBlockedView`).
/// Mirrors Android `AccountSuspendedScreen`.
struct AccountSuspendedView: View {
    @State private var checking = false
    @State private var stillSuspended = false
    @State private var loggingOut = false
    /// Conferma di un'app esterna aperta da una push. Un account sospeso resta
    /// socio valido per le app esterne (il server le tiene nell'allowlist), quindi
    /// la push deve poter aprire il foglio anche qui, dove `MainTabView` non c'è.
    @State private var confirmation: NotificationTarget?
    @ObservedObject private var pendingDeepLink = PendingDeepLink.shared

    var body: some View {
        // Un NavigationStack tutto suo: da qui si raggiunge solo l'elenco delle
        // notifiche (e il suo dettaglio), con le viste di sempre in modalità
        // `restricted`. La shell dell'app non c'è.
        NavigationStack {
            home
                .navigationDestination(isPresented: $showNotifications) {
                    NotificationsListView(restricted: true)
                }
        }
    }

    @State private var showNotifications = false

    private var home: some View {
        ZStack {
            AppTheme.brandGradient.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    Spacer(minLength: 40)

                    MensaMark(size: 88, inBlueBadge: false)
                        .accessibilityLabel("Mensa Italia")

                    Image(systemName: "nosign")
                        .font(.system(size: 30, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 64, height: 64)
                        .background(Color.white.opacity(0.18), in: Circle())

                    Text(tr("app.account.suspended.title", fallback: "Account sospeso"))
                        .font(.title2.bold())
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.center)

                    Text(tr("app.account.suspended.body",
                            fallback: "Il tuo account è stato sospeso e non puoi usare l'app. Per informazioni contatta la segreteria di Mensa Italia."))
                        .font(.subheadline)
                        .foregroundStyle(.white.opacity(0.88))
                        .multilineTextAlignment(.center)

                    retryButton

                    if stillSuspended {
                        Text(tr("app.account.suspended.still", fallback: "L'account risulta ancora sospeso."))
                            .font(.footnote)
                            .foregroundStyle(.white.opacity(0.9))
                    }

                    // Le notifiche restano leggibili (allowlist del server): chi si è
                    // perso la richiesta di conferma di un'app esterna la riapre da lì.
                    Button {
                        showNotifications = true
                    } label: {
                        Text(tr("app.account.suspended.notifications", fallback: "Notifiche"))
                            .font(.callout.weight(.medium))
                            .foregroundStyle(.white)
                    }
                    .padding(.top, 4)

                    Button {
                        guard !loggingOut else { return }
                        loggingOut = true
                        Task { try? await koin.auth.logout() }
                    } label: {
                        Text(tr("views.settings.tile.logout.title", fallback: "Esci"))
                            .font(.callout.weight(.medium))
                            .foregroundStyle(.white.opacity(0.9))
                    }
                    .padding(.top, 4)

                    Spacer(minLength: 40)
                }
                .padding(.horizontal, 28)
                .frame(maxWidth: .infinity)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .sheet(item: $confirmation) { target in
            ExAppConfirmationSheet(target: target, onDismiss: { confirmation = nil })
        }
        .onAppear { adoptPendingConfirmation() }
        .onChange(of: pendingDeepLink.target) { _, _ in adoptPendingConfirmation() }
        .onReceive(NotificationCenter.default.publisher(for: .mensaDeepLink)) { note in
            if let target = note.userInfo?[PushDeepLinkRouter.payloadKey] as? NotificationTarget,
               target.isExAppConfirmation {
                confirmation = target
                _ = pendingDeepLink.consume()
            }
        }
    }

    /// Prende dalla coda solo le conferme; ogni altro target resta dov'è, e lo
    /// raccoglie `MainTabView` quando l'account torna normale.
    private func adoptPendingConfirmation() {
        if let pending = pendingDeepLink.target, pending.isExAppConfirmation {
            confirmation = pendingDeepLink.consume()
        }
    }

    private var retryButton: some View {
        Button {
            guard !checking else { return }
            checking = true
            stillSuspended = false
            Task {
                // Se la sospensione è stata tolta, la fase cambia e questa view sparisce.
                let active = (try? await koin.auth.recheckAccount())?.boolValue ?? false
                stillSuspended = !active
                checking = false
            }
        } label: {
            HStack(spacing: 10) {
                if checking {
                    ProgressView().tint(.white).controlSize(.small)
                }
                Text(tr("app.account.suspended.retry", fallback: "Riprova"))
                    .fontWeight(.semibold)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .overlay(
                RoundedRectangle(cornerRadius: AppTheme.Radius.button)
                    .strokeBorder(.white.opacity(0.9), lineWidth: 1.5)
            )
            .foregroundStyle(.white)
        }
        .disabled(checking)
    }
}
