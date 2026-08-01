import SwiftUI

/// Sign-in surface — `Form` nativa con sezioni standard.
///
/// iOS 26 applica Liquid Glass alle righe `Form`/`List` automaticamente,
/// gestisce keyboard avoidance senza l'inset-trap che avevamo prima, e dà i
/// divider + raggi + sfondi senza che li dobbiamo dipingere a mano.
struct LoginView: View {
    @State private var vm = LoginViewModel()
    @FocusState private var focus: Field?
    enum Field { case email, password }

    /// Stesso `@AppStorage` letto da RootView. Quando "Esplora senza account"
    /// flippa questo a true, RootView swappa root → PublicAreaShell.
    @AppStorage("guestModeEnabled") private var guestMode: Bool = false

    private var canSubmit: Bool {
        !vm.email.isEmpty && !vm.password.isEmpty && !vm.loading && !vm.passkeyLoading
    }

    /// Il bottone passkey si mostra sempre e chiede solo l'email: sondare il
    /// server per sapere se questa email ha una passkey lo trasformerebbe in un
    /// oracolo su chi e' socio.
    private var canUsePasskey: Bool {
        !vm.email.isEmpty && !vm.loading && !vm.passkeyLoading
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    heroRow
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                        .listRowInsets(EdgeInsets(top: 24, leading: 16, bottom: 16, trailing: 16))
                }

                Section {
                    HStack(spacing: 12) {
                        Image(systemName: "envelope")
                            .foregroundStyle(focus == .email ? AppTheme.Colors.brandTintAdaptive : .secondary)
                            .frame(width: 22)
                        TextField(
                            tr("views.signin.form.field.hint.email", fallback: "Email"),
                            text: $vm.email
                        )
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.emailAddress)
                        .textContentType(.username)
                        .submitLabel(.next)
                        .focused($focus, equals: .email)
                        .onSubmit { focus = .password }
                    }

