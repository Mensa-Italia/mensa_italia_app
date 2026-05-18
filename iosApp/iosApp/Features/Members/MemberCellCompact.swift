import SwiftUI
import Shared

/// Compact directory cell, iPhone Contacts style:
/// avatar + "FirstName **LastName**", no chevron, no secondary line.
struct MemberCellCompact: View {
    let member: RegSociModel

    var body: some View {
        HStack(spacing: 12) {
            MemberAvatar(member: member, size: 40)

            styledName
                .lineLimit(1)
                .foregroundStyle(.primary)

            Spacer(minLength: 0)
        }
        .padding(.vertical, 2)
        .contentShape(Rectangle())
    }

    /// First name regular, last name bold — matches Apple's Contacts default.
    /// Words are normalized to Title Case (the backend stores UPPERCASE).
    private var styledName: Text {
        let parts = member.name
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

/// Circular avatar with cached remote image and a gradient initial fallback.
struct MemberAvatar: View {
    let member: RegSociModel
    let size: CGFloat

    private var resolvedURL: URL? {
        let raw = member.image
        guard !raw.isEmpty, !isPlaceholderURL(raw) else { return nil }
        if raw.hasPrefix("http://") || raw.hasPrefix("https://") {
            return URL(string: raw)
        }
        // PocketBase serve solo i thumb DICHIARATI nella field config; richiedere
        // una size non in lista risponde 400 e il render finisce sulle iniziali.
        // `200x200` è l'unico thumb che combaci sia con la lista (40pt) sia con
        // l'hero del dettaglio (120pt). Slight under-res a 3× retina sul detail,
        // ma è la scelta sicura. Per qualità maggiore: configurare un thumb più
        // grande lato PB e aggiornarlo qui.
        return Files.url(
            collection: "members_registry",
            recordId: member.id,
            filename: raw,
            thumb: "0x100"
        )
    }

    var body: some View {
        Group {
            if let url = resolvedURL {
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
        .overlay(
            Circle().stroke(.white.opacity(0.6), lineWidth: 1)
        )
    }

    private var initials: String {
        let parts = member.name.split(separator: " ").prefix(2)
        let chars = parts.compactMap { $0.first }
        let result = String(chars).uppercased()
        return result.isEmpty ? "?" : result
    }

    private var initialBubble: some View {
        ZStack {
            AppTheme.brandGradient
            Text(initials)
                .font(.system(size: size * 0.4, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
        }
    }

    /// The legacy backend often returns this Cloud32 silhouette for users
    /// without a photo. Treat it as missing so we render the initials.
    private func isPlaceholderURL(_ url: String) -> Bool {
        url.contains("cloud32.it/Associazioni/img/Uomo-1.png")
    }
}
