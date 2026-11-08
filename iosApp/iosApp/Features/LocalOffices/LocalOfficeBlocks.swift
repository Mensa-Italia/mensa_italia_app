import Shared
import SwiftUI

// Blocks of the local office page, shared by `LocalOfficeView` and the
// "Gruppo locale" tab (`LocalGroupView`): one implementation each.

// MARK: - Hero

/// The office's cover, full width and 250 pt tall, stretching when pulled
/// down. The page's ScrollView ignores the top safe area so it reaches the
/// screen's edge.
struct LocalOfficeHero: View {
    let office: LocalOfficeModel
    /// `false` (the office page): full-bleed at the very top, under an inline
    /// bar, stretching on overscroll. `true` (a root tab under a large title):
    /// a rounded card in the content, no stretch, as `EventRowCard`.
    var inset: Bool = false

    private var coverURL: URL? {
        guard !office.image.isEmpty else { return nil }
        return Files.url(
            collection: "local_offices",
            recordId: office.id,
            filename: office.image,
            thumb: "0x500"
        )
    }

    var body: some View {
        if inset {
            GeometryReader { geo in
                cover(width: geo.size.width, height: geo.size.height)
            }
            .frame(height: 200)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .padding(.horizontal, 20)
            .padding(.top, 8)
        } else {
            GeometryReader { geo in
                let minY = geo.frame(in: .global).minY
                let stretch = max(0, minY)
                cover(width: geo.size.width, height: geo.size.height + stretch)
                    .offset(y: -stretch)
            }
            .frame(height: 250)
        }
    }

    private func cover(width: CGFloat, height: CGFloat) -> some View {
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
            .frame(width: width, height: height)
            .clipped()

            LinearGradient(
                colors: [.black.opacity(0), .black.opacity(0.45)],
                startPoint: .top, endPoint: .bottom
            )
            .frame(width: width, height: height)
        }
        .frame(width: width, height: height)
        .clipped()
    }
}

// MARK: - Kicker, name, bio

/// "GRUPPO LOCALE", the office's name in the serif title and its bio, under the hero.
struct LocalOfficeIntro: View {
    let office: LocalOfficeModel
    /// The eyebrow above the name; nil keeps "GRUPPO LOCALE" (the office
    /// page). The local group tab passes the region, its title already says
    /// "Gruppo locale".
    var kicker: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text((kicker ?? tr("local_office.kicker", fallback: "GRUPPO LOCALE")).uppercased())
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
    }
}

// MARK: - Section scaffolding

/// A section's title on the office page.
struct LocalOfficeSectionHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(.title3.weight(.bold))
            .foregroundStyle(.primary)
    }
}

/// Wraps a section in the page's padding and its staggered entrance.
struct LocalOfficeSectionBlock<Content: View>: View {
    let index: Int
    let appeared: Bool
    @ViewBuilder let content: () -> Content

    var body: some View {
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
}

// MARK: - Avatar

/// A person's photo in a circle, or their initials on the brand gradient.
struct LocalOfficeAvatar: View {
    let url: URL?
    let name: String
    let size: CGFloat

    var body: some View {
        Group {
            if let url = url {
                CachedAsyncImage(url: url) { img in
                    img.resizable().scaledToFill()
                } placeholder: {
                    initialBubble
                }
            } else {
                initialBubble
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(Circle().stroke(.white.opacity(0.6), lineWidth: 1))
    }

    private var initialBubble: some View {
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
        let result = String(chars).uppercased()
        return result.isEmpty ? "?" : result
    }
}

// MARK: - Linktree

/// The office's links: the first two root links, with "Vedi tutti" (and
/// "Modifica" for whoever can edit) opening `LocalOfficeLinktreeView`
/// through `LocalOfficeLinktreeRoute`, which the page must register.
struct LocalOfficeLinktreeSection: View {
    let officeId: String
    let rows: [LocalOfficeLinktreeRowModel]
    var canEdit: Bool = false
    var title: String = tr("local_office.linktree.title", fallback: "Link utili")

    var body: some View {
        let sorted = rows.sorted { $0.sortOrder < $1.sortOrder }
        let rootLinks = sorted.filter { $0.parent == "" && $0.kind == "link" }
        let hasSections = sorted.contains { $0.parent == "" && $0.kind == "section" }
        let previewLinks = Array(rootLinks.prefix(2))
        let showSeeAll = rootLinks.count > 2 || hasSections

        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .center) {
                LocalOfficeSectionHeader(title: title)
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
                    LocalOfficeLinktreeRow(row: link)
                }
                if rows.isEmpty {
                    Text(tr("local_office.linktree.empty", fallback: "Nessun link"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .padding(.vertical, 8)
                }
            }
        }
    }
}

/// One link of the linktree, opened outside the app.
struct LocalOfficeLinktreeRow: View {
    let row: LocalOfficeLinktreeRowModel

