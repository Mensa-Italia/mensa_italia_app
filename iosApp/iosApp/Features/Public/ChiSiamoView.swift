import SwiftUI

/// "Chi siamo" — landing informativa pre-login. Lista nativa con sezioni
/// standard: iOS 26 ci mette il materiale Liquid Glass automaticamente,
/// niente `GlassCard` custom, niente background gradient.
struct ChiSiamoView: View {
    var body: some View {
        List {
            Section {
                heroRow
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16))
            }

            Section(tr("public.chi_siamo.section.numbers", fallback: "In numeri")) {
                // Cifre non vengono tradotte (formato uniforme); solo i caption.
                statRow(value: "2%", caption: tr("public.chi_siamo.stat.population", fallback: "della popolazione"))
                statRow(value: "150.000+", caption: tr("public.chi_siamo.stat.members_world", fallback: "soci nel mondo"))
                statRow(value: "100+", caption: tr("public.chi_siamo.stat.countries", fallback: "Paesi"))
                statRow(value: "1946", caption: tr("public.chi_siamo.stat.founded", fallback: "anno di fondazione"))
            }

            Section(tr("public.chi_siamo.section.what_is", fallback: "Cos'è il Mensa")) {
                Text(tr(
                    "public.chi_siamo.what_is.body",
                    fallback: "Mensa è un club internazionale di persone curiose. L'unico requisito per entrare è un test di logica: se rientri nel 2% più alto della popolazione, sei dentro. Da lì in poi non conta più cosa hai studiato, che lavoro fai o quanti anni hai. Il nome viene dal latino *mensa*, la tavola rotonda: tra soci si sta tutti allo stesso livello."
                ))
                .font(.body)
                .foregroundStyle(.primary)
            }

            // Brand name nel titolo di sezione — non tradotto.
            Section("Mensa Italia") {
                Text(tr(
                    "public.chi_siamo.mensa_italia.body",
                    fallback: "Dal 1983 siamo la sezione italiana di Mensa. Oltre 2.600 soci in venti gruppi locali, dal Trentino alla Sicilia, che si incontrano per cene, conferenze, weekend di giochi e progetti comuni. Niente politica, niente religione, niente scopo di lucro."
                ))
                .font(.body)
                .foregroundStyle(.primary)
            }

            Section(tr("public.chi_siamo.section.what_we_do", fallback: "Cosa facciamo")) {
                Label {
                    VStack(alignment: .leading) {
                        Text(tr("public.chi_siamo.do.local.title", fallback: "Gruppi locali"))
                            .font(.body)
                        Text(tr("public.chi_siamo.do.local.subtitle", fallback: "Aperitivi, cene, gite, vicino a te"))
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                } icon: { Image(systemName: "person.3") }

                Label {
                    VStack(alignment: .leading) {
                        Text(tr("public.chi_siamo.do.contests.title", fallback: "Concorsi"))
                            .font(.body)
                        // "Il Brain" / "Mensa Ludo" sono nomi propri di iniziativa.
                        Text(tr("public.chi_siamo.do.contests.subtitle", fallback: "Il Brain e Mensa Ludo, ogni anno"))
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                } icon: { Image(systemName: "trophy") }

                Label {
                    VStack(alignment: .leading) {
                        Text(tr("public.chi_siamo.do.podcasts.title", fallback: "Podcast"))
                            .font(.body)
                        // Nomi propri di podcast — non tradotti.
                        Text("Brainwaves, She Talks, Mensa Talk")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                } icon: { Image(systemName: "headphones") }

                Label {
                    VStack(alignment: .leading) {
                        // "QUID" nome rivista — non tradotto.
                        Text(tr("public.chi_siamo.do.quid.title", fallback: "Rivista QUID"))
                            .font(.body)
                        Text(tr("public.chi_siamo.do.quid.subtitle", fallback: "Approfondimenti scritti dai soci"))
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                } icon: { Image(systemName: "book") }

                Label {
                    VStack(alignment: .leading) {
                        Text(tr("public.chi_siamo.do.research.title", fallback: "Ricerca"))
                            .font(.body)
                        Text(tr("public.chi_siamo.do.research.subtitle", fallback: "Cosa significa davvero \"essere intelligenti\""))
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                } icon: { Image(systemName: "lightbulb") }
            }

            Section {
                Text(tr(
                    "public.chi_siamo.how_to.test_body",
                    fallback: "Il test ufficiale dura circa 20 minuti: 45 sequenze di figure, niente matematica, niente cultura generale. L'esito arriva in due settimane. Se sei nel 2% più alto, ti scriviamo per darti il benvenuto."
                ))
                .font(.body)
                Text(tr(
                    "public.chi_siamo.how_to.bypass_body",
                    fallback: "Hai già fatto un test del QI riconosciuto altrove? Puoi chiedere l'ammissione diretta senza rifarlo."
                ))
                .font(.body)
                LabeledContent(tr("public.chi_siamo.how_to.fees_label", fallback: "Quote annuali")) {
                    // Importi specifici Italia — non tradotti.
                    Text("€25 primo anno · €50 standard · €25 under 26")
                        .font(.footnote)
                        .multilineTextAlignment(.trailing)
                }
            } header: {
                Text(tr("public.chi_siamo.section.how_to_join", fallback: "Come si entra"))
            }

            Section(tr("public.chi_siamo.section.contacts", fallback: "Contatti")) {
                Link(destination: URL(string: "mailto:info@mensa.it")!) {
                    Label {
                        VStack(alignment: .leading) {
                            Text(tr("public.chi_siamo.contact.email.title", fallback: "Scrivici una mail"))
                                .font(.body)
                            // Indirizzo email — non tradotto.
                            Text("info@mensa.it")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    } icon: { Image(systemName: "envelope") }
                }
                Link(destination: URL(string: "maps://?address=Viale+Lunigiana+7+20125+Milano")!) {
                    Label {
                        VStack(alignment: .leading) {
                            Text(tr("public.chi_siamo.contact.address.title", fallback: "Sede nazionale, Milano"))
                                .font(.body)
                            // Indirizzo fisico — non tradotto.
                            Text("Viale Lunigiana 7")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    } icon: { Image(systemName: "mappin.and.ellipse") }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(tr("public.chi_siamo.title", fallback: "Chi siamo"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private var heroRow: some View {
        VStack(spacing: 8) {
            Image("MensaLogo")
                .resizable()
                .scaledToFit()
                .frame(maxHeight: 60)
            // Brand name — non tradotto.
            Text("Mensa")
                .font(.title3.weight(.semibold))
            Text(tr("public.chi_siamo.hero.tagline", fallback: "Persone curiose. Un test in comune."))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    private func statRow(value: String, caption: String) -> some View {
        LabeledContent(caption) {
            Text(value)
                .font(.body.weight(.semibold))
                .monospacedDigit()
        }
    }
}

#Preview { NavigationStack { ChiSiamoView() } }
