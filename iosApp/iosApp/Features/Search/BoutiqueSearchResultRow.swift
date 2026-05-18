import SwiftUI
import Shared

/// Compact horizontal row for a boutique product, used inside the search
/// results list. The full `BoutiqueProductCard` is grid-only; in a List it
/// would be too tall — this row mirrors the same data (image + name + price)
/// but in a leading-image horizontal layout.
struct BoutiqueSearchResultRow: View {
    let product: BoutiqueModel

    private var imageURL: URL? {
        guard let first = product.image.first, !first.isEmpty else { return nil }
        return Files.url(
            collection: "boutique",
            recordId: product.id,
            filename: first,
            thumb: "200x200"
        )
    }

    private var priceText: String {
        BoutiqueFormatting.formatPrice(amount: product.amount)
    }

    var body: some View {
        HStack(spacing: 12) {
            Group {
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
            .frame(width: 56, height: 56)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            VStack(alignment: .leading, spacing: 3) {
                Text(product.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(2)
                Text(priceText)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            }

            Spacer(minLength: 0)
        }
        .padding(.vertical, 2)
        .contentShape(Rectangle())
    }

    private var placeholder: some View {
        ZStack {
            AppTheme.brandGradient
            Image(systemName: "bag.fill")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(.white.opacity(0.9))
        }
    }
}
