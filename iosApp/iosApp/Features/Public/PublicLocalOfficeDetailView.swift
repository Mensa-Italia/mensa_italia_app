import SwiftUI
import Shared

/// Detail pre-login di un gruppo locale.
///
/// Header con la cover image (riga di List con sfondo trasparente) + sezioni
/// `List` native per date dei test, segretari e assistenti. Niente
/// `GlassCard`, niente custom hero — iOS 26 applica Liquid Glass alle righe.
struct PublicLocalOfficeDetailView: View {
    let officeId: String

    @State private var resolvedOffice: LocalOfficeModel?
    @State private var admins: [LocalOfficeAdminModel]     = []
    @State private var assistants: [LocalOfficeAssistantModel] = []
    @State private var testDates: [LocalOfficeTestDateModel]  = []

    @State private var error: String?

    @State private var adminsSub: Closeable?
    @State private var assistantsSub: Closeable?
    @State private var testDatesSub: Closeable?

    var body: some View {
        Group {
            if let err = error {
                ContentUnavailableView(err, systemImage: "exclamationmark.triangle")
            } else if let office = resolvedOffice {
                officeContent(office)
            } else {
                ProgressView()
            }
        }
        .navigationTitle(resolvedOffice?.name ?? tr("public.local_office.loading_title", fallback: "Gruppo locale"))
        .navigationBarTitleDisplayMode(.inline)
        .task { await start() }
        .onDisappear { stop() }
    }

    /// Clone della struttura del `LocalOfficeView` privato: ScrollView +
    /// VStack con hero parallax 250pt + kicker/titolo/bio + sezioni
    /// "manuali" (sectionHeader + righe glass). Le righe admin/assistant
    /// sono NavigationLink alla `PublicMemberContactView` (no edit
    /// affordances perche' pre-login).
    @ViewBuilder
    private func officeContent(_ office: LocalOfficeModel) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                heroRow(office)

                // Kicker + nome + bio.
                VStack(alignment: .leading, spacing: 10) {
                    Text(tr("public.local_office.kicker", fallback: "GRUPPO LOCALE"))
                        .font(.caption2.weight(.semibold))
                        .tracking(1.8)
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)

                    Text(office.name)
                        .font(.system(.largeTitle, design: .serif).weight(.bold))
                        .foregroundStyle(.primary)
                        .fixedSize(horizontal: false, vertical: true)

                    if !office.region.isEmpty,
                       office.region.compare(office.name, options: .caseInsensitive) != .orderedSame {
                        Text(office.region)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }

