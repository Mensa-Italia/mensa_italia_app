import SwiftUI

/// Hardback passport cover: Mensa cobalt leather, pebbled and darkened where
/// it wraps the board, stitched round the edge, with the Mensa mark and the
/// lettering hot-stamped in gold foil. Designed to be rotated around its
/// leading edge via rotation3DEffect.
///
/// Cheap to draw on purpose: it zooms in as the page opens and curls as it's
/// swiped, so the grain is a pre-drawn tile and nothing in it is blurred.
struct PassportCover: View {
    let width: CGFloat
    let height: CGFloat
    /// 0 when the cover fills the screen edge to edge: the display's own
    /// corners round it.
    var cornerRadius: CGFloat = 18
    /// Where the cover runs under something: the leather goes there, the
    /// stitching and the lettering don't.
    var insets = EdgeInsets()
    /// Line under the lettering inviting to open; the default is a tap hint.
    var hint: String = tr("passport.swipe_to_open", fallback: "· SWIPE TO OPEN ·")

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        ZStack {
            leather

            // Stitching, a few points in from the edge.
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(
                    Color(red: 0.36, green: 0.42, blue: 0.62).opacity(0.85),
                    style: StrokeStyle(lineWidth: 1.1, lineCap: .round, dash: [5, 3.5])
                )
                // The holes the thread runs through, pressed into the leather.
                .shadow(color: .black.opacity(0.6), radius: 0.4, x: 0, y: 0.8)
                .padding(10)
                .padding(insets)

            // Blind-embossed frame: a line pressed in, no colour of its own.
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(.black.opacity(0.35), lineWidth: 1)
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(.white.opacity(0.10), lineWidth: 1)
                        .offset(y: 1)
                )
                .padding(26)
                .padding(insets)

            VStack(spacing: 8) {
                Spacer()
                GoldFoil {
                    Image("TesseraLogoMark")
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                }
                .frame(width: min(width * 0.36, 150), height: min(width * 0.36, 150))
                Spacer()
                GoldFoil {
                    Text("MENSA ITALIA")
                        .font(.system(size: 18, weight: .black, design: .serif))
                        .tracking(6)
                }
                .fixedSize()
                GoldFoil {
                    Text("TABLEPORT")
                        .font(.system(size: 13, weight: .bold, design: .serif))
                        .tracking(8)
                }
                .fixedSize()
                Spacer().frame(height: 24)
                Text(hint)
                    .font(.system(size: 9, weight: .semibold, design: .serif))
                    .tracking(3)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(PassportPalette.gold.opacity(0.7))
                Spacer().frame(height: 30)
            }
            .padding(.horizontal, 28)
            .padding(insets)
        }
        .frame(width: width, height: height)
        .clipShape(shape)
        .overlay(
            // Spine on the leading edge, where the cover bends.
            HStack(spacing: 0) {
                LinearGradient(
                    colors: [.black.opacity(0.45), .black.opacity(0.0)],
                    startPoint: .leading, endPoint: .trailing
                )
                .frame(width: 16)
                Spacer()
            }
            .allowsHitTesting(false)
        )
    }

    /// The hide: cobalt lit from the upper left, a pebbled grain, darker
    /// where it folds over the board's edges.
    private var leather: some View {
        ZStack {
            RadialGradient(
                colors: [PassportPalette.coverHi, PassportPalette.coverMid, PassportPalette.coverDeep],
                center: .init(x: 0.35, y: 0.25),
                startRadius: 6,
                endRadius: max(width, height) * 0.95
            )
            // The grain, drawn once into a tile and repeated: drawn whole,
            // tens of thousands of bumps were redrawn on every frame of the
            // zoom that opens the page, which then took seconds.
            Image(uiImage: LeatherGrain.tile)
                .resizable(resizingMode: .tile)
            // Where the leather wraps the board, it darkens.
            EllipticalGradient(
                colors: [.clear, .clear, .black.opacity(0.42)],
                center: .center,
                startRadiusFraction: 0,
                endRadiusFraction: 0.72
            )
            // A soft sheen across the top left.
            LinearGradient(
                colors: [.white.opacity(0.10), .clear],
                startPoint: .topLeading,
                endPoint: .center
            )
            .blendMode(.plusLighter)
        }
    }
}

