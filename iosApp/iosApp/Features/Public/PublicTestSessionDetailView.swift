import SwiftUI
import Shared

/// Dettaglio pubblico di una singola sessione di test.
///
/// Mostra data + luogo + note + capienza max e, soprattutto, la lista degli
/// assistenti assegnati a QUESTA sessione (filtrati per user-id da
/// `LocalOfficeTestDateModel.assistants`). Ogni assistente e' un
/// `NavigationLink` alla `PublicMemberContactView` per scrivere via mail.
struct PublicTestSessionDetailView: View {
    let testDate: LocalOfficeTestDateModel
    let office: LocalOfficeModel
    /// Tutti gli assistenti del gruppo locale, gia' caricati dal parent.
    /// Filtriamo qui per `user` ID per ottenere solo quelli di questa sessione.
    let allAssistants: [LocalOfficeAssistantModel]

    private var sessionAssistants: [LocalOfficeAssistantModel] {
        let ids = Set(testDate.assistants)
        return allAssistants.filter { ids.contains($0.user) }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Kicker + data prominente.
                VStack(alignment: .leading, spacing: 10) {
                    Text(tr("public.test_session.kicker", fallback: "SESSIONE DI TEST"))
                        .font(.caption2.weight(.semibold))
                        .tracking(1.8)
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)

                    Text(italianDate(from: testDate.date))
                        .font(.system(.largeTitle, design: .serif).weight(.bold))
                        .foregroundStyle(.primary)
                        .fixedSize(horizontal: false, vertical: true)

                    Text(office.name)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 8)

                // Card info: location / notes / capienza.
                sectionBlock {
                    VStack(alignment: .leading, spacing: 10) {
                        sectionHeader(tr("public.test_session.section.details", fallback: "Dettagli"))
                        VStack(alignment: .leading, spacing: 12) {
                            if !testDate.location.isEmpty {
                                infoRow(
                                    icon: "mappin.and.ellipse",
                                    title: tr("public.test_session.info.where", fallback: "Dove"),
                                    value: testDate.location
                                )
                            }
                            if testDate.maxParticipants > 0 {
                                infoRow(
                                    icon: "person.2",
                                    title: tr("public.test_session.info.seats", fallback: "Posti"),
                                    value: tr(
                                        "public.test_session.info.seats_value",
                                        fallback: "max {n}",
                                        ["n": "\(testDate.maxParticipants)"]
                                    )
                                )
                            }
                            if !testDate.notes.isEmpty {
                                infoRow(
                                    icon: "note.text",
                                    title: tr("public.test_session.info.notes", fallback: "Note"),
                                    value: testDate.notes
                                )
                            }
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .glassEffect(.regular, in: .rect(cornerRadius: 16))
                    }
                }

                // Assistenti di questa sessione.
                if !sessionAssistants.isEmpty {
                    sectionBlock {
                        VStack(alignment: .leading, spacing: 10) {
                            sectionHeader(tr(
                                "public.test_session.section.assistants",
                                fallback: "Assistenti di questa sessione"
                            ))

                            ForEach(sessionAssistants, id: \.id) { assistant in
                                NavigationLink {
                                    PublicMemberContactView(
                                        name: assistant.name,
                                        roleLabel: tr("public.local_office.role.test_assistant", fallback: "Assistente al test"),
                                        email: assistant.email,
                                        imageURL: assistantImageURL(assistant),
                                        officeName: office.name,
                                        region: office.region,
                                        area: assistant.area,
                                        state: assistant.state,
                                        city: assistant.city
                                    )
                                } label: {
                                    assistantGlassRow(assistant)
                                }
                                .buttonStyle(.plain)
                            }

                            Text(tr(
                                "public.test_session.assistants_footer",
                                fallback: "Tocca un nome per scrivere all'assistente e prenotare il posto."
                            ))
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                                .padding(.top, 4)
                        }
                    }
                } else {
                    sectionBlock {
                        VStack(alignment: .leading, spacing: 10) {
                            sectionHeader(tr(
                                "public.test_session.section.assistants_empty",
                                fallback: "Assistenti"
                            ))
                            Text(tr(
                                "public.test_session.assistants_empty_body",
                                fallback: "Nessun assistente assegnato a questa sessione. Scrivi a un referente del gruppo per informazioni."
                            ))
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .padding(14)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .glassEffect(.regular, in: .rect(cornerRadius: 16))
                        }
                    }
                }

                Color.clear.frame(height: 40)
            }
        }
        .navigationTitle(tr("public.test_session.title", fallback: "Sessione di test"))
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Subviews (stessa estetica della detail principale)

    @ViewBuilder
    private func sectionBlock<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            content()
        }
        .padding(.horizontal, 20)
        .padding(.top, 24)
    }

    @ViewBuilder
    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.title3.weight(.bold))
            .foregroundStyle(.primary)
    }

    @ViewBuilder
    private func infoRow(icon: String, title: String, value: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                .frame(width: 20)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
                Text(value)
                    .font(.subheadline)
                    .foregroundStyle(.primary)
            }
        }
    }

    @ViewBuilder
    private func assistantGlassRow(_ a: LocalOfficeAssistantModel) -> some View {
        HStack(spacing: 12) {
            avatar(url: assistantImageURL(a), name: a.name)

            VStack(alignment: .leading, spacing: 2) {
                Text(titleCase(a.name))
                    .font(.subheadline)
                    .foregroundStyle(.primary)
                    .lineLimit(1)
                if let subtitle = assistantSubtitle(a) {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }

            Spacer(minLength: 0)

            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .glassEffect(.regular, in: .rect(cornerRadius: 14))
        .contentShape(.rect(cornerRadius: 14))
    }

    @ViewBuilder
    private func avatar(url: URL?, name: String) -> some View {
        Group {
            if let url = url {
                CachedAsyncImage(url: url) { img in
                    img.resizable().scaledToFill()
                } placeholder: {
                    initialsBubble(name: name)
                }
            } else {
                initialsBubble(name: name)
            }
        }
        .frame(width: 36, height: 36)
        .clipShape(Circle())
    }

    private func initialsBubble(name: String) -> some View {
        ZStack {
            Color(.tertiarySystemFill)
            Text(initials(from: name))
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
        }
    }

    // MARK: - Helpers (mirror del parent)

    private func assistantImageURL(_ a: LocalOfficeAssistantModel) -> URL? {
        guard !a.image.isEmpty else { return nil }
        return Files.url(
            collection: "view_local_office_assistants",
            recordId: a.id,
            filename: a.image,
            thumb: "200x200"
        )
    }

    private func assistantSubtitle(_ a: LocalOfficeAssistantModel) -> String? {
        var seen: [String] = []
        var parts: [String] = []
        for raw in [a.area, a.state, a.city] {
            let v = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !v.isEmpty else { continue }
            let k = v.lowercased()
            if seen.contains(k) { continue }
            seen.append(k)
            parts.append(v)
        }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
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

    private func italianDate(from instant: Kotlinx_datetimeInstant) -> String {
        DateFormatters.shared.italianLongDate(instant: instant, includeTime: true)
    }
}
