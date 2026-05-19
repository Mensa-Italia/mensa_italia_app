import SwiftUI
import Shared

@MainActor @Observable
final class MemberDetailViewModel {
    var member: RegSociModel?
    var loading = false
    var error: String?
    private var sub: Closeable?

    func start(id: String) {
        sub = FlowBridgeKt.subscribeNullable(
            flow: koin.regSoci.observeOne(id: id),
            onEach: { [weak self] value in
                Task { @MainActor in
                    self?.member = value as? RegSociModel
                }
            },
            onError: { _ in }
        )
        Task { await fetch(id: id) }
    }

    func stop() { sub?.close() }

    func fetch(id: String) async {
        if member == nil { loading = true }
        defer { loading = false }
        do {
            let fetched = try await koin.regSoci.getById(id: id)
            if let fetched { member = fetched }
        } catch {
            self.error = (error as NSError).localizedDescription
        }
    }
}

struct MemberDetailView: View {
    let memberId: String
    @State private var vm = MemberDetailViewModel()
    @State private var heroScale: CGFloat = 0.85
    @State private var heroOpacity: Double = 0
    @State private var sectionsAppeared = false

    var body: some View {
        Group {
            if vm.loading && vm.member == nil {
                LoadingDots()
            } else if let m = vm.member {
                content(m)
            } else if let err = vm.error {
                ContentUnavailableView(err, systemImage: "exclamationmark.triangle")
            } else {
                LoadingDots()
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task {
            vm.start(id: memberId)
            withAnimation(.spring(response: 0.55, dampingFraction: 0.78)) {
                heroScale = 1.0
                heroOpacity = 1.0
            }
            withAnimation(.spring(response: 0.5, dampingFraction: 0.85).delay(0.1)) {
                sectionsAppeared = true
            }
        }
        .onDisappear { vm.stop() }
    }

    @ViewBuilder
    private func content(_ m: RegSociModel) -> some View {
        ScrollView {
            VStack(spacing: 18) {
                hero(m)

                section(
                    title: tr("members.section.profile", fallback: "Anagrafica"),
                    icon: "person.text.rectangle",
                    rows: profileRows(m),
                    delay: 0
                )

                section(
                    title: tr("members.section.mensa", fallback: "Mensa"),
                    icon: "star.circle.fill",
                    rows: mensaRows(m),
                    delay: 0.06
                )

                let contacts = contactRows(m)
                if !contacts.isEmpty {
                    section(
                        title: tr("members.section.contacts", fallback: "Contatti"),
                        icon: "envelope.fill",
                        rows: contacts,
                        delay: 0.12
                    )
                }

                let sigs = sigRows(m)
                if !sigs.isEmpty {
                    section(
                        title: tr("members.section.sig", fallback: "Community"),
                        icon: "person.3.fill",
                        rows: sigs,
                        delay: 0.18
                    )
                }

            }
            .padding(16)
        }
        .navigationTitle(m.name.capitalized)
    }

    // MARK: - Hero

    @ViewBuilder
    private func hero(_ m: RegSociModel) -> some View {
        VStack(spacing: 12) {
            MemberHeroAvatar(member: m, size: 120)
                .shadow(color: .black.opacity(0.18), radius: 18, x: 0, y: 12)
                .scaleEffect(heroScale)
                .opacity(heroOpacity)

            VStack(spacing: 2) {
                Text(m.name.capitalized)
                    .font(.title.bold())
                    .multilineTextAlignment(.center)

                if !m.id.isEmpty {
                    Text(m.id)
                        .font(.caption.monospaced())
                        .foregroundStyle(.secondary)
                }
            }
            .opacity(heroOpacity)

            if let bd = m.birthdate {
                HStack(spacing: 6) {
                    Image(systemName: "gift.fill")
                    Text(birthdateText(bd))
                        .font(.caption.weight(.semibold))
                }
                .foregroundStyle(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(
                    Capsule().fill(LinearGradient(
                        colors: [.purple, .pink],
                        startPoint: .leading, endPoint: .trailing
                    ))
                )
                .opacity(heroOpacity)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
    }

    // MARK: - Sections

    @ViewBuilder
    private func section(title: String, icon: String, rows: [(String, String)], delay: Double) -> some View {
        GlassCard(padding: 16, cornerRadius: 20) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 8) {
                    Image(systemName: icon)
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                    Text(title)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(.secondary)
                        .textCase(.uppercase)
                }

                VStack(spacing: 10) {
                    ForEach(Array(rows.enumerated()), id: \.offset) { idx, row in
                        HStack(alignment: .top, spacing: 12) {
                            Text(row.0)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .frame(width: 110, alignment: .leading)
                            Text(row.1)
                                .font(.subheadline.weight(.medium))
                                .foregroundStyle(.primary)
                                .frame(maxWidth: .infinity, alignment: .trailing)
                                .multilineTextAlignment(.trailing)
                                .textSelection(.enabled)
                        }
                        if idx < rows.count - 1 {
                            Divider().opacity(0.4)
                        }
                    }
                }
            }
        }
        .opacity(sectionsAppeared ? 1 : 0)
        .offset(y: sectionsAppeared ? 0 : 12)
        .animation(.spring(response: 0.5, dampingFraction: 0.85).delay(delay), value: sectionsAppeared)
    }

    // MARK: - Row builders

    private func profileRows(_ m: RegSociModel) -> [(String, String)] {
        var rows: [(String, String)] = []
        if !m.name.isEmpty {
            rows.append((tr("members.field.name", fallback: "Nome"), m.name.capitalized))
        }
        if !m.city.isEmpty {
            rows.append((tr("members.field.city", fallback: "Città"), m.city.capitalized))
        }
        if !m.state.isEmpty {
            rows.append((tr("members.field.state", fallback: "Regione"), m.state.capitalized))
        }
        if let bd = m.birthdate {
            rows.append((tr("members.field.birthdate", fallback: "Data di nascita"), birthdateText(bd)))
        }
        // Walk fullData for any non-contact / non-sig info.
        for (k, v) in extractFullData(m) where isProfileKey(k) {
            rows.append((cleanLabel(k), v))
        }
        return rows
    }

    private func mensaRows(_ m: RegSociModel) -> [(String, String)] {
        var rows: [(String, String)] = []
        if !m.id.isEmpty {
            rows.append((tr("members.field.member_id", fallback: "ID Socio"), m.id))
        }
        for (k, v) in extractFullData(m) where isMensaKey(k) {
            rows.append((cleanLabel(k), v))
        }
        if rows.isEmpty {
            rows.append((tr("members.field.member_id", fallback: "ID Socio"), m.id))
        }
        return rows
    }

    private func contactRows(_ m: RegSociModel) -> [(String, String)] {
        var rows: [(String, String)] = []
        for (k, v) in extractFullData(m) where isContactKey(k) {
            rows.append((cleanLabel(k), prettifyContact(v)))
        }
        return rows
    }

    private func sigRows(_ m: RegSociModel) -> [(String, String)] {
        var rows: [(String, String)] = []
        for (k, v) in extractFullData(m) where isSigKey(k) {
            rows.append((cleanLabel(k), v))
        }
        return rows
    }

    // MARK: - fullData helpers

    /// JsonElement values are exposed to Swift as opaque
    /// `Kotlinx_serialization_jsonJsonElement` instances. Their `description`
    /// returns the canonical JSON serialization (with quotes around strings),
    /// which we trim here.
    private func extractFullData(_ m: RegSociModel) -> [(String, String)] {
        let dict = m.fullData as? [String: AnyObject] ?? [:]
        return dict
            .map { (key, value) -> (String, String) in
                var s = String(describing: value)
                if s.hasPrefix("\"") && s.hasSuffix("\"") && s.count >= 2 {
                    s = String(s.dropFirst().dropLast())
                }
                return (key, s)
            }
            .filter { !$0.1.isEmpty && $0.1 != "null" }
            .sorted { $0.0.localizedCaseInsensitiveCompare($1.0) == .orderedAscending }
    }

    private func isContactKey(_ k: String) -> Bool {
        let lk = k.lowercased()
        return lk.contains("email") || lk.contains("mail") || lk.contains("phone") ||
               lk.contains("tel") || lk.contains("cell") || lk.contains("facebook") ||
               lk.contains("instagram") || lk.contains("website") || lk.contains("sito")
    }

    private func isSigKey(_ k: String) -> Bool {
        let lk = k.lowercased()
        return lk.contains("sig") || lk.contains("gruppo")
    }

    private func isMensaKey(_ k: String) -> Bool {
        let lk = k.lowercased()
        return lk.contains("iscriz") || lk.contains("scaden") || lk.contains("tessera") ||
               lk.contains("membership") || lk.contains("expire") || lk.contains("local")
    }

    private func isProfileKey(_ k: String) -> Bool {
        !isContactKey(k) && !isSigKey(k) && !isMensaKey(k)
    }

    private func cleanLabel(_ k: String) -> String {
        var s = k
        if s.hasSuffix(":") { s.removeLast() }
        return s
    }

    private func prettifyContact(_ v: String) -> String {
        if v.hasPrefix("mailto:") { return String(v.dropFirst("mailto:".count)) }
        if v.hasPrefix("tel:") { return String(v.dropFirst("tel:".count)) }
        return v
    }

    private func birthdateText(_ instant: Kotlinx_datetimeInstant) -> String {
        let ms = instant.toEpochMilliseconds()
        let date = Date(timeIntervalSince1970: TimeInterval(ms) / 1000.0)
        let fmt = DateFormatter()
        fmt.dateFormat = "dd MMMM yyyy"
        return fmt.string(from: date)
    }
}
