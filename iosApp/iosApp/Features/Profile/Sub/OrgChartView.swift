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
        // "Tavola rotonda": nessuna hero card — tutti hanno la stessa tile,
        // i responsabili (is_master) vengono solo ordinati per primi e
        // distinti da bordo accent + badge dentro la card.
        let ordered = group.members.filter { $0.isMaster }
            + group.members.filter { !$0.isMaster }

        VStack(alignment: .leading, spacing: 18) {
            sectionHeader(title: localizedGroupTitle(group.title),
                          count: group.members.count,
                          inactive: isInactive)

            memberGrid(ordered, startIndex: 0)
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

/// Tile unica per TUTTI i membri ("tavola rotonda": nessuna gerarchia di
/// dimensioni). Foto del socio come background con scrim graduale per il
/// contrasto WCAG del testo; gradient brand + iniziali in fallback. I
/// responsabili (is_master) si distinguono solo per bordo accent + badge.
private struct OrgMemberCard: View {
    let member: OrgChartMember

    private var photoURL: URL? {
        let raw = member.image
        guard !raw.isEmpty,
              !raw.contains("cloud32.it/Associazioni/img/Uomo-1.png")
        else { return nil }
        if raw.hasPrefix("http") { return URL(string: raw) }
        // `0x500` è il thumb "retina hero" condiviso col dettaglio socio —
        // l'unico dichiarato lato PocketBase oltre a 0x100 (troppo piccolo
        // come background).
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
        .frame(maxWidth: .infinity)
        .frame(minHeight: 168)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(
                    member.isMaster
                        ? AppTheme.Colors.brandTintAdaptive
                        : Color.primary.opacity(0.06),
                    lineWidth: member.isMaster ? 2 : 1
                )
        )
        .overlay(alignment: .topTrailing) {
            if member.isMaster {
                masterBadge.padding(8)
            }
        }
        .overlay(alignment: .bottomTrailing) {
            if member.inactive {
                inactiveBadge.padding(8)
            }
        }
        .opacity(member.inactive ? 0.55 : 1)
    }

    // MARK: Layers

    @ViewBuilder
    private var background: some View {
        if let url = photoURL {
            // Il thumb adotterebbe la sua size intrinseca e gonfierebbe il
            // layout: `Color.clear` reclama solo lo spazio proposto e l'immagine
            // vive nel background, croppata dai bounds della card.
            Color.clear
                .background(
                    CachedAsyncImage(url: url) { img in
                        img.resizable().scaledToFill()
                    } placeholder: {
                        AppTheme.brandGradient
                    }
                )
                .clipped()
        } else {
            AppTheme.brandGradient
                .overlay(
                    Text(initials)
                        .font(.system(size: 44, weight: .bold, design: .rounded))
                        .foregroundStyle(.white.opacity(0.35))
                )
        }
    }

    /// Scrim graduale: trasparente in alto, scuro in basso. Sempre presente
    /// (anche sul gradient) così il testo bianco resta leggibile ovunque.
    private var scrim: some View {
        LinearGradient(
            stops: [
                .init(color: .black.opacity(0.02), location: 0.0),
                .init(color: .black.opacity(0.30), location: 0.5),
                .init(color: .black.opacity(0.72), location: 1.0)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }

    private var label: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(member.name)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.white)
                .lineLimit(2)
                .minimumScaleFactor(0.85)
                .multilineTextAlignment(.leading)
                .shadow(color: .black.opacity(0.4), radius: 3, y: 1)
            if !member.role.isEmpty {
                Text(member.role)
                    .font(.caption2)
                    .foregroundStyle(.white.opacity(0.88))
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .shadow(color: .black.opacity(0.4), radius: 3, y: 1)
            }
        }
        .padding(12)
    }

    /// Pillola "Responsabile" — chiave Tolgee `app.org_chart.master_badge`.
    private var masterBadge: some View {
        HStack(spacing: 3) {
            Image(systemName: "star.fill")
                .font(.system(size: 8, weight: .bold))
            Text(tr("app.org_chart.master_badge", fallback: "Responsabile"))
                .font(.system(size: 9, weight: .bold))
                .tracking(0.3)
                .textCase(.uppercase)
        }
        .foregroundStyle(.white)
        .padding(.horizontal, 7)
        .padding(.vertical, 3)
        .background(Capsule().fill(AppTheme.Colors.brandTintAdaptive))
    }

    /// Pillola "Dimissionario" — chiave Tolgee `app.org_chart.inactive_badge`.
    private var inactiveBadge: some View {
        Text(tr("app.org_chart.inactive_badge", fallback: "Dimissionario"))
            .font(.system(size: 9, weight: .bold))
            .tracking(0.5)
            .textCase(.uppercase)
            .foregroundStyle(.white)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(Capsule().fill(.ultraThinMaterial))
            .overlay(
                Capsule().stroke(.white.opacity(0.25), lineWidth: 0.5)
            )
    }

    private var initials: String {
        let parts = member.name.split(separator: " ").prefix(2)
        let chars = parts.compactMap { $0.first }
        let s = String(chars).uppercased()
        return s.isEmpty ? "?" : s
    }
}

@MainActor
@Observable
final class OrgChartViewModel {
    var groups: [OrgChartGroup] = []
    var loading = true
    var errorMessage: String?

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
