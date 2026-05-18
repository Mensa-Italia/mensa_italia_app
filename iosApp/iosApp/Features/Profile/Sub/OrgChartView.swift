import SwiftUI
import Shared

/// Organigramma — versione editoriale. Niente `List`: ScrollView custom per
/// controllo totale su tipografia, spaziature e motion. Sezioni con header
/// uppercase + filetto accento brand, card socio con avatar gradient e
/// ingressi sfalsati. Tap → `MemberDetailView`.
struct OrgChartView: View {
    @State private var vm = OrgChartViewModel()
    @State private var appeared = false
    @State private var searchText: String = ""

    /// Quando la view viene aperta da un risultato di ricerca org_group,
    /// pre-popoliamo la barra di filtro interna così l'utente atterra
    /// direttamente sul gruppo cercato. `nil` = nessun pre-fill.
    private let initialSearchText: String?

    init(initialSearchText: String? = nil) {
        self.initialSearchText = initialSearchText
    }

    /// Diacritic + case insensitive substring match on the group's localized
    /// title (the same string the user actually sees as section header).
    /// Members of matching groups are shown in full — we're filtering at the
    /// GROUP level, which is what users actually scan for ("Mensa Ludo",
    /// "Direttivo", "Comunicazione"…). Empty query → all groups.
    private var filteredGroups: [OrgChartGroup] {
        let raw = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        let nonEmpty = vm.groups.filter { !$0.members.isEmpty }
        guard !raw.isEmpty else { return nonEmpty }
        let needle = raw.folding(options: .diacriticInsensitive, locale: .current).lowercased()
        return nonEmpty.filter { group in
            let title = localizedGroupTitle(group.title)
                .folding(options: .diacriticInsensitive, locale: .current)
                .lowercased()
            return title.contains(needle)
        }
    }

    var body: some View {
        content
            .navigationTitle(tr("app.org_chart.title", fallback: "Organigramma"))
            .navigationBarTitleDisplayMode(.large)
            .cleanNavBar()
            .searchable(
                text: $searchText,
                placement: .navigationBarDrawer(displayMode: .automatic),
                prompt: Text(tr("app.org_chart.search_placeholder",
                                fallback: "Cerca un gruppo"))
            )
            .task {
                // Pre-popola il filtro se atterriamo qui da un risultato di
                // ricerca org_group. La match logica usa `localizedGroupTitle`,
                // quindi è sicuro passare la key grezza (es. "consiglio") o il
                // titolo localizzato — entrambi matchano.
                if let initial = initialSearchText, searchText.isEmpty {
                    searchText = initial
                }
                await vm.load()
                withAnimation(.spring(response: 0.65, dampingFraction: 0.82)) {
                    appeared = true
                }
            }
            .refreshable { await vm.load() }
    }

    // MARK: - Content

