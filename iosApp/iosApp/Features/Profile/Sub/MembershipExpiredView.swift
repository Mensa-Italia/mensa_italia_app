import SwiftUI
import Shared

/// Muro del rinnovo.
///
/// Con la tessera scaduta l'app si apriva normalmente e la scadenza si vedeva
/// solo entrando in Profilo → Tessera. Adesso questa è l'unica schermata
/// disponibile: si rinnova sul portale, si ricontrolla, oppure si esce.
///
/// Non è una pagina dentro l'app: è un caso di `RootPhase`, quindi non ha
/// navigazione, non ha back e non ha tab bar da cui rientrare.
struct MembershipExpiredView: View {
    @State private var user: UserModel? = koin.auth.currentUser.value as? UserModel
    @State private var userSub: Closeable?
    @State private var checking = false
    @State private var loggingOut = false

    /// Esito dell'ultimo "Ho rinnovato". Nessun caso "andata bene": se il
    /// rinnovo risulta, `RootView` cambia fase e questa view sparisce, quindi
    /// un messaggio di successo non farebbe in tempo a vedersi.
    private enum RecheckOutcome { case stillExpired, unreachable, needsPassword, wrongPassword }
    @State private var recheck: RecheckOutcome?

    /// Password dell'area soci, chiesta solo quando la scadenza arriva dal
    /// database e non dal portale (vedi `AuthRepository.recheckMembership`).
    @State private var password = ""
    @FocusState private var passwordFocused: Bool

    private var asksPassword: Bool {
        recheck == .needsPassword || recheck == .wrongPassword
    }

    private var renewURL: URL? { URL(string: Membership.shared.RENEWAL_URL) }

    private var expiryString: String {
        guard let u = user, u.expireMembership.epochSeconds > 0 else { return "—" }
        return formatAppDate(u.expireMembership)
    }

    /// Giorni di ritardo. `daysUntilExpiry` è negativo dopo la scadenza.
    private var daysOverdue: Int? {
        guard let days = Membership.shared.daysUntilExpiry(user: user)?.intValue,
              days < 0 else { return nil }
        return -days
    }

    var body: some View {
        ZStack {
            AppTheme.brandGradient.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    Spacer(minLength: 40)

                    MensaMark(size: 88, inBlueBadge: false)
                        .accessibilityLabel("Mensa Italia")

                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 34, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 64, height: 64)
                        .background(Color.white.opacity(0.18), in: Circle())

                    Text(tr("app.renew.gate.title", fallback: "La tua tessera è scaduta"))
                        .font(.title2.bold())
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.center)

                    Text(tr("app.renew.gate.body",
                            fallback: "Rinnova l'iscrizione per tornare a usare l'app. Il rinnovo si completa sul portale dei soci: appena risulta registrato, tocca «Ho rinnovato»."))
                        .font(.subheadline)
                        .foregroundStyle(.white.opacity(0.88))
                        .multilineTextAlignment(.center)

                    expiryCard

