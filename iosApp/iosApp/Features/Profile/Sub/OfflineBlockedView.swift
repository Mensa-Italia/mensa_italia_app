import SwiftUI
import Shared

/// Offline da più di un giorno: per continuare serve collegarsi.
///
/// L'app parte dalla cache e offline mostra i dati salvati, ma non per sempre:
/// tessera, timbri e dati vanno riverificati dal server almeno una volta ogni
/// 24 ore (vedi `Connectivity` in :shared). La schermata se ne va da sola
/// appena il server risponde — il controllo gira in sottofondo anche senza
/// toccare "Riprova". Stile del muro del rinnovo (`MembershipExpiredView`).
/// Mirrors Android `OfflineBlockedScreen`.
struct OfflineBlockedView: View {
    @State private var store = ConnectivityStore.shared
    @State private var checking = false
    @State private var stillOffline = false
    @State private var loggingOut = false

    var body: some View {
        ZStack {
            AppTheme.brandGradient.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    Spacer(minLength: 40)

                    MensaMark(size: 88, inBlueBadge: false)
                        .accessibilityLabel("Mensa Italia")

                    Image(systemName: "icloud.slash")
                        .font(.system(size: 30, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 64, height: 64)
                        .background(Color.white.opacity(0.18), in: Circle())

                    Text(tr("app.offline.blocked.title", fallback: "Collegati a internet"))
                        .font(.title2.bold())
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.center)

                    Text(tr("app.offline.blocked.body",
                            fallback: "L'app è offline da più di un giorno. Per continuare serve un collegamento: tessera e dati vanno verificati almeno una volta ogni 24 ore."))
                        .font(.subheadline)
                        .foregroundStyle(.white.opacity(0.88))
                        .multilineTextAlignment(.center)

                    if let date = store.lastContactText {
                        Text(tr("app.offline.blocked.last_contact",
                                fallback: "Ultimo collegamento: {date}", ["date": date]))
                            .font(.caption)
                            .foregroundStyle(.white.opacity(0.75))
                    }

                    retryButton

                    if stillOffline {
                        Text(tr("app.offline.blocked.still_offline", fallback: "Ancora nessuna connessione."))
                            .font(.footnote)
                            .foregroundStyle(.white.opacity(0.9))
                    }

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
    }

    private var retryButton: some View {
        Button {
            guard !checking else { return }
            checking = true
            stillOffline = false
            Task {
                // Se risponde, la fase cambia e questa schermata sparisce.
                let answered = (try? await koin.connectivity.probeNow())?.boolValue ?? false
                stillOffline = !answered
                checking = false
            }
        } label: {
            HStack(spacing: 10) {
                if checking {
                    ProgressView().tint(.white).controlSize(.small)
                }
                Text(tr("app.offline.blocked.retry", fallback: "Riprova"))
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
