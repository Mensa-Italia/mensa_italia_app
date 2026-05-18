import SwiftUI
import Shared

/// Settings/profile screen — stock iOS list style with account, app preferences and logout.
struct ProfileView: View {
    @State private var vm = ProfileViewModel()
    @State private var showLogoutConfirm = false

    @ObservedObject private var locale = LocaleManager.shared

    /// Trailing value for the "Lingua" row: native name of the chosen language
    /// when an override is set, "Sistema · {device locale}" when following
    /// the device.
    private var languageRowValue: String {
        if let override = locale.override, !override.isEmpty {
            return locale.nativeName(for: override).capitalized
        }
        let systemLabel = tr("app.language.system", fallback: "Sistema")
        return "\(systemLabel) · \(locale.nativeName(for: locale.activeTag).capitalized)"
    }

    private var appVersion: String {
        let v = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let b = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(v) (\(b))"
    }

    var body: some View {
        List {
            // MARK: Header
            Section {
                profileHeader
            }
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets())

            // MARK: Account
            Section(tr("app.profile.section_account", fallback: "Account")) { // i18n
                NavigationLink {
                    RenewMembershipView()
                } label: {
                    ProfileRowLabel(icon: "creditcard",
                                    title: tr("app.profile.membership", fallback: "Membership"))
                }
                NavigationLink {
                    PaymentMethodsView()
                } label: {
                    ProfileRowLabel(icon: "banknote",
                                    title: tr("app.profile.payments", fallback: "Pagamenti"))
                }
                NavigationLink {
                    DevicesView()
                } label: {
                    ProfileRowLabel(icon: "iphone",
                                    title: tr("views.devices.title", fallback: "Dispositivi"))
                }
            }

            // MARK: Donation
            Section(tr("app.profile.section_donation", fallback: "Donazione")) { // i18n
                NavigationLink {
                    MakeDonationView()
                } label: {
                    ProfileRowLabel(icon: "heart.fill",
                                    title: tr("views.make_donation.title", fallback: "Fai una donazione"),
                                    iconColor: .pink)
                }
                NavigationLink {
                    CalendarLinkerView()
                } label: {
                    ProfileRowLabel(icon: "calendar.badge.plus",
                                    title: tr("app.calendar_link.title", fallback: "Calendario"))
                }
            }

            // MARK: Associazione
            Section(tr("app.profile.section_association", fallback: "Associazione")) {
                NavigationLink {
                    OrgChartView()
                } label: {
                    ProfileRowLabel(icon: "person.2.badge.gearshape",
                                    title: tr("app.org_chart.title", fallback: "Organigramma"))
                }
            }

            // MARK: App settings
            Section(tr("app.profile.section_app", fallback: "App")) { // i18n
                NavigationLink {
                    LanguagePickerView()
                } label: {
                    ProfileRow(
                        icon: "globe",
                        title: tr("app.profile.language", fallback: "Lingua"), // i18n
                        value: languageRowValue
                    )
                }
                .buttonStyle(.plain)

                HStack {
                    Image(systemName: "moon.circle")
                        .frame(width: 28, height: 28)
                    Text(tr("app.profile.theme", fallback: "Tema")) // i18n
                    Spacer()
                    Picker(tr("app.profile.theme", fallback: "Tema"), selection: $vm.selectedTheme) { // i18n
                        ForEach(ThemeChoice.allCases) { choice in
                            Text(choice.rawValue).tag(choice)
                        }
                    }
                    .pickerStyle(.menu)
                }

                HStack {
                    Image(systemName: "bell")
                        .frame(width: 28, height: 28)
                    Toggle(tr("views.notificaiton.title", fallback: "Notifiche"), isOn: $vm.notificationsEnabled) // i18n
                }
            }

            // MARK: Info
            Section(tr("app.profile.section_info", fallback: "Info")) { // i18n
                ProfileRow(icon: "info.circle", title: tr("app.profile.version", fallback: "Versione"), value: appVersion) // i18n

                Button(action: { openURL("https://www.mensa.it/privacy") }) {
                    ProfileRow(icon: "lock.shield", title: tr("views.settings.tile.privacypolicy.title", fallback: "Privacy Policy"), action: {}) // i18n
                }
                .buttonStyle(.plain)

                Button(action: { openURL("https://www.mensa.it/termini") }) {
                    ProfileRow(icon: "doc.text", title: tr("app.profile.terms", fallback: "Termini di utilizzo"), action: {}) // i18n
                }
                .buttonStyle(.plain)

                NavigationLink {
                    CreditsView()
                } label: {
                    ProfileRowLabel(icon: "sparkles",
                                    title: tr("app.profile.credits", fallback: "Crediti")) // i18n
                }
            }

            // MARK: Logout — Apple Settings convention: a single centered
            // destructive text row, no icon, no border. Matches "Sign Out"
            // in iOS Settings / Apple Music / Mail.
            Section {
                Button(role: .destructive, action: { showLogoutConfirm = true }) {
                    HStack {
                        Spacer(minLength: 0)
                        if vm.loggingOut {
                            ProgressView().tint(.red)
                        } else {
                            Text(tr("views.settings.tile.logout.title", fallback: "Esci")) // i18n
                                .font(.body)
                        }
                        Spacer(minLength: 0)
                    }
                    .frame(maxWidth: .infinity)
                    .contentShape(Rectangle())
                }
                .disabled(vm.loggingOut)
                .listRowBackground(Color(.secondarySystemGroupedBackground))
            } footer: {
                if let email = vm.user?.email, !email.isEmpty {
                    Text(tr(
                        "app.profile.signed_in_as",
                        fallback: "Hai effettuato l'accesso come {email}",
                        ["email": email]
                    )) // i18n
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.top, 4)
                }
            }
        }
        .navigationTitle(tr("app.profile.title", fallback: "Profilo")) // i18n
        .navigationBarTitleDisplayMode(.large)
        .cleanNavBar()
        .preferredColorScheme(vm.selectedTheme.colorScheme)
        .task { await vm.load() }
        // Apple convention for destructive account actions (cf. iOS Settings
        // "Esci dal mio Apple ID"): a centered modal `.alert`, not an action
        // sheet. Removes the "dialog appears at the top" anchoring quirk that
        // `.confirmationDialog` exhibits when attached to a wide Form.
        .alert(
            tr("app.profile.logout_confirm_title", fallback: "Vuoi uscire dall'account?"), // i18n
            isPresented: $showLogoutConfirm
        ) {
            Button(tr("views.make_donation.cancel", fallback: "Annulla"), role: .cancel) {} // i18n
            Button(tr("views.settings.tile.logout.title", fallback: "Esci"), role: .destructive) { // i18n
                Task { await vm.logout() }
            }
        } message: {
            Text(tr("app.profile.logout_confirm_message", fallback: "Dovrai accedere di nuovo per usare l'app.")) // i18n
        }
        .alert(tr("app.error.title", fallback: "Errore"), isPresented: .init( // i18n
            get: { vm.errorMessage != nil },
            set: { if !$0 { vm.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { vm.errorMessage = nil }
        } message: {
            Text(vm.errorMessage ?? "")
        }
    }

    // MARK: - Profile Header

    private var profileHeader: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [AppTheme.Colors.brandPrimary, AppTheme.Colors.brandSecondary],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 88, height: 88)

                if let url = vm.avatarURL {
                    CachedAsyncImage(url: url) { img in
                        img.resizable().scaledToFill()
                    } placeholder: {
                        Image(systemName: "person.fill")
                            .font(.system(size: 36))
                            .foregroundStyle(.white)
                    }
                    .clipShape(Circle())
                    .frame(width: 88, height: 88)
                } else {
                    Image(systemName: "person.fill")
                        .font(.system(size: 36))
                        .foregroundStyle(.white)
                }
            }

            VStack(spacing: 4) {
                Text(vm.fullName)
                    .font(.title3.weight(.semibold))
                if !vm.email.isEmpty && vm.email != "-" {
                    Text(vm.email)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
    }

    // MARK: - Helpers

    private func openURL(_ urlString: String) {
        guard let url = URL(string: urlString) else { return }
        UIApplication.shared.open(url)
    }
}

/// Lightweight label for NavigationLink rows — chevron is supplied by the link.
struct ProfileRowLabel: View {
    let icon: String
    let title: String
    var iconColor: Color = .primary

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .frame(width: 28, height: 28)
                .foregroundStyle(iconColor)
            Text(title)
            Spacer()
        }
    }
}

#Preview {
    NavigationStack {
        ProfileView()
    }
}
