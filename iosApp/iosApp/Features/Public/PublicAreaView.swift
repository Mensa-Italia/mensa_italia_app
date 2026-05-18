import SwiftUI

/// Pre-login entry point. `List` `.insetGrouped` con tre macro-sezioni —
/// iOS 26 applica Liquid Glass alle righe in automatico, nessun chrome
/// custom. Voci esterne (sito ufficiale per iscrizione test) usano `Link`
/// nativo cosi' aprono Safari direttamente.
struct PublicAreaView: View {

    /// Flip-back della scelta "esplora senza account": quando l'utente tocca
    /// "Sei socio? Accedi", torniamo a LoginView come root via `RootView`.
    /// Stesso `@AppStorage` chiave letta li' — settarla a false fa
    /// re-renderizzare RootView con LoginView.
    @AppStorage("guestModeEnabled") private var guestMode: Bool = false

    /// URL del sito ufficiale Mensa Italia, pagina ammissione tramite test
    /// ufficiale. Non e' il test d'esempio in-app: questa e' la procedura
    /// vera per diventare socio.
    private let registerForTestURL = URL(
        string: "https://www.mensa.it/ammissione-tramite-test-ufficiale/"
    )!

    var body: some View {
        List {
            Section {
                heroRow
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 24, leading: 16, bottom: 24, trailing: 16))
            }

            Section(tr("public.area.section.discover", fallback: "Scopri Mensa")) {
                NavigationLink {
                    ChiSiamoView()
                } label: {
                    Label(tr("public.area.about", fallback: "Chi siamo"),
                          systemImage: "info.circle")
                }

                NavigationLink {
                    PublicLocalOfficesListView()
                } label: {
                    Label(tr("public.area.local_offices", fallback: "Gruppi locali"),
                          systemImage: "building.2")
                }
            }

            Section {
                NavigationLink {
                    IQTestView()
                } label: {
                    Label(tr("public.area.try_test", fallback: "Mettiti alla prova"),
                          systemImage: "brain.head.profile")
                }

                Link(destination: registerForTestURL) {
                    HStack {
                        Label(tr("public.area.register_test", fallback: "Iscriviti per fare il test"),
                              systemImage: "person.crop.circle.badge.plus")
                            .foregroundStyle(.primary)
                        Spacer()
                        Image(systemName: "arrow.up.right")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(.secondary)
                    }
                }
            } header: {
                Text(tr("public.area.section.become_member", fallback: "Diventa socio"))
            } footer: {
                Text(tr(
                    "public.area.section.become_member_footer",
                    fallback: "Prova un test ufficiale d'esempio in-app oppure prenota il test ufficiale sul sito di Mensa Italia."
                ))
            }

            Section(tr("public.area.section.explore", fallback: "Esplora")) {
                NavigationLink {
                    PublicEventsView()
                } label: {
                    Label(tr("public.area.events", fallback: "Eventi pubblici"),
                          systemImage: "calendar")
                }

                NavigationLink {
                    PodcastsListView()
                } label: {
                    Label(tr("public.area.podcasts", fallback: "Podcast"),
                          systemImage: "headphones")
                }

                NavigationLink {
                    QuidIssuesView()
                } label: {
                    Label(tr("public.area.quid", fallback: "Quid"),
                          systemImage: "book")
                }
            }

            Section {
                Button {
                    guestMode = false
                } label: {
                    Label(tr("public.area.member_login", fallback: "Sei socio? Accedi"),
                          systemImage: "person.crop.circle.badge.checkmark")
                        .foregroundStyle(.primary)
                }
            } footer: {
                Text(tr("public.area.member_login_footer", fallback: "Torni alla schermata di accesso."))
            }

            // Spacer di coda: lascia respiro sopra il home indicator e
            // sopra l'overlay del mini-player (quando attivo). Riga
            // fantasma — nessun chrome di Section, solo whitespace.
            Section {
                Color.clear.frame(height: 24)
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets())
            }
        }
        .listStyle(.insetGrouped)
        // Niente navigationTitle: il logo in cima fa da brand mark e scorre
        // con il contenuto (intenzionale — niente nav bar che persiste).
        .toolbar(.hidden, for: .navigationBar)
    }

    // MARK: - Hero

    private var heroRow: some View {
        VStack(spacing: 8) {
            Image("MensaLogo")
                .resizable()
                .scaledToFit()
                .frame(maxHeight: 72)

            // Brand name — non tradotto.
            Text("Mensa Italia")
                .font(.title2.weight(.semibold))
                .foregroundStyle(.primary)

            // Brand tagline ufficiale internazionale — resta in inglese.
            Text("The High I.Q. Society")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

#Preview { NavigationStack { PublicAreaView() } }
