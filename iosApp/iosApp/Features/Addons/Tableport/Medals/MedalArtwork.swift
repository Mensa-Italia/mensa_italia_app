import SwiftUI

/// A struck metal medal: brushed rim with a milled edge, a domed field and
/// the emblem embossed in it. `sheen` (−1…1 each way) moves the light across
/// it, as the medal turns.
struct MedalArtwork: View {
    let tier: Medal.Tier
    let symbol: String
    let size: CGFloat
    var locked = false
    var sheen: CGSize = .zero
    /// The reverse: engraved lettering instead of the emblem.
    var back: (title: String, caption: String)?

    var body: some View {
        let metal = tier.metal
        ZStack {
            // Rim, brushed: the light catches it in bands.
            Circle()
                .fill(AngularGradient(
                    colors: [metal.hi, metal.mid, metal.lo, metal.mid, metal.hi, metal.mid, metal.lo, metal.mid, metal.hi],
                    center: .center,
                    angle: .degrees(sheen.width * 50 + sheen.height * 20)
                ))
            // Milled edge.
            Circle()
                .strokeBorder(metal.lo.opacity(0.55), style: StrokeStyle(lineWidth: size * 0.035, dash: [1, 1.8]))
            // Domed field.
            Circle()
                .fill(RadialGradient(
                    colors: [metal.hi, metal.mid, metal.lo],
                    center: UnitPoint(x: 0.36 - sheen.width * 0.18, y: 0.30 - sheen.height * 0.18),
                    startRadius: 0,
                    endRadius: size * 0.62
                ))
                .padding(size * 0.09)
            // Bevel where the field meets the rim, and an inner border.
            Circle()
                .strokeBorder(
                    LinearGradient(colors: [metal.lo, metal.hi], startPoint: .topLeading, endPoint: .bottomTrailing),
                    lineWidth: max(1, size * 0.014)
                )
                .padding(size * 0.09)
            Circle()
                .strokeBorder(
                    LinearGradient(colors: [metal.hi.opacity(0.9), metal.lo.opacity(0.8)], startPoint: .topLeading, endPoint: .bottomTrailing),
                    lineWidth: max(0.6, size * 0.008)
                )
                .padding(size * 0.16)

            if let back {
                reverse(back, metal: metal)
            } else {
                Image(systemName: symbol)
                    .font(.system(size: size * 0.34, weight: .bold))
                    .foregroundStyle(LinearGradient(colors: [metal.mid, metal.lo], startPoint: .top, endPoint: .bottom))
                    // Embossed: lit on the upper left, shaded on the lower right.
                    .shadow(color: metal.hi, radius: 0, x: -size * 0.006, y: -size * 0.006)
                    .shadow(color: .black.opacity(0.35), radius: size * 0.004, x: size * 0.008, y: size * 0.008)
            }

            // Specular highlight, sliding with the light.
            Ellipse()
                .fill(RadialGradient(colors: [.white.opacity(0.6), .clear], center: .center, startRadius: 0, endRadius: size * 0.3))
                .frame(width: size * 0.6, height: size * 0.3)
                .rotationEffect(.degrees(-32))
                .offset(x: -size * 0.14 + sheen.width * size * 0.24, y: -size * 0.2 + sheen.height * size * 0.2)
                .blendMode(.screen)
                .allowsHitTesting(false)
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        // Not earned: a dull, unlit shape of what will be there.
        .saturation(locked ? 0 : 1)
        .brightness(locked ? -0.45 : 0)
        .opacity(locked ? 0.55 : 1)
    }

    private func reverse(_ back: (title: String, caption: String), metal: (hi: Color, mid: Color, lo: Color)) -> some View {
        VStack(spacing: size * 0.03) {
            Text("MENSA ITALIA")
                .font(.system(size: size * 0.065, weight: .heavy, design: .serif))
                .tracking(size * 0.012)
            Rectangle().frame(width: size * 0.3, height: max(0.5, size * 0.005))
            Text(back.title.uppercased())
                .font(.system(size: size * 0.085, weight: .bold, design: .serif))
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .minimumScaleFactor(0.6)
            Text(back.caption)
                .font(.system(size: size * 0.06, weight: .semibold, design: .serif))
        }
        .frame(width: size * 0.6)
        // Engraved: cut into the metal, lit on the lower edge.
        .foregroundStyle(metal.lo)
        .shadow(color: metal.hi.opacity(0.9), radius: 0, x: size * 0.004, y: size * 0.005)
    }
}

/// The medal button's icon, its colours cycling through the rainbow: it's
/// meant to be noticed. Still for Reduce Motion.
///
/// Toolbars only treat an `Image` as an icon (anything else they show as the
/// title, and iPhone Duo's vertical bar drops it), and repaint a template
/// symbol in their own colour. So the rainbow is pre-rendered into a cycle
/// of original-colour images, swapped a dozen times a second.
struct RainbowMedalIcon: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let frames = Self.frames
        TimelineView(.periodic(from: .now, by: 1.0 / 12)) { context in
            let index = reduceMotion
                ? 0
                : Int(context.date.timeIntervalSinceReferenceDate * 12) % max(frames.count, 1)
            if frames.indices.contains(index) {
                Image(uiImage: frames[index])
            } else {
                Image(systemName: "medal.fill")
            }
        }
    }

    @MainActor private static let frames: [UIImage] = (0..<30).compactMap { step in
        let renderer = ImageRenderer(content: RainbowMedalGlyph(angle: Double(step) * 12))
        renderer.scale = 3
        return renderer.uiImage?.withRenderingMode(.alwaysOriginal)
    }
}

/// One frame: the medal symbol filled with the rainbow, turned by `angle`.
private struct RainbowMedalGlyph: View {
    let angle: Double

    var body: some View {
        AngularGradient(
            colors: [.red, .orange, .yellow, .green, .cyan, .blue, .purple, .red],
            center: .center,
            angle: .degrees(angle)
        )
        .mask {
            Image(systemName: "medal.fill")
                .font(.system(size: 19, weight: .semibold))
        }
        .frame(width: 24, height: 24)
    }
}
