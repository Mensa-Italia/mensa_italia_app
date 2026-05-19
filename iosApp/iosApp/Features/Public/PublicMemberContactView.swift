import SwiftUI
import Shared

/// Mini detail pubblico per un referente o assistente del gruppo locale.
///
/// Pre-login, espone solo i campi che la view PocketBase pubblica restituisce
/// (`view_local_office_admins` / `view_local_office_assistants`): foto, nome,
/// ruolo, gruppo locale, email. L'email diventa un `mailto:` con oggetto
/// precompilato — il path canonico Apple per il contatto cross-app.
struct PublicMemberContactView: View {
    let name: String
    /// Ruolo già localizzato: "Segretario" / "Cosegretario" / "Assistente al test".
    let roleLabel: String
    let email: String
    let imageURL: URL?
    let officeName: String
    let region: String
    /// Macro-area geografica (es. "Nord", "Centro", "Sud") — esposta solo
    /// per gli assistenti al test. Vuoto su admins/segretari.
    var area: String = ""
    /// Provincia/state dell'assistente. Vuoto su admins/segretari.
    var state: String = ""
    /// Citta' dell'assistente. Vuoto su admins/segretari.
    var city: String = ""

    var body: some View {
        List {
            Section {
                heroRow
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 24, leading: 16, bottom: 24, trailing: 16))
            }

            Section {
                if !email.isEmpty, let url = mailtoURL {
                    Link(destination: url) {
                        Label(email, systemImage: "envelope")
                    }
                } else {
                    Label(tr("public.member.email_unavailable", fallback: "Email non disponibile"),
                          systemImage: "envelope")
                        .foregroundStyle(.secondary)
                }
            } header: {
                Text(tr("public.member.section.contact", fallback: "Contatta"))
            } footer: {
                Text(tr(
                    "public.member.section.contact_footer",
                    fallback: "Scrivi per richiedere informazioni sul test ufficiale Mensa o sulle attività del gruppo locale."
                ))
            }

            Section(tr("public.member.section.local_office", fallback: "Gruppo locale")) {
                // Dedup case-insensitive: spesso piu' campi coincidono
                // ("Lombardia"/"Lombardia", "Milano"/"Milano"), evitiamo
                // righe doppione mostrando ogni valore una sola volta in
                // ordine di gerarchia (gruppo → regione → area → provincia
                // → citta'). La prima etichetta che reclama un valore lo
                // tiene; le successive lo saltano.
                let rows = dedupedLocationRows()
                ForEach(rows, id: \.label) { row in
                    LabeledContent(row.label, value: row.value)
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(titleCase(name))
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Hero

    private var heroRow: some View {
        VStack(spacing: 12) {
            Group {
                if let url = imageURL {
                    CachedAsyncImage(url: url) { img in
                        img.resizable().scaledToFill()
                    } placeholder: {
                        initialsBubble
                    }
                } else {
                    initialsBubble
                }
            }
            .frame(width: 88, height: 88)
            .clipShape(Circle())

            VStack(spacing: 2) {
                Text(titleCase(name))
                    .font(.title3.weight(.semibold))
                Text(roleLabel)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity)
    }

    private var initialsBubble: some View {
        ZStack {
            Color(.tertiarySystemFill)
            Text(initials(from: name))
                .font(.title.weight(.semibold))
                .foregroundStyle(.secondary)
        }
    }

    // MARK: - Helpers

    /// Una riga del blocco "Gruppo locale".
    private struct LocationRow {
        let label: String
        let value: String
    }

    /// Costruisce le righe in ordine gerarchico saltando i campi vuoti e
    /// quelli che ripetono (case-insensitive) un valore gia' mostrato.
    /// Esempio reale: officeName "Lombardia", region "Lombardia",
    /// area "Nord", state "Milano", city "Milano" → mostra:
    /// Gruppo: Lombardia · Area: Nord · Provincia: Milano.
    private func dedupedLocationRows() -> [LocationRow] {
        let candidates: [(String, String)] = [
            (tr("public.member.label.group", fallback: "Gruppo"), officeName),
            (tr("public.member.label.region", fallback: "Regione"), region),
            (tr("public.member.label.area", fallback: "Area"), area),
            (tr("public.member.label.province", fallback: "Provincia"), state),
            (tr("public.member.label.city", fallback: "Città"), city)
        ]
        var seen: [String] = []   // valori gia' usati (lowercased + trimmed)
        var out: [LocationRow] = []
        for (label, raw) in candidates {
            let value = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !value.isEmpty else { continue }
            let key = value.lowercased()
            if seen.contains(key) { continue }
            seen.append(key)
            out.append(LocationRow(label: label, value: value))
        }
        return out
    }

    private var mailtoURL: URL? {
        guard !email.isEmpty else { return nil }
        let subject = tr("public.member.mail_subject", fallback: "Informazioni sul test Mensa")
            .addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        return URL(string: "mailto:\(email)?subject=\(subject)")
    }

    private func initials(from name: String) -> String {
        let parts = name.split(separator: " ").prefix(2)
        let chars = parts.compactMap { $0.first }
        let result = String(chars).uppercased()
        return result.isEmpty ? "?" : result
    }

    private func titleCase(_ s: String) -> String {
        TextFormatters.shared.titleCase(s: s)
    }
}
