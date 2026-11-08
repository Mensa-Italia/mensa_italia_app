import SwiftUI

// Blocks of the Today page, shared by `TodayView` and the "Gruppo locale"
// tab (`LocalGroupView`): one implementation each.

/// A section's small uppercase title with its icon.
struct TodaySectionLabel: View {
    let title: String
    let icon: String

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: icon)
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text(title.uppercased())
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
                .tracking(0.5)
        }
    }
}

/// An icon and a message in a glass box, where a section has nothing to show.
struct TodayEmptyState: View {
    let icon: String
    let message: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundStyle(.secondary)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .compatGlass(cornerRadius: 14)
    }
}

/// A row of the membership card: colored icon tile, title, and on the right
/// an optional medal count, an optional value and the chevron. `detail`
/// goes under the title (as in `LeaderboardMembershipRow`); `showsChevron`
/// off for rows that only show a figure.
struct TodayMembershipRow: View {
    let icon: String
    let color: Color
    let title: String
    var detail: String? = nil
    var value: String? = nil
    var medalCount: Int? = nil
    var showsChevron: Bool = true

    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 7, style: .continuous)
                    .fill(color)
                    .frame(width: 30, height: 30)
                Image(systemName: icon)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
            }
            if let detail {
                VStack(alignment: .leading, spacing: 1) {
                    Text(title)
                        .font(.body)
                        .foregroundStyle(.primary)
                    Text(detail)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            } else {
                Text(title)
                    .font(.body)
                    .foregroundStyle(.primary)
            }
            Spacer()
            if let medalCount {
                HStack(spacing: 4) {
                    Image(systemName: "medal.fill")
                        .font(.footnote.weight(.semibold))
                    Text("\(medalCount)")
                        .font(.subheadline.weight(.semibold).monospacedDigit())
                }
                .foregroundStyle(.secondary)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Text(
                    tr("medals.count_a11y", fallback: "%d medaglie").replacingOccurrences(of: "%d", with: "\(medalCount)")
                ))
            }
            if let value {
                Text(value)
                    .font(.headline.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
            if showsChevron {
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }
}

/// The "Vedi tutte" line at the bottom of a card: accent text and a chevron.
struct TodaySeeAllRow: View {
    let title: String

    var body: some View {
        HStack(spacing: 6) {
            Text(title)
                .font(.subheadline.weight(.medium))
            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
            Spacer()
        }
        .foregroundStyle(Color.accentColor)
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }
}
