import SwiftUI
import Shared

/// Compact list-row variant of the SIG card — same iconography and chip used
/// in `SigListView` but laid out for a dense inset-grouped list inside the
/// search results screen. The full `SigRowCard` (hero image) would dwarf
/// every other row, so we use a Discover-style icon + title + type chip.
struct SigSearchResultRow: View {
    let sig: SigModel

    private var imageURL: URL? {
        guard !sig.image.isEmpty else { return nil }
        if sig.image.hasPrefix("http") { return URL(string: sig.image) }
        return Files.url(
            collection: "sigs",
            recordId: sig.id,
            filename: sig.image,
            thumb: "200x200"
        )
    }

    private var typeIcon: String {
        let lower = sig.groupType.lowercased()
        if lower.contains("facebook") { return "f.cursive.circle.fill" }
        if lower.contains("chat")     { return "paperplane.fill" }
        if lower.contains("local")    { return "mappin.and.ellipse" }
        return "person.3.fill"
    }

    private var typeLabel: String? {
        let lower = sig.groupType.lowercased()
        if lower.contains("chat")     { return tr("community.filter.telegram", fallback: "Gruppi Telegram") } // i18n
        if lower.contains("local")    { return tr("community.filter.local", fallback: "Gruppi ufficiali") }  // i18n
        if lower.contains("sig")      { return tr("community.filter.sig", fallback: "SIG") } // i18n
        return sig.groupType.isEmpty ? nil : sig.groupType.replacingOccurrences(of: "_", with: " ").capitalized
    }

    var body: some View {
        HStack(spacing: 12) {
            artwork
                .frame(width: 44, height: 44)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

            VStack(alignment: .leading, spacing: 3) {
                Text(sig.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(2)
                if let label = typeLabel {
                    HStack(spacing: 5) {
                        Image(systemName: typeIcon)
                            .font(.caption2)
                            .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                        Text(label)
                            .font(.caption2.weight(.medium))
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    }
                }
            }

            Spacer(minLength: 0)
        }
        .padding(.vertical, 2)
        .contentShape(Rectangle())
    }

    @ViewBuilder
    private var artwork: some View {
        if let url = imageURL {
            CachedAsyncImage(url: url) { img in
                img.resizable().scaledToFill()
            } placeholder: {
                placeholder
            }
        } else {
            placeholder
        }
    }

    private var placeholder: some View {
        ZStack {
            AppTheme.brandGradient
            Image(systemName: typeIcon)
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(.white.opacity(0.95))
        }
    }
}
