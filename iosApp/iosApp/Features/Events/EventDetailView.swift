import SwiftUI
import MapKit
import Shared

@MainActor @Observable
final class EventDetailViewModel {
    var event: EventModel? = nil
    var schedules: [EventScheduleModel] = []
    var loading = false
    var error: String? = nil
    private var sub: Closeable?

    func start(id: String) {
        // Pull from DB synchronously first (cached → instant render).
        Task { [weak self] in
            if let e = try? await koin.events.getById(id: id) {
                await MainActor.run { self?.event = e }
            }
            await self?.refreshRemote(id: id)
        }
        subscribeSchedules(id: id)
    }

    func stop() {
        sub?.close(); sub = nil
    }

    private func subscribeSchedules(id: String) {
        let flow = koin.eventSchedules.observeForEvent(eventId: id) as Kotlinx_coroutines_coreFlow
        sub = subscribeFlow(flow) { [weak self] (list: NSArray) in
            Task { @MainActor [weak self] in
                self?.schedules = ((list as? [EventScheduleModel]) ?? [])
                    .sorted { $0.whenStart.epochSeconds < $1.whenStart.epochSeconds }
            }
        }
    }

    private func refreshRemote(id: String) async {
        loading = true
        defer { loading = false }
        do {
            try await koin.events.refresh(filter: nil, sort: "when_end")
            try await koin.eventSchedules.refresh(eventId: id)
            if let e = try? await koin.events.getById(id: id) {
                self.event = e
            }
        } catch {
            self.error = (error as NSError).localizedDescription
        }
    }
}

struct EventDetailView: View {
    let eventId: String
    @State private var vm = EventDetailViewModel()
    @State private var calendarError: String? = nil
    @State private var calendarSuccess = false
    @State private var showBooking = false
    @State private var scrollOffset: CGFloat = 0
    @State private var showEdit = false

    /// Edit ha due path:
    ///   1. Utente "super" / con power "events" / "events_helper" → autorizzato
    ///      globalmente, deciso SINCRONO al primo frame, niente attesa di flow.
    ///   2. Utente owner di QUESTO evento → richiede l'EventModel caricato.
    private var canEditEvent: Bool {
        let user = koin.auth.currentUser.value as? UserModel
        if hasPower("events", user: user) { return true }
        if let u = user, let e = vm.event, !u.id.isEmpty, u.id == e.ownerId {
            return true
        }
        return false
    }