    @ViewBuilder
    private var content: some View {
        if vm.loading && vm.groups.isEmpty {
            ProgressView().controlSize(.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let err = vm.errorMessage, vm.groups.isEmpty {
            ContentUnavailableView(err, systemImage: "exclamationmark.triangle")
        } else {
            let groups = filteredGroups
            ScrollView {
                VStack(alignment: .leading, spacing: 36) {
                    if groups.isEmpty && !searchText.isEmpty {
                        // Search returned nothing — system-styled empty state.
                        ContentUnavailableView.search(text: searchText)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 40)
                    } else {
                        ForEach(Array(groups.enumerated()),
                                id: \.element.id) { gIdx, group in
                            groupSection(group, index: gIdx)
                        }
                    }
                    footer
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 48)
            }
            .scrollIndicators(.hidden)
            // Disable the horizontal rubber-band: this is a vertical-only
            // scroll surface, but SwiftUI bounces both axes by default.
            // `.basedOnSize` allows bouncing only on axes whose content
            // genuinely overflows.
            .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
            .scrollDismissesKeyboard(.interactively)
        }
    }

    private var footer: some View {
        HStack(spacing: 6) {
            Image(systemName: "sparkles")
                .font(.caption2)
            Text(tr("app.org_chart.footer",
                    fallback: "Aggiornato dal Consiglio Direttivo"))
                .font(.caption)
        }
        .foregroundStyle(.tertiary)
        .frame(maxWidth: .infinity)
        .padding(.top, 12)
    }

    // MARK: - Group section

    @ViewBuilder
    private func groupSection(_ group: OrgChartGroup, index gIdx: Int) -> some View {
        let isInactive = group.members.allSatisfy { $0.inactive }
        let masters = group.members.filter { $0.isMaster }
        let rest    = group.members.filter { !$0.isMaster }

        VStack(alignment: .leading, spacing: 18) {
            sectionHeader(title: localizedGroupTitle(group.title),
                          count: group.members.count,
                          inactive: isInactive)

            // Hero card per ogni membro con is_master=true. Più di uno → stack.
            ForEach(Array(masters.enumerated()), id: \.offset) { _, m in
                heroCard(m)
            }

            if !rest.isEmpty {
                memberGrid(rest, startIndex: masters.count)
            }
        }
        .opacity(appeared ? 1 : 0)
        .offset(y: appeared ? 0 : 24)
        .animation(
            .spring(response: 0.65, dampingFraction: 0.85)
                .delay(0.06 + 0.07 * Double(gIdx)),
            value: appeared
        )
    }

    /// Risolve un titolo gruppo via Tolgee. Il campo `title` su PocketBase è
     /// pensato come chiave di traduzione (eg. "consiglio"); se Tolgee non
     /// l'ha ancora, mostriamo la chiave grezza — meglio del placeholder.
    private func localizedGroupTitle(_ raw: String) -> String {
        let pretty = raw
            .replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: "-", with: " ")
            .capitalized
        return tr(raw, fallback: pretty)
    }

    private func sectionHeader(title: String, count: Int, inactive: Bool) -> some View {
        HStack(alignment: .center, spacing: 12) {
            Rectangle()
                .fill(
                    LinearGradient(
                        colors: inactive
                            ? [.secondary.opacity(0.6), .secondary.opacity(0.2)]
                            : [AppTheme.Colors.brandPrimary,
                               AppTheme.Colors.brandSecondary],
                        startPoint: .top, endPoint: .bottom
                    )
                )
                .frame(width: 3, height: 22)
                .clipShape(Capsule())

            Text(title)
                .font(.system(.title3, design: .serif).weight(.bold))
                .foregroundStyle(inactive ? .secondary : .primary)

            Spacer(minLength: 8)

            Text("\(count)")
                .font(.caption.weight(.semibold))
                .monospacedDigit()
                .foregroundStyle(.secondary)
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(
                    Capsule().fill(Color(.tertiarySystemFill))
                )
        }
    }

    // MARK: - Hero card (is_master)

