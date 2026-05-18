import SwiftUI
import Shared

// MARK: - Route types

/// Deep-link / navigation value for the local office screen.
/// Internal navigation value — always carries the PocketBase office id
/// (the only unambiguous primary key).
struct LocalOfficeRoute: Hashable {
    let officeId: String
}

/// Navigation value for entry points that have only a slug (search hits,
/// `mensa://local-office/<slug>` deep links). The destination view
/// (`LocalOfficeBySlugLoader`) resolves the slug to an office id and then
/// renders the canonical `LocalOfficeView`.
struct LocalOfficeSlugRoute: Hashable {
    let slug: String
}

/// Navigation value for jumping to an event detail from within LocalOfficeView.
private struct EventDetailRoute: Hashable { let eventId: String }

/// Navigation value for jumping to a sig detail from within LocalOfficeView.
private struct SigDetailRoute: Hashable { let sigId: String }

// MARK: - Main view

struct LocalOfficeView: View {
    /// PocketBase office id — the only key this view accepts. External entry
    /// points that arrive with a slug must resolve it first (see
    /// `LocalOfficeBySlugLoader`).
    let officeId: String

    // Resolved from slug (may be pre-populated via the convenience init)
    @State private var resolvedOffice: LocalOfficeModel? = nil

    // Per-office data
    @State private var linktree:   [LocalOfficeLinktreeRowModel] = []
    @State private var admins:     [LocalOfficeAdminModel]       = []
    @State private var assistants: [LocalOfficeAssistantModel]   = []
    @State private var testDates:  [LocalOfficeTestDateModel]    = []
    @State private var events:     [EventModel]                  = []
    @State private var sigs:       [SigModel]                    = []

    @State private var loading = false
    @State private var error: String? = nil
    @State private var appeared = false
    /// Same opt-in pattern as the global search Eventi chip: past events are
    /// hidden by default and surfaced via a toggle row at the bottom of the
    /// events section.
    @State private var showPastEvents = false

    /// Letto sincrono dall'auth — session-stable, cambia solo a login/logout.
    private var currentUser: UserModel? {
        koin.auth.currentUser.value as? UserModel
    }

    // Editor sheets — test dates
    @State private var creatingTestDate = false
    @State private var editingTestDate: LocalOfficeTestDateModel? = nil
    @State private var testDateToDelete: LocalOfficeTestDateModel? = nil
    @State private var showDeleteTestDateConfirm = false

    // Editor sheets — linktree
    @State private var creatingLinkMode: LinkEditorMode? = nil
    @State private var editingLink: LocalOfficeLinktreeRowModel? = nil
    @State private var linkToDelete: LocalOfficeLinktreeRowModel? = nil
    @State private var showDeleteLinkConfirm = false

    // Flow subscriptions (6)
    @State private var linktreeSub:   Closeable? = nil
    @State private var adminsSub:     Closeable? = nil
    @State private var assistantsSub: Closeable? = nil
    @State private var testDatesSub:  Closeable? = nil
    @State private var eventsSub:     Closeable? = nil
    @State private var sigsSub:       Closeable? = nil

    // MARK: - Permission gate

    /// I poteri di edit hanno due path:
    ///   1. Utente "super" → autorizzato globalmente, deciso SINCRONO,
    ///      la toolbar mostra i bottoni dal primo frame.
    ///   2. Admin/assistant di QUESTO office → richiede `admins`/`assistants`
    ///      dal flow, per forza async.
    private var canEdit: Bool {
        guard let user = currentUser else { return false }
        if user.powers.contains("super") { return true }
        let adminIds = admins.map { $0.user }
        let assistantIds = assistants.map { $0.user }
        return adminIds.contains(user.id) || assistantIds.contains(user.id)
    }

    // MARK: Initializers

    init(officeId: String) {
        self.officeId = officeId
    }

    init(office: LocalOfficeModel) {
        self.officeId = office.id
        self._resolvedOffice = State(initialValue: office)
    }

    // MARK: - Body