    var body: some View {
        Group {
            if let event = vm.event {
                eventDetail(event)
            } else if vm.loading {
                LoadingDots()
            } else if let err = vm.error {
                ContentUnavailableView(err, systemImage: "exclamationmark.triangle")
            } else {
                LoadingDots()
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            // Share è SEMPRE in toolbar — al primo frame condivide un
            // testo segnaposto (solo l'URL dell'evento se ce l'abbiamo),
            // appena il vm.event arriva mostra il testo completo. Niente
            // snap del pulsante.
            ToolbarItem(placement: .topBarTrailing) {
                ShareLink(item: shareText(vm.event)) {
                    Image(systemName: "square.and.arrow.up")
                }
            }
            // Edit visibile dal primo frame se l'utente ha power events/super.
            // Per gli owner di questo specifico evento il bottone compare
            // appena `vm.event` arriva (necessario per matchare l'ownerId).
            ToolbarItem(placement: .topBarTrailing) {
                if canEditEvent {
                    Button { showEdit = true } label: { Image(systemName: "pencil") }
                        .disabled(vm.event == nil)
                }
            }
        }
        .task { vm.start(id: eventId) }
        .onDisappear { vm.stop() }
        .sheet(isPresented: $showEdit) {
            if let e = vm.event {
                NavigationStack { AddEventView(event: e) }
            }
        }
        .sheet(isPresented: $showBooking) {
            if let link = vm.event?.bookingLink, let url = URL(string: link) {
                SafariView(url: url).ignoresSafeArea()
            }
        }
        .alert(tr("events.detail.calendar_added", fallback: "Evento aggiunto al calendario"),
               isPresented: $calendarSuccess) { Button("OK") {} }
        .alert(tr("app.error.title", fallback: "Errore"),
               isPresented: Binding(get: { calendarError != nil },
                                    set: { _ in calendarError = nil })) {
            Button("OK") { calendarError = nil }
        } message: { Text(calendarError ?? "") }
    }

    // MARK: - Body

    @ViewBuilder
    private func eventDetail(_ event: EventModel) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                heroHeader(event)
                    .padding(.bottom, 4)

                VStack(alignment: .leading, spacing: 16) {
                    // Title
                    Text(event.name)
                        .font(.largeTitle.bold())

                    // Dates / location
                    infoStack(event)

                    // CTAs
                    actionStack(event)

                    if !event.description_.isEmpty {
                        Divider()
                        Text(tr("events.detail.description", fallback: "Descrizione"))
                            .font(.headline)
                        Text(autolinkedDescription(event.description_))
                            .font(.body)
                            .foregroundStyle(.primary)
                            .tint(AppTheme.Colors.brandTintAdaptive)
                    }

                    if hasContactInfo(event) {
                        Divider()
                        contactsSection(event)
                    }

                    if !vm.schedules.isEmpty {
                        Divider()
                        Text(tr("events.detail.schedule", fallback: "Programma"))
                            .font(.headline)
                        scheduleStack
                    }

                    if let pos = event.position {
                        Divider()
                        Text(tr("events.detail.location", fallback: "Dove"))
                            .font(.headline)
                        embeddedMap(pos: pos, name: event.name)
                    }
                }
                .padding(.horizontal)

                Color.clear.frame(height: 32)
            }
        }
        .ignoresSafeArea(edges: .top)
    }

    // MARK: - Hero w/ parallax

    @ViewBuilder
    private func heroHeader(_ event: EventModel) -> some View {
        let url = imageURL(for: event)
        GeometryReader { geo in
            let minY = geo.frame(in: .global).minY
            let stretch = max(0, minY)
            ZStack(alignment: .bottom) {
                Group {
                    if let url {
                        CachedAsyncImage(url: url) { img in
                            img.resizable().aspectRatio(contentMode: .fill)
                        } placeholder: {
                            gradientPlaceholder
                        }
                    } else {
                        gradientPlaceholder
                    }
                }
                .frame(width: geo.size.width, height: geo.size.height + stretch)
                .clipped()

                LinearGradient(
                    colors: [Color.black.opacity(0.0), Color.black.opacity(0.45)],
                    startPoint: .top, endPoint: .bottom
                )
                .frame(width: geo.size.width, height: geo.size.height + stretch)
            }
            .frame(width: geo.size.width, height: geo.size.height + stretch)
            .clipped()
            .offset(y: -stretch)
        }
        .frame(height: 320)
    }

    private var gradientPlaceholder: some View {
        LinearGradient(
            colors: [AppTheme.Colors.brandPrimary, AppTheme.Colors.brandSecondary],
            startPoint: .topLeading, endPoint: .bottomTrailing
        )
    }

    private func imageURL(for event: EventModel) -> URL? {
        guard !event.image.isEmpty else { return nil }
        if event.image.hasPrefix("http") { return URL(string: event.image) }
        return Files.url(
            collection: "events",
            recordId: event.id,
            filename: event.image,
            thumb: "1200x0"
        )
    }

    // MARK: - Info

    @ViewBuilder
    private func infoStack(_ event: EventModel) -> some View {
        let start = EventDateUtil.date(event.whenStart)
        let end = EventDateUtil.date(event.whenEnd)
        VStack(alignment: .leading, spacing: 8) {
            Label {
                Text("\(tr("events.detail.from", fallback: "Inizio")): \(EventDateUtil.fullFormatter.string(from: start))")
            } icon: { Image(systemName: "calendar") }

            Label {
                Text("\(tr("events.detail.to", fallback: "Fine")): \(EventDateUtil.fullFormatter.string(from: end))")
            } icon: { Image(systemName: "calendar.badge.clock") }
                .foregroundStyle(.secondary)

            if let pos = event.position {
                let label = pos.address.isEmpty ? pos.name : "\(pos.name) – \(pos.address)"
                Label(label, systemImage: "mappin.and.ellipse")
                    .foregroundStyle(.secondary)
            }

            HStack(spacing: 6) {
                if event.isNational {
                    Label(tr("events.tag.national", fallback: "Nazionale"), systemImage: "globe")
                        .labelStyle(.titleAndIcon)
                        .font(.caption.weight(.semibold))
                        .padding(.horizontal, 10).padding(.vertical, 4)
                        .background(AppTheme.Colors.brandPrimary.opacity(0.15), in: Capsule())
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                }
                if event.isSpot {
                    Label(tr("events.tag.spot", fallback: "Spot"), systemImage: "sparkles")
                        .font(.caption.weight(.semibold))
                        .padding(.horizontal, 10).padding(.vertical, 4)
                        .background(Color.orange.opacity(0.15), in: Capsule())
                        .foregroundStyle(.orange)
                }
            }
        }
        .font(.subheadline)
    }

    // MARK: - Actions

    @ViewBuilder
    private func actionStack(_ event: EventModel) -> some View {
        VStack(spacing: 12) {
            PrimaryButton(
                tr("events.detail.add_to_calendar", fallback: "Aggiungi al calendario"),
                icon: "calendar.badge.plus"
            ) {
                await addToCalendar(event)
            }

            if !event.bookingLink.isEmpty {
                Button {
                    showBooking = true
                } label: {
                    HStack {
                        Image(systemName: "ticket")
                        Text(tr("events.detail.booking", fallback: "Prenota"))
                    }
                    .frame(maxWidth: .infinity).frame(height: 48)
                }
                .buttonStyle(.glassProminent)
                .tint(AppTheme.Colors.brandSecondary)
            }

            if let pos = event.position {
                Button {
                    openInMaps(pos: pos, name: event.name)
                } label: {
                    HStack {
                        Image(systemName: "map")
                        Text(tr("events.detail.open_in_maps", fallback: "Apri in Mappe"))
                    }
                    .frame(maxWidth: .infinity).frame(height: 48)
                }
                .buttonStyle(.glass)
            }
        }
    }

    // MARK: - Schedule

    @ViewBuilder
    private var scheduleStack: some View {
        VStack(alignment: .leading, spacing: 10) {
            ForEach(Array(vm.schedules.enumerated()), id: \.element.id) { idx, s in
                let start = EventDateUtil.date(s.whenStart)
                let end = EventDateUtil.date(s.whenEnd)
                HStack(alignment: .top, spacing: 12) {
                    VStack {
                        Text(EventDateUtil.dayMonthFormatter.string(from: start))
                            .font(.caption.weight(.bold))
                            .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                        Text(EventDateUtil.timeFormatter.string(from: start))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    .frame(width: 56)
                    .padding(.vertical, 6)
                    .background(AppTheme.Colors.brandPrimary.opacity(0.1),
                                in: RoundedRectangle(cornerRadius: 10, style: .continuous))

                    VStack(alignment: .leading, spacing: 4) {
                        Text(s.title.isEmpty ? tr("events.schedule.session", fallback: "Sessione") : s.title)
                            .font(.subheadline.weight(.semibold))
                        if !s.description_.isEmpty {
                            Text(s.description_)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .lineLimit(3)
                        }
                        Text("\(EventDateUtil.timeFormatter.string(from: start)) – \(EventDateUtil.timeFormatter.string(from: end))")
                            .font(.caption2)
                            .foregroundStyle(.tertiary)
                    }
                    Spacer(minLength: 0)
                }
                .padding(10)
                .glassEffect(.regular, in: .rect(cornerRadius: 14))
                .modifier(StaggerAppear(index: idx))
            }
        }
    }

    // MARK: - Map

    @ViewBuilder
    private func embeddedMap(pos: LocationModel, name: String) -> some View {
        let coord = CLLocationCoordinate2D(latitude: pos.lat, longitude: pos.lon)
        Map(initialPosition: .region(MKCoordinateRegion(
            center: coord,
            span: MKCoordinateSpan(latitudeDelta: 0.02, longitudeDelta: 0.02)
        ))) {
            Marker(name, coordinate: coord)
                .tint(AppTheme.Colors.brandTintAdaptive)
        }
        .frame(height: 200)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .allowsHitTesting(false)
        .overlay(alignment: .bottomTrailing) {
            Button {
                openInMaps(pos: pos, name: name)
            } label: {
                Image(systemName: "arrow.up.right.square")
                    .padding(8)
            }
            .buttonStyle(.glass)
            .padding(10)
        }
    }

    // MARK: - Helpers

    private func addToCalendar(_ event: EventModel) async {
        do {
            try await EventKitHelper.addEvent(
                title: event.name,
                notes: event.description_.isEmpty ? nil : event.description_,
                location: event.position.map { p in p.address.isEmpty ? p.name : "\(p.name), \(p.address)" },
                start: EventDateUtil.date(event.whenStart),
                end: EventDateUtil.date(event.whenEnd)
            )
            calendarSuccess = true
        } catch {
            calendarError = error.localizedDescription
        }
    }

    private func openInMaps(pos: LocationModel, name: String) {
        let encoded = name.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        if let url = URL(string: "maps://?ll=\(pos.lat),\(pos.lon)&q=\(encoded)"),
           UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
        }
    }

    /// Overload Optional: il bottone ShareLink in toolbar viene costruito al
    /// primo frame, prima che `vm.event` sia disponibile. Restituiamo un
    /// segnaposto generico finché i dati non arrivano — al tap arriva sempre
    /// il testo completo (SwiftUI ri-valuta il content al momento dello share).
    private func shareText(_ event: EventModel?) -> String {
        guard let event else { return tr("events.share.placeholder", fallback: "Evento Mensa Italia") }
        var parts = [event.name]
        if !event.infoLink.isEmpty { parts.append(event.infoLink) }
        return parts.joined(separator: "\n")
    }

    // MARK: - Auto-linking

    /// Turns bare URLs and emails inside free-form text into a Markdown-linked
    /// `AttributedString` so SwiftUI's `Text` renders them as tappable links.
    private func autolinkedDescription(_ raw: String) -> AttributedString {
        // First, escape characters that markdown would otherwise consume.
        var escaped = raw
        for ch in ["\\", "[", "]", "*", "_", "`"] {
            escaped = escaped.replacingOccurrences(of: ch, with: "\\\(ch)")
        }

        // URLs.
        if let urlRx = try? NSRegularExpression(
            pattern: #"https?://[^\s<>")]+"#,
            options: [.caseInsensitive]
        ) {
            escaped = applyLinkRegex(urlRx, on: escaped) { $0 }
        }
        // Emails.
        if let mailRx = try? NSRegularExpression(
            pattern: #"[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}"#,
            options: []
        ) {
            escaped = applyLinkRegex(mailRx, on: escaped) { "mailto:\($0)" }
        }

        if let attr = try? AttributedString(
            markdown: escaped,
            options: AttributedString.MarkdownParsingOptions(
                interpretedSyntax: .inlineOnlyPreservingWhitespace
            )
        ) {
            return attr
        }
        return AttributedString(raw)
    }

    private func applyLinkRegex(
        _ regex: NSRegularExpression,
        on text: String,
        href: (String) -> String
    ) -> String {
        let ns = text as NSString
        let matches = regex.matches(in: text, range: NSRange(location: 0, length: ns.length))
        guard !matches.isEmpty else { return text }
        var out = ""
        var cursor = 0
        for m in matches {
            let r = m.range
            if r.location > cursor {
                out += ns.substring(with: NSRange(location: cursor, length: r.location - cursor))
            }
            let match = ns.substring(with: r)
            // Trim trailing punctuation that's usually not part of the link.
            var trimmed = match
            var trailing = ""
            while let last = trimmed.last, ".,;:!?)".contains(last) {
                trailing = String(last) + trailing
                trimmed.removeLast()
            }
            out += "[\(trimmed)](\(href(trimmed)))\(trailing)"
            cursor = r.location + r.length
        }
        if cursor < ns.length {
            out += ns.substring(with: NSRange(location: cursor, length: ns.length - cursor))
        }
        return out
    }

    // MARK: - Contacts

    private func hasContactInfo(_ event: EventModel) -> Bool {
        let mail = event.contact.trimmingCharacters(in: .whitespacesAndNewlines)
        let link = event.infoLink.trimmingCharacters(in: .whitespacesAndNewlines)
        return contactMailURL(mail) != nil || infoSiteURL(link) != nil
    }

    @ViewBuilder
    private func contactsSection(_ event: EventModel) -> some View {
        let mail = event.contact.trimmingCharacters(in: .whitespacesAndNewlines)
        let link = event.infoLink.trimmingCharacters(in: .whitespacesAndNewlines)

        VStack(alignment: .leading, spacing: 10) {
            Text(tr("events.detail.contacts", fallback: "Contatti"))
                .font(.headline)

            HStack(spacing: 10) {
                if let mailURL = contactMailURL(mail) {
                    Link(destination: mailURL) {
                        HStack {
                            Image(systemName: "envelope")
                            Text(tr("events.detail.email", fallback: "Email"))
                        }
                        .frame(maxWidth: .infinity).frame(height: 44)
                    }
                    .buttonStyle(.glass)
                }
                if let siteURL = infoSiteURL(link) {
                    Link(destination: siteURL) {
                        HStack {
                            Image(systemName: "safari")
                            Text(tr("events.detail.site", fallback: "Sito"))
                        }
                        .frame(maxWidth: .infinity).frame(height: 44)
                    }
                    .buttonStyle(.glass)
                }
            }
        }
    }

    private func contactMailURL(_ raw: String) -> URL? {
        guard !raw.isEmpty else { return nil }
        // Accept either a plain email or an explicit mailto:.
        let mail = raw.hasPrefix("mailto:") ? String(raw.dropFirst("mailto:".count)) : raw
        guard mail.contains("@"), mail.contains(".") else { return nil }
        return URL(string: "mailto:\(mail)")
    }

    private func infoSiteURL(_ raw: String) -> URL? {
        guard !raw.isEmpty else { return nil }
        let normalized = raw.hasPrefix("http") ? raw : "https://\(raw)"
        guard let url = URL(string: normalized), url.host != nil else { return nil }
        return url
    }
}

#Preview {
    NavigationStack { EventDetailView(eventId: "preview") }
}