    /// Tile espansa per i master. Se il socio ha una foto, la usa come
    /// background con scrim graduale per garantire contrasto del testo;
    /// altrimenti torna al gradient brand. Stile HIG: angoli continui,
    /// scrim sotto al testo, no testo bianco su sfondo chiaro senza overlay.
    private func heroCard(_ member: OrgChartMember) -> some View {
        NavigationLink {
            MemberDetailView(memberId: member.userId)
        } label: {
            HeroCardContent(member: member)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(member.name), \(member.role)")
        .accessibilityHint(tr("app.org_chart.view_profile",
                              fallback: "Apri profilo"))
        .accessibilityAddTraits(.isButton)
    }

    // MARK: - Member grid

    private func memberGrid(_ members: [OrgChartMember], startIndex: Int) -> some View {
        let columns = [GridItem(.flexible(), spacing: 12),
                       GridItem(.flexible(), spacing: 12)]
        return LazyVGrid(columns: columns, spacing: 12) {
            ForEach(Array(members.enumerated()), id: \.offset) { idx, m in
                NavigationLink {
                    MemberDetailView(memberId: m.userId)
                } label: {
                    OrgMemberCard(member: m)
                }
                .buttonStyle(.plain)
                .opacity(appeared ? 1 : 0)
                .offset(y: appeared ? 0 : 12)
                .animation(
                    .spring(response: 0.6, dampingFraction: 0.88)
                        .delay(0.04 * Double(startIndex + idx)),
                    value: appeared
                )
            }
        }
    }
}

// MARK: - Member card

private struct OrgMemberCard: View {
    let member: OrgChartMember

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            avatar
            VStack(alignment: .leading, spacing: 4) {
                Text(member.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(member.inactive ? .secondary : .primary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                Text(member.role)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .frame(minHeight: 150)
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(Color(.secondarySystemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(.primary.opacity(0.06), lineWidth: 1)
        )
        // Badge ancorato al box reale della card (dopo background/frame).
        .overlay(alignment: .bottomTrailing) {
            if member.inactive {
                inactiveBadge
                    .padding(10)
            }
        }
        .opacity(member.inactive ? 0.55 : 1)
    }

    /// Pillola "Dimissionario" — chiave Tolgee `app.org_chart.inactive_badge`.
    private var inactiveBadge: some View {
        Text(tr("app.org_chart.inactive_badge", fallback: "Dimissionario"))
            .font(.system(size: 9, weight: .bold))
            .tracking(0.5)
            .textCase(.uppercase)
            .foregroundStyle(.secondary)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(
                Capsule().fill(Color(.tertiarySystemFill))
            )
            .overlay(
                Capsule().stroke(.secondary.opacity(0.25), lineWidth: 0.5)
            )
    }

    /// Foto reale del socio, con iniziali in fallback. Logica gemella di
    /// `MemberAvatar`, ma evitiamo di costruire un `RegSociModel` da Swift
    /// (data class Kotlin → init bridge richiede tutti i campi).
    private var avatar: some View {
        OrgAvatar(
            userId: member.userId,
            image: member.image,
            name: member.name,
            size: 44
        )
        .opacity(member.inactive ? 0.55 : 1)
    }
}

// MARK: - Hero content

private struct HeroCardContent: View {
    let member: OrgChartMember

    private var photoURL: URL? {
        let raw = member.image
        guard !raw.isEmpty,
              !raw.contains("cloud32.it/Associazioni/img/Uomo-1.png")
        else { return nil }
        if raw.hasPrefix("http") { return URL(string: raw) }
        // PocketBase serve solo i thumb dichiarati nella field config; `800x400`
        // non è tra questi e il server o rigenera al volo o spara l'originale —
        // entrambi lenti. `0x500` è il thumb "retina hero" condiviso col
        // dettaglio socio, decisamente più rapido. `.scaledToFill()` croppa al
        // riquadro del card senza problemi.
        return Files.url(
            collection: "members_registry",
            recordId: member.userId,
            filename: raw,
            thumb: "0x500"
        )
    }

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            background
            scrim
            label
        }
        // Was a hard `frame(height: 220)` — long names ("Mela Amedeo Anna",
        // "Bartolomei Maria Cristina", …) were getting clipped at the bottom
        // because the label couldn't grow. `minHeight` keeps the visual rhythm
        // (most cards stay 220pt) while letting tall labels push the card
        // a few points taller when needed.
        .frame(minHeight: 220)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            // Bordo sottile per separare la card dal background in light mode.
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(.white.opacity(0.08), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.18), radius: 14, y: 8)
    }

    // MARK: Layers

    @ViewBuilder
    private var background: some View {
        if let url = photoURL {
            // CachedAsyncImage with `.scaledToFill()` adopts the image's
            // intrinsic pixel size by default — for an 800×400 thumb that's
            // 800pt of layout width, which propagates up the VStack →
            // ScrollView and lets the page scroll horizontally.
            //
            // Fix: render the image as the BACKGROUND of a clear-color shim.
            // The `Color.clear` claims minimal layout footprint (it adopts
            // whatever the parent proposes), and `.background` overlays the
            // image without driving the parent size. `.clipped()` ensures
            // the overflow doesn't bleed past the card bounds.
            Color.clear
                .background(
                    CachedAsyncImage(url: url) { img in
                        img.resizable().scaledToFill()
                    } placeholder: {
                        brandGradient
                    }
                )
                .clipped()
        } else {
            brandGradient
                .overlay(alignment: .topTrailing) {
                    Circle()
                        .strokeBorder(.white.opacity(0.18), lineWidth: 1)
                        .frame(width: 220, height: 220)
                        .offset(x: 70, y: -70)
                }
                .overlay(alignment: .bottomTrailing) {
                    Circle()
                        .strokeBorder(.white.opacity(0.12), lineWidth: 1)
                        .frame(width: 160, height: 160)
                        .offset(x: 50, y: 60)
                }
        }
    }

