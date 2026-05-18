import SwiftUI
import Shared

/// Membership status + renewal CTA. The actual renewal is handled externally
/// (cloud32.it portal) — we open Safari with the official URL.
struct RenewMembershipView: View {
    @State private var vm = RenewMembershipViewModel()
    @State private var appeared = false

    private let renewURL = URL(string: "https://www.cloud32.it/Associazioni/utenti/richirinnovo")!

    var body: some View {
        ScrollView {
            VStack(spacing: 22) {
                statusCard
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 12)
                    .animation(.spring(response: 0.6, dampingFraction: 0.8), value: appeared)

                countdownCard
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 12)
                    .animation(
                        .spring(response: 0.6, dampingFraction: 0.8).delay(0.08),
                        value: appeared
                    )

                renewCTA
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 12)
                    .animation(
                        .spring(response: 0.6, dampingFraction: 0.8).delay(0.16),
                        value: appeared
                    )

                infoBlock
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 12)
                    .animation(
                        .spring(response: 0.6, dampingFraction: 0.8).delay(0.24),
                        value: appeared
                    )
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 24)
        }
        .navigationTitle(tr("app.renew.title", fallback: "Tessera"))
        .navigationBarTitleDisplayMode(.large)
        .background(
            LinearGradient(
                colors: [
                    AppTheme.Colors.brandPrimary.opacity(0.06),
                    Color(.systemBackground)
                ],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()
        )
        .task {
            await vm.load()
            withAnimation { appeared = true }
        }
    }

    private var statusCard: some View {
        GlassCard(tint: vm.isExpired
                  ? .red.opacity(0.18)
                  : AppTheme.Colors.brandSecondary.opacity(0.18)) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: vm.isExpired
                          ? "exclamationmark.triangle.fill"
                          : "checkmark.seal.fill")
                        .font(.title2)
                        .foregroundStyle(vm.isExpired
                                         ? .red
                                         : AppTheme.Colors.brandPrimary)
                    Text(vm.isExpired
                         ? tr("app.renew.status_expired", fallback: "Tessera scaduta")
                         : tr("app.renew.status_active", fallback: "Tessera attiva"))
                        .font(.headline)
                    Spacer()
                }
                Text(tr("app.renew.expiry_label", fallback: "Scadenza"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .textCase(.uppercase)
                Text(vm.expiryString)
                    .font(.title2.weight(.semibold))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var countdownCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(tr("app.renew.countdown_label",
                    fallback: "Tempo rimanente"))
                .font(.caption)
                .foregroundStyle(.secondary)
                .textCase(.uppercase)
            Text(vm.countdownString)
                .font(.title.weight(.bold))
                .foregroundStyle(
                    vm.isExpired ? .red : AppTheme.Colors.brandPrimary
                )
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(
            Color(.secondarySystemBackground),
            in: RoundedRectangle(cornerRadius: AppTheme.Radius.card)
        )
    }

    private var renewCTA: some View {
        Link(destination: renewURL) {
            HStack {
                Image(systemName: "arrow.up.right.square.fill")
                Text(vm.isExpired
                     ? tr("app.renew.cta_now", fallback: "Rinnova ora")
                     : tr("app.renew.cta_early", fallback: "Rinnova in anticipo"))
                    .fontWeight(.semibold)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(AppTheme.brandGradient,
                        in: RoundedRectangle(cornerRadius: AppTheme.Radius.button))
            .foregroundStyle(.white)
        }
    }

    private var infoBlock: some View {
        VStack(alignment: .leading, spacing: 10) {
            Label(tr("app.renew.info_title",
                     fallback: "Come funziona il rinnovo"),
                  systemImage: "info.circle.fill")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            Text(tr("app.renew.info_body",
                    fallback: "Verrai reindirizzato al portale cloud32.it per completare il pagamento della quota associativa annuale. Una volta confermato, la tua tessera verrà aggiornata automaticamente."))
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(
            AppTheme.Colors.brandPrimary.opacity(0.06),
            in: RoundedRectangle(cornerRadius: 16)
        )
    }
}

@MainActor
@Observable
final class RenewMembershipViewModel {
    /// Session-stable: `expireMembership` viene aggiornata via `/api/cs/me`
    /// dentro `AuthRepository.init()` PRIMA che la view venga mostrata.
    /// Cambia solo a login/logout, in entrambi i casi la view è smontata.
    var user: UserModel? {
        koin.auth.currentUser.value as? UserModel
    }

    var expiryDate: Date? {
        guard let u = user, u.expireMembership.epochSeconds > 0 else { return nil }
        return Date(timeIntervalSince1970: Double(u.expireMembership.epochSeconds))
    }

    var isExpired: Bool {
        guard let d = expiryDate else { return true }
        return d < Date()
    }

    var expiryString: String {
        guard let u = user else { return "-" }
        return formatItalianDate(u.expireMembership)
    }

    var countdownString: String {
        guard let d = expiryDate else { return "-" }
        let interval = d.timeIntervalSinceNow
        if interval <= 0 {
            return tr("app.renew.expired_already", fallback: "Scaduta")
        }
        let days = Int(interval / 86400)
        if days > 30 {
            // Tolgee placeholder syntax: {name} resolved by tr()'s args dict.
            // The Swift fallback uses the SAME literal so the extractor script
            // pushes the right template to Tolgee.
            return tr("app.renew.days_left",
                      fallback: "{days} giorni",
                      ["days": "\(days)"])
        } else if days > 0 {
            return tr("app.renew.days_warning",
                      fallback: "{days} giorni: rinnova presto",
                      ["days": "\(days)"])
        } else {
            let hours = Int(interval / 3600)
            return tr("app.renew.hours_left",
                      fallback: "{hours} ore",
                      ["hours": "\(hours)"])
        }
    }

    /// No-op kept for back-compat con `.task { await vm.load() }`. Lo stato
    /// è derivato sincrono da `koin.auth.currentUser.value`.
    func load() async {}
}

#Preview {
    NavigationStack { RenewMembershipView() }
}
