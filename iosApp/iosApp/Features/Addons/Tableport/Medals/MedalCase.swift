import SwiftUI

// MARK: - The stage

/// The deep blue room the ranking and the medals are set in. Same values on
/// Android (`DarkStage`).
enum Stage {
    static let surface = Color(red: 0.055, green: 0.067, blue: 0.106)
    static let light = Color(red: 1.0, green: 0.95, blue: 0.82)
}

/// The stage's backdrop: deep blue going to black, with a warm wash of light
/// from the ceiling.
struct StageBackdrop: View {
    var body: some View {
        LinearGradient(
            colors: [Color(red: 0.086, green: 0.106, blue: 0.173),
                     Color(red: 0.043, green: 0.055, blue: 0.094),
                     Color(red: 0.020, green: 0.024, blue: 0.039)],
            startPoint: .top,
            endPoint: .bottom
        )
        .overlay(alignment: .top) {
            RadialGradient(colors: [Stage.light.opacity(0.08), .clear], center: .top, startRadius: 0, endRadius: 560)
        }
    }
}

extension View {
    /// A page that is always dark, whatever the app's appearance: dark
    /// system components, dark bars, the stage behind. Sheets it presents
    /// set `preferredColorScheme(.dark)` themselves.
    func darkStage() -> some View {
        self
            .scrollContentBackground(.hidden)
            .background(StageBackdrop().ignoresSafeArea())
            .environment(\.colorScheme, .dark)
            .toolbarColorScheme(.dark, for: .navigationBar, .tabBar)
    }
}

// MARK: - The case

/// A medal in its glass case, on a lacquered plinth: earned medals hang in a
/// light the colour of their metal, with a metal trim and their metal
/// engraved under the name; the others wait in the dark with how far along
/// they are. Mirrors Android `MedalCase`.
struct MedalCase: View {
    let medal: Medal

    private static let caseHeight: CGFloat = 172
    private var caseShape: UnevenRoundedRectangle {
        UnevenRoundedRectangle(topLeadingRadius: 22, bottomLeadingRadius: 4, bottomTrailingRadius: 4, topTrailingRadius: 22, style: .continuous)
    }

    var body: some View {
        let metal = medal.tier.metal
        VStack(spacing: 0) {
            ZStack {
                // What never moves is drawn once into a single image: the
                // back wall with its light, and the glass. Only the medal
                // between them moves.
                ZStack {
                    LinearGradient(colors: [Color(red: 0.10, green: 0.12, blue: 0.18), Color(red: 0.05, green: 0.063, blue: 0.094)],
                                   startPoint: .top, endPoint: .bottom)
                    CaseLight(tint: medal.isEarned ? metal.hi : nil)
                }
                .drawingGroup()
                FloatingMedal(medal: medal, size: 86)
                GlassFront(shape: caseShape)
                    .drawingGroup()
            }
            .frame(height: Self.caseHeight)
            .clipShape(caseShape)

            // The metal trim between case and plinth.
            LinearGradient(
                colors: medal.isEarned
                    ? [metal.lo, metal.hi, metal.lo]
                    : [Color(white: 0.19), Color(white: 0.36), Color(white: 0.19)],
                startPoint: .leading,
                endPoint: .trailing
            )
            .frame(height: 3)

            plinth
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(medal.title))
        .accessibilityValue(Text(medal.isEarned
            ? tr("medals.earned", fallback: "Ottenuta")
            : tr("medals.locked", fallback: "Non ancora ottenuta")))
    }

    private var plinth: some View {
        let metal = medal.tier.metal
        return VStack(spacing: 5) {
            Text(medal.title)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(medal.isEarned ? .white : .white.opacity(0.55))
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            if medal.isEarned {
                Text(medal.tier.name.uppercased())
                    .font(.caption2.weight(.bold))
                    .tracking(2)
                    .foregroundStyle(LinearGradient(colors: [metal.hi, metal.mid], startPoint: .leading, endPoint: .trailing))
            } else if !medal.isHidden, medal.next != nil {
                ProgressView(value: medal.progress)
                    .tint(metal.mid)
                    .padding(.vertical, 4)
            } else {
                // A past year's medal missed stays dark; a personal one left is a secret.
                Text((medal.region != nil
                      ? tr("medals.region.missed", fallback: "Non ottenuta")
                      : tr("medals.secret.short", fallback: "Segreta")).uppercased())
                    .font(.caption2.weight(.bold))
                    .tracking(2)
                    .foregroundStyle(.white.opacity(0.35))
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(colors: [Color(red: 0.17, green: 0.19, blue: 0.24), Color(red: 0.08, green: 0.09, blue: 0.125)],
                           startPoint: .top, endPoint: .bottom),
            in: UnevenRoundedRectangle(topLeadingRadius: 4, bottomLeadingRadius: 16, bottomTrailingRadius: 16, topTrailingRadius: 4, style: .continuous)
        )
        // Its shadow on the floor, drawn: a `.shadow` here was rendered
        // again off screen on every frame of a scroll.
        .background(alignment: .bottom) {
            Ellipse()
                .fill(RadialGradient(colors: [.black.opacity(0.45), .clear], center: .center, startRadius: 0, endRadius: 90))
                .frame(height: 28)
                .offset(y: 10)
        }
    }
}

/// The case's lighting: a cone from a small lamp in its roof and the pool it
/// leaves round the medal, in the metal's colour once earned; a faint grey
/// glow otherwise.
private struct CaseLight: View {
    let tint: Color?