    private var brandGradient: some View {
        LinearGradient(
            colors: [
                AppTheme.Colors.brandPrimary,
                AppTheme.Colors.brandSecondary,
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    /// Scrim graduale: trasparente in alto, ~70% opacità nera in basso.
    /// Garantisce contrasto WCAG AA per il testo bianco sopra qualsiasi foto.
    /// Solo se c'è una foto — sul gradient brand non serve (già abbastanza scuro).
    @ViewBuilder
    private var scrim: some View {
        if photoURL != nil {
            LinearGradient(
                stops: [
                    .init(color: .black.opacity(0.05), location: 0.0),
                    .init(color: .black.opacity(0.35), location: 0.45),
                    .init(color: .black.opacity(0.75), location: 1.0),
                ],
                startPoint: .top,
                endPoint: .bottom
            )
        }
    }

    private var label: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 6) {
                Image(systemName: "star.fill")
                    .font(.caption2)
                Text(member.role.uppercased())
                    .font(.caption.weight(.bold))
                    .tracking(1.5)
                    .lineLimit(1)
            }
            .foregroundStyle(.white.opacity(0.92))
            .shadow(color: .black.opacity(0.4), radius: 4, y: 1)

            Text(member.name)
                // Smarter sizing: name shrinks aggressively (down to 50%) and
                // tightens letter spacing before wrapping. With `lineLimit(2)`
                // and `fixedSize(vertical:)` the label requests its true
                // height so the parent ZStack/frame can accommodate it.
                .font(.system(size: 28, weight: .bold, design: .serif))
                .foregroundStyle(.white)
                .lineLimit(2)
                .minimumScaleFactor(0.5)
                .allowsTightening(true)
                .fixedSize(horizontal: false, vertical: true)
                .shadow(color: .black.opacity(0.45), radius: 6, y: 2)

            HStack(spacing: 8) {
                Text(tr("app.org_chart.view_profile",
                        fallback: "Apri profilo"))
                    .font(.footnote.weight(.semibold))
                Image(systemName: "arrow.right")
                    .font(.footnote.weight(.semibold))
            }
            .foregroundStyle(.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 7)
            .background(
                // Material `.thin` su foto dà glass legibility HIG-style.
                Capsule().fill(.ultraThinMaterial)
            )
            .overlay(Capsule().stroke(.white.opacity(0.25), lineWidth: 1))
        }
        .padding(22)
    }
}

// MARK: - Avatar

private struct OrgAvatar: View {
    let userId: String
    let image: String
    let name: String
    let size: CGFloat

    private var resolvedURL: URL? {
        let raw = image
        guard !raw.isEmpty, !isPlaceholderURL(raw) else { return nil }
        if raw.hasPrefix("http://") || raw.hasPrefix("https://") {
            return URL(string: raw)
        }
        // Standard `0x100` come MemberAvatar / Spotlight engine — l'unica size
        // sicuramente configurata lato PocketBase per le foto soci.
        return Files.url(
            collection: "members_registry",
            recordId: userId,
            filename: raw,
            thumb: "0x100"
        )
    }

    var body: some View {
        Group {
            if let url = resolvedURL {
                CachedAsyncImage(url: url) { img in
                    img.resizable().scaledToFill()
                } placeholder: { bubble }
            } else {
                bubble
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(Circle().stroke(.white.opacity(0.6), lineWidth: 1))
    }

    private var bubble: some View {
        ZStack {
            AppTheme.brandGradient
            Text(initials)
                .font(.system(size: size * 0.4, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
        }
    }

    private var initials: String {
        let parts = name.split(separator: " ").prefix(2)
        let chars = parts.compactMap { $0.first }
        let s = String(chars).uppercased()
        return s.isEmpty ? "?" : s
    }

    private func isPlaceholderURL(_ url: String) -> Bool {
        url.contains("cloud32.it/Associazioni/img/Uomo-1.png")
    }
}

@MainActor
@Observable
final class OrgChartViewModel {
    var groups: [OrgChartGroup] = []
    var loading = true
    var errorMessage: String? = nil

    func load() async {
        if groups.isEmpty { loading = true }
        defer { loading = false }
        do {
            let model = try await koin.orgChart.fetch()
            self.groups = model.groups
            self.errorMessage = nil
        } catch {
            self.errorMessage = error.localizedDescription
        }
    }
}

#Preview {
    NavigationStack { OrgChartView() }
}
