import SwiftUI
import Shared

/// Search-result row for a member. One uniform row shape for every person —
/// role-holders are NOT promoted to a separate hero tile. They're amalgamated
/// with the rest of the directory and distinguished only by:
///   • a thin brand-tint ring around the avatar
///   • a small star + role + group chip beneath the name
/// This keeps the people section visually coherent: a long unified list of
/// equal-weight rows that the eye can scan continuously.
///
/// `MemberCellCompact` (used by `MembersDirectoryView`) is intentionally NOT
/// reused here because we need to overlay the avatar ring without leaking the
/// role badge into every list of the app.
struct PersonSearchResultRow: View {
    let member: RegSociModel
    let role: String?
    let group: String?
    /// Local-office roles derived from the `local_office_admin` / `test_assistant`
    /// search hit types. Rendered as small brand-tint chips below the name.
    let localOfficeAffiliations: [LocalOfficeAffiliation]

    init(
        member: RegSociModel,
        role: String?,
        group: String?,
        localOfficeAffiliations: [LocalOfficeAffiliation] = []
    ) {
        self.member = member
        self.role = role
        self.group = group
        self.localOfficeAffiliations = localOfficeAffiliations
    }

    /// Locally fetched copy of the member, used when the upstream `member`
    /// has an empty `image` (the `members_registry` LIST endpoint frequently
    /// returns image=""; only `getById` returns the canonical filename).
    /// We hold the fetched result here instead of relying on the cache →
    /// flow → SearchViewModel.rebuildIfPossible chain, which was flaky in
    /// practice (the avatar would only appear after opening the detail).
    /// Owning the fetched record at the row level decouples the avatar from
    /// upstream cache propagation entirely.
    @State private var fetchedMember: RegSociModel? = nil

    /// Render-time member: prefer the locally-fetched record (if any), fall
    /// back to whatever the upstream gave us.
    private var displayMember: RegSociModel {
        fetchedMember ?? member
    }

    private var hasRole: Bool {
        if let role, !role.isEmpty { return true }
        return false
    }

    var body: some View {
        HStack(spacing: 12) {
            avatar

            VStack(alignment: .leading, spacing: 3) {
                styledName
                    .lineLimit(1)
                    .foregroundStyle(.primary)

                if hasRole {
                    roleLine
                } else if !displayMember.city.isEmpty {
                    Text(displayMember.city.capitalized)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }

                if !localOfficeAffiliations.isEmpty {
                    affiliationChips
                }
            }

            Spacer(minLength: 0)
        }
        .padding(.vertical, 2)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
        // On first appearance (and when the row is reused for a different id):
        // if the upstream member has no photo, fetch the full record directly
        // and store it in @State. We use the fetched object for rendering — no
        // dependency on the SQLDelight cache propagating back. `try?` keeps
        // failures silent (the row simply stays on initials).
        .task(id: member.id) {
            Log.ui.info("[avatar] task fired id=\(member.id) image='\(member.image)'")
            // If the upstream already has the photo, nothing to do.
            guard member.image.isEmpty else {
                Log.ui.info("[avatar] skip — image already present")
                return
            }
            // If we already fetched this same id, skip.
            if let f = fetchedMember, f.id == member.id {
                Log.ui.info("[avatar] skip — already fetched")
                return
            }
            do {
                Log.ui.info("[avatar] calling getById id=\(member.id)")
                if let fresh = try await koin.regSoci.getById(id: member.id) {
                    Log.ui.info("[avatar] getById OK id=\(member.id) freshImage='\(fresh.image)'")
                    fetchedMember = fresh
                } else {
                    Log.ui.info("[avatar] getById returned nil id=\(member.id)")
                }
            } catch {
                Log.ui.error("[avatar] getById threw id=\(member.id) err=\(error.localizedDescription)")
            }
        }
    }

    // MARK: - Avatar with optional ring

    /// 40pt circular member avatar. When the member holds an orgchart role,
    /// a thin brand-tint ring (1.5pt) is inset around it — a quiet authority
    /// signal that doesn't disrupt the row rhythm.
    private var avatar: some View {
        MemberAvatar(member: displayMember, size: 40)
            .overlay {
                if hasRole {
                    Circle()
                        .strokeBorder(
                            AppTheme.Colors.brandTintAdaptive.opacity(0.55),
                            lineWidth: 1.5
                        )
                        .padding(-2) // ring sits just outside the avatar edge
                }
            }
    }

    // MARK: - Local office affiliation chips

    /// Horizontal stack of small brand-tint capsules, one per affiliation
    /// (e.g. "★ Segretario di Lombardia"). Different leading glyph for
    /// admins vs test-assistants — kept compact so the row height doesn't
    /// blow up when a member has multiple roles.
    @ViewBuilder
    private var affiliationChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(localOfficeAffiliations, id: \.self) { aff in
                    HStack(spacing: 4) {
                        Image(systemName: aff.kind == .admin ? "star.fill" : "graduationcap.fill")
                            .font(.system(size: 9, weight: .bold))
                        Text(aff.label)
                            .font(.caption2.weight(.semibold))
                            .lineLimit(1)
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(
                        Capsule().fill(AppTheme.Colors.brandPrimary.opacity(0.12))
                    )
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                }
            }
        }
        .scrollClipDisabled()
    }

    // MARK: - Role line (chip-styled, single line, gracefully truncated)

    /// "★ Ruolo · Gruppo" rendered as a single composed `Text` so the
    /// system handles truncation cleanly when both pieces are long. Star
    /// glyph is interpolated so it shares the truncation budget instead of
    /// being clipped first.
    @ViewBuilder
    private var roleLine: some View {
        let r = role ?? ""
        let g = localizedGroup(group ?? "")
        let line: Text = {
            var t = Text(Image(systemName: "star.fill"))
                .font(.caption2)
                .foregroundColor(AppTheme.Colors.brandTintAdaptive)
                + Text("  ")
                + Text(r)
                    .font(.caption2.weight(.semibold))
                    .foregroundColor(AppTheme.Colors.brandTintAdaptive)
            if !g.isEmpty {
                t = t
                    + Text("  ·  ")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    + Text(g)
                        .font(.caption2)
                        .foregroundColor(.secondary)
            }
            return t
        }()
        line
            .lineLimit(1)
            .truncationMode(.tail)
    }

    /// The orgchart `group` field on PocketBase is a translation key
    /// (eg. "consiglio", "team_comunicazione"). Mirror `OrgChartView`'s
    /// `localizedGroupTitle(_:)`: try Tolgee first, fall back to a
    /// prettified version (underscores/dashes → spaces, Title Case) so
    /// the user never sees the raw key.
    private func localizedGroup(_ raw: String) -> String {
        guard !raw.isEmpty else { return "" }
        let pretty = raw
            .replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: "-", with: " ")
            .capitalized
        return tr(raw, fallback: pretty)
    }

    // MARK: - Name styling

    /// First name regular, last name semibold — matches Apple Contacts and
    /// the directory cell. Words normalized to Title Case from the upper-case
    /// backend.
    private var styledName: Text {
        let parts = displayMember.name
            .split(separator: " ")
            .map { word -> String in
                guard let first = word.first else { return String(word) }
                return String(first).uppercased() + word.dropFirst().lowercased()
            }
        guard let last = parts.last else { return Text("") }
        let first = parts.dropLast().joined(separator: " ")
        if first.isEmpty {
            return Text(last).font(.body.weight(.semibold))
        }
        return Text(first).font(.body)
            + Text(" ")
            + Text(last).font(.body.weight(.semibold))
    }
}