    var body: some View {
        Button {
            ExternalLink.open(row.url)
        } label: {
            HStack(spacing: 12) {
                LocalOfficeLinkIcon(raw: row.icon)
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
            .compatGlass(cornerRadius: 14)
        }
        .buttonStyle(.plain)
    }
}

/// Resolves a link's icon field: emoji → Text, brand name → SF symbol, SF symbol, fallback link.
struct LocalOfficeLinkIcon: View {
    let raw: String

    var body: some View {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.unicodeScalars.count == 1 && trimmed.unicodeScalars.first.map({ $0.value > 127 }) == true {
            // Single non-ASCII scalar → treat as emoji
            Text(trimmed).font(.system(size: 18))
        } else if let sfForBrand = Self.brandSFSymbol(trimmed.lowercased()) {
            Image(systemName: sfForBrand)
        } else if !trimmed.isEmpty && UIImage(systemName: trimmed) != nil {
            Image(systemName: trimmed)
        } else {
            Image(systemName: "link")
        }
    }

    static func brandSFSymbol(_ name: String) -> String? {
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
}

// MARK: - Admins (referenti)

/// The secretary first, then the co-secretaries by name; each row opens the
/// member's page through `RegSociRoute`, which the page must register.
/// `showsMail` adds an envelope that writes to the person (the "Gruppo
/// locale" tab, where the admins' mail is known).
struct LocalOfficeAdminsSection: View {
    let admins: [LocalOfficeAdminModel]
    var title: String = tr("local_office.admins.title", fallback: "Referenti")
    var showsMail: Bool = false

    var body: some View {
        let sorted = admins.sorted { a, b in
            if a.isTheOfficer != b.isTheOfficer { return a.isTheOfficer }
            return a.name < b.name
        }

        VStack(alignment: .leading, spacing: 10) {
            LocalOfficeSectionHeader(title: title)

            ForEach(sorted, id: \.id) { admin in
                NavigationLink(value: RegSociRoute(id: admin.user)) {
                    LocalOfficeAdminRow(admin: admin, showsMail: showsMail)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

/// An admin: avatar, name, and the Segretario / Cosegretario capsule.
struct LocalOfficeAdminRow: View {
    let admin: LocalOfficeAdminModel
    var showsMail: Bool = false
    @Environment(\.openURL) private var openURL

    private var imageURL: URL? {
        guard !admin.image.isEmpty else { return nil }
        return Files.url(
            collection: "view_local_office_admins",
            recordId: admin.id,
            filename: admin.image,
            thumb: "200x200"
        )
    }

    var body: some View {
        HStack(spacing: 12) {
            LocalOfficeAvatar(url: imageURL, name: admin.name, size: 40)

            Text(TextFormatters.shared.titleCase(s: admin.name))
                .font(.subheadline)
                .foregroundStyle(.primary)
                .lineLimit(1)

            Spacer(minLength: 0)

            if showsMail, !admin.email.isEmpty, let url = URL(string: "mailto:\(admin.email)") {
                // A Button of its own (not a Link) inside the row's
                // NavigationLink: borderless keeps the two taps separate.
                Button {
                    openURL(url)
                } label: {
                    Image(systemName: "envelope")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                        .frame(width: 32, height: 32)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(Text(tr("local_group.contact.write", fallback: "Scrivi")))
            }

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
        .compatGlass(cornerRadius: 14)
        .contentShape(.rect(cornerRadius: 14))
    }
}