    var body: some View {
        Group {
            if let err = error {
                ContentUnavailableView(
                    err,
                    systemImage: "exclamationmark.triangle"
                )
            } else if let office = resolvedOffice {
                officeContent(office)
            } else {
                LoadingDots()
            }
        }
        .navigationTitle(resolvedOffice?.name ?? tr("local_office.loading_title", fallback: "Gruppo locale"))
        .navigationBarTitleDisplayMode(.inline)
        .cleanNavBar()
        .task { await start() }
        .onDisappear { stop() }
        .navigationDestination(for: EventDetailRoute.self) { route in
            EventDetailView(eventId: route.eventId)
        }
        .navigationDestination(for: SigDetailRoute.self) { route in
            SigDetailView(sigId: route.sigId)
        }
        .navigationDestination(for: RegSociRoute.self) { route in
            MemberDetailView(memberId: route.id)
        }
        .navigationDestination(for: LocalOfficeLinktreeRoute.self) { route in
            LocalOfficeLinktreeView(officeId: route.officeId)
        }
        // Test date editor sheets
        .sheet(isPresented: $creatingTestDate) {
            LocalOfficeTestDateEditorSheet(
                officeId: officeId,
                assistantsCandidates: assistants,
                mode: .create
            )
        }
        .sheet(item: $editingTestDate) { td in
            LocalOfficeTestDateEditorSheet(
                officeId: officeId,
                assistantsCandidates: assistants,
                mode: .edit(existing: td)
            )
        }
        // Linktree editor sheets
        .sheet(item: $creatingLinkMode) { linkMode in
            LocalOfficeLinkEditorSheet(
                officeId: officeId,
                siblings: linktree,
                parentCandidates: linktree.filter { $0.kind == "section" },
                mode: linkMode
            )
        }
        .sheet(item: $editingLink) { link in
            LocalOfficeLinkEditorSheet(
                officeId: officeId,
                siblings: linktree.filter { $0.parent == link.parent },
                parentCandidates: linktree.filter { $0.kind == "section" },
                mode: .edit(existing: link)
            )
        }
        // Delete confirmations
        .alert(
            tr("local_office.test_dates.delete_confirm", fallback: "Vuoi eliminare questa sessione?"),
            isPresented: $showDeleteTestDateConfirm,
            presenting: testDateToDelete
        ) { td in
            Button(tr("local_office.editor.delete", fallback: "Elimina"), role: .destructive) {
                Task { await deleteTestDate(td) }
            }
            Button(tr("local_office.editor.cancel", fallback: "Annulla"), role: .cancel) {}
        } message: { td in
            Text(italianDate(from: td.date))
        }
        .alert(
            tr("local_office.links.delete_confirm", fallback: "Vuoi eliminare questa voce?"),
            isPresented: $showDeleteLinkConfirm,
            presenting: linkToDelete
        ) { link in
            Button(tr("local_office.editor.delete", fallback: "Elimina"), role: .destructive) {
                Task { await deleteLink(link) }
            }
            Button(tr("local_office.editor.cancel", fallback: "Annulla"), role: .cancel) {}
        } message: { link in
            Text(link.title)
        }
    }

    // MARK: - Scrollable content

    @ViewBuilder
    private func officeContent(_ office: LocalOfficeModel) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // 1. Hero
                heroSection(office)

                // 2. Kicker + title + bio
                VStack(alignment: .leading, spacing: 10) {
                    Text(tr("local_office.kicker", fallback: "GRUPPO LOCALE").uppercased())
                        .font(.caption2.weight(.semibold))
                        .tracking(1.8)
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)

                    Text(office.name)
                        .font(.system(.largeTitle, design: .serif).weight(.bold))
                        .foregroundStyle(.primary)
                        .fixedSize(horizontal: false, vertical: true)

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

                // 3. Linktree
                if !linktree.isEmpty || canEdit {
                    sectionBlock(index: 0) {
                        linktreeSection
                    }
                }

                // 4. Test dates
                if !testDates.isEmpty || canEdit {
                    sectionBlock(index: 1) {
                        testDatesSection
                    }
                }

