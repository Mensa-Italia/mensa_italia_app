import SwiftUI

/// The ink of each podium place, light enough to read on the card's blue:
/// gold, silver, bronze. Same values on Android.
func stampInk(_ place: Int) -> Color {
    switch place {
    case 1: Color(red: 0.949, green: 0.769, blue: 0.322)
    case 2: Color(red: 0.867, green: 0.890, blue: 0.925)
    default: Color(red: 0.890, green: 0.604, blue: 0.408)
    }
}

/// Where the stamp lands on the card's front: drawn once per launch, so it
/// sits still while the member looks and lands somewhere else next time, as
/// a real rubber stamp would. Fractions of the card, left or right of the
/// logo, clear of the edges and of the chevron.
private enum StampPlacement {
    private static let left = Bool.random()
    static let x = left ? CGFloat.random(in: 0.17...0.23) : CGFloat.random(in: 0.77...0.82)
    static let y = left ? CGFloat.random(in: 0.27...0.71) : CGFloat.random(in: 0.27...0.57)
    static let rotation = Double.random(in: -26...26)
}

/// The podium stamp on the front of the membership card, for the first three
/// of the ranking: pressed on at a random spot and angle, with a thump each
/// time that side comes into view. Sized as a share of the card.
struct PodiumStampOverlay: View {
    let place: Int
    /// The front is showing: the stamp comes down.
    let shown: Bool

    var body: some View {
        GeometryReader { geo in
            let side = geo.size.width * 0.31
            PodiumStamp(place: place)
                .frame(width: side, height: side)
                .rotationEffect(.degrees(StampPlacement.rotation))
                .scaleEffect(shown ? 1 : 1.7)
                .opacity(shown ? 1 : 0)
                .animation(shown ? .spring(response: 0.32, dampingFraction: 0.55).delay(0.25) : .none, value: shown)
                .position(x: geo.size.width * StampPlacement.x, y: geo.size.height * StampPlacement.y)
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

/// A round rubber stamp: two rings, the words around, the place in the middle.
struct PodiumStamp: View {
    let place: Int

    private var year: Int { Calendar.current.component(.year, from: Date()) }

    var body: some View {
        let ink = stampInk(place)
        let top = tr("leaderboard.stamp.top", fallback: "Classifica soci").uppercased()
        let bottom = tr("leaderboard.stamp.bottom", fallback: "Mensa Italia").uppercased()
        Canvas { context, size in
            let r = min(size.width, size.height) / 2
            let c = CGPoint(x: size.width / 2, y: size.height / 2)
            func circle(_ radius: CGFloat) -> Path {
                Path(ellipseIn: CGRect(x: c.x - radius, y: c.y - radius, width: radius * 2, height: radius * 2))
            }
            context.stroke(circle(r * 0.95), with: .color(ink), lineWidth: r * 0.075)
            context.stroke(circle(r * 0.66), with: .color(ink), lineWidth: r * 0.03)

            // The words around: over the top left to right, and under the
            // bottom left to right too, so neither reads upside down.
            let band = r * 0.80
            let font = Font.system(size: r * 0.17, weight: .bold)
            arc(top, over: true)
            arc(bottom, over: false)

            func arc(_ text: String, over: Bool) {
                let glyphs = text.map { context.resolve(Text(String($0)).font(font).foregroundColor(ink)) }
                let tracking = r * 0.17 * 0.12
                let widths = glyphs.map { $0.measure(in: CGSize(width: 1000, height: 1000)).width + tracking }
                let total = widths.reduce(0, +) - tracking
                // Angle swept per point of text along the band.
                var angle = -total / 2 / band
                for (glyph, width) in zip(glyphs, widths) {
                    let mid = angle + (width - tracking) / 2 / band
                    var g = context
                    g.translateBy(x: c.x, y: c.y)
                    if over {
                        g.rotate(by: .radians(Double(mid)))
                        g.draw(glyph, at: CGPoint(x: 0, y: -band), anchor: .center)
                    } else {
                        g.rotate(by: .radians(Double(-mid)))
                        g.draw(glyph, at: CGPoint(x: 0, y: band), anchor: .center)
                    }
                    angle += width / band
                }
            }

            // Stars between the two lines of words.
            for side in [-1.0, 1.0] {
                context.draw(Text("★").font(.system(size: r * 0.16)).foregroundColor(ink),
                             at: CGPoint(x: c.x + side * band, y: c.y), anchor: .center)
            }
            // The place, big, and the year under it.
            context.draw(Text("\(place)°").font(.system(size: r * 0.62, weight: .heavy)).foregroundColor(ink),
                         at: CGPoint(x: c.x + r * 0.05, y: c.y - r * 0.06), anchor: .center)
            context.draw(Text(String(year)).font(.system(size: r * 0.15, weight: .bold)).tracking(r * 0.03).foregroundColor(ink),
                         at: CGPoint(x: c.x, y: c.y + r * 0.40), anchor: .center)

            // Worn ink: spots where the rubber didn't touch, always the same for a place.
            var wear = SeededRandom(seed: UInt64(place * 7919))
            context.blendMode = .destinationOut
            for _ in 0..<170 {
                let a = wear.next() * 2 * .pi
                let d = sqrt(wear.next()) * r
                let dot = r * (0.006 + wear.next() * 0.022)
                let p = CGPoint(x: c.x + cos(a) * d, y: c.y + sin(a) * d)
                context.fill(Path(ellipseIn: CGRect(x: p.x - dot, y: p.y - dot, width: dot * 2, height: dot * 2)), with: .color(.black))
            }
            // A lighter streak, where the stamp was pressed unevenly.
            let start = wear.next() * 360
            var streak = Path()
            streak.move(to: c)
            streak.addArc(center: c, radius: r, startAngle: .degrees(Double(start)), endAngle: .degrees(Double(start) + 70), clockwise: false)
            streak.closeSubpath()
            context.fill(streak, with: .color(.black.opacity(0.35)))
        }
        .compositingGroup()
        .opacity(0.86)
    }
}

/// A small deterministic generator, so a place always wears the same way.
private struct SeededRandom {
    private var state: UInt64
    init(seed: UInt64) { state = seed &+ 0x9E3779B97F4A7C15 }
    mutating func next() -> CGFloat {
        state = state &* 6364136223846793005 &+ 1442695040888963407
        return CGFloat(state >> 11) / CGFloat(1 << 53)
    }
}
