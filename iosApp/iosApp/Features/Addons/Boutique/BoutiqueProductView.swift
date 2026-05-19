import SwiftUI
import SafariServices
import Shared

/// Detail view for a single boutique product.
struct BoutiqueProductView: View {
    let productId: String

    @State private var product: BoutiqueModel?
    @State private var sub: Closeable?
    @State private var showSafari: URL?
    @State private var showContactAlert = false

    var body: some View {
        Group {
            if let product {
                content(product: product)
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .navigationTitle(product?.name ?? tr("addons.boutique.product", fallback: "Prodotto"))
        .navigationBarTitleDisplayMode(.inline)
        .task { start() }
        .onDisappear { stop() }
        .sheet(item: $showSafari) { url in
            SafariSheet(url: url)
        }
        .alert(
            tr("addons.boutique.contact.title", fallback: "Contatta Mensa per ordinare"),
            isPresented: $showContactAlert
        ) {
            Button("OK") {}
        } message: {
            Text(tr(
                "addons.boutique.contact.message",
                fallback: "Per acquistare questo prodotto contatta la segreteria nazionale."
            ))
        }
    }

    @ViewBuilder
    private func content(product: BoutiqueModel) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                heroImage(product: product)

                VStack(alignment: .leading, spacing: 8) {
                    Text(product.name)
                        .font(.title2.bold())
                    Text(BoutiqueFormatting.formatPrice(amount: product.amount))
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                }

                if !product.alternativeOf.isEmpty {
                    Text(product.alternativeOf)
                        .font(.caption.weight(.semibold))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(AppTheme.Colors.mensaCyan.opacity(0.25), in: Capsule())
                        .foregroundStyle(AppTheme.Colors.mensaBlueDeep)
                }

                if !product.description_.isEmpty {
                    Text(product.description_)
                        .font(.body)
                        .foregroundStyle(.primary)
                }

                Button {
                    if let url = orderURL(for: product) {
                        showSafari = url
                    } else {
                        showContactAlert = true
                    }
                } label: {
                    Text(tr("addons.boutique.order_now", fallback: "Ordina ora"))
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                }
                .buttonStyle(.glassProminent)
                .tint(AppTheme.Colors.mensaBlue)
                .padding(.top, 8)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
    }

    @ViewBuilder
    private func heroImage(product: BoutiqueModel) -> some View {
        if let first = product.image.first,
           !first.isEmpty,
           let url = Files.url(
                collection: "boutique",
                recordId: product.id,
                filename: first,
                thumb: "1200x900"
           ) {
            CachedAsyncImage(url: url) { img in
                img.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                AppTheme.brandGradient
            }
            .frame(height: 280)
            .frame(maxWidth: .infinity)
            .clipped()
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        } else {
            AppTheme.brandGradient
                .frame(height: 280)
                .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                .overlay(
                    Image(systemName: "bag.fill")
                        .font(.system(size: 48, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.85))
                )
        }
    }

    /// `BoutiqueModel` does not currently carry an explicit `orderUrl` field;
    /// some installations encode the order URL inside `description`. We try to
    /// extract the first http(s) link from the description.
    private func orderURL(for product: BoutiqueModel) -> URL? {
        let detector = try? NSDataDetector(
            types: NSTextCheckingResult.CheckingType.link.rawValue
        )
        let range = NSRange(location: 0, length: product.description_.utf16.count)
        guard
            let match = detector?.firstMatch(
                in: product.description_,
                options: [],
                range: range
            ),
            let url = match.url,
            url.scheme?.hasPrefix("http") == true
        else { return nil }
        return url
    }

    private func start() {
        sub?.close()
        sub = FlowBridgeKt.subscribe(
            flow: koin.boutique.observeOne(id: productId),
            onEach: { value in
                Task { @MainActor in
                    self.product = value as? BoutiqueModel
                }
            },
            onError: { _ in }
        )
    }

    private func stop() { sub?.close(); sub = nil }
}

// MARK: - Safari helper

private struct SafariSheet: View, Identifiable {
    let url: URL
    var id: URL { url }
    var body: some View { SafariRepresentable(url: url).ignoresSafeArea() }
}

extension URL: @retroactive Identifiable {
    public var id: String { absoluteString }
}

private struct SafariRepresentable: UIViewControllerRepresentable {
    let url: URL
    func makeUIViewController(context: Context) -> SFSafariViewController {
        SFSafariViewController(url: url)
    }
    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {}
}