                    if !office.bio.isEmpty {
                        Text(office.bio)
                            .font(.body)
                            .foregroundStyle(.primary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 20)
                .padding(.bottom, 8)

                if !testDates.isEmpty {
                    sectionBlock { testDatesGlassSection(office: office) }
                }
                if !admins.isEmpty {
                    sectionBlock { adminsGlassSection(office: office) }
                }
                if !assistants.isEmpty {
                    sectionBlock { assistantsGlassSection(office: office) }
                }

                Color.clear.frame(height: 40)
            }
        }
        .ignoresSafeArea(.container, edges: .top)
        .refreshable { await refresh() }
    }

    // MARK: - Section wrapper (stesso del privato)

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

    // MARK: - Test dates section (glass)

    @ViewBuilder
    private func testDatesGlassSection(office: LocalOfficeModel) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionHeader(tr("public.local_office.section.test_dates", fallback: "Prossime sessioni di test"))

            ForEach(testDates, id: \.id) { td in
                NavigationLink {
                    PublicTestSessionDetailView(
                        testDate: td,
                        office: office,
                        allAssistants: assistants
                    )
                } label: {
                    testDateGlassCard(td)
                }
                .buttonStyle(.plain)
            }

            Text(tr(
                "public.local_office.test_dates_footer",
                fallback: "Tocca una sessione per vedere chi contattare e prenotare il posto."
            ))
                .font(.footnote)
                .foregroundStyle(.secondary)
                .padding(.top, 4)
        }
    }

    @ViewBuilder
    private func testDateGlassCard(_ td: LocalOfficeTestDateModel) -> some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                Text(italianDate(from: td.date))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)

                if !td.location.isEmpty {
                    Label(td.location, systemImage: "mappin.and.ellipse")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                if !td.notes.isEmpty {
                    Text(td.notes)
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                        .lineLimit(3)
                }
            }

            Spacer(minLength: 8)

            VStack(alignment: .trailing, spacing: 4) {
                if td.maxParticipants > 0 {
                    Text(tr(
                        "public.local_office.test_date.max_short",
                        fallback: "max {n}",
                        ["n": "\(td.maxParticipants)"]
                    ))
                        .font(.caption2.weight(.semibold))
                        .padding(.horizontal, 8).padding(.vertical, 4)
                        .background(AppTheme.Colors.brandPrimary.opacity(0.12), in: Capsule())
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                }
                if !td.assistants.isEmpty {
                    Text(td.assistants.count == 1
                         ? tr("public.local_office.test_date.assistants_one", fallback: "1 assistente")
                         : tr(
                            "public.local_office.test_date.assistants_other",
                            fallback: "{n} assistenti",
                            ["n": "\(td.assistants.count)"]
                         ))
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
            }

            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.tertiary)
                .padding(.leading, 4)
        }
        .padding(14)
        .glassEffect(.regular, in: .rect(cornerRadius: 16))
        .contentShape(.rect(cornerRadius: 16))
    }

    // MARK: - Admins section (glass)

    @ViewBuilder
    private func adminsGlassSection(office: LocalOfficeModel) -> some View {
        let sorted = admins.sorted { a, b in
            if a.isTheOfficer != b.isTheOfficer { return a.isTheOfficer }
            return a.name < b.name
        }

        VStack(alignment: .leading, spacing: 10) {
            sectionHeader(tr("public.local_office.section.admins", fallback: "Referenti"))

            ForEach(sorted, id: \.id) { admin in
                NavigationLink {
                    PublicMemberContactView(
                        name: admin.name,
                        roleLabel: admin.isTheOfficer
                            ? tr("public.local_office.role.officer", fallback: "Segretario")
                            : tr("public.local_office.role.co_officer", fallback: "Cosegretario"),
                        email: admin.email,
                        imageURL: adminImageURL(admin),
                        officeName: office.name,
                        region: office.region
                    )
                } label: {
                    adminGlassRow(admin)
                }
                .buttonStyle(.plain)
            }

            Text(tr(
                "public.local_office.admins_footer",
                fallback: "Tocca un nome per scrivere al referente."
            ))
                .font(.footnote)
                .foregroundStyle(.secondary)
                .padding(.top, 4)
        }
    }

    @ViewBuilder
    private func adminGlassRow(_ admin: LocalOfficeAdminModel) -> some View {
        HStack(spacing: 12) {
            avatar(url: adminImageURL(admin), name: admin.name)

            Text(titleCase(admin.name))
                .font(.subheadline)
                .foregroundStyle(.primary)
                .lineLimit(1)

            Spacer(minLength: 8)

            if admin.isTheOfficer {
                Text(tr("public.local_office.role.officer", fallback: "Segretario"))
                    .font(.caption2.weight(.semibold))
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(AppTheme.Colors.brandPrimary.opacity(0.15), in: Capsule())
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            } else {
                Text(tr("public.local_office.role.co_officer", fallback: "Cosegretario"))
                    .font(.caption2.weight(.semibold))
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(Color.secondary.opacity(0.15), in: Capsule())
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .glassEffect(.regular, in: .rect(cornerRadius: 14))
        .contentShape(.rect(cornerRadius: 14))
    }

    // MARK: - Assistants section (glass)

    @ViewBuilder
    private func assistantsGlassSection(office: LocalOfficeModel) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionHeader(tr("public.local_office.section.assistants", fallback: "Assistenti al test"))

            ForEach(assistants, id: \.id) { assistant in
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
                "public.local_office.assistants_footer",
                fallback: "Gli assistenti gestiscono le sessioni di test. Toccane uno per contattarlo."
            ))
                .font(.footnote)
                .foregroundStyle(.secondary)
                .padding(.top, 4)
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
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .glassEffect(.regular, in: .rect(cornerRadius: 14))
        .contentShape(.rect(cornerRadius: 14))
    }

    // MARK: - Rows

    /// Hero parallax stretchy clonato dal `LocalOfficeView` privato: 250pt,
    /// brand gradient di placeholder, gradient nero in coda. `GeometryReader`
    /// misura il pull-down e estende l'immagine cosi' resta sempre attaccata
    /// in cima durante l'overscroll.
    @ViewBuilder
    private func heroRow(_ office: LocalOfficeModel) -> some View {
        let coverURL: URL? = office.image.isEmpty ? nil : Files.url(
            collection: "view_local_office",
            recordId: office.id,
            filename: office.image,
            thumb: "0x500"
        )

        GeometryReader { geo in
            let minY = geo.frame(in: .global).minY
            let stretch = max(0, minY)
            ZStack(alignment: .bottom) {
                Group {
                    if let url = coverURL {
                        CachedAsyncImage(url: url) { img in
                            img.resizable().aspectRatio(contentMode: .fill)
                        } placeholder: {
                            AppTheme.brandGradient
                        }
                    } else {
                        AppTheme.brandGradient
                            .overlay(
                                Image(systemName: "building.2.fill")
                                    .font(.system(size: 48, weight: .semibold))
                                    .foregroundStyle(.white.opacity(0.85))
                            )
                    }
                }
                .frame(width: geo.size.width, height: geo.size.height + stretch)
                .clipped()

                LinearGradient(
                    colors: [.black.opacity(0), .black.opacity(0.45)],
                    startPoint: .top, endPoint: .bottom
                )
                .frame(width: geo.size.width, height: geo.size.height + stretch)
            }
            .frame(width: geo.size.width, height: geo.size.height + stretch)
            .clipped()
            .offset(y: -stretch)
        }
        .frame(height: 250)
    }

    /// Sottotitolo della riga: area · provincia · citta', con dedup
    /// case-insensitive sui valori uguali (es. "Milano" provincia +
    /// "Milano" citta' → resta uno solo).
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

    // MARK: - Helpers

    private func adminImageURL(_ admin: LocalOfficeAdminModel) -> URL? {
        guard !admin.image.isEmpty else { return nil }
        return Files.url(
            collection: "view_local_office_admins",
            recordId: admin.id,
            filename: admin.image,
            thumb: "400x400"
        )
    }

    private func assistantImageURL(_ a: LocalOfficeAssistantModel) -> URL? {
        guard !a.image.isEmpty else { return nil }
        return Files.url(
            collection: "view_local_office_assistants",
            recordId: a.id,
            filename: a.image,
            thumb: "200x200"
        )
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

    // MARK: - Data lifecycle

    private func start() async {
        if resolvedOffice == nil {
            do {
                // Usa SEMPRE il lookup pubblico (view_local_office) — siamo
                // pre-login, l'endpoint autenticato darebbe 401.
                if let office = try await koin.localOffices.officeByIdPublic(id: officeId) {
                    await MainActor.run { resolvedOffice = office }
                } else {
                    await MainActor.run { self.error = tr("public.local_office.not_found", fallback: "Gruppo locale non trovato.") }
                    return
                }
            } catch {
                await MainActor.run { self.error = (error as NSError).localizedDescription }
                return
            }
        }

        guard resolvedOffice != nil else { return }
        subscribeAll(officeId: officeId)

        try? await koin.localOffices.refreshAdmins(officeId: officeId)
        try? await koin.localOffices.refreshAssistants(officeId: officeId)
        try? await koin.localOffices.refreshUpcomingTestDates(officeId: officeId)
    }

    private func subscribeAll(officeId: String) {
        adminsSub?.close()
        adminsSub = FlowBridgeKt.subscribe(
            flow: koin.localOffices.observeAdmins(officeId: officeId),
            onEach: { value in
                Task { @MainActor in
                    self.admins = (value as? [LocalOfficeAdminModel]) ?? []
                }
            },
            onError: { _ in }
        )

        assistantsSub?.close()
        assistantsSub = FlowBridgeKt.subscribe(
            flow: koin.localOffices.observeAssistants(officeId: officeId),
            onEach: { value in
                Task { @MainActor in
                    self.assistants = (value as? [LocalOfficeAssistantModel]) ?? []
                }
            },
            onError: { _ in }
        )

        testDatesSub?.close()
        testDatesSub = FlowBridgeKt.subscribe(
            flow: koin.localOffices.observeUpcomingTestDates(officeId: officeId),
            onEach: { value in
                Task { @MainActor in
                    self.testDates = (value as? [LocalOfficeTestDateModel]) ?? []
                }
            },
            onError: { _ in }
        )
    }

    private func stop() {
        adminsSub?.close();     adminsSub = nil
        assistantsSub?.close(); assistantsSub = nil
        testDatesSub?.close();  testDatesSub = nil
    }

    private func refresh() async {
        try? await koin.localOffices.refreshAdmins(officeId: officeId)
        try? await koin.localOffices.refreshAssistants(officeId: officeId)
        try? await koin.localOffices.refreshUpcomingTestDates(officeId: officeId)
    }
}