                    renewButton
                    recheckButton
                    recheckNotice
                    if asksPassword { passwordForm }

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
        .task {
            // La scadenza cambia sotto i piedi quando `refreshCurrentUser()`
            // porta a casa il rinnovo: seguiamo il flow invece di leggere il
            // valore una volta sola.
            let flow = koin.auth.currentUser as Kotlinx_coroutines_coreFlow
            userSub = subscribeOptionalFlow(flow) { (u: UserModel?) in
                Task { @MainActor in user = u }
            } onError: { _ in }
        }
        .onDisappear {
            userSub?.close()
            userSub = nil
        }
    }

    private func noticeText(_ outcome: RecheckOutcome) -> String {
        switch outcome {
        case .stillExpired:
            return tr("app.renew.gate.still_expired",
                      fallback: "Il rinnovo non risulta ancora registrato. Se lo hai appena pagato può volerci qualche minuto.")
        case .unreachable:
            return tr("app.renew.gate.unreachable",
                      fallback: "Non riesco a contattare il server. Controlla la connessione e riprova.")
        case .needsPassword:
            return tr("app.renew.gate.needs_password",
                      fallback: "Sul portale risulti in regola? L'app rilegge la scadenza dall'area soci solo quando accedi con la password. Scrivila qui sotto per aggiornarla.")
        case .wrongPassword:
            return tr("app.renew.gate.wrong_password",
                      fallback: "Password non accettata. È quella con cui entri nell'area soci.")
        }
    }

    private func noticeIcon(_ outcome: RecheckOutcome) -> String {
        switch outcome {
        case .stillExpired:  return "clock.badge.exclamationmark"
        case .unreachable:   return "wifi.exclamationmark"
        case .needsPassword: return "key.fill"
        case .wrongPassword: return "exclamationmark.lock.fill"
        }
    }

    private func outcome(_ result: MembershipRecheck) -> RecheckOutcome? {
        switch result {
        case .renewed:        return nil
        case .stillExpired:   return .stillExpired
        case .needsPassword:  return .needsPassword
        case .wrongPassword:  return .wrongPassword
        default:              return .unreachable
        }
    }

    private var passwordForm: some View {
        VStack(spacing: 12) {
            SecureField(
                "",
                text: $password,
                prompt: Text(tr("app.renew.gate.password_placeholder", fallback: "Password dell'area soci"))
                    .foregroundStyle(.white.opacity(0.6))
            )
            .textContentType(.password)
            .focused($passwordFocused)
            .submitLabel(.go)
            .onSubmit(submitPassword)
            .foregroundStyle(.white)
            .padding(14)
            .background(Color.white.opacity(0.14), in: RoundedRectangle(cornerRadius: 14))

            Button(action: submitPassword) {
                HStack(spacing: 10) {
                    if checking {
                        ProgressView().tint(AppTheme.Colors.mensaBlue).controlSize(.small)
                    }
                    Text(tr("app.renew.gate.password_submit", fallback: "Aggiorna la scadenza"))
                        .fontWeight(.semibold)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(.white, in: RoundedRectangle(cornerRadius: AppTheme.Radius.button))
                .foregroundStyle(AppTheme.Colors.mensaBlue)
            }
            .disabled(checking || password.isEmpty)
            .opacity(password.isEmpty ? 0.6 : 1)
        }
    }

    private func submitPassword() {
        guard !checking, !password.isEmpty else { return }
        checking = true
        passwordFocused = false
        let typed = password
        Task {
            let result = (try? await koin.auth.recheckMembershipWithPassword(password: typed)) ?? .unreachable
            recheck = outcome(result)
            if recheck == .wrongPassword { password = "" }
            checking = false
        }
    }

    @ViewBuilder
    private var recheckNotice: some View {
        if let recheck {
            Label(noticeText(recheck), systemImage: noticeIcon(recheck))
                .font(.footnote)
                .foregroundStyle(.white.opacity(0.9))
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(14)
                .background(Color.white.opacity(0.14),
                            in: RoundedRectangle(cornerRadius: 14))
        }
    }

    private var expiryCard: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(tr("app.renew.expiry_label", fallback: "Scadenza").uppercased())
                .font(.caption2)
                .foregroundStyle(.white.opacity(0.7))
            Text(expiryString)
                .font(.title3.weight(.semibold))
                .foregroundStyle(.white)
            if let overdue = daysOverdue {
                Text(tr("app.renew.gate.overdue",
                        fallback: "Scaduta da {days} giorni",
                        ["days": "\(overdue)"]))
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.75))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.white.opacity(0.14),
                    in: RoundedRectangle(cornerRadius: AppTheme.Radius.card))
    }

    @ViewBuilder
    private var renewButton: some View {
        if let renewURL {
            Link(destination: renewURL) {
                HStack {
                    Image(systemName: "arrow.up.right.square.fill")
                    Text(tr("app.renew.cta_now", fallback: "Rinnova ora"))
                        .fontWeight(.semibold)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(.white, in: RoundedRectangle(cornerRadius: AppTheme.Radius.button))
                .foregroundStyle(AppTheme.Colors.mensaBlue)
            }
        }
    }

    private var recheckButton: some View {
        Button {
            guard !checking else { return }
            checking = true
            recheck = nil
            Task {
                // Rifà il login per intero quando le credenziali ci sono, che è
                // l'unico modo in cui il backend rilegge la scadenza dal
                // portale. Senza (passkey, password cambiata) il dato è quello
                // del database e l'esito lo dice: si chiede la password invece
                // di ripetere "non risulta ancora" per sempre.
                let result = (try? await koin.auth.recheckMembership()) ?? .unreachable
                // Se non è più scaduta l'esito è nil: RootView rivaluta la fase
                // sul nuovo `currentUser` e smonta questa schermata.
                recheck = outcome(result)
                checking = false
            }
        } label: {
            HStack(spacing: 10) {
                if checking {
                    ProgressView().tint(.white).controlSize(.small)
                }
                Text(checking
                     ? tr("app.renew.gate.checking", fallback: "Controllo in corso…")
                     : tr("app.renew.gate.recheck", fallback: "Ho rinnovato"))
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

#Preview {
    MembershipExpiredView()
}
