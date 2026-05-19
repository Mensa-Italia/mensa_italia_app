import SwiftUI
import Shared

/// Boutique — grid 2-col of products with cached-first data.
struct BoutiqueView: View {
    @State private var products: [BoutiqueModel] = []
    @State private var refreshing = false
    @State private var appeared = false
    @State private var sub: Closeable?

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    var body: some View {
        Group {
            if products.isEmpty && !refreshing {
                ContentUnavailableView(
                    tr("addons.boutique.empty", fallback: "Boutique vuota"),
                    systemImage: "bag",
                    description: Text(tr(
                        "addons.boutique.empty_description",
                        fallback: "Non ci sono prodotti disponibili al momento."
                    ))
                )
            } else {
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(Array(products.enumerated()), id: \.element.id) { idx, product in
                            NavigationLink(value: BoutiqueProductRoute(productId: product.id)) {
                                BoutiqueProductCard(product: product)
                            }
                            .buttonStyle(.plain)
                            .opacity(appeared ? 1 : 0)
                            .offset(y: appeared ? 0 : 14)
                            .animation(
                                .spring(response: 0.5, dampingFraction: 0.86)
                                    .delay(Double(min(idx, 12)) * 0.06),
                                value: appeared
                            )
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                }
                .refreshable { await refresh() }
            }
        }
        .navigationTitle(tr("addons.boutique.title", fallback: "Boutique"))
        .task {
            start()
            withAnimation(.easeOut(duration: 0.35)) { appeared = true }
        }
        .onDisappear { stop() }
    }

    private func start() {
        sub?.close()
        sub = FlowBridgeKt.subscribe(
            flow: koin.boutique.observeAll(),
            onEach: { value in
                Task { @MainActor in
                    self.products = (value as? [BoutiqueModel]) ?? []
                }
            },
            onError: { _ in }
        )
        Task { await refresh() }
    }

    private func stop() { sub?.close(); sub = nil }

    private func refresh() async {
        refreshing = true
        defer { refreshing = false }
        do { try await koin.boutique.refresh() } catch { }
    }
}

// MARK: - Card

private struct BoutiqueProductCard: View {
    let product: BoutiqueModel

    private var imageURL: URL? {
        guard let first = product.image.first, !first.isEmpty else { return nil }
        return Files.url(
            collection: "boutique",
            recordId: product.id,
            filename: first,
            thumb: "600x600"
        )
    }

    private var priceText: String {
        BoutiqueFormatting.formatPrice(amount: product.amount)
    }

    var body: some View {
        GlassCard(padding: 0, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 0) {
                CachedAsyncImage(url: imageURL) { img in
                    img.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    AppTheme.brandGradient
                        .overlay(
                            Image(systemName: "bag.fill")
                                .font(.system(size: 28, weight: .semibold))
                                .foregroundStyle(.white.opacity(0.85))
                        )
                }
                .frame(height: 150)
                .frame(maxWidth: .infinity)
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

                VStack(alignment: .leading, spacing: 4) {
                    Text(product.name)
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(2)
                        .foregroundStyle(.primary)
                    Text(priceText)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

// MARK: - Formatting

enum BoutiqueFormatting {
    /// Amount comes from PocketBase as int (cents-or-euros depending on source).
    /// We treat the value as integer euros to match the Flutter app's behaviour.
    static func formatPrice(amount: Int32) -> String {
        let f = NumberFormatter()
        f.numberStyle = .currency
        f.currencyCode = "EUR"
        f.locale = Locale(identifier: "it_IT")
        f.maximumFractionDigits = 2
        f.minimumFractionDigits = 0
        return f.string(from: NSNumber(value: amount)) ?? "€ \(amount)"
    }
}