/// Hot-stamped gold foil: a metallic gradient in the shape of [content],
/// pressed slightly into the leather (a dark lip above, light caught below).
private struct GoldFoil<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        // Sized by the content; the gradient only fills its shape.
        content().hidden().overlay(foil.mask(content()))
            .shadow(color: .black.opacity(0.55), radius: 0.6, x: 0, y: -0.6)
            .shadow(color: PassportPalette.goldHi.opacity(0.35), radius: 0.4, x: 0, y: 1)
    }

    private var foil: some View {
        LinearGradient(
            stops: [
                .init(color: PassportPalette.goldHi, location: 0),
                .init(color: PassportPalette.gold, location: 0.35),
                .init(color: PassportPalette.goldHi.opacity(0.95), location: 0.5),
                .init(color: PassportPalette.goldDeep, location: 0.8),
                .init(color: PassportPalette.gold, location: 1)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

/// The inside of the cover, seen as it turns: the leather's lining, a plain
/// deep cobalt with a faint woven texture.
struct PassportCoverLining: View {
    let width: CGFloat
    let height: CGFloat

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [PassportPalette.coverMid, PassportPalette.coverDeep],
                startPoint: .topTrailing,
                endPoint: .bottomLeading
            )
            Image(uiImage: LeatherGrain.weave)
                .resizable(resizingMode: .tile)
        }
        .frame(width: width, height: height)
    }
}

/// The cover's textures, each drawn once into a tile the views repeat.
enum LeatherGrain {
    /// Pebbled leather: small irregular bumps packed edge to edge, each
    /// shaded on its lower right and caught by the light on its upper left.
    static let tile: UIImage = {
        let side: CGFloat = 220
        let format = UIGraphicsImageRendererFormat()
        format.opaque = false
        return UIGraphicsImageRenderer(size: CGSize(width: side, height: side), format: format).image { context in
            let cg = context.cgContext
            var seed: UInt64 = 0x5DEECE66D
            func next() -> CGFloat {
                seed = seed &* 6364136223846793005 &+ 1442695040888963407
                return CGFloat(seed >> 33) / CGFloat(UInt32.max >> 1)
            }
            let shade = CGMutablePath()
            let light = CGMutablePath()
            for _ in 0..<Int(side * side / 5) {
                let x = next() * side
                let y = next() * side
                let w = 1.1 + next() * 1.6
                let rect = CGRect(x: x, y: y, width: w, height: w * (0.75 + next() * 0.4))
                // Bumps crossing an edge are drawn on the opposite one too, so
                // the tile repeats without seams.
                for dx in [0, -side, side] where rect.offsetBy(dx: dx, dy: 0).intersects(CGRect(x: 0, y: 0, width: side, height: side)) {
                    for dy in [0, -side, side] {
                        let r = rect.offsetBy(dx: dx, dy: dy)
                        guard r.intersects(CGRect(x: 0, y: 0, width: side, height: side)) else { continue }
                        shade.addEllipse(in: r.offsetBy(dx: 0.35, dy: 0.45))
                        light.addEllipse(in: r)
                    }
                }
            }
            cg.addPath(shade)
            cg.setFillColor(UIColor.black.withAlphaComponent(0.11).cgColor)
            cg.fillPath()
            cg.addPath(light)
            cg.setFillColor(UIColor.white.withAlphaComponent(0.025).cgColor)
            cg.fillPath()
        }
    }()

    /// The lining's weave: a fine square grid.
    static let weave: UIImage = {
        let side: CGFloat = 10
        return UIGraphicsImageRenderer(size: CGSize(width: side, height: side)).image { context in
            let cg = context.cgContext
            cg.setStrokeColor(UIColor.black.withAlphaComponent(0.07).cgColor)
            cg.setLineWidth(0.5)
            var step: CGFloat = 0
            while step < side {
                cg.move(to: CGPoint(x: step, y: 0)); cg.addLine(to: CGPoint(x: step, y: side))
                cg.move(to: CGPoint(x: 0, y: step)); cg.addLine(to: CGPoint(x: side, y: step))
                step += 2.5
            }
            cg.strokePath()
        }
    }()
}
