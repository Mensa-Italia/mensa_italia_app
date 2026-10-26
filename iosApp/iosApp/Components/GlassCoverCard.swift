import SwiftUI

/// A cover image as a Liquid Glass card: inset from the edges by the caller,
/// rounded, with the image running right to the glass's edge so the glass
/// lights its rim.
///
/// Edge to edge, a cover read as cut off wherever something sits beside the
/// content (iPhone Duo's vertical bar, a split view's sidebar), even with the
/// whole image in view. The card takes the image's own proportions (same
/// store as the list cards), so it's never cropped either; past `maxHeight`
/// the image is shown whole over a blurred copy of itself.
struct GlassCoverCard<Placeholder: View>: View {
    let url: URL?
    var maxHeight: CGFloat = 440
    var cornerRadius: CGFloat = 28
    @ViewBuilder var placeholder: () -> Placeholder

    var body: some View {
        let ratio = CoverRatioStore.shared.ratio(for: url?.absoluteString)
        CoverLayout(ratio: ratio, maxHeight: maxHeight) {
            Group {
                if let url {
                    CachedAsyncImage(
                        url: url,
                        onLoad: { CoverRatioStore.shared.remember($0, for: url.absoluteString) }
                    ) { img in
                        ZStack {
                            // Only shows where the image doesn't reach,
                            // when capped by maxHeight.
                            img.resizable().aspectRatio(contentMode: .fill)
                                .blur(radius: 30)
                                .overlay(Color.black.opacity(0.2))
                            img.resizable().aspectRatio(contentMode: .fit)
                        }
                    } placeholder: {
                        placeholder()
                    }
                } else {
                    placeholder()
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        }
        .compatGlass(cornerRadius: cornerRadius)
        .shadow(color: .black.opacity(0.12), radius: 18, x: 0, y: 8)
    }
}

/// Full width, as tall as the image's ratio asks, capped at `maxHeight`.
/// `aspectRatio` + `frame(maxHeight:)` would narrow the card instead once
/// the cap kicks in.
private struct CoverLayout: Layout {
    let ratio: CGFloat
    let maxHeight: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? 0
        return CGSize(width: width, height: min(width / max(ratio, 0.01), maxHeight))
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        for subview in subviews {
            subview.place(at: bounds.origin, proposal: ProposedViewSize(bounds.size))
        }
    }
}
