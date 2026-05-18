import SwiftUI
import Shared

// The old flat-feed `QuidListView` was retired when the addon switched to the
// Issues → Articles hierarchy. The card + chip subviews live on because they
// are still reused by `QuidIssueView` and the global Search results — they
// just no longer have a parent list view in this file.

// MARK: - Card

struct QuidArticleCard: View {
    let article: QuidArticle

    private var coverURL: URL? {
        guard let raw = article.coverImageUrl, !raw.isEmpty else { return nil }
        return URL(string: raw)
    }

    private var relativeDateText: String {
        guard let date = QuidDateParser.parse(article.date) else { return article.date }
        let formatter = RelativeDateTimeFormatter()
        formatter.locale = Locale(identifier: "it_IT")
        formatter.unitsStyle = .full
        return formatter.localizedString(for: date, relativeTo: Date())
    }

    var body: some View {
        GlassCard(padding: 0, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 0) {
                // Cover image — locked to 16:9 of card width regardless of source aspect.
                Color.clear
                    .aspectRatio(16 / 9, contentMode: .fit)
                    .overlay {
                        CachedAsyncImage(url: coverURL) { img in
                            img.resizable().aspectRatio(contentMode: .fill)
                        } placeholder: {
                            AppTheme.brandGradient
                                .overlay(
                                    Image(systemName: "magazine.fill")
                                        .font(.system(size: 32, weight: .semibold))
                                        .foregroundStyle(.white.opacity(0.85))
                                )
                        }
                    }
                    .clipped()
                    .clipShape(
                        UnevenRoundedRectangle(
                            topLeadingRadius: 18,
                            bottomLeadingRadius: 0,
                            bottomTrailingRadius: 0,
                            topTrailingRadius: 18,
                            style: .continuous
                        )
                    )

                VStack(alignment: .leading, spacing: 6) {
                    if !article.categoryNames.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 6) {
                                ForEach(article.categoryNames, id: \.self) { cat in
                                    QuidCategoryChip(label: cat)
                                }
                            }
                        }
                    }

                    Text(article.titlePlain)
                        .font(.headline)
                        .foregroundStyle(.primary)
                        .lineLimit(3)

                    Text(relativeDateText)
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    if !article.excerptPlain.isEmpty {
                        Text(article.excerptPlain)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .lineLimit(3)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

// MARK: - Category chip

/// Adaptive chip — uses `.thinMaterial` so the fill works against any background
/// (light/dark), and `.foregroundStyle(.primary)` so the label stays legible.
struct QuidCategoryChip: View {
    let label: String

    var body: some View {
        Text(label)
            .font(.caption2.weight(.semibold))
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(.thinMaterial, in: Capsule())
            .overlay(
                Capsule().stroke(Color.primary.opacity(0.12), lineWidth: 0.5)
            )
            .foregroundStyle(.primary)
    }
}