                // 5. Events
                if !events.isEmpty {
                    sectionBlock(index: 2) {
                        eventsSection
                    }
                }

                // 6. Sigs
                if !sigs.isEmpty {
                    sectionBlock(index: 3) {
                        sigsSection
                    }
                }

                // 7. Admins (referenti)
                if !admins.isEmpty {
                    sectionBlock(index: 4) {
                        adminsSection
                    }
                }

                // 8. Assistants
                if !assistants.isEmpty {
                    sectionBlock(index: 5) {
                        assistantsSection
                    }
                }

                Color.clear.frame(height: 40)
            }
        }
        .ignoresSafeArea(.container, edges: .top)
        .refreshable { await refresh() }
    }

    // MARK: - Hero

    @ViewBuilder
    private func heroSection(_ office: LocalOfficeModel) -> some View {
        let coverURL: URL? = {
            guard !office.image.isEmpty else { return nil }
            return Files.url(
                collection: "local_offices",
                recordId: office.id,
                filename: office.image,
                thumb: "0x500"
            )
        }()

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

    // MARK: - Linktree section (preview — top-2 root links + "Vedi tutti")

    @ViewBuilder
    private var linktreeSection: some View {
        let sorted = linktree.sorted { $0.sortOrder < $1.sortOrder }
        let rootLinks = sorted.filter { $0.parent == "" && $0.kind == "link" }
        let hasSections = sorted.contains { $0.parent == "" && $0.kind == "section" }
        let previewLinks = Array(rootLinks.prefix(2))
        let showSeeAll = rootLinks.count > 2 || hasSections

        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .center) {
                sectionHeader(tr("local_office.linktree.title", fallback: "Link utili"))
                Spacer(minLength: 8)
                HStack(spacing: 12) {
                    if canEdit {
                        NavigationLink(value: LocalOfficeLinktreeRoute(officeId: officeId)) {
                            Text(tr("local_office.linktree.edit", fallback: "Modifica"))
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                        }
                        .buttonStyle(.plain)
                    }
                    if showSeeAll {
                        NavigationLink(value: LocalOfficeLinktreeRoute(officeId: officeId)) {
                            Text(tr("local_office.linktree.see_all", fallback: "Vedi tutti"))
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            VStack(spacing: 8) {
                ForEach(previewLinks, id: \.id) { link in
                    linktreeRow(link)
                }
                if linktree.isEmpty {
                    Text(tr("local_office.linktree.empty", fallback: "Nessun link"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .padding(.vertical, 8)
                }
            }
        }
    }

    @ViewBuilder
    private func linktreeRow(_ row: LocalOfficeLinktreeRowModel) -> some View {
        Button {
            if let url = URL(string: row.url) {
                UIApplication.shared.open(url)
            }
        } label: {
            HStack(spacing: 12) {
                iconView(row.icon)
                    .frame(width: 24, height: 24)
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)

                Text(row.title)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.primary)
                    .lineLimit(1)

                Spacer(minLength: 0)

                Image(systemName: "chevron.up.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
            }
            .frame(height: 52)
            .padding(.horizontal, 16)
            .glassEffect(.regular, in: .rect(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }

    /// Resolves the icon field: emoji → Text, brand name → SF symbol, SF symbol, fallback link.
    @ViewBuilder
    private func iconView(_ raw: String) -> some View {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.unicodeScalars.count == 1 && trimmed.unicodeScalars.first.map({ $0.value > 127 }) == true {
            // Single non-ASCII scalar → treat as emoji
            Text(trimmed).font(.system(size: 18))
        } else if let sfForBrand = brandSFSymbol(trimmed.lowercased()) {
            Image(systemName: sfForBrand)
        } else if !trimmed.isEmpty && UIImage(systemName: trimmed) != nil {
            Image(systemName: trimmed)
        } else {
            Image(systemName: "link")
        }
    }

    private func brandSFSymbol(_ name: String) -> String? {
        switch name {
        case "instagram":  return "camera.filters"
        case "facebook":   return "f.cursive.circle.fill"
        case "twitter":    return "bird.fill"
        case "tiktok":     return "music.note"
        case "youtube":    return "play.rectangle.fill"
        case "telegram":   return "paperplane.fill"
        case "whatsapp":   return "message.fill"
        case "linkedin":   return "person.crop.rectangle.stack.fill"
        case "github":     return "chevron.left.forwardslash.chevron.right"
        case "email":      return "envelope.fill"
        default:           return nil
        }
    }

    // MARK: - Test dates section

    @ViewBuilder
    private var testDatesSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .center) {
                sectionHeader(tr("local_office.test_dates.title", fallback: "Prossime sessioni di test"))
                Spacer(minLength: 8)
                if canEdit {
                    Button {
                        creatingTestDate = true
                    } label: {
                        Label(tr("local_office.test_dates.add", fallback: "Aggiungi sessione"), systemImage: "plus.circle")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                    }
                    .buttonStyle(.plain)
                }
            }

            ForEach(testDates, id: \.id) { td in
                GlassCard(padding: 14, cornerRadius: 16) {
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(italianDate(from: td.date))
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(.primary)

                            if !td.location.isEmpty {
                                Text(td.location)
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

                        HStack(spacing: 8) {
                            if td.maxParticipants > 0 {
                                Text("max \(td.maxParticipants) \(tr("local_office.test_dates.participants", fallback: "partecipanti"))")
                                    .font(.caption2.weight(.semibold))
                                    .padding(.horizontal, 8).padding(.vertical, 4)
                                    .background(AppTheme.Colors.brandPrimary.opacity(0.12), in: Capsule())
                                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                            }

                            if canEdit {
                                Menu {
                                    Button {
                                        editingTestDate = td
                                    } label: {
                                        Label(tr("local_office.links.edit", fallback: "Modifica"), systemImage: "pencil")
                                    }
                                    Button(role: .destructive) {
                                        testDateToDelete = td
                                        showDeleteTestDateConfirm = true
                                    } label: {
                                        Label(tr("local_office.editor.delete", fallback: "Elimina"), systemImage: "trash")
                                    }
                                } label: {
                                    Image(systemName: "ellipsis.circle")
                                        .font(.body)
                                        .foregroundStyle(.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private func italianDate(from instant: Kotlinx_datetimeInstant) -> String {
        DateFormatters.shared.italianLongDate(instant: instant, includeTime: true)
    }

    // MARK: - Events section

    private var nowSeconds: Int64 { Int64(Date().timeIntervalSince1970) }

    /// Split the events list into upcoming (whenEnd >= now) and past, preserving
    /// the backend ordering inside each bucket.
    private var splitEvents: (upcoming: [EventModel], past: [EventModel]) {
        var upcoming: [EventModel] = []
        var past:     [EventModel] = []
        let now = nowSeconds
        for ev in events {
            if ev.whenEnd.epochSeconds >= now { upcoming.append(ev) }
            else                              { past.append(ev) }
        }
        return (upcoming, past)
    }

    @ViewBuilder
    private var eventsSection: some View {
        let split = splitEvents
        VStack(alignment: .leading, spacing: 10) {
            sectionHeader(tr("local_office.events.title", fallback: "Eventi del gruppo"))

            // Upcoming — always visible.
            ForEach(split.upcoming, id: \.id) { ev in
                NavigationLink(value: EventDetailRoute(eventId: ev.id)) {
                    EventRowCard(event: ev)
                }
                .buttonStyle(.plain)
            }

            // Past events — opt-in via a toggle row (same UX as the search Eventi chip).
            if !split.past.isEmpty {
                if showPastEvents {
                    ForEach(split.past, id: \.id) { ev in
                        NavigationLink(value: EventDetailRoute(eventId: ev.id)) {
                            EventRowCard(event: ev)
                        }
                        .buttonStyle(.plain)
                    }
                }
                togglePastEventsRow(hiddenCount: split.past.count, showing: showPastEvents)
            }
        }
    }

    /// Button-shaped capsule that mirrors the brand-tinted toggle used in
    /// global search. Tap flips `showPastEvents` with a quick ease animation.
    @ViewBuilder
    private func togglePastEventsRow(hiddenCount: Int, showing: Bool) -> some View {
        Button {
            withAnimation(.easeInOut(duration: 0.2)) { showPastEvents.toggle() }
        } label: {
            HStack(spacing: 8) {
                Image(systemName: showing ? "eye.slash" : "clock.arrow.circlepath")
                    .font(.subheadline.weight(.semibold))
                Text(showing
                     ? tr("local_office.events.hide_past", fallback: "Nascondi eventi passati")
                     : tr("local_office.events.show_past", fallback: "Mostra eventi passati"))
                    .font(.subheadline.weight(.semibold))
                if !showing && hiddenCount > 0 {
                    Text("(\(hiddenCount))")
                        .font(.subheadline.monospacedDigit())
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: showing ? "chevron.up" : "chevron.down")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
            }
            .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(Color.primary.opacity(0.06), lineWidth: 0.5)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .padding(.top, 4)
    }

    // MARK: - Sigs section

    @ViewBuilder
    private var sigsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionHeader(tr("app.discover.groups", fallback: "Gruppi e interessi"))

            ForEach(sigs, id: \.id) { sig in
                NavigationLink(value: SigDetailRoute(sigId: sig.id)) {
                    sigRow(sig)
                }
                .buttonStyle(.plain)
            }
        }
    }

    @ViewBuilder
    private func sigRow(_ sig: SigModel) -> some View {
        let imageURL: URL? = {
            guard !sig.image.isEmpty else { return nil }
            if sig.image.hasPrefix("http") { return URL(string: sig.image) }
            return Files.url(collection: "sigs", recordId: sig.id, filename: sig.image, thumb: "200x200")
        }()

        HStack(spacing: 12) {
            Group {
                if let url = imageURL {
                    CachedAsyncImage(url: url) { img in
                        img.resizable().scaledToFill()
                    } placeholder: {
                        AppTheme.brandGradient
                    }
                } else {
                    AppTheme.brandGradient
                }
            }
            .frame(width: 40, height: 40)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

            Text(sig.name)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.primary)
                .lineLimit(1)

            Spacer(minLength: 0)

            if !sig.groupType.isEmpty {
                Text(sig.groupType.replacingOccurrences(of: "_", with: " ").capitalized)
                    .font(.caption2.weight(.semibold))
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(AppTheme.Colors.brandPrimary.opacity(0.12), in: Capsule())
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .glassEffect(.regular, in: .rect(cornerRadius: 14))
    }

    // MARK: - Admins (referenti) section

    @ViewBuilder
    private var adminsSection: some View {
        let sorted = admins.sorted { a, b in
            if a.isTheOfficer != b.isTheOfficer { return a.isTheOfficer }
            return a.name < b.name
        }

        VStack(alignment: .leading, spacing: 10) {
            sectionHeader(tr("local_office.admins.title", fallback: "Referenti"))

            ForEach(sorted, id: \.id) { admin in
                NavigationLink(value: RegSociRoute(id: admin.user)) {
                    adminRow(admin)
                }
                .buttonStyle(.plain)
            }
        }
    }

    @ViewBuilder
    private func adminRow(_ admin: LocalOfficeAdminModel) -> some View {
        let imageURL: URL? = {
            guard !admin.image.isEmpty else { return nil }
            return Files.url(
                collection: "view_local_office_admins",
                recordId: admin.id,
                filename: admin.image,
                thumb: "200x200"
            )
        }()

        HStack(spacing: 12) {
            avatarView(url: imageURL, name: admin.name, size: 40)

            Text(titleCase(admin.name))
                .font(.subheadline)
                .foregroundStyle(.primary)
                .lineLimit(1)

            Spacer(minLength: 0)

            if admin.isTheOfficer {
                Text(tr("local_office.admin.officer", fallback: "Segretario"))
                    .font(.caption2.weight(.semibold))
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(AppTheme.Colors.brandPrimary.opacity(0.15), in: Capsule())
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            } else {
                Text(tr("local_office.admin.co_officer", fallback: "Cosegretario"))
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

    // MARK: - Assistants section

    @ViewBuilder
    private var assistantsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionHeader(tr("local_office.assistants.title", fallback: "Assistenti al test"))

            ForEach(assistants, id: \.id) { assistant in
                NavigationLink(value: RegSociRoute(id: assistant.user)) {
                    assistantRow(assistant)
                }
                .buttonStyle(.plain)
            }
        }
    }

    @ViewBuilder
    private func assistantRow(_ assistant: LocalOfficeAssistantModel) -> some View {
        let imageURL: URL? = {
            guard !assistant.image.isEmpty else { return nil }
            return Files.url(
                collection: "view_local_office_assistants",
                recordId: assistant.id,
                filename: assistant.image,
                thumb: "200x200"
            )
        }()

        HStack(spacing: 12) {
            avatarView(url: imageURL, name: assistant.name, size: 40)

            Text(titleCase(assistant.name))
                .font(.subheadline)
                .foregroundStyle(.primary)
                .lineLimit(1)

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .glassEffect(.regular, in: .rect(cornerRadius: 14))
        .contentShape(.rect(cornerRadius: 14))
    }

    // MARK: - Shared subviews

    @ViewBuilder
    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.title3.weight(.bold))
            .foregroundStyle(.primary)
    }

    /// Wraps a section in standard padding + stagger-appear animation.
    @ViewBuilder
    private func sectionBlock<Content: View>(index: Int, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            content()
        }
        .padding(.horizontal, 20)
        .padding(.top, 24)
        .opacity(appeared ? 1 : 0)
        .offset(y: appeared ? 0 : 14)
        .animation(
            .spring(response: 0.5, dampingFraction: 0.86)
                .delay(Double(index) * 0.07),
            value: appeared
        )
    }

    @ViewBuilder
    private func avatarView(url: URL?, name: String, size: CGFloat) -> some View {
        Group {
            if let url = url {
                CachedAsyncImage(url: url) { img in
                    img.resizable().scaledToFill()
                } placeholder: {
                    initialBubble(name: name, size: size)
                }
            } else {
                initialBubble(name: name, size: size)
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(Circle().stroke(.white.opacity(0.6), lineWidth: 1))
    }

    private func initialBubble(name: String, size: CGFloat) -> some View {
        ZStack {
            AppTheme.brandGradient
            Text(initials(from: name))
                .font(.system(size: size * 0.4, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
        }
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

    // MARK: - Data lifecycle

    private func start() async {
        loading = true
        defer { loading = false }

        // Resolution priority:
        //   1. Pre-populated via `init(office:)` — skip everything.
        //   2. `officeId` passed in the route (from an in-app list tap) — look
        //      up by id in the all-offices cache, refreshing if needed.
        //   3. Slug fallback (deep-link entry) — `loadBySlug` resolves and
        //      caches both office + linktree.
        if resolvedOffice == nil {
            do {
                if let office = try await koin.localOffices.officeById(id: officeId) {
                    await MainActor.run { resolvedOffice = office }
                    // Linktree isn't loaded by listAll — pull it now.
                    try? await koin.localOffices.refreshLinktreeByOffice(officeId: office.id)
                } else {
                    await MainActor.run {
                        self.error = tr(
                            "local_office.not_found",
                            fallback: "Gruppo locale non trovato."
                        )
                    }
                    return
                }
            } catch {
                await MainActor.run { self.error = error.localizedDescription }
                return
            }
        }

        guard let office = resolvedOffice else { return }
        subscribeAll(officeId: office.id)

        // Fresh fetch (resolution above already fetched linktree+office;
        // this catches admins/assistants/test_dates/events/sigs).
        try? await koin.localOffices.refreshAllForOffice(officeId: office.id)
        withAnimation(.easeOut(duration: 0.35)) { appeared = true }
    }

    private func subscribeAll(officeId: String) {
        linktreeSub?.close()
        linktreeSub = FlowBridgeKt.subscribe(
            flow: koin.localOffices.observeLinktree(officeId: officeId),
            onEach: { value in
                Task { @MainActor in
                    self.linktree = (value as? [LocalOfficeLinktreeRowModel]) ?? []
                }
            },
            onError: { _ in }
        )

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

        eventsSub?.close()
        eventsSub = FlowBridgeKt.subscribe(
            flow: koin.localOffices.observeEvents(officeId: officeId),
            onEach: { value in
                Task { @MainActor in
                    self.events = (value as? [EventModel]) ?? []
                }
            },
            onError: { _ in }
        )

        sigsSub?.close()
        sigsSub = FlowBridgeKt.subscribe(
            flow: koin.localOffices.observeSigs(officeId: officeId),
            onEach: { value in
                Task { @MainActor in
                    self.sigs = (value as? [SigModel]) ?? []
                }
            },
            onError: { _ in }
        )
    }

    private func stop() {
        linktreeSub?.close();     linktreeSub = nil
        adminsSub?.close();       adminsSub = nil
        assistantsSub?.close();   assistantsSub = nil
        testDatesSub?.close();    testDatesSub = nil
        eventsSub?.close();       eventsSub = nil
        sigsSub?.close();         sigsSub = nil
    }

    // MARK: - Delete actions

    private func deleteTestDate(_ td: LocalOfficeTestDateModel) async {
        do {
            try await koin.localOffices.deleteTestDate(officeId: officeId, id: td.id)
        } catch {
            // Silently ignore — the cached list will remain unchanged; user can retry.
        }
    }

    private func deleteLink(_ link: LocalOfficeLinktreeRowModel) async {
        do {
            try await koin.localOffices.deleteLink(officeId: officeId, id: link.id)
        } catch {
            // Silently ignore — the cached list will remain unchanged; user can retry.
        }
    }

    private func refresh() async {
        guard let office = resolvedOffice else { return }
        try? await koin.localOffices.refreshAllForOffice(officeId: office.id)
    }
}

// MARK: - Slug → id loader (deep-link / search entry)

/// Thin wrapper that resolves a slug to a PocketBase office id, then renders
/// the canonical `LocalOfficeView`. Used as the destination view for every
/// external entry point that carries a slug — push notifications, search hits,
/// `mensa://local-office/<slug>` deep links. Keeps slug handling out of the
/// main view's contract.
struct LocalOfficeBySlugLoader: View {
    let slug: String

    @State private var officeId: String? = nil
    @State private var error: String? = nil

    var body: some View {
        Group {
            if let id = officeId {
                LocalOfficeView(officeId: id)
            } else if let err = error {
                ContentUnavailableView(err, systemImage: "exclamationmark.triangle")
            } else {
                LoadingDots()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .task { await resolve() }
    }

    private func resolve() async {
        guard officeId == nil, error == nil else { return }
        do {
            if let office = try await koin.localOffices.officeBySlug(slug: slug) {
                await MainActor.run { officeId = office.id }
            } else {
                await MainActor.run {
                    error = tr("local_office.not_found", fallback: "Gruppo locale non trovato.")
                }
            }
        } catch {
            await MainActor.run { self.error = error.localizedDescription }
        }
    }
}

// MARK: - Identifiable conformances for sheet(item:)

extension LocalOfficeTestDateModel: Identifiable {}
extension LocalOfficeLinktreeRowModel: Identifiable {}

// Make LinkEditorMode Identifiable so it can be used with sheet(item:)
extension LinkEditorMode: Identifiable {
    var id: String {
        switch self {
        case .createSection: return "create-section"
        case .createLink: return "create-link"
        case .edit(let existing): return "edit-\(existing.id)"
        }
    }
}

#Preview {
    NavigationStack { LocalOfficeBySlugLoader(slug: "lombardia") }
}
