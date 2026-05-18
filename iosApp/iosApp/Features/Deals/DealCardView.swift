import SwiftUI
import Shared

/// Compact full-width tile used by `DealListView`.
/// Title + optional category chip + subtitle on the leading side;
/// prominent discount badge on the trailing side.
struct DealCardView: View {
    let deal: DealModel

    /// Heuristic: surface any "NN%" hint we can reliably extract from
    /// `details` / `who`. Falls back to a generic "Sconto" label.
    private var discountBadge: String {
        let candidates = [deal.details, deal.who].compactMap { $0 }
        for text in candidates {
            if let range = text.range(
                of: #"(\d{1,3})\s?%"#,
                options: .regularExpression
            ) {
                let raw = String(text[range]).replacingOccurrences(of: " ", with: "")
                return "-\(raw)"
            }
        }
        return tr("app.deals.discount_generic", fallback: "Sconto") // i18n
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
                    .font(.headline)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .foregroundStyle(.primary)

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
                .font(.subheadline.bold())
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(
                    Capsule().fill(AppTheme.Colors.brandPrimary)
                )
                .foregroundStyle(.white)
                .fixedSize(horizontal: true, vertical: false)
        }
        .padding(12)
        .frame(maxWidth: .infinity, minHeight: 76, alignment: .leading)
        .glassEffect(.regular, in: .rect(cornerRadius: 16))
        .contentShape(Rectangle())
    }
}
