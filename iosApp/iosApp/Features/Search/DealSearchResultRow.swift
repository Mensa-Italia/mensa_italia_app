import SwiftUI
import Shared

/// Compact row for a deal inside the global search List.
/// `DealCardView` (used in `DealListView`) lives inside a `LazyVStack` and
/// applies `.glassEffect` — that combo doesn't play well with the System List
/// hit-testing when wrapped in a `NavigationLink { … }` (the tap is swallowed).
/// This row mirrors the same data — title, optional sector chip, location /
/// details subtitle, prominent discount badge — without the glass surface, so
/// it integrates cleanly with `.insetGrouped` list styling.
struct DealSearchResultRow: View {
    let deal: DealModel

    /// Same heuristic as `DealCardView`: surface "NN%" if we can extract it,
    /// otherwise show a generic "Sconto" label.
    private var discountBadge: String {
        if let percent = DealParsers.shared.extractDiscountPercentFromCandidates(
            candidates: [deal.details, deal.who]
        ) {
            return "-\(percent.intValue)%"
        }
        return tr("app.deals.discount_generic", fallback: "Sconto")
    }

    private var subtitle: String? {
        if let loc = deal.position {
            let city = loc.name.trimmingCharacters(in: .whitespaces)
            let state = loc.state.trimmingCharacters(in: .whitespaces)
            let parts = [city, state].filter { !$0.isEmpty }
            if !parts.isEmpty { return parts.joined(separator: ", ") }
        }
        if let details = deal.details?.trimmingCharacters(in: .whitespacesAndNewlines),
           !details.isEmpty {
            return details
        }
        return nil
    }

    private var category: String? {
        let s = deal.commercialSector.trimmingCharacters(in: .whitespaces)
        return s.isEmpty ? nil : s
    }

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(deal.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)

                if let category {
                    Text(category)
                        .font(.caption2.weight(.semibold))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(
                            Capsule().fill(AppTheme.Colors.brandPrimary.opacity(0.12))
                        )
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                        .lineLimit(1)
                }

                if let subtitle {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Text(discountBadge)
                .font(.caption.bold())
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Capsule().fill(AppTheme.Colors.brandPrimary))
                .foregroundStyle(.white)
                .fixedSize(horizontal: true, vertical: false)
        }
        .padding(.vertical, 2)
        .contentShape(Rectangle())
    }
}