                    HStack(spacing: 12) {
                        Image(systemName: "lock")
                            .foregroundStyle(focus == .password ? AppTheme.Colors.brandTintAdaptive : .secondary)
                            .frame(width: 22)
                        SecureFieldWithReveal(
                            text: $vm.password,
                            placeholder: tr("views.signin.form.field.hint.password", fallback: "Password")
                        )
                        .textContentType(.password)
                        .submitLabel(.go)
                        .focused($focus, equals: .password)
                        .onSubmit { Task { _ = await vm.login() } }
                    }
                } footer: {
                    HStack {
                        Spacer()
                        Link(
                            tr("views.signin.form.button.recover_password.text", fallback: "Password dimenticata?"),
                            destination: URL(string: "https://www.mensa.it/area-soci/password-dimenticata/")!
                        )
                    }
                }

                if let err = vm.error {
                    Section {
                        Label {
                            Text(err)
                                .foregroundStyle(.primary)
                        } icon: {
                            Image(systemName: "exclamationmark.circle.fill")
                                .foregroundStyle(.red)
                        }
                        .font(.footnote)
                    }
                }

                Section {
                    Button {
                        focus = nil
                        Task { _ = await vm.login() }
                    } label: {
                        HStack {
                            Spacer()
                            if vm.loading {
                                ProgressView().tint(.white)
                            } else {
                                Text(tr("views.signin.title2", fallback: "Accedi"))
                                    .font(.body.weight(.semibold))
                            }
                            Spacer()
                        }
                    }
                    .compatGlassProminentButtonStyle()
                    .tint(AppTheme.Colors.mensaBlue)
                    .controlSize(.large)
                    .disabled(!canSubmit)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                }

                // ── Passkey ──────────────────────────────────────────────────
                // Serve solo l'email: la Session API di Zitadel pretende il check
                // utente prima di poter creare la challenge WebAuthn, quindi il
                // login usernameless non e' possibile e il flusso resta a due
                // passi (email → prompt biometrico).
                // `enable_passkeys` off → via l'intera sezione, footer incluso:
                // la nota "nessuna passkey su questo dispositivo" non ha senso
                // se la feature e' spenta.
                if koin.featureFlags.passkeysEnabled {
                Section {
                    Button {
                        focus = nil
                        Task { _ = await vm.loginWithPasskey() }
                    } label: {
                        HStack {
                            Spacer()
                            if vm.passkeyLoading {
                                ProgressView()
                            } else {
                                Label(
                                    tr("app.login.passkey.action", fallback: "Accedi con una passkey"),
                                    systemImage: "person.badge.key"
                                )
                            }
                            Spacer()
                        }
                    }
                    .controlSize(.large)
                    .disabled(!canUsePasskey)
                } footer: {
                    // Nota neutra, non un errore: chi non ha una passkey su questo
                    // dispositivo non ha sbagliato niente.
                    if let notice = vm.passkeyNotice {
                        Label {
                            Text(notice)
                        } icon: {
                            Image(systemName: "info.circle")
                        }
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .padding(.top, 4)
                    }
                }
                }

                Section {
                    // `public_area_enabled` off → niente ingresso ospite. Il
                    // gate sta anche in RootView, perche' `guestModeEnabled` e'
                    // persistente: chi era gia' in area pubblica quando il flag
                    // e' stato spento deve comunque uscirne.
                    // Il footer qui sotto resta: e' il link marketing verso
                    // mensa.it, non fa parte dell'area pubblica in-app.
                    if koin.featureFlags.publicAreaEnabled {
                        // Swap root invece di push: settiamo `guestMode = true`
                        // e RootView re-renderizza con `PublicAreaShell` come
                        // pagina principale. Niente back button perche' siamo
                        // a livello root, e cold-launch successivi aprono
                        // direttamente l'area pubblica (preferenza persistente).
                        Button {
                            guestMode = true
                        } label: {
                            Label(
                                tr("app.login.explore_no_account", fallback: "Esplora senza account"),
                                systemImage: "rectangle.portrait.and.arrow.right"
                            )
                            .foregroundStyle(.primary)
                        }
                    }
                } footer: {
                    HStack(spacing: 4) {
                        Text(tr("app.login.no_member", fallback: "Non sei socio?"))
                            .foregroundStyle(.secondary)
                        Link(
                            tr("app.login.discover", fallback: "Scopri Mensa"),
                            destination: URL(string: "https://www.mensa.it")!
                        )
                    }
                    .font(.footnote)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.top, 4)
                }
            }
            .scrollDismissesKeyboard(.interactively)
            .task { await vm.autoLoginIfRequested() }
        }
        // Player audio sopra il bottom safe area. Visibile su login + tutte
        // le view pushate (PublicArea, PodcastsListView, PodcastEpisodesView,
        // etc.) cosi' l'utente che fa partire un podcast pre-login non ne
        // perde il controllo navigando.
        .miniPlayerOverlay()
    }

    // MARK: - Hero

    private var heroRow: some View {
        VStack(spacing: 14) {
            MensaMark(size: 72, inBlueBadge: true)
                .accessibilityLabel("Mensa Italia")

            VStack(spacing: 4) {
                Text(tr("app.login.title", fallback: "Bentornato in Mensa"))
                    .font(.title.weight(.semibold))
                    .multilineTextAlignment(.center)
                Text(tr("app.login.subtitle", fallback: "Accedi all'area soci per continuare."))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
    }
}

/// Inline secure field with reveal toggle on the trailing edge — SwiftUI non
/// ha il toggle nativo, quindi è l'unica parte custom giustificata.
private struct SecureFieldWithReveal: View {
    @Binding var text: String
    let placeholder: String
    @State private var revealed = false

    var body: some View {
        HStack(spacing: 0) {
            if revealed {
                TextField(placeholder, text: $text)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            } else {
                SecureField(placeholder, text: $text)
            }
            Button { revealed.toggle() } label: {
                Image(systemName: revealed ? "eye.slash" : "eye")
                    .foregroundStyle(.secondary)
                    .frame(width: 30, height: 30)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(revealed
                ? tr("app.login.hide_password", fallback: "Nascondi password")
                : tr("app.login.show_password", fallback: "Mostra password"))
        }
    }
}

#Preview { LoginView() }
