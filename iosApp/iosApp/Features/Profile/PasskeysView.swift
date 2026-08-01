import SwiftUI
import Shared
import UIKit

/// Gestione delle passkey: elenco, attivazione su questo dispositivo, revoca.
///
/// E' anche l'unica strada per chi ha rifiutato la proposta automatica dopo il
/// login, e per chi cambia telefono.
struct PasskeysView: View {
    @State private var vm = PasskeysViewModel()
    @State private var pendingDelete: PasskeyModel?

    var body: some View {
        Form {
            Section {
                Button {
                    Task { await vm.add() }
                } label: {
                    HStack {
                        Label(
                            tr("views.passkeys.add", fallback: "Attiva su questo dispositivo"),
                            systemImage: "person.badge.key"
                        )
                        Spacer()
                        if vm.adding { ProgressView() }
                    }
                }
                .disabled(vm.adding)
            } header: {
                Text(tr("views.passkeys.title", fallback: "Passkey"))
            } footer: {
                VStack(alignment: .leading, spacing: 8) {
                    // Suffisso `.ios`: vedi PasskeyEnrollmentPrompt: Android
                    // usa `.android` perché nomina un sensore diverso.
                    Text(tr(
                        "views.passkeys.explainer.ios",
                        fallback: "Con una passkey entri con Face ID o Touch ID, senza digitare la password. La password resta sempre valida."
                    ))
                    if let message = vm.message {
                        Label(message, systemImage: "info.circle")
                            .foregroundStyle(.secondary)
                    }
                }
            }

            if !vm.passkeys.isEmpty {
                Section {
                    ForEach(vm.passkeys, id: \.id) { passkey in
                        HStack {
                            Image(systemName: "person.badge.key")
                                .foregroundStyle(.secondary)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(passkey.name.isEmpty ? "Passkey" : passkey.name)
                                if !passkey.ready {
                                    Text(tr(
                                        "views.passkeys.not_ready",
                                        fallback: "Attivazione non completata"
                                    ))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                }
                            }
                            Spacer()
                        }
                        .swipeActions {
                            Button(role: .destructive) {
                                pendingDelete = passkey
                            } label: {
                                Label(tr("common.remove", fallback: "Rimuovi"), systemImage: "trash")
                            }
                        }
                    }
                }
            } else if !vm.loading {
                Section {
                    Text(tr("views.passkeys.empty", fallback: "Nessuna passkey attiva."))
                        .foregroundStyle(.secondary)
                }
            }
        }
        .navigationTitle(tr("views.passkeys.title", fallback: "Passkey"))
        .task { await vm.refresh() }
        .overlay {
            if vm.loading && vm.passkeys.isEmpty { ProgressView() }
        }
        // Non e' un lockout e non va drammatizzato: la password resta valida.
        .confirmationDialog(
            tr("views.passkeys.remove_title", fallback: "Rimuovere la passkey?"),
            isPresented: Binding(
                get: { pendingDelete != nil },
                set: { if !$0 { pendingDelete = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button(tr("common.remove", fallback: "Rimuovi"), role: .destructive) {
                if let passkey = pendingDelete {
                    Task { await vm.remove(passkey) }
                }
                pendingDelete = nil
            }
            Button(tr("common.cancel", fallback: "Annulla"), role: .cancel) {
                pendingDelete = nil
            }
        } message: {
            Text(tr(
                "views.passkeys.remove_body",
                fallback: "Potrai ancora accedere con la password, e riattivarla quando vuoi."
            ))
        }
    }
}

@MainActor @Observable
final class PasskeysViewModel {
    var passkeys: [PasskeyModel] = []
    var loading = true
    var adding = false
    var message: String?

    private let authenticator = PasskeyAuthenticator()

    func refresh() async {
        loading = true
        defer { loading = false }
        passkeys = (try? await koin.passkeys.list()) ?? []
    }

    /// Il duplicato non lo controlliamo noi: le creation options arrivano con
    /// `excludeCredentials` popolato da Zitadel, quindi se una passkey per questo
    /// account esiste gia' qui e' l'authenticator a rifiutare.
    func add() async {
        guard !adding else { return }
        adding = true
        message = nil
        defer { adding = false }

        guard let challenge = try? await koin.passkeys.beginRegistration() else {
            message = tr(
                "views.passkeys.begin_failed",
                fallback: "Non riesco ad avviare l'attivazione. Riprova più tardi."
            )
            return
        }

        let credentialJSON: String
        do {
            credentialJSON = try await authenticator.create(
                creationOptionsJSON: challenge.creationOptionsJson
            )
        } catch PasskeyAuthenticator.Failure.cancelledOrNoCredential {
            // Chiusura del prompt: scelta dell'utente, nessun messaggio.
            return
        } catch {
            Log.auth.error("Passkey registration failed: \(error.localizedDescription)")
            message = tr(
                "views.passkeys.add_failed",
                fallback: "Attivazione non completata. Riprova."
            )
            return
        }

        // `finishRegistration` ritorna Boolean e non Result: un Result Kotlin
        // arriverebbe qui non-nil anche se incapsula un fallimento, e il
        // controllo sul nil scambierebbe l'errore per un successo. Il Boolean
        // attraversa il bridge come `Any` (NSNumber), da cui il cast.
        let outcome = try? await koin.passkeys.finishRegistration(
            passkeyId: challenge.passkeyId,
            credentialJson: credentialJSON,
            name: Self.deviceName()
        )
        guard (outcome as? Bool) == true else {
            message = tr(
                "views.passkeys.add_failed",
                fallback: "Attivazione non completata. Riprova."
            )
            return
        }
        await refresh()
    }

    func remove(_ passkey: PasskeyModel) async {
        let outcome = try? await koin.passkeys.remove(passkeyId: passkey.id)
        guard (outcome as? Bool) == true else {
            message = tr(
                "views.passkeys.remove_failed",
                fallback: "Non riesco a rimuovere la passkey. Riprova."
            )
            return
        }
        await refresh()
    }

    /// Etichetta della passkey. Da iOS 16 `UIDevice.name` restituisce il modello
    /// e non il nome scelto dall'utente, che per un'etichetta va benissimo.
    private static func deviceName() -> String {
        let name = UIDevice.current.name.trimmingCharacters(in: .whitespacesAndNewlines)
        return String((name.isEmpty ? "iPhone" : name).prefix(100))
    }
}