    var body: some View {
        let color = tint ?? .white
        let strength = tint == nil ? 0.25 : 1.0
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            ZStack {
                Path { path in
                    path.move(to: CGPoint(x: w * 0.42, y: 0))
                    path.addLine(to: CGPoint(x: w * 0.58, y: 0))
                    path.addLine(to: CGPoint(x: w * 0.86, y: h))
                    path.addLine(to: CGPoint(x: w * 0.14, y: h))
                    path.closeSubpath()
                }
                .fill(LinearGradient(colors: [color.opacity(0.20 * strength), .clear], startPoint: .top, endPoint: .bottom))
                RadialGradient(colors: [color.opacity(0.22 * strength), .clear],
                               center: UnitPoint(x: 0.5, y: 0.48), startRadius: 0, endRadius: w * 0.55)
                // The lamp itself.
                RadialGradient(colors: [Stage.light.opacity(0.55 * strength), .clear],
                               center: .top, startRadius: 0, endRadius: w * 0.18)
            }
        }
        .allowsHitTesting(false)
    }
}

/// The medal hovering in its case, bobbing slowly, its shadow on the floor
/// shrinking as it rises. No two medals bob in step.
private struct FloatingMedal: View {
    let medal: Medal
    let size: CGFloat

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var up = false

    private var delay: Double { Double(medal.id.unicodeScalars.reduce(0) { $0 + Int($1.value) } % 23) / 10 }

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            // The medal and its shadow drawn once into a single image (room
            // left round it for the shadow): the bobbing then only moves it,
            // instead of redrawing a dozen layers and a shadow every frame.
            MedalArtwork(tier: medal.tier, symbol: medal.symbol, size: size, locked: !medal.isEarned)
                .shadow(color: .black.opacity(0.5), radius: 8, y: 6)
                .padding(16)
                .drawingGroup()
                .padding(-16)
                .offset(y: up ? -6 : 0)
            Spacer(minLength: 0)
            Ellipse()
                .fill(RadialGradient(colors: [.black.opacity(0.6), .clear], center: .center, startRadius: 0, endRadius: size * 0.4))
                .frame(width: size * 0.8, height: 12)
                .scaleEffect(x: up ? 0.92 : 1)
                .padding(.bottom, 6)
        }
        .onAppear {
            guard !reduceMotion else { return }
            withAnimation(.easeInOut(duration: 2.25).repeatForever(autoreverses: true).delay(delay)) {
                up = true
            }
        }
    }
}

/// The glass front: faintly tinted, lit along its edges, with reflections.
/// Drawn rather than Liquid Glass, which frosts the medal behind it.
private struct GlassFront: View {
    let shape: UnevenRoundedRectangle

    var body: some View {
        ZStack {
            LinearGradient(colors: [.white.opacity(0.09), .white.opacity(0.02), .white.opacity(0.05)],
                           startPoint: .top, endPoint: .bottom)
            // A broad streak and a thin one, across the pane.
            GeometryReader { geo in
                let w = geo.size.width
                let h = geo.size.height
                ZStack(alignment: .topLeading) {
                    LinearGradient(colors: [.clear, .white.opacity(0.13), .clear], startPoint: .leading, endPoint: .trailing)
                        .frame(width: w * 0.22, height: h * 1.6)
                        .offset(x: w * 0.18, y: -h * 0.3)
                    Color.white.opacity(0.08)
                        .frame(width: w * 0.035, height: h * 1.6)
                        .offset(x: w * 0.52, y: -h * 0.3)
                }
                .rotationEffect(.degrees(22))
            }
            // The roof pane, seen edge-on.
            VStack {
                Capsule().fill(.white.opacity(0.35)).frame(height: 1.5).padding(.horizontal, 16).padding(.top, 2)
                Spacer()
            }
        }
        .overlay(
            shape.strokeBorder(
                LinearGradient(colors: [.white.opacity(0.55), .white.opacity(0.16), .white.opacity(0.06)],
                               startPoint: .top, endPoint: .bottom),
                lineWidth: 1
            )
        )
        .allowsHitTesting(false)
    }
}
