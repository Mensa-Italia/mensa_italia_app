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
            Task {
                _ = try? await koin.auth.refreshCurrentUser()
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
